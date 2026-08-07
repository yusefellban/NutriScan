# Plan: Daily Tracking Backend Sync (Room + 8-endpoint API)

**Status: Design approved, ready for implementation.**

**Scope note:** this is subsystem 1 of a larger request. The full request was
decomposed (with the user, during brainstorming) into five independent
pieces, each with its own plan/implementation cycle:

1. **Daily Tracking backend sync** — this document.
2. Exercise local persistence (Room, no backend field exists yet).
3. Calories screen TDEE/BMI (scrollable card).
4. ~~Main/Home screen widget~~ — **dropped**, user decided Home stays
   untouched ("not related to my calories screen, no need").
5. Daily Tracking History screen (view-only, swipe between past days).

Subsystem 1 is the foundation the others (2, 3, 5) build on, so it goes
first.

## 1. Context

The backend's Daily Tracking controller
(`https://nutriscan.dev/swagger-ui/index.html#/`, OpenAPI at
`/v3/api-docs`) exposes 8 endpoints under `/api/v1/daily-tracking`:

| Method | Path | Purpose |
|---|---|---|
| GET | `/daily-tracking/today` | Today's record |
| GET | `/daily-tracking/{date}` | A specific day's record |
| GET | `/daily-tracking` | Paginated history (`page`, `size`) |
| PATCH | `/daily-tracking/{date}` | Update `targetWaterCnt`/`waterCnt`/`stepsCnt` |
| DELETE | `/daily-tracking/{date}` | Delete a whole day's record |
| POST | `/daily-tracking/{date}/meals` | Add a meal (`scanId`, `mealCnt`) |
| PUT | `/daily-tracking/{date}/meals/{scanId}` | Update a meal's `mealCnt` |
| DELETE | `/daily-tracking/{date}/meals/{scanId}` | Remove a meal |

Critically, `DailyTrackingRequest`/`Response` only has `date`,
`targetWaterCnt`, `waterCnt`, `stepsCnt`, `meals[]` — **no exercise or
calories-burned field exists anywhere in the schema.** Exercise data stays
local-only (subsystem 2) until backend adds support.

Before this change:
- **Meals** (`FoodLogEntry`/`FoodLogEntity`) are already real, Room-backed,
  per-user, offline-first (see `2026-07-22-food-log-persistence.md`) — but
  never pushed to the backend.
- **Steps** are already real (device sensor via `StepsRepositoryImpl`,
  baseline persisted in `SharedPreferences`) — also never pushed.
- **Water** is pure in-memory `CaloriesState` — resets on app restart, not
  per-user, no persistence at all.
- `stepsGoal` (10000) is hardcoded, not persisted/configurable.

### Confirmed scope decisions (asked directly of the user before implementation)

- **Meal sync is live, offline-first**: Room write happens first (instant,
  works offline); the matching backend call fires immediately after,
  best-effort. Failure doesn't block or fail the user-visible action — it
  just marks the row for retry.
- **Steps + water sync together**, batched once a day (not live) — one
  `PATCH` call per day carries both, since backend already supports both
  fields.
- **Push timing**: end of day = **midnight Africa/Cairo**, via a WorkManager
  job (not "only when app is opened").
- **Day boundary is Cairo-anchored for everyone**, regardless of device
  timezone — this replaces the existing device-local `LocalDate.now()` calls
  in `StepsRepositoryImpl` and `FoodLogRepositoryImpl`.
- **Calories burned for steps** is client-computed (no backend field, not a
  sensor value) using a **weight-based formula**:
  `kcal ≈ steps × weightKg × 0.0005`, using `weightKg` from the user's
  already-fetched profile (`IUserRepository`).
- **Startup/login reconciliation**: `GET /daily-tracking/today` is called on
  login/cold app start; if Room has no row for today yet, it's seeded from
  the response (water/steps + any meals not already present locally).
- **GET history-list and DELETE-day** get real repository methods now but no
  dedicated UI consumer in this subsystem — DELETE-day gets a consumer in
  subsystem 5 (History screen); GET-list backs subsystem 5's day-gap
  fallback fetch.

## 2. Feature Summary

Introduce a `domain/dailytracking` + `data` layer that wires all 8 endpoints
to a Room-backed, offline-first local store. Existing `FoodLogRepositoryImpl`
gains best-effort backend sync side effects on add/remove. A new
`DailyTrackingEntity` (one row per user+date, permanent — never deleted)
becomes the local source of truth for water/steps/sync-state, read by the
Calories screen (replacing hardcoded/in-memory values) and later by History
(subsystem 5). A new Cairo-anchored date utility replaces scattered
device-local `LocalDate.now()` calls. A WorkManager job pushes the previous
day's unsynced data at Cairo midnight.

## 3. Files Created

**Domain**
- `domain/dailytracking/model/DailyTracking.kt` — `date: LocalDate,
  targetWaterCnt: Int, waterCnt: Int, stepsCnt: Int, caloriesBurnedSteps:
  Int, syncedToBackend: Boolean`.
- `domain/dailytracking/model/DailyTrackingSummary.kt` — lighter shape for
  the paginated history list (`id, date, targetWaterCnt, waterCnt,
  stepsCnt, mealCount`), mirrors `DailyTrackingSummaryResponse`.
- `domain/dailytracking/repository/IDailyTrackingRepository.kt`:
  ```kotlin
  interface IDailyTrackingRepository {
      fun observeToday(): Flow<DailyTracking>
      suspend fun getByDate(date: LocalDate): Result<DailyTracking>
      suspend fun getHistoryPage(page: Int, size: Int): Result<List<DailyTrackingSummary>>
      suspend fun updateWaterCnt(waterCnt: Int): Result<Unit>
      suspend fun updateTargetWaterCnt(targetWaterCnt: Int): Result<Unit>
      suspend fun updateStepsCnt(stepsCnt: Int): Result<Unit>
      suspend fun deleteDay(date: LocalDate): Result<Unit>
      suspend fun pushMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit>
      suspend fun updateMeal(date: LocalDate, scanId: String, mealCnt: Int): Result<Unit>
      suspend fun deleteMeal(date: LocalDate, scanId: String): Result<Unit>
      suspend fun syncPendingDay(date: LocalDate): Result<Unit> // nightly job entry point
  }
  ```
- `domain/dailytracking/usecase/` — one class per method above, `operator
  fun invoke`, matching `GetExercisesUseCase` convention (only the ones
  actually consumed by a ViewModel get created in this phase; `deleteDay`'s
  use case is added in subsystem 5, not here).
- `domain/common/CairoDateProvider.kt` — pure Kotlin, no Android deps:
  `fun today(): LocalDate` using `Clock.system(ZoneId.of("Africa/Cairo"))`.
  No interface/DI needed — it's a stateless pure function, not something
  ever faked differently in tests (tests pass explicit `LocalDate`s instead
  of calling it).

**Data**
- `data/remote/dto/DailyTrackingDto.kt` — mirrors swagger exactly:
  `DailyTrackingRequestDto`, `DailyTrackingResponseDto`,
  `DailyTrackingMealRequestDto`, `UpdateMealRequestDto`,
  `DailyTrackingMealResponseDto`, `DailyTrackingSummaryResponseDto`,
  `PageDailyTrackingSummaryResponseDto`.
- `data/remote/api/DailyTrackingApiService.kt` — 8 Retrofit methods, 1:1
  with the endpoint table in §1.
- `data/db/entity/DailyTrackingEntity.kt` — `@Entity(tableName =
  "daily_tracking", primaryKeys = ["userId", "date"])`: `userId, date
  (String ISO), targetWaterCnt, waterCnt, stepsCnt, caloriesBurnedSteps,
  syncedToBackend: Boolean`. Never deleted by app logic (permanent history
  for subsystem 5) except via the explicit `deleteDay` flow.
- `data/db/dao/DailyTrackingDao.kt` — `observeByUserAndDate(userId, date):
  Flow<DailyTrackingEntity?>`, `getByUserAndDate(userId, date):
  DailyTrackingEntity?`, `upsert(entity)`, `getUnsyncedForDate(userId,
  date): DailyTrackingEntity?`, `markSynced(userId, date)`,
  `deleteByUserAndDate(userId, date)`.
- `data/repository/DailyTrackingRepositoryImpl.kt` — Room is source of truth
  for reads; `runCatchingCancellable` + `@IoDispatcher`, same shape as
  `FoodLogRepositoryImpl`. Water/steps writes update Room only
  (`syncedToBackend = false`); `syncPendingDay(date)` is what actually calls
  `PATCH` and flips the flag. Meal methods (`pushMeal`/`updateMeal`/
  `deleteMeal`) call the API directly — no local meal state of their own
  (that stays `FoodLogEntity`'s job); a failure here is what
  `FoodLogRepositoryImpl` catches to set `pendingSync = true`.
- `data/repository/mapper/DailyTrackingMapper.kt` — DTO ⇄ entity ⇄ domain
  conversions.

**App**
- `app/work/DailyTrackingSyncWorker.kt` — `CoroutineWorker`: resolves
  "yesterday" via `CairoDateProvider`, calls
  `IDailyTrackingRepository.syncPendingDay(yesterday)`, then separately
  queries `FoodLogDao` for any `pendingSync = true` rows (any date) and
  retries their push/delete via the repository's meal methods.
- `app/work/DailyTrackingSyncScheduler.kt` — schedules the periodic
  WorkManager request anchored to the next Cairo midnight; called once from
  `Application.onCreate()`.

**Tests**
- `data/src/test/kotlin/.../repository/DailyTrackingRepositoryImplTest.kt`
- `domain/src/test/kotlin/.../CairoDateProviderTest.kt`
- `app/src/test/kotlin/.../work/DailyTrackingSyncWorkerTest.kt` (Robolectric
  + WorkManager `TestListenableWorkerBuilder`)

## 4. Files Modified

- `data/db/entity/FoodLogEntity.kt` — add `pendingSync: Boolean = false`.
- `data/db/Migrations.kt` — new migration adding that column.
- `data/repository/FoodLogRepositoryImpl.kt` — inject
  `IDailyTrackingRepository`; `addFoodEntry`/`removeFoodEntry` call
  `pushMeal`/`deleteMeal` after the local Room write, catching failure into
  `pendingSync = true` rather than failing the operation.
- `data/repository/StepsRepositoryImpl.kt` — replace `LocalDate.now()` with
  `CairoDateProvider.today()` for baseline day-rollover checks.
- `data/repository/mapper/FoodLogMapper.kt` — `today()` uses
  `CairoDateProvider` instead of `LocalDate.now()`.
- `data/db/NutriScanDatabase.kt` — add `DailyTrackingEntity`, bump version,
  `abstract fun dailyTrackingDao(): DailyTrackingDao`.
- `app/di/NetworkModule.kt` — `DailyTrackingApiService` provider (reuses the
  existing authenticated Retrofit client, since this is a real
  authenticated backend endpoint, not a public dataset like Exercises).
- `app/di/RepositoryModule.kt` — bind `IDailyTrackingRepository`.
- `app/di/DatabaseModule.kt` — provide `DailyTrackingDao`.
- `app/NutriScanApplication.kt` (or wherever `Application.onCreate()`
  lives) — call `DailyTrackingSyncScheduler.schedule()`.
- `main/calories/viewmodel/CaloriesViewModel.kt` — inject
  `IDailyTrackingRepository`; `waterConsumed`/`waterGoal`/`steps` now come
  from `observeToday()` instead of in-memory mutation; water cup
  tap/long-press call `updateWaterCnt`/`updateTargetWaterCnt` instead of
  mutating local state directly.
- `main/calories/state/CaloriesState.kt` — `waterConsumed`/`waterGoal`
  defaults become `0`/whatever the entity default is (no longer hardcoded
  `4`/`8`); `stepsGoal` becomes persisted (stored in `DailyTrackingEntity`
  or a small preferences entry — reusing `IStepsPreferencesDataSource` for
  the goal specifically, since it's a user setting, not a daily metric).
- On login (`AuthRepositoryImpl` or wherever the post-login flow lives) —
  trigger the one-time `GET /daily-tracking/today` reconciliation described
  in §1.

## 5. Layer Breakdown

### Domain
- New models: `DailyTracking`, `DailyTrackingSummary`.
- New repository interface: `IDailyTrackingRepository`.
- New use cases: one per repository method actually consumed this phase.
- New pure utility: `CairoDateProvider`.

### Data
- New entity/DAO: `DailyTrackingEntity`/`DailyTrackingDao`.
- New DTOs/API service: `DailyTrackingDto.kt`/`DailyTrackingApiService`.
- New repository impl: `DailyTrackingRepositoryImpl`.
- Modified: `FoodLogRepositoryImpl` (sync side effects), `StepsRepositoryImpl`
  + `FoodLogMapper` (Cairo date), `FoodLogEntity` (+`pendingSync` column).

### Presentation
- `CaloriesViewModel`/`CaloriesState` — water/steps become
  Room-Flow-backed instead of in-memory.

### App
- New WorkManager job + scheduler.
- DI wiring (additive only).

## 6. Testing Plan

- `DailyTrackingRepositoryImplTest` — local-write-then-sync happy path;
  sync failure leaves `syncedToBackend = false`; null `getCurrentUserId()`
  short-circuits (empty/failure, matching `FoodLogRepositoryImpl`
  convention); `observeToday()` seeds Room only when no row exists yet.
- `FoodLogRepositoryImplTest` (extended) — add/remove now also invokes the
  (faked) daily-tracking push/delete; failure sets `pendingSync = true`
  without failing the local add/remove.
- `DailyTrackingSyncWorkerTest` — unsynced row → API called → marked
  synced; API failure → stays unsynced, worker returns `Result.retry()`.
- `CairoDateProviderTest` — fixed-instant cases straddling the Cairo UTC
  offset boundary (a UTC time that's still "yesterday" in Cairo and vice
  versa).
- `CaloriesViewModelTest` (updated) — water/steps assertions now go through
  the fake `IDailyTrackingRepository` flow instead of asserting on
  in-memory state mutation directly.

## 7. Edge Cases

- Offline entirely — every write lands in Room regardless; backend calls
  fail silently, flagged for retry; no error dialog for a background sync
  failure.
- Nightly job deferred by Doze/no connectivity — WorkManager retries at the
  next eligible window; data is delayed, never lost.
- App killed mid meal-add before the push call returns — `pendingSync` was
  already true before the call started (optimistic), so worst case is one
  extra harmless retry later.
- Two devices update water same day before either syncs — last `PATCH` to
  land wins; no merge logic. Known, accepted limitation — not solved here.
- `getCurrentUserId()` null (logged out/bad token) — mirrors
  `FoodLogRepositoryImpl`: empty reads, `Result.failure` writes, worker
  skips a session with no valid user.
- Meal delete backend-sync fails, then app is reinstalled before retry
  succeeds — local tombstone is lost with the reinstall; the meal
  reappears on the next `GET today` pull since backend never received the
  delete. Rare (reinstall + exact timing), accepted.

## 8. Definition of Done

- [ ] All files in §3/§4 created/modified as described.
- [ ] `./gradlew test` green, including all new test files in §6.
- [ ] `./gradlew :app:assembleDebug` compiles (DI graph wired).
- [ ] No hardcoded strings/colors introduced.
- [ ] Manual verification: add/remove food while online → confirm it shows
  up via `GET /daily-tracking/today` in a REST client; add food while in
  airplane mode → confirm it still appears locally instantly, then re-enable
  network and confirm the pending push succeeds on next app open or the
  nightly job; toggle water cups, force the WorkManager job (via `adb shell
  cmd jobscheduler` or a debug trigger button) and confirm the backend
  record updates; kill and reopen the app to confirm the login-time
  `GET today` reconciliation doesn't duplicate meals already present
  locally.
