# Offline-First Audit — Repository Layer

Audit of every `*RepositoryImpl.kt` in `data/src/main/kotlin/iti/grad/nutriscan/data/repository/`
against the project's documented offline-first pattern (`data/AGENTS.md`): Room as source of
truth, remote fetch merged/written into Room, exposed as a `Flow` from the DAO, wrapped in
`runCatchingCancellable` + `@IoDispatcher`. `FoodLogRepositoryImpl` is the reference implementation.

## Offline-first (Room-backed, remote merged in) — no action needed

| Repository | Pattern |
|---|---|
| `FoodLogRepositoryImpl` | Reference impl — DAO `Flow` + remote merge |
| `AllergyRepositoryImpl` | `channelFlow` merges remote into `allergyDao` |
| `DiseaseRepositoryImpl` | Same pattern via `diseaseDao` |
| `DailyTrackingRepositoryImpl` | `dao` / `DailyTrackingApiService` merged via `channelFlow` |
| `FamilyMemberRepositoryImpl` | Reads/writes `userDao` (shares `UserEntity`) |
| `SavedScanRepositoryImpl` | Writes `savedScanDao` first (pendingSync), best-effort remote push |
| `NotificationHistoryRepositoryImpl` | Reads `dao` Flow directly (local-only feature) |
| `StreakRepositoryImpl` | `streakDao` + `dailyTrackingDao`/`foodLogDao` combine |
| `UserRepositoryImpl` | `userDao`/`streakDao` local cache with remote sync |
| `WorkoutRepositoryImpl` | `WorkoutLogDao` |
| `StepHistoryRepositoryImpl` | `DailyTrackingDao` (no separate step table) |

## Fixed (2026-08-16) — cache-fallback pattern, see `docs/plans/2026-08-16-offline-first-gaps.md`

### `ExercisesRepositoryImpl` — was already fine, audit was stale
Re-checked the source: `getExercises()`/`getExerciseById()`/`getCategories()` already write
through to `ExercisesDao` on a successful fetch and `.recoverCatching` back to the DAO on failure.
This audit's original claim ("no DAO read/write in the fetch path") no longer matches the code —
no work needed here.

### `ScanRepositoryImpl` — fixed
Added `ScannedProductEntity` (keyed by barcode) + `ScannedProductDao`. `getProductByBarcode()` now
writes a successful OpenFoodFacts lookup to Room and falls back to the cached row on failure — a
previously-scanned barcode resolves offline. `getRecentScans()`'s in-memory cache is unchanged
(works within a process lifetime; not persisted across restarts — lower priority, not the "blank
screen offline" case).

### `NewsRepositoryImpl` — fixed
Added `NewsArticleEntity` (keyed by `url` + `feedKey`, so the headlines feed and each search never
collide) + `NewsDao`. Headlines and search both cache-write on success and cache-read on failure,
same shape as `ExercisesRepositoryImpl`.

Room version bumped 17 → 18 (`fallbackToDestructiveMigration()`, no manual migration needed — see
`NutriScanDatabase` kdoc). Tests: `NewsRepositoryImplTest.kt` (new),
`ScanRepositoryImplTest.kt` (extended) cover cache-write-on-success and cache-fallback-on-failure
for both.

## Open — needs product confirmation before building

### `NutriGptRepositoryImpl`
- `NutriGptApiService` only. Chat history lives in an in-memory `MutableStateFlow` — no persistence
  entity at all, so history is lost on app relaunch, not just offline.
- **Work**: add a chat-message entity/DAO if persistence across relaunches is wanted; otherwise this
  may be an accepted product decision (confirm before building — cheapest fix might be "no fix").

## Not gaps — not Room-backed by design, no action needed

- `AuthRepositoryImpl` — tokens, network-only by design.
- `OnboardingRepositoryImpl`, `LanguageRepositoryImpl`, `ThemeRepositoryImpl`,
  `NotificationRepositoryImpl` (prefs) — DataStore-backed, not Room, not network-dependent.
- `StepsRepositoryImpl` — sensor-derived, in-memory today's count. Step *history* is already
  covered separately via `StepHistoryRepositoryImpl` / `DailyTrackingDao`.

## Suggested priority

1. `ExercisesRepositoryImpl` — schema already exists, smallest diff.
2. `ScanRepositoryImpl` — highest user impact (core flow), needs new schema.
3. `NewsRepositoryImpl` — needs new schema, lower priority (non-critical feature).
4. `NutriGptRepositoryImpl` — confirm product intent before building (persistence may not be wanted).
