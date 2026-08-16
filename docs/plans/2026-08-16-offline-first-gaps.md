# Offline-first gaps — close network-only repositories

## Why
`docs/offline-first-audit.md` found 4 repositories that don't follow the
project's documented offline-first pattern (`data/AGENTS.md`): Room as
source of truth, remote merged in, exposed as DAO `Flow`, wrapped in
`runCatchingCancellable` + `@IoDispatcher`. Reference shape:
`FoodLogRepositoryImpl`.

## Scope — 4 repositories, in priority order

### Phase 1 — `ExercisesRepositoryImpl` (smallest diff)
`ExercisesDao` / `ExerciseEntity` / `ExerciseCategoryEntity` already exist
in the Room schema but sit unused — `getExercises()`, `getExerciseById()`,
`getCategories()` call `api.*` directly with no DAO read/write.
- Write remote results into `ExercisesDao` on fetch.
- Read from the DAO `Flow` as source of truth (`channelFlow` merge, same
  shape as `AllergyRepositoryImpl`/`DiseaseRepositoryImpl`).
- No new entity/migration needed — schema is already there.

### Phase 2 — `ScanRepositoryImpl` (highest impact, core flow)
Barcode/product scanning is fully network-only (`OpenFoodFactsApiService`
/ `ScanApiService`). No DAO/entity for scan *lookups* exists —
`SavedScanEntity` only covers user-saved favorites, not general results.
Offline today = blank/error on the app's core feature.
- Add new entity + DAO for scanned-product results, cached at minimum by
  barcode.
- Room migration required (new table).
- Read-DAO-first-then-refresh, same shape as reference impl.

### Phase 3 — `NewsRepositoryImpl`
`NewsApiService` only, no `NewsEntity`/DAO. Offline = empty
headlines/search.
- Add `NewsEntity` + DAO, cache last-fetched articles.
- Room migration required (new table).
- Merge like `FoodLogRepositoryImpl`.

### Phase 4 — `NutriGptRepositoryImpl` (confirm intent first)
Chat history lives in an in-memory `MutableStateFlow` only — lost on
relaunch, not just offline. **Before building**: confirm with product
whether chat persistence across relaunches is actually wanted, or whether
today's ephemeral behavior is intentional. Cheapest fix might be "no fix."
- If confirmed: add a chat-message entity/DAO, same merge pattern.

## Out of scope (audit confirmed not gaps)
`AuthRepositoryImpl` (network-only by design), DataStore-backed prefs
repos (`OnboardingRepositoryImpl`, `LanguageRepositoryImpl`,
`ThemeRepositoryImpl`, `NotificationRepositoryImpl`), `StepsRepositoryImpl`
(sensor-derived, history already covered by `StepHistoryRepositoryImpl`).

## Test impact
Each phase needs `*RepositoryImplTest.kt` (or equivalent) coverage for:
Room-flow-first read, remote-merge-on-refresh, offline (remote failure)
still serving cached Room data. Follow `FoodLogRepositoryImpl`'s existing
test as the template.

## Sequencing
Ship phases independently — each is a self-contained repository change,
no cross-phase dependency. Land Phase 1 first (cheapest, validates the
pattern once more before the two schema-adding phases).
