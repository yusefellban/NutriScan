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

## Gaps — network-only, blank/error screen with no connectivity

### 1. `ExercisesRepositoryImpl` — cheapest fix, table already exists
- `ExercisesDao` / `ExerciseEntity` / `ExerciseCategoryEntity` already exist in the Room schema.
- But `getExercises()`, `getExerciseById()`, `getCategories()` call `api.*` directly — no DAO
  read/write in the fetch path. The schema is dead weight until this is wired up.
- **Work**: follow `FoodLogRepositoryImpl`'s shape — write remote results into `ExercisesDao` on
  fetch, read from the DAO `Flow` as the source of truth, merge like the other repos above.

### 2. `ScanRepositoryImpl` — biggest gap, core flow
- Pure `OpenFoodFactsApiService` / `ScanApiService` calls. No DAO/entity for scan *lookups* at all
  (only `SavedScanEntity` exists, and that's for user-saved favorites, not general scan results).
- Barcode/product scanning is fully network-only today — no connectivity means a blank/error result
  for the app's core feature.
- **Work**: needs a new entity/DAO for scanned-product results (at minimum cache-by-barcode), then
  the same read-DAO-first-then-refresh pattern.

### 3. `NewsRepositoryImpl`
- `NewsApiService` only, no `NewsEntity`/DAO anywhere in `db/entity` or `db/dao`.
- Offline = empty headlines/search.
- **Work**: add `NewsEntity` + DAO, cache last-fetched articles, merge like `FoodLogRepositoryImpl`.

### 4. `NutriGptRepositoryImpl`
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
