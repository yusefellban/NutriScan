# Plan: Real Food Log (Add-from-Saved, Swipe-to-Remove, Room Persistence)

**Status: Implemented.** All items below are done; see the Definition of
Done checklist at the bottom for verification evidence.

## Context

Before this change, tapping "Add Food" on the Calories dashboard did two
disconnected fake things: it navigated to a **placeholder** route
(`SavedProductsRoute`, not the real Saved screen), and it separately
appended the next item from a hardcoded `MOCK_FOODS` list (deterministic
index-cycling, not random) to `CaloriesState.addedFoods`. The real Saved
screen's swipe gesture (`SwipeToAddTriggered`) only showed a snackbar —
nothing was persisted, searched, or connected between the two screens.
There was no domain/data layer for food entries at all (no repository, no
use case) — this was a genuinely new vertical slice, not a small ViewModel
edit.

This plan wires them together for real: **Add Food → real Saved screen →
swipe a product → it lands in today's food log**, persisted offline-first
via Room, scoped to the logged-in user, with swipe-to-remove (+
confirmation) replacing the old fake add-tap, and calorie totals computed
live from that data. It also reuses `ProductCard` for the food-log list
instead of the separate `FoodEntryCard`, per the user's explicit ask.

Confirmed scope decisions (asked directly of the user before implementation):
- **Saved product catalog stays mock** — only the food-log (what gets
  added/removed) becomes real/Room. Browsing/scanning real products into the
  Saved catalog is a separate, future feature.
- **Real per-user scoping** — no `currentUserId` existed anywhere before this
  change (only a login-token boolean). This plan adds
  `IAuthRepository.getCurrentUserId()`, decoding the `sub` claim out of the
  already-issued OIDC `idToken` (previously fetched but silently discarded —
  `TokenManager` never stored it).
- **Docs**: add a root `CLAUDE.md` (none existed) pointing at the existing,
  very thorough root + per-module `AGENTS.md` files, and update those
  `AGENTS.md` files with the concrete patterns this feature establishes
  (first real Room entity, `ProductCard` swipe-mode convention, confirm-dialog
  convention, `getCurrentUserId()`).
- **Calorie totals go live** — `caloriesGained` becomes the live sum of
  today's food-log entries instead of a disconnected mock number.

Everything below follows the existing `AGENTS.md` §12.3 required plan
sections.

## 1. Feature Summary

Replace the mock "Add Food" flow with a real one: Calories → Saved screen →
swipe a product → persisted `FoodLogEntry` (Room, offline-first, scoped to
`getCurrentUserId()`) → shown on the Calories dashboard using `ProductCard` in
a new swipe-to-remove mode, gated by the existing `ConfirmationDialog`
pattern. `caloriesGained` becomes the live sum of today's entries.

## 2. Files Created

**Domain**
- `domain/common/model/ProductVerdict.kt` — moved here from
  `presentation/common/model/ProductVerdict.kt` (a plain 3-value enum; domain
  must not depend on presentation, and both Saved and FoodLog need it).
- `domain/common/RunCatchingCancellable.kt` — the `runCatchingCancellable`
  helper `AGENTS.md` §6.4 documents but nothing previously implemented.
- `domain/foodlog/model/FoodLogEntry.kt` — `id, productId, name, calories,
  imageUrl, verdict: ProductVerdict, loggedDate: LocalDate, addedAt: Instant`.
- `domain/foodlog/repository/IFoodLogRepository.kt` —
  `observeTodayFoodLog(): Flow<List<FoodLogEntry>>`,
  `addFoodEntry(entry): Result<Unit>`, `removeFoodEntry(id): Result<Unit>`.
- `domain/foodlog/usecase/ObserveTodayFoodLogUseCase.kt`,
  `AddFoodEntryUseCase.kt`, `RemoveFoodEntryUseCase.kt` — one class per
  `invoke()`, matching the existing `CheckStepsPermissionUseCase` pattern.

**Data**
- `data/di/DispatcherQualifiers.kt` (`@IoDispatcher`, `@DefaultDispatcher`)
  and `app/di/DispatcherModule.kt` — the qualifier/provider pair `AGENTS.md`
  documents but nothing previously implemented.
- `data/db/entity/FoodLogEntity.kt` — `@Entity(tableName = "food_log")`:
  `id (PK), userId, productId?, name, calories, imageUrl?, verdict (String),
  loggedDate (String, ISO), addedAtEpochMillis (Long)`.
- `data/db/dao/FoodLogDao.kt` — `observeByUserAndDate(userId, date):
  Flow<List<FoodLogEntity>>`, `insert(entity)`,
  `deleteByIdForUser(id, userId)` (scoped delete — defense in depth).
- `data/repository/FoodLogRepositoryImpl.kt` — implements
  `IFoodLogRepository`; resolves `authRepository.getCurrentUserId()` per call;
  if null (not authenticated / undecodable token) → empty flow for observe,
  `Result.failure` for add/remove. Uses `runCatchingCancellable` +
  `@IoDispatcher` — the first repository in the codebase to actually follow
  that pattern end to end.
- `data/repository/mapper/FoodLogMapper.kt` — `FoodLogEntity.toDomain()`
  / `FoodLogEntry.toEntity(userId)` extension functions + `today()`.
- `data/local/util/JwtDecoder.kt` — pure-Kotlin (JVM-testable, no
  Android/Robolectric needed): splits a JWT on `.`, base64url-decodes the
  payload segment (`java.util.Base64`, manual padding fix), parses with
  `kotlinx.serialization.json.Json` already in the project, returns the `sub`
  claim string or `null` on any malformed input.

**Presentation**
- `presentation/common/model/ProductUiModel.kt` — the shared UI model that
  replaced `SavedProductUiModel` (`id, productName, imageUrl, verdict,
  calories`); used by **both** `SavedState.products` and
  `CaloriesState.addedFoods`.
- `presentation/common/components/ProductCardSwipeAction.kt` — sealed
  interface with `Add`/`Remove` variants, each carrying `hintResId` +
  `onTriggered`. `Add` keeps the arrow icon + accent color; `Remove` uses a
  new `ic_trash.xml` drawable + `AppTheme.colors.Error`.

**Tests**
- `presentation/src/test/kotlin/.../saved/SavedViewModelTest.kt` (was zero
  coverage) — 11 tests: initial mock-load, search filter, `ProductClicked`
  nav, `SwipeToAddTriggered` success/failure/unknown-id, bottom-nav tab
  effects.
- `data/src/test/kotlin/.../repository/FoodLogRepositoryImplTest.kt` — 5
  tests against a fake in-memory `FoodLogDao`: null `getCurrentUserId()` →
  empty/failure paths, happy-path add/observe/remove round-trip.
- `data/src/test/kotlin/.../local/util/JwtDecoderTest.kt` — 7 tests: valid
  token → correct `sub`; null/blank/one-segment/non-base64/non-JSON/no-claim
  → `null`.
- Two `getCurrentUserId` tests added to the existing
  `data/src/test/kotlin/.../repository/AuthRepositoryImplTest.kt`.

## 3. Files Modified

**Auth (per-user scoping)**
- `domain/auth/repository/IAuthRepository.kt` — added
  `suspend fun getCurrentUserId(): String?`.
- `data/repository/AuthRepositoryImpl.kt` — implemented via
  `JwtDecoder.extractSubjectClaim(tokenManager.getIdToken())`; `idToken` now
  passed through in `saveTokens(...)` (previously fetched into
  `AuthTokens.idToken` but never persisted).
- `data/local/datasource/TokenManager.kt` — `saveTokens(access, refresh,
  idToken = null)`, added `getIdToken(): String?`.

**Saved feature**
- `saved/state/SavedState.kt` — `products`/`filteredProducts` retyped to
  `ImmutableList<ProductUiModel>`; `SavedProductUiModel.kt` deleted.
- `saved/viewmodel/SavedViewModel.kt` — injects `AddFoodEntryUseCase`;
  `SwipeToAddTriggered` maps the tapped product → `FoodLogEntry` and calls
  the use case; success → `ShowAddedToFoodLogSnackbar`; failure →
  `ShowAddErrorSnackbar`.
- `saved/state/SavedEffect.kt` — `ShowAddedToListSnackbar` renamed
  `ShowAddedToFoodLogSnackbar`; added `ShowAddErrorSnackbar`.
- `saved/view/SavedScreen.kt` — snackbar text now from `stringResource(...)`
  (was hardcoded `"Added ... to shopping list"` — a direct §14.4 "zero
  hardcoded text" violation, fixed here).
- `saved/view/components/SavedProductGrid.kt` — passes
  `ProductCardSwipeAction.Add(...)` into `ProductCard` instead of separate
  `onSwipeToAdd`/`swipeHintResId` params.

**Shared component**
- `common/components/ProductCard.kt` — `onSwipeToAdd`/`swipeHintResId`
  replaced by a single nullable `swipeAction: ProductCardSwipeAction? =
  null`; private `SwipeToAddButton` renamed `SwipeActionButton`, branching
  icon/color on `Add` vs `Remove`; `null` renders no swipe row.
- `common/model/ProductVerdict.kt` — deleted (moved to domain); every import
  site updated.

**Calories feature**
- `main/calories/state/CaloriesState.kt` — `addedFoods:
  ImmutableList<ProductUiModel>` (was `ImmutableList<FoodEntry>`);
  `caloriesGained` default now `0` (was a disconnected mock `2100`); added
  `pendingRemoveFoodId: String? = null`.
- `main/calories/state/CaloriesEvent.kt` — added
  `FoodItemSwipedToRemove(entryId)`, `RemoveFoodConfirmed`,
  `RemoveFoodDismissed`.
- `main/calories/viewmodel/CaloriesViewModel.kt` — injects
  `ObserveTodayFoodLogUseCase`, `RemoveFoodEntryUseCase`; `init` collects the
  flow into `addedFoods` and recomputes `caloriesGained =
  entries.sumOf { it.calories }`; `addFoodClicked()` now *only* emits the
  nav effect (the `MOCK_FOODS` append is gone); new handlers for
  swipe/confirm/dismiss.
- `main/calories/view/CaloriesScreen.kt` — renders `ProductCard` (swipe =
  `Remove`) instead of `FoodEntryCard`; shows `ConfirmationDialog` (at the
  Scaffold body level, not inside the `LazyColumn`) when
  `pendingRemoveFoodId != null`.
- Deleted `main/calories/model/FoodEntry.kt` and
  `common/components/FoodEntryCard.kt` — both fully superseded.

**Database wiring**
- `data/db/NutriScanDatabase.kt` — `entities = [FoodLogEntity::class]`
  (dropped `DummyEntity`), `abstract fun foodLogDao(): FoodLogDao`,
  `exportSchema = true` (was `false`).
- `data/build.gradle.kts` — added `ksp { arg("room.schemaLocation", ...) }`,
  required once `exportSchema = true`.
- Deleted `data/db/entity/DummyEntity.kt`.
- `app/di/DatabaseModule.kt` — provides `FoodLogDao`.
- `app/di/RepositoryModule.kt` — binds `IFoodLogRepository`.

**Navigation cleanup**
- `app/navigation/Route.kt` — removed `SavedProductsRoute` (dead placeholder
  now that Add Food goes to the real `SavedRoute`).
- `app/navigation/NavGraph.kt` — deleted the `SavedProductsRoute` placeholder
  `composable<>` block; `onNavigateToSavedProducts = {
  navController.navigateToTab(SavedRoute) }`, matching the `navigateToTab`
  convention already used for Home/Calories/Saved/Profile tab peers.
  `CameraScanScreen`'s stale `onNavigateToShopping`/`BottomNavTab.HISTORY`
  references (left over from a prior merge) were also fixed to
  `onNavigateToCalories`/`BottomNavTab.CALORIES` while touching this file.

**Tests / strings**
- `presentation/src/test/kotlin/.../CaloriesViewModelTest.kt` — removed the
  `MOCK_FOODS`-append test; added a `Food Log` nested test class covering
  observe/swipe/confirm/dismiss/failure.
- `presentation/src/main/res/values/strings.xml` +
  `values-ar/strings.xml` — new `food_log_*` keys added; dead
  `saved_products_title` removed (nothing referenced it after the
  placeholder route's removal).

## 4. Layer Breakdown

### Domain
- **Models**: `FoodLogEntry`, `ProductVerdict` (relocated).
- **UseCases**: `ObserveTodayFoodLogUseCase`, `AddFoodEntryUseCase`,
  `RemoveFoodEntryUseCase`.
- **Repository interface changes**: new `IFoodLogRepository`;
  `IAuthRepository` gained `getCurrentUserId(): String?`.

### Data
- **Entities**: `FoodLogEntity` (replaced the placeholder `DummyEntity`).
- **DAO**: `FoodLogDao` — user+date scoped observe, insert, scoped delete.
- **Mappers**: `FoodLogEntity ⇄ FoodLogEntry` (`FoodLogMapper.kt`).
- **Repository impl**: `FoodLogRepositoryImpl` — reference implementation of
  `runCatchingCancellable` + `@IoDispatcher`.
- **Auth changes**: `TokenManager` persists `idToken`; `AuthRepositoryImpl`
  implements `getCurrentUserId()` via `JwtDecoder`.

### Presentation
- **State**: `CaloriesState.addedFoods` (typed `ProductUiModel`),
  `pendingRemoveFoodId`; `SavedState.products`/`filteredProducts` retyped to
  the shared `ProductUiModel`.
- **Events**: `CaloriesEvent.FoodItemSwipedToRemove`, `RemoveFoodConfirmed`,
  `RemoveFoodDismissed` (new); `SavedEvent` unchanged in shape.
- **Effects**: `SavedEffect.ShowAddErrorSnackbar` (new); Calories reuses the
  existing generic `CaloriesEffect.ShowSnackbar(@StringRes)` for the
  remove-failure case.
- **ViewModel logic**: `CaloriesViewModel` now observes real data instead of
  mutating mock state; `SavedViewModel` now calls a use case instead of a
  no-op lookup.

## 5. Navigation Changes

No new routes. `SavedProductsRoute` (placeholder) was **removed**; the
existing "Add Food" nav effect is repointed at the real `SavedRoute` via
`navigateToTab`, exactly like the other tab-peer screens.

## 6. Strings — New Keys (EN + AR)

| Key (R.string.xxx)                | English Value                                  | Arabic Value                                  |
|------------------------------------|-------------------------------------------------|------------------------------------------------|
| `food_log_swipe_remove_hint`       | "Swipe to remove"                               | "اسحب للإزالة"                                 |
| `food_log_remove_confirm_title`    | "Remove item?"                                  | "إزالة العنصر؟"                                |
| `food_log_remove_confirm_message`  | "This will remove it from today's food log."    | "سيتم إزالته من سجل الطعام اليوم."             |
| `food_log_added_snackbar`          | "Added %1$s to today's food log"                | "تمت إضافة %1$s إلى سجل الطعام اليوم"          |
| `food_log_add_error`               | "Couldn't add this item. Please try again."     | "تعذّرت إضافة هذا العنصر. حاول مرة أخرى."      |
| `food_log_remove_error`            | "Couldn't remove this item. Please try again."  | "تعذّرت إزالة هذا العنصر. حاول مرة أخرى."      |

(`action_remove` and `action_cancel` already existed and are reused for the
dialog's confirm/cancel labels.)

## 7. Testing Plan (executed)

- `SavedViewModelTest.kt` (new, 11 tests): initial state loads mock catalog;
  search filters; `ProductClicked` → `NavigateToProductDetail`;
  `SwipeToAddTriggered` (known id) → `AddFoodEntryUseCase` invoked with a
  correctly-mapped `FoodLogEntry` → success snackbar; use case failure →
  error snackbar; unknown id → no use-case call, no effect; bottom-nav tab
  effects.
- `CaloriesViewModelTest.kt` (updated): removed the `MOCK_FOODS`-append test;
  added a `Food Log` nested class — observed flow populates `addedFoods` and
  `caloriesGained`; `FoodItemSwipedToRemove` sets `pendingRemoveFoodId` only;
  `RemoveFoodConfirmed` calls `RemoveFoodEntryUseCase` and clears the pending
  id; `RemoveFoodDismissed` clears it without calling the use case;
  `RemoveFoodConfirmed` failure emits `ShowSnackbar`.
- `FoodLogRepositoryImplTest.kt` (new, 5 tests): fake in-memory DAO; null
  `getCurrentUserId()` → empty observe / `Result.failure` on add/remove;
  happy-path add→observe→remove round-trip.
- `JwtDecoderTest.kt` (new, 7 tests): valid token → correct `sub`;
  null/blank/one-segment/non-base64/non-JSON/no-claim → `null`.
- `AuthRepositoryImplTest.kt` (updated, +2 tests): `getCurrentUserId` decodes
  a real token's `sub`; returns `null` when there's no stored id token.

All of the above pass — see §9.

## 8. Edge Cases (handled)

- `getCurrentUserId()` returns `null` (idToken missing, expired session,
  malformed token) — Calories food log observes as empty rather than
  crashing; Saved's swipe-to-add fails gracefully with the error snackbar.
- Rapid double-swipe-to-remove on the same card before the confirm dialog is
  dismissed — `pendingRemoveFoodId` is a single nullable field, so a second
  swipe while the dialog is open just replaces the pending target.
- Removing the last remaining food-log entry — `addedFoods` becomes empty,
  falls back to the existing single full-width `DashedActionCard` empty
  state.
- Calories parsed from `ProductUiModel.calories` (a display `String`) into
  `Int` via `toIntOrNull() ?: 0` — guards non-numeric input.
- App restart with a stale/never-refreshed `idToken` — `getCurrentUserId()`
  never throws on decode failure, only returns `null` (covered by
  `JwtDecoderTest`).

## 9. Definition of Done

- [x] All files in §2/§3 created/modified/deleted as described.
- [x] `SavedViewModelTest` (11), `CaloriesViewModelTest` (updated, all
      nested classes passing), `FoodLogRepositoryImplTest` (5),
      `JwtDecoderTest` (7), `AuthRepositoryImplTest` (+2) all passing —
      verified via `./gradlew test`, 0 failures/errors across every touched
      test file.
- [x] All new strings in both `values/strings.xml` and `values-ar/strings.xml`.
- [x] No hardcoded colors/strings/dimensions in any touched Composable.
- [x] `ProductCard` used (via `ProductCardSwipeAction`) on both Saved (Add)
      and Calories food log (Remove) — `FoodEntryCard` fully removed.
- [x] `./gradlew compileDebugKotlin` and `./gradlew test` both pass.
- [x] Root `CLAUDE.md` created; root + `data`/`domain`/`presentation`
      `AGENTS.md` updated with this feature's new conventions (§10).

## 10. Documentation Updates

- **New `CLAUDE.md`** at repo root: points Claude Code at `AGENTS.md` (root +
  module files), plus a condensed TL;DR of the non-negotiables
  (plan-before-code, mandatory ViewModel tests, zero hardcoded strings,
  layer boundaries, Room/offline-first pattern, destructive-action dialog
  pattern) and a "where to look for precedent" pointer to
  `FoodLogRepositoryImpl` and `ProductCardSwipeAction`.
- **Root `AGENTS.md`**: §8.1 Room — added a callout naming
  `FoodLogEntity`/`FoodLogDao` as the first real worked example (the
  existing multi-entity snippet is the long-term target shape, not current
  state). §13.9 Destructive Action Contract — reconciled the old
  Effect-based sketch with the actual state-boolean-gated
  `ConfirmationDialog` convention, with the Calories food-log removal flow
  as a third real example alongside `AppSettingsScreen`/`UserProfileScreen`.
  §13.1 Authentication — added a "reality check" callout (real stack is
  Keycloak/OIDC + `EncryptedSharedPreferences`, not Firebase/Proto DataStore
  as the stale text says) plus documentation of `getCurrentUserId()`.
- **`presentation/AGENTS.md`** §7 Shared Component Catalogue — replaced the
  `FoodEntryCard` entry with `ProductCard`'s full signature and documented
  the `ProductCardSwipeAction` sealed-interface convention as the sanctioned
  way to add a new swipe mode.
- **`data/AGENTS.md`** §9 — added a callout naming `FoodLogRepositoryImpl` as
  the first repository to actually follow `runCatchingCancellable` +
  `@IoDispatcher` end to end.

## Verification

- `./gradlew compileDebugKotlin -q` — passes.
- `./gradlew test -q` — passes, 0 failures/errors (confirmed via test-result
  XML: `tests="7|5|5|1|3|2|3|5|1|4|6|11" failures="0" errors="0"` across
  every new/updated test file).
- Manual verification (device/emulator) was **not** performed in this
  session — this was a headless coding session with no browser/emulator
  tooling invoked. Before shipping: launch app → Calories tab → tap "Add
  Food" → confirm it lands on the real Saved screen (not a placeholder) →
  swipe a product → snackbar confirms → navigate back to Calories tab →
  confirm the item appears via `ProductCard` and `caloriesGained` reflects
  it → swipe that card → confirm the dialog appears → confirm → item
  removed, totals update; dismiss → item stays. Repeat with dark theme and
  Arabic (RTL) locale to confirm swipe direction and theming hold up, per
  the existing RTL-aware `SwipeActionButton` logic.
