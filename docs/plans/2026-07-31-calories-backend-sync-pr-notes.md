# Wire Calories/daily-tracking to the backend, fix cross-account data leaks

## Summary

The Calories screen now talks to all 8 `daily-tracking` endpoints for real,
survives app reinstall, and syncs across devices on the same account. Along
the way, three separate cross-account data leaks were found and fixed (recent
scans, streak, and the `local_device_user` fallback bucket) — all stemming
from the same root cause: login never requested the `openid` scope, so no ID
token was issued and `getCurrentUserId()` silently returned `null`.

12 commits, grouped by phase. Each is independently buildable and reviewable.

---

## 1. `2ed9c19` — Track a real per-product meal count in the food log

**Problem:** the food log stored one row per "add" tap. Adding the same
product four times created four identical rows with no way to represent
"this meal now has a count of 4" to the backend.

**Change:**
- `FoodLogEntity` gains `mealCnt: Int` and `backendCreated: Boolean`.
- `FoodLogDao` gains `getByUserProductAndDate`, `updateMealCnt`,
  `markBackendCreated`.
- Room schema bump (`fallbackToDestructiveMigration`, per project
  convention — no migration path needed pre-release).

**Why `backendCreated` matters:** a pending-sync row that has never reached
the server needs a `POST`; one that already exists there needs a `PUT`. Without
this flag there's no way to tell the two apart once a row falls behind.

---

## 2. `b50ba84` — Increment and decrement mealCnt instead of duplicating rows

**Problem:** the `PUT /daily-tracking/{date}/meals/{scanId}` endpoint existed
in the API service but nothing ever called it.

**Change:**
- `addFoodEntry`: looks up an existing row for `(userId, productId, date)`.
  Found → increment `mealCnt`, call `updateMeal` (PUT). Not found → insert,
  call `pushMeal` (POST).
- `removeFoodEntry`: computes `newCnt = existing.mealCnt - 1`. `> 0` → PUT the
  decrement. `== 0` → soft-delete locally, DELETE remotely.
- This is the literal implementation of the requested workaround: *"make the
  count of them 1 using the edit meals count and remove 1 item... like a
  workaround to make meals -1 each swipe to delete."*
- `resolveUserId()` now **fails** instead of falling back to a shared
  `local_device_user` bucket — that fallback was the mechanism of one of the
  three cross-account leaks (see commit 6).

**Tests:** rewrote 3 `FoodLogRepositoryImplTest` cases that had asserted the
old shared-bucket behavior (they were encoding the bug); added coverage for
increment/decrement/zero-out transitions. Added a scoped `stubAndroidLog()`
helper rather than a module-wide `isReturnDefaultValues` flag, which was tried
first and broke an unrelated test relying on `android.graphics` throwing.

---

## 3. `fb45d04` — Fix the daily-tracking endpoints and stop sparse responses failing to parse

**Problem 1:** every path in `DailyTrackingApiService` had its own `api/`
prefix (`api/v1/daily-tracking/...`), but the Retrofit base URL already ends
in `/api/`. Every request hit `/api/api/v1/...` and the server answered `500`
— which looked like a backend bug for a while, since a bad-token 401 was ruled
out first (a working `GET /scans` call on the same client seconds earlier
proved the token/interceptor was fine).

**Problem 2:** the user asked for this explicitly — *"i want the whole
dailytrackingdto to be nullable to make sure all works perfectly."* Every
field across `DailyTrackingResponseDto` and its nested DTOs is now nullable,
so a response missing e.g. `date` or `scanId` no longer fails to deserialize
the entire payload.

---

## 4. `14469e5` — Push changes promptly without spamming the API, and never lose steps

**Problem:** water/steps/target changes sat in Room until the 6-hour sync
worker ran. A second device on the same account could show stale data for
most of a day.

**Change:**
- Every water/steps/target write now fires a debounced push
  (`PUSH_DEBOUNCE_MS = 2_000L`) on the repository's own scope, so 8 rapid
  water-cup taps collapse into a single `PATCH` instead of 8.
- Idle-poll interval (`RECONCILE_INTERVAL_MS`) raised from 20s to 60s —
  180 req/hr → 60 req/hr at idle.
- **Steps monotonic guard, write side:** `if (stepsCnt <= current.stepsCnt)
  return` — a lower reading (device-local step sensor reset, or opening the
  same account on a second phone) can no longer overwrite a higher one.
- **Steps monotonic guard, read side:** on pull, `stepsCnt =
  maxOf(snapshot.stepsCnt, existing.stepsCnt)`; if the local value won, the
  row is left `syncedToBackend = false` so the higher value still gets pushed
  rather than stranded on-device.
- `syncAllPendingDays()` flushes **every** day still owing a push, not just
  today — a day that ended unsynced (logged late at night, or offline) used to
  never retry, leaving a permanent hole in history.

**Direct user report this fixes:** *"the steps glitches at zero while it is
39 at back... turned the phone too and backend became 0 this isn't good it
should cont on 39."*

---

## 5. `7888955` — Seed the backend's meal counts on login and app start

**Problem:** reinstalling the app (or a fresh login) left the food log empty
even though the backend still had the day's meals recorded.

**Change:** `ReconcileTodayUseCase` now reads `GET /daily-tracking/today` and
seeds each meal with the `mealCnt` the server reports, instead of assuming a
count of 1 per meal (or nothing at all).

---

## 6. `e9ebcdc` — Fall back to the access token when there is no ID token

**Root cause of the cross-account leaks.** Keycloak only issues an `id_token`
when the login request includes `scope=openid` — this app's login request
didn't. `getCurrentUserId()` returned `null` for every signed-in user, which
routed all of them into the same `local_device_user` fallback bucket
throughout the data layer.

**Change:** `getCurrentUserId()` now falls back to decoding the access token
if the ID token is absent. Both tokens carry the same `sub` claim, so this is
a correct fix, not a workaround — confirmed by checking `saveTokens`, which
only overwrites the stored ID token when a new one is actually present.

**Also included:** repaired 2 unrelated test call-signature drifts
(`RegisterUseCaseTest`, `AuthRepositoryImplTest`) and a `FamilyMemberRepositoryImplTest`
fake DAO whose `insertOrUpdateUser` stub captured the write without
propagating it to the `Flow` the repository re-reads immediately after.

---

## 7. `b602bda` — Give each account its own streak

**Problem:** `StreakEntity` was `@PrimaryKey val id: Int = 0` — a single row,
shared by every account that ever signed into the device.

**Change:** re-keyed to `@PrimaryKey val userId: String`.

---

## 8. `1fb18b5` — Scope the recent-scans cache to the account that fetched it

**Problem (reported directly by the user):** *"all accounts shares recent
scans while each one should have its own."* `ScanRepositoryImpl.localRecentScans`
is a plain field on a Hilt singleton. `database.clearAllTables()` on logout
clears Room but does nothing to an in-memory field — the next account to sign
in was served the previous account's scans from memory, with **no network
request at all**, until the process happened to die (which is why restarting
the app appeared to "fix" it).

**Change:** added `cachedScansUserId` tracking. The cache is dropped whenever
a different `userId` asks for recent scans. `getScanResult` (which appends a
newly-completed scan into the cache) is also guarded, so a scan finishing
right after an account switch can't land in the previous account's list.

**Tests:** new `ScanRepositoryImplTest` — verified against real backend data
during this session (`GET /scans` returning per-account results correctly).

---

## 9. `4f6ceaf` — Show the real meal count on the food log card

**Change:** `CaloriesViewModel.observeFoodLog()` no longer groups duplicate
rows to fake a quantity badge — it reads `mealCnt` directly, and
`caloriesGained = entries.sumOf { it.calories * it.mealCnt }` replaces the old
row-count-based total.

---

## 10. `f555a80` — Show every macro the backend sends on meal details

**Problem 1:** the screen rendered calories, sugar, and fat only.
`proteinGrams`, `carbsGrams`, `fiberGrams`, `sodiumMg` were already on the
domain model and in every API response — just never wired to the UI.
`saturatedFat` is removed; the backend has no such field, and it was
hardcoded to `"0"`.

**Problem 2 (found while fixing problem 1):** bookmarking a product zeroed
every macro except sugar/fat on the round-trip back into Room.

**Problem 3 (the one actually reported — screenshot showed `0 g` across every
macro despite Postman returning real values):** `ProductDetailsViewModel`
short-circuited on the local Room row whenever one existed. That row comes
from the backend's **favorites-list summary** endpoint, which returns
`calories` only — every other macro is written as `0f` by design (see comment
in `SavedScanRepositoryImpl.toEntity`). The full breakdown only exists on
`GET /scans/{id}`. The view model now always fetches that and falls back to
Room only when offline; the saved row still decides the bookmark flag.

**Also:** `55.00` now renders as `55`, not `55.0` — new strings added in
English and Arabic for protein/carbs/fiber/sodium.

---

## 11. `3929a78` — Build the step history from real days instead of numbers off a screenshot

**Problem:** `StepHistoryRepositoryImpl` returned four hardcoded
`StepHistorySummary` objects per period, with a comment reading `// From
screenshots`.

**Change:**
- New `DailyTrackingDao.getRange()` query.
- History is built from `GET /daily-tracking` (covers days recorded on other
  devices) merged with local Room rows — local wins, since today's steps are
  sensor-counted and only pushed ~2s later (the debounce from commit 4).
- Bucketed per period: 7 daily buckets (week), 4 weekly buckets (month), or
  monthly buckets (3/6 months).
- Distance = `steps × (heightCm × 0.415 / 100) / 1000` (stride-length
  estimate from height). Active minutes = `steps / 100` (average cadence).
  Both per explicit user direction: *"derive distance and time from steps +
  profile height."*

---

## 12. `5713c0c` — Add pull-to-refresh with a shimmer placeholder to the main screens

**Change:**
- One reusable `PullToRefreshShimmerBox` wraps Home, Calories, Saved, and
  Profile. It swaps content for a shimmer skeleton and **holds the skeleton
  for a fixed 2 seconds** — explicit user choice, made after I flagged that it
  deliberately makes a fast cached refresh feel slower than it is.
- Each screen has its own skeleton shape (`ScreenShimmers.kt`) mirroring its
  real layout — a single generic list placeholder looked wrong on every
  screen except the one it was originally modeled after.
- `Saved` had no refresh path at all: `ISavedScanRepository.refresh()` +
  `RefreshSavedScansUseCase` added — flushes pending saves, then re-pulls
  favorites immediately instead of waiting out the background poll interval.
  `Home` re-pulls recent scans and today's tracking; `Profile` re-syncs the
  profile.

**Bugs found and fixed during this same commit's development (not separate
commits — squashed in before push):**
- Shimmer never disappeared: `LaunchedEffect(isRefreshing)` re-keyed and
  cancelled the in-flight `delay()` the instant the ViewModel cleared
  `isRefreshing` (~200ms in), so `showShimmer = false` never ran.
- Fixed attempt #1 (`snapshotFlow` keyed on `Unit`) then never showed the
  shimmer at all: `snapshotFlow` only observes snapshot `State`, and
  `isRefreshing` was a plain `Boolean` parameter captured once at first
  composition. Final fix wraps it in `rememberUpdatedState` so the flow has
  real `State` to observe.

---

## Known follow-ups (deliberately out of scope for this PR)

- **Exercise kcal/minutes still local-only.** `DailyTrackingRequestDto` has no
  exercise fields on the backend yet, so `addExerciseWorkout` never marks its
  row unsynced and will not survive a reinstall. Needs backend field names.
- **`GET /daily-tracking/{date}` and `GET /daily-tracking` (history) are wired
  but unused** — built ahead of the calories-history screen, which was
  explicitly deferred (YAGNI) during planning.
- **`daily-tracking/today` returns full `nutritionFacts` per meal** and
  nothing reads it yet — the food log only stores calories. Wiring that up
  would let meal details work fully offline with real macros instead of
  requiring a live `GET /scans/{id}` call.
