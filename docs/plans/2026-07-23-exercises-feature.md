# Exercises Feature — Phase 1: Foundations + Exercises List Screen

## How to use this doc
This is **Phase 1 of 4** of the Exercises feature build-out. Each phase is handed to a
different AI developer, one at a time, together with `PROGRESS.md`. Read `PROGRESS.md`
first — it tells you which phase is currently active and what prior phases actually
delivered (not just what was planned). When you finish this phase, update
`PROGRESS.md` yourself before handing off.

**Do not start Phase 2, 3, or 4 work.** Their plans do not exist yet — the AI developer
assigned to each phase writes their own plan (per `AGENTS.md`/`SKILL.md` §1) once this
phase is merged, informed by what Phase 1 actually built.

## Feature Overview (for context — full flow spans all 4 phases)
1. **Exercises screen** — list of exercises with search + category chips. Tapping a row
   opens a bottom sheet with instructions; "read more" expands to a full-text view;
   "Start Workout" opens the Exercise (workout) screen. → **Phase 1 (list) + Phase 2 (bottom sheet)**
2. **Exercise (workout) screen** — timer with Start/Restart/Pause/Cancel. On Pause, UI
   branches on exercise type: Cardio shows Finish directly; Normal Workout shows
   editable Sets/Reps cards above Finish. → **Phase 3**
3. **Calorie calculation + Daily Products integration** — on Finish, compute burnt kcal
   (`kcalPerMin × minutes` for cardio, `sets × reps × minutes` for normal workout — see
   note in Phase 4 about this formula), navigate back to the Calories ("Daily Products")
   screen, show a result dialog, and update `ExerciseCard`'s kcal/minutes totals and the
   Calorie Goals card. → **Phase 4**

`kcalPerMin` / `kcalPerRep` are mock-data-only fields the UI never displays.

## Goal Description (Phase 1 scope)
Build the **data foundations and the Exercises list screen only**:
- Domain/UI models for an exercise, with mock data (2 fixed entries — one cardio, one
  normal workout — for later phase testing, plus filler entries).
- The Exercises screen: header, search bar, category chips row, and the scrollable list
  of exercise rows — reusing existing shared components wherever the design matches.
- Navigation wiring from the existing `ExercisesRoute` placeholder into the real screen.
- The `person-exercise` artwork added to the project and used as the exercise
  illustration in the list rows (and reserved for reuse in Phase 2/3).
- All strings, colors, and text styles centralized per `SKILL.md` §2–3 and this
  project's existing `AppTypography`/`AppColors`/`strings.xml` conventions.

**Out of scope for Phase 1** (leave for later phases — do not build these now):
- The instructions bottom sheet and "read more" full-instructions view (Phase 2).
- The Exercise workout screen, timer, sets/reps editing, dialogs (Phase 3).
- Calorie calculation, the result dialog, and `ExerciseCard`/Calorie Goals updates on
  the Daily Products screen (Phase 4).
- Any backend/API integration. This entire feature is **UI-only with mock data** for
  all 4 phases — `kcalPerMin`/`kcalPerRep` are placeholders for a future backend
  attribute; do not build a real data source or API client for them.

---

## Codebase Facts Established By Research (read this before writing code)

- Module layout: `app` (nav graph), `domain`, `data`, `presentation` (Clean
  Architecture + MVI, per existing screens).
- `ExercisesRoute` **already exists** as a placeholder destination, wired from the
  Calories ("Daily Products") screen:
  `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`, composable #28:
  ```kotlin
  composable<ExercisesRoute> {
      PlaceholderScreen(
          title = stringResource(R.string.exercises_title),
          buttonText = stringResource(R.string.action_go_back),
      ) { navController.navigateUp() }
  }
  ```
  Phase 1 replaces this composable body with the real `ExercisesScreen`. The route
  itself (`object ExercisesRoute` in `Route.kt`) does not need to change.
- `CaloriesScreen` already calls `onNavigateToExercises = { navController.navigate(ExercisesRoute) }`
  via the `+` button on `ExerciseCard` — this is already correct, no change needed there
  in Phase 1. (Phase 4 will add a *return* path with a result payload — leave a note,
  don't implement it now.)
- No `exercises` package exists yet anywhere in `domain`, `data`, or `presentation` —
  this is a greenfield feature package.
- `presentation/common/components/ExerciseCard.kt` already exists but is the **Daily
  Products dashboard summary card** (kcal/minutes totals), unrelated to the exercise
  list row. Do not confuse the two — Phase 1 introduces a *new*, distinctly named
  component for exercise list rows (see naming below).
- Reusable components confirmed to exist and fit this screen's design:
  - `presentation/saved/view/components/SavedSearchBar.kt` — rounded `OutlinedTextField`
    + circular search `IconButton`, already theme- and string-resource-driven. Visually
    matches `Exercises.png`'s search bar. **Reuse pattern, don't duplicate** — see
    component decision below.
  - `presentation/news/view/components/NewsTopicChipRow.kt` and
    `presentation/profile_setup/view/components/SelectableChip.kt` — existing
    chip-row / selectable-chip patterns to model the category chips
    ("All", "Warm Up", "Yoga", "Biceps", …) on.
  - `presentation/common/components/AppBackButton.kt` — back chevron button, matches
    the back button in `Exercises.png`/`Exercise.png` headers.
  - `presentation/common/components/AppButton.kt` — check its signature/variants before
    adding a new button component for "Start Workout" (Phase 2) or "Finish" (Phase 3/4).
- Typography convention: Material's fixed 15-slot `Typography` (`AppTheme.typography.*`)
  is used first; when a design needs a size/weight combo that doesn't exist there, a
  small `object FooTypography { val someStyle = TextStyle(...) }` is added to
  `AppTypography.kt`, following the existing `HomeTypography` / `CaloriesTypography` /
  `ProductDetailsTypography` pattern. Phase 1 must follow this, not inline `TextStyle`s
  in composables.
- Colors: no `Color(0xFF...)` in composables — always `AppTheme.colors.*` (see
  `AppColors.kt`). If a needed color/tint isn't defined yet, add it there with a
  semantic name, matching the light/dark pairing pattern already used.
- Strings: `presentation/src/main/res/values/strings.xml` (English) +
  `values-ar/strings.xml` (Arabic) — every user-facing string added to both.
- Plans live in `docs/plans/YYYY-MM-DD-feature-name.md` per `SKILL.md` §1 — this file
  follows that.

### The `person-exercise.svg` asset — important technical correction
The uploaded `person-exercise.svg` is **not a vector graphic**. Its `<svg>` wrapper only
contains a `<pattern>`/`<image>` that embeds a single base64-encoded **raster PNG**
(600×344) stretched over a 216×120 viewBox. There are no vector paths to trace, so it
is not possible to "convert it to XML" as a `VectorDrawable` without effectively
redrawing the artwork by hand from pixels — which would not "match exactly" as
requested and would misrepresent the source asset.

**Correct, honest approach for Phase 1:**
1. Decode the embedded base64 PNG out of the SVG and save it as
   `presentation/src/main/res/drawable-xxhdpi/img_exercise_person.png` (or the
   appropriate density bucket / an `.webp` re-encode for size, using the project's
   existing convention for raster assets — check whether other screens ship PNG/WEBP
   drawables under `res/drawable*` and match that).
2. Reference it with `painterResource(R.drawable.img_exercise_person)` — this is a
   pixel-perfect, exact match, unlike a hand-authored vector approximation.
3. Note in the PR/plan output that this is intentionally a raster asset, not a
   `VectorDrawable`, and explain why (above), so no later reviewer assumes it should
   have been vectorized.
If a true vector source (e.g. an `.svg` with real `<path>` elements, or a Figma export)
becomes available later, it can be swapped in as a proper `ImageVector`/`VectorDrawable`
at that point — but do not fabricate paths now.

---

## Design Reference (from screenshots, Phase 1 scope only)

**`Exercises.png`** (list screen):
- Header row: `AppBackButton` + "Exercises" title (Medium 20px → `titleMedium`, which is
  already Medium/20sp/24sp in `AppTypography` — confirm exact match, else add a token).
- Search bar: rounded pill `OutlinedTextField` with placeholder "Search here" (Regular
  14px → `bodyMedium`, already Regular/14sp) + circular teal search icon button on the
  right — matches `SavedSearchBar` almost exactly.
- Category chip row directly below the search bar: "All" (selected, teal outline/fill),
  "Warm Up", "Yoga", "Biceps", "Ch..." (cut off, more categories scroll horizontally).
  "All" text style: Regular 14px → `bodyMedium`.
- Below that, a scrollable list of exercise rows, each a rounded, filled card
  (light teal background) containing:
  - Left: the `person-exercise` illustration in a small rounded thumbnail.
  - Title, e.g. "Full Body Warm Up" (Medium 16px → `titleSmall`, already Medium/16sp).
  - Subtitle "equipment  •  target" (Regular 14px → `bodyMedium`).
  - Right: a chevron ("›") affordance indicating it's tappable.

**`Exercise-bottomsheet.png` / `Exercise-bottomsheet-seemore.png` / `Exercise.png`**:
Shown here for full-flow context only — **not built in Phase 1**. Do not implement the
bottom sheet, "read more" view, or workout screen. They're listed in the Phase
boundaries above and will get their own plans.

---

## Proposed Changes

### 1. Domain / UI Models

Given this is UI-only work with mock data (matching the precedent set by the Saved
screen's `2026-07-22-saved-screen.md` plan, which kept mock data at the presentation
layer), Phase 1 introduces a **presentation-layer UI model**, not a full domain
use-case layer. If a later phase needs a domain model (e.g. once persistence is added
in a future non-mock phase), that's a separate, larger change outside this feature's
current 4 phases — flag it, don't build it now.

#### [NEW] `presentation/common/model/ExerciseType.kt`
```kotlin
package iti.grad.nutriscan.presentation.common.model

enum class ExerciseType { CARDIO, NORMAL_WORKOUT }
```
Lives in `common/model` (next to `ProductUiModel.kt`) because Phase 3/4 (the workout
screen) and the Daily Products integration will also reference it, not just the list
screen.

#### [NEW] `presentation/common/model/ExerciseUiModel.kt`
```kotlin
package iti.grad.nutriscan.presentation.common.model

data class ExerciseUiModel(
    val id: String,
    val name: String,
    val equipment: String,
    val target: String,
    val instructions: String,
    val type: ExerciseType,
    // Mock-data-only backend placeholders. NEVER rendered in any composable.
    val kcalPerMin: Double? = null,   // present when type == CARDIO
    val kcalPerRep: Double? = null,   // present when type == NORMAL_WORKOUT
)
```
- `instructions` holds the full instructions text (used by the truncated preview in
  Phase 2's bottom sheet and the full "read more" view — Phase 1 just needs the field
  to exist and be populated in mock data so Phase 2 doesn't have to touch this model).
- Exactly one of `kcalPerMin`/`kcalPerRep` is non-null, matching `type`. Consider a
  sealed class instead of nullable pair if the reviewer prefers stronger typing —
  either is acceptable, but whichever is chosen, the two backend-only fields must
  never be read by any `@Composable` in this or later phases (only by the future
  Phase 4 calorie calculation, in a ViewModel/domain layer, not the UI tree).

#### [NEW] `presentation/exercises/model/ExerciseCategory.kt`
```kotlin
package iti.grad.nutriscan.presentation.exercises.model

data class ExerciseCategory(val id: String, val labelRes: Int)
```
Backing model for the chip row, mirroring how `NewsTopicChipRow`/`SelectableChip`
represent their filter options — check the exact existing pattern in
`news/state/NewsState.kt` and `profile_setup/.../SelectableChip.kt` before finalizing
this shape, and match it rather than inventing a new one.

#### [NEW] `presentation/exercises/mock/ExercisesMockData.kt`
Mock list used by the ViewModel (Phase 1 only wires this into the list screen; Phase 2
consumes the same objects for the bottom sheet — do not duplicate the data source).
- **First entry**: `ExerciseType.CARDIO`, with a populated `kcalPerMin` (e.g. `0.23`),
  `kcalPerRep = null`.
- **Second entry**: `ExerciseType.NORMAL_WORKOUT`, with a populated `kcalPerRep` (e.g.
  `0.3`), `kcalPerMin = null`.
- Remaining ~6–8 entries (to match the row count visible in `Exercises.png`): either
  type, filler equipment/target/instructions text.
- All exercise names/equipment/target/instructions strings used here must still go
  through `stringResource` at the point they're *displayed* — i.e., store string
  resource IDs (`@StringRes Int`) in the mock model fields that are user-facing text,
  not raw hardcoded Kotlin strings, so `SKILL.md` §2's "no hardcoded user-facing
  strings" rule holds even for mock data. (This mirrors the `ExerciseCategory.labelRes`
  approach above.) Re-check `ExerciseUiModel`'s `name`/`equipment`/`target`/
  `instructions` field types against this — they should be `@StringRes Int`, not
  `String`, if this project's other mock-data screens (e.g. Saved screen's mock
  products) follow that convention. **Verify against `SavedViewModel`'s mock data
  approach before implementing** and match whichever convention it actually uses.

---

### 2. Presentation — New Reusable Component

#### [NEW] `presentation/common/components/ExerciseListItemCard.kt`
A new, distinctly-named component (do not reuse or rename the existing dashboard
`ExerciseCard`) for a single row in the Exercises list:
```kotlin
@Composable
fun ExerciseListItemCard(
    exercise: ExerciseUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```
- Rounded card, light teal fill (`AppTheme.colors.*` — reuse whatever token
  `calorieCardSurface()` / similar existing modifier extension in `ShadowExt.kt` or
  `ExerciseCard.kt` already uses for this exact background+corner-radius+padding
  combo, rather than hand-rolling a new `Modifier.background(...).clip(...)`).
- Thumbnail: `painterResource(R.drawable.img_exercise_person)` (see asset section
  above), rounded corners, fixed small size, `contentScale = ContentScale.Crop` (adjust
  to match the screenshot's crop).
- Title: `exercise.name` (resolved via `stringResource`), `AppTheme.typography.titleSmall`.
- Subtitle: `"${stringResource(equipment)}  •  ${stringResource(target)}"` via
  `AppTheme.typography.bodyMedium`, `AppTheme.colors.ExerciseSecondaryText` (already
  exists, used by the dashboard `ExerciseCard` — reuse it here for consistency rather
  than adding a new near-duplicate color token).
- Trailing chevron icon, tappable row via `Modifier.clickable { onClick() }`.
- Put this in `common/components` (not `exercises/view/components`) because Phase 2's
  bottom sheet and Phase 3's workout screen header will likely want the same
  thumbnail-rendering logic — confirm with whoever plans Phase 2 rather than
  duplicating the `painterResource` call site three times.

Live under `presentation/exercises/view/components/` instead **only if** research at
implementation time shows this project's convention is "feature-local unless proven
reused 2+ times" (check `SavedSearchBar`'s location as precedent — it lives under
`saved/view/components`, not `common/components`, despite being visually generic).
**Match whichever precedent is stronger — do not guess; grep for how `ProductCard` vs.
`SavedSearchBar` placement was decided and follow the same logic.**

---

### 3. Presentation — Exercises Screen (MVI)

New package: `presentation/exercises/`

#### [NEW] `presentation/exercises/state/ExercisesState.kt`
```kotlin
data class ExercisesState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val categories: ImmutableList<ExerciseCategory> = persistentListOf(),
    val selectedCategoryId: String = "all",
    val exercises: ImmutableList<ExerciseUiModel> = persistentListOf(),
    // filtered by searchQuery + selectedCategoryId; exposed separately so the
    // Composable never re-filters on every recomposition
    val visibleExercises: ImmutableList<ExerciseUiModel> = persistentListOf(),
)
```
Use `kotlinx.collections.immutable` (`ImmutableList`), matching the MVI contract rule
already established in the Saved-screen plan.

#### [NEW] `presentation/exercises/state/ExercisesEvent.kt`
```kotlin
sealed interface ExercisesEvent {
    data class OnSearchQueryChange(val query: String) : ExercisesEvent
    data class OnCategorySelected(val categoryId: String) : ExercisesEvent
    data class OnExerciseClick(val exerciseId: String) : ExercisesEvent // Phase 2 opens the sheet on this
    data object OnBackClick : ExercisesEvent
}
```

#### [NEW] `presentation/exercises/state/ExercisesEffect.kt`
```kotlin
sealed interface ExercisesEffect {
    data object NavigateBack : ExercisesEffect
    // Phase 2 will likely add something like ShowExerciseDetails(exerciseId) here —
    // don't pre-build it now, just leave this file easy to extend.
}
```

#### [NEW] `presentation/exercises/viewmodel/ExercisesViewModel.kt`
- `@HiltViewModel`, follows the exact MVI skeleton used by `CaloriesViewModel`/
  `SavedViewModel` (state `StateFlow`, effect `Channel`/`SharedFlow`, `onEvent(...)`
  dispatcher). Match whichever of those two is more structurally similar (Saved is
  closer in shape — list + search — so prefer that as the template).
- Loads `ExercisesMockData` on init.
- `OnSearchQueryChange` / `OnCategorySelected` recompute `visibleExercises` (simple
  in-memory filter over `exercises`, case-insensitive substring match on the resolved
  name/equipment/target strings — since these will be `@StringRes Int`, filtering
  needs the resolved string, which means either resolving strings in the ViewModel via
  `@ApplicationContext` `Context.getString()` — check how `SavedViewModel`/
  `NewsViewModel` handle search filtering against resource-backed mock data, since this
  project already has this exact problem solved somewhere; **do not invent a new
  pattern, copy the existing one.**
- `OnExerciseClick` — for Phase 1, this event exists in the contract but its handler
  can be a no-op / TODO comment pointing at Phase 2, since the bottom sheet doesn't
  exist yet. Do not stub in a `Toast` or fake navigation — leave it inert and clearly
  marked, so Phase 2 has an obvious single place to implement it.
- `OnBackClick` → emits `ExercisesEffect.NavigateBack`.

#### [NEW] `presentation/exercises/view/ExercisesScreen.kt`
```kotlin
@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
)
```
- Collect `state` via `collectAsStateWithLifecycle()`; handle `effect` via
  `collectLatest` inside `LaunchedEffect`, exactly like `CaloriesScreen`'s pattern
  (`NavigateBack -> onNavigateBack()`).
- Layout, top to bottom:
  1. Header: `AppBackButton` + "Exercises" title text (`titleMedium`-equivalent —
     confirm token match against the 20px/Medium spec; add a
     `ExercisesTypography.screenTitle` token in `AppTypography.kt` only if
     `titleMedium` doesn't line up exactly, following the `CaloriesTypography`
     precedent — don't add a redundant token if an existing one already matches).
  2. Search bar — reuse `SavedSearchBar` directly if its API (query/onQueryChange) is
     generic enough, or extract a shared `AppSearchBar` into `common/components` if the
     project's convention favors that (check for signs this refactor was already
     anticipated, e.g. a TODO comment in `SavedSearchBar.kt`, before doing an
     unplanned rename/move that could break the Saved screen — if in doubt, wrap it
     rather than move/rename the existing file, to keep this phase's diff scoped).
  3. Category chip row — horizontally scrollable `Row` (or `LazyRow`) built from
     `state.categories`, each rendered with the existing `SelectableChip` composable if
     its API fits, else a small new `ExerciseCategoryChip` modeled directly on it.
  4. `LazyColumn` of `ExerciseListItemCard`s over `state.visibleExercises`, each
     `onClick = { viewModel.onEvent(ExercisesEvent.OnExerciseClick(it.id)) } `.
  5. Empty-state: if `visibleExercises` is empty after filtering, reuse
     `EmptyStateWidget` (already exists in `common/components`) rather than a new
     bespoke empty state.
- No bottom nav bar on this screen (it's a secondary/detail screen reached by pushing
  from the Calories tab, like `ProductDetailsScreen`, not a tab root) — confirm this
  assumption against `Exercises.png`'s design (no bottom nav visible) and against how
  `ProductDetailsScreen` is wired in `NavGraph.kt` (plain `composable<...>`, not through
  `navigateToTab`), and mirror that.

---

### 4. Navigation Wiring

#### [MODIFY] `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`
Replace composable #28's `PlaceholderScreen` body:
```kotlin
// 28. Exercises
composable<ExercisesRoute> {
    ExercisesScreen(
        onNavigateBack = { navController.navigateUp() },
    )
}
```
Add the corresponding import for `ExercisesScreen`. No other route/graph changes are
needed in Phase 1 — the entry point from `CaloriesScreen` already targets
`ExercisesRoute` correctly.

---

### 5. Resources

#### [MODIFY] `presentation/src/main/res/values/strings.xml`
Add (exact keys to confirm against project's naming convention, e.g. check whether
existing keys are `snake_case` grouped by screen prefix like `saved_search_hint`):
```xml
<!-- Exercises Screen -->
<string name="exercises_search_hint">Search here</string>
<string name="exercises_category_all">All</string>
<!-- other category labels as needed by mock categories -->
<string name="exercises_row_separator">•</string>
<!-- exercise name/equipment/target/instructions string keys per mock entry -->
```
Note: `exercises_title` already exists (used by the current placeholder) — reuse it
for the real header, don't duplicate.

#### [MODIFY] `presentation/src/main/res/values-ar/strings.xml`
Add Arabic translations for every key above. Do not leave any English-only string —
this is a hard `SKILL.md` §2 requirement, not optional polish.

#### [NEW] Exercise illustration asset
Per the asset section above: decode and add
`presentation/src/main/res/drawable-xxhdpi/img_exercise_person.png` (raster, exact
match to source). Do not create a hand-traced `VectorDrawable` claiming SVG parity.

#### [NEW/MODIFY] `AppColors.kt` (only if needed)
Add any missing semantic tokens discovered while matching the screenshot exactly
(e.g. the list card's teal fill, the chip selected/unselected colors) — reuse existing
tokens first (`Teal200`/`Teal1000`/etc. already exist per `ExerciseCard.kt`); only add
new ones if no existing token matches both light and dark screenshots.

#### [MODIFY] `AppTypography.kt` (only if needed)
Add `ExercisesTypography` object, following the `CaloriesTypography`/`HomeTypography`
pattern, **only** for any of the Phase 1 text styles that don't already have an exact
Material slot match. Cross-check every style listed in the user's spec against the
existing `AppTypography` scale before adding anything new:
- "Exercises" (Medium/20px) → likely `titleMedium` (Medium/20sp) — verify line-height
  doesn't matter for the match, reuse directly.
- "Search here" (Regular/14px) → `bodyMedium` (Regular/14sp) — reuse directly.
- "All" (Regular/14px) → `bodyMedium` — reuse directly.
- "Full Body Warm Up" list title (Medium/16px) → `titleSmall` (Medium/16sp) — reuse
  directly.
- "equipment"/"target" (Regular/14px) → `bodyMedium` — reuse directly.
No new tokens are expected to be necessary for Phase 1's five listed styles — confirm
this during implementation instead of assuming; add only if a genuine mismatch is
found.

---

## Verification Plan

### Automated
- Unit test `ExercisesViewModelTest` (JUnit 5 + Turbine + coroutines-test, per
  `SKILL.md` §4's exact template):
  - Initial state loads mock exercises into `exercises`/`visibleExercises`.
  - `OnSearchQueryChange` filters `visibleExercises` correctly (case-insensitive,
    matches name/equipment/target).
  - `OnCategorySelected` filters `visibleExercises` to the selected category only, and
    `"all"` shows everything.
  - `OnBackClick` emits `ExercisesEffect.NavigateBack` (via `effect.test { ... }`).
- Confirm `./gradlew :presentation:testDebugUnitTest` passes for the new test class.

### Manual
- Launch app → Daily Products (Calories) tab → tap the `+` on the exercise summary
  card → lands on the new Exercises screen (not the placeholder).
- Verify header, search bar, chip row, and list render pixel-reasonably close to
  `Exercises.png` in both light and dark theme (toggle via device/emulator theme).
- Type in the search bar → list filters live, no hardcoded English leaking in when
  device locale is set to Arabic (switch device language to Arabic and re-check every
  string, including chip labels and the empty-state message).
- Confirm the person-exercise thumbnail renders identically to the source PNG (no
  visible re-compression artifacts vs. the uploaded file).
- Tap a category chip → list filters; tap "All" → list resets.
- Tap an exercise row → currently a no-op (expected — Phase 2 wires this up); confirm
  it doesn't crash and doesn't navigate anywhere unexpected.
- Back button → returns to Daily Products, not some other screen.
- Rotate/resize (if tablet/foldable configs are in scope for this project — check
  whether other screens test this; if not, skip).

---

# Exercises Feature — Phase 2: Instructions Bottom Sheet, Read More, Start Workout Nav

**Author:** Phase 2 planning pass (per `PROGRESS.md`)
**Depends on:** Phase 1 (`docs/plans/2026-07-23-exercises-feature-phase-1.md`) — see
"Dependencies on Phase 1" below. Phase 1 had not been implemented at the time this plan
was written, so the contracts below are the ones Phase 2 *requires*; if Phase 1 ships
something different, reconcile before writing Phase 2 code and note the deviation in
`PROGRESS.md`.

---

## 1. Goal Description

From the `ExercisesScreen` list (Phase 1), tapping an `ExerciseListItemCard` opens a
bottom sheet showing that exercise's instructions (`Exercise-bottomsheet.png`), truncated
to 3 lines with a trailing "read more" link. Tapping "read more" expands the same sheet
in place to show the full instructions text with no truncation (`Exercise-bottomsheet-
seemore.png`) — same sheet, same card, no separate screen/route. Tapping "Start Workout"
(present in both states) navigates to the Exercise (workout) screen that Phase 3 owns,
passing the selected exercise's id and type.

This phase does **not** touch the workout screen itself, the timer, or calorie math —
only the sheet and the hand-off into Phase 3's route.

## 2. Dependencies on Phase 1

Phase 2 needs the following to already exist; if any is missing or named differently,
Phase 2 must adapt rather than duplicate:

- `ExerciseUiModel` with at least: `id: String`, `name: String`, `equipment: String`,
  `target: String`, `type: ExerciseType` (`CARDIO` / `NORMAL_WORKOUT`), and a
  **full, untruncated** `instructions: String`. Truncation is a UI concern (Phase 2),
  not a data concern — mock data must hold the complete instructions text, not a
  pre-shortened version.
- `ExercisesState` exposing the list the sheet is opened from, so Phase 2 only adds
  sheet-related fields to it (see §3.1) rather than introducing a parallel state holder.
- `ExercisesEvent.OnExerciseClick(exerciseId: String)` currently a no-op per Phase 1's
  handoff notes — Phase 2 makes it functional.
- `person-exercise` drawable resource name (Phase 1 handoff notes) for the sheet's
  illustration — same asset as the list thumbnails, larger size, teal circular
  background per `Exercise-bottomsheet.png`.
- Confirm from Phase 1's "Open questions" section whether `ExerciseListItemCard`'s
  thumbnail composable is reusable as-is; if so, reuse it inside the sheet instead of
  duplicating image-loading/sizing logic.

**Open question for Phase 1 to confirm before Phase 2 starts (do not guess):** does
`ExerciseUiModel` carry `kcalPerMin`/`kcalPerRep` already (per the global mock-data rule
in `PROGRESS.md`)? Phase 2 never reads or renders these fields, but must not strip them
when passing the model forward to Phase 3's nav argument.

## 3. Proposed Changes

### 3.1 State — extend `ExercisesViewModel` (no new ViewModel)

The sheet is opened from the same screen, so its visibility belongs in the existing
`ExercisesState`/`Event`/`Effect`, not a second MVI stack:

```kotlin
data class ExercisesState(
    // ...Phase 1 fields unchanged...
    val selectedExercise: ExerciseUiModel? = null,
    val isInstructionsExpanded: Boolean = false, // false = truncated sheet, true = "read more" state
)

sealed interface ExercisesEvent {
    // ...Phase 1 events unchanged...
    data class OnExerciseClick(val exerciseId: String) : ExercisesEvent
    data object OnDismissInstructions : ExercisesEvent
    data object OnReadMoreClick : ExercisesEvent
    data object OnStartWorkoutClick : ExercisesEvent
}

sealed interface ExercisesEffect {
    // ...Phase 1 effects unchanged...
    data class NavigateToExerciseWorkout(val exercise: ExerciseUiModel) : ExercisesEffect
}
```

`OnExerciseClick` looks up the exercise from the already-loaded list state and sets
`selectedExercise`; `isInstructionsExpanded` resets to `false` each time a new exercise
is selected. `OnDismissInstructions` clears `selectedExercise` back to `null`.
`OnStartWorkoutClick` emits `NavigateToExerciseWorkout(selectedExercise)` — Phase 2 does
not clear `selectedExercise` before emitting, so the sheet doesn't visibly collapse
mid-navigation-transition.

### 3.2 UI — `ExerciseInstructionsBottomSheet`

New file: `presentation/.../exercises/view/components/ExerciseInstructionsBottomSheet.kt`
(feature-local, not `common/components/` — it's built entirely around `ExerciseUiModel`
and has no reuse candidate elsewhere yet; if a later feature needs an instructions sheet
for a non-exercise model, promote it then).

Built on Material3 `ModalBottomSheet` (`rememberModalBottomSheetState`,
`skipPartiallyExpanded = false`) so it gets the standard scrim, drag handle, and
swipe-to-dismiss for free — matches the visible drag handle at the top of both
reference screenshots.

Content column, top to bottom, matching `Exercise-bottomsheet.png` /
`-seemore.png`:

1. Row: `person-exercise` illustration (small, circular teal background) + Column of
   `exercise.name` (`AppTheme.typography.titleSmall` — matches the spec'd "3/4 sit-up"
   16px/Medium) and `"${exercise.equipment} • ${exercise.target}"`
   (`AppTheme.typography.bodyMedium` — matches "body weight" 14px/Regular). Build the
   "•" separator through a string resource with placeholders
   (`R.string.exercise_equipment_target`, `"%1$s • %2$s"`), not string concatenation, so
   RTL bidi and translated separators work correctly.
2. `"Instructions"` header — `stringResource(R.string.exercise_instructions_title)`,
   `AppTheme.typography.titleMedium` (matches the spec'd 20px/Medium — same slot already
   used for the "Exercises" screen title, no new typography token needed).
3. Instructions body text, `AppTheme.typography.bodyMedium` (matches 14px/Regular):
   - Collapsed (`isInstructionsExpanded == false`): `maxLines = 3`,
     `overflow = TextOverflow.Ellipsis`, with a trailing inline "read more" link built via
     `buildAnnotatedString` + `LinkAnnotation.Clickable` appended after the truncated
     text (not a separate `Text` composable below it — screenshot shows it inline at the
     end of the last visible line). Link style: `AppTheme.colors.Primary` (teal),
     `AppTheme.typography.bodyMedium`. Tapping it dispatches `OnReadMoreClick`.
   - Expanded (`isInstructionsExpanded == true`): full `exercise.instructions`, no
     `maxLines`/overflow, no link.
   - Do not implement this as two different composables gated by an `if` at the call
     site if it can be one `InstructionsBody(text, isExpanded, onReadMoreClick)`
     composable — keeps the sheet's main body simple.
4. `AppButton` (existing shared component) for "Start Workout" —
   `stringResource(R.string.action_start_workout)`, dispatches `OnStartWorkoutClick`.
   Reused as-is; do not fork it for this screen.

`OnReadMoreClick` only flips `isInstructionsExpanded` — it must **not** call
`sheetState.expand()` itself as a side effect inside the composable; let Material3's
`ModalBottomSheet` re-measure and animate to fit the new (larger) content naturally when
the state recomposes with the full text. If in manual testing the sheet doesn't grow to
show the full text (i.e. it stays capped at the partial-expand height), then explicitly
call `scope.launch { sheetState.expand() }` in response to the state change as a
targeted fix — don't add it speculatively before confirming it's needed.

### 3.3 Wiring into `ExercisesScreen`

`ExercisesScreen` observes `state.selectedExercise` and renders
`ExerciseInstructionsBottomSheet` when non-null, passing `state.isInstructionsExpanded`
and forwarding the three new events. Collect `NavigateToExerciseWorkout` from the effect
flow exactly the way Phase 1 already collects other effects, and call a new
`onNavigateToWorkout: (ExerciseUiModel) -> Unit` lambda parameter on `ExercisesScreen`
(mirrors the existing `onNavigateBack`-style parameter pattern used across the codebase's
screens — see `CaloriesScreen`'s `onNavigateToExercises` for the sibling convention).

### 3.4 Navigation — `NavGraph.kt`

Replace the current `ExercisesRoute` placeholder composable. Add a new
`ExerciseWorkoutRoute` **type declaration only** (id/type fields) for Phase 3 to render —
Phase 2 does not build the destination's screen, only the route it navigates to, since
Phase 3 owns everything past this hand-off:

```kotlin
composable<ExercisesRoute> {
    ExercisesScreen(
        onNavigateBack = { navController.navigateUp() },
        onNavigateToWorkout = { exercise ->
            navController.navigate(ExerciseWorkoutRoute(exerciseId = exercise.id))
        }
    )
}

// Phase 3 replaces this placeholder with the real Exercise workout screen.
composable<ExerciseWorkoutRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<ExerciseWorkoutRoute>()
    PlaceholderScreen(
        title = "Exercise Workout\nExercise ID: ${route.exerciseId}",
        buttonText = stringResource(R.string.action_go_back)
    ) {
        navController.navigateUp()
    }
}
```

Passing only `exerciseId` (not the full `ExerciseUiModel`) keeps the nav argument a
plain string — avoids needing a custom `NavType` the way `ProductDetailsRoute` needed one
for `ProductUiModel`. Phase 3's screen re-looks-up the exercise from the same mock data
source Phase 1 built, by id. Flag this choice explicitly in the Phase 2 handoff notes
below so Phase 3 doesn't assume the full model arrives via nav args.

### 3.5 Strings (EN + AR)

Add to both `values/strings.xml` and `values-ar/strings.xml`:
- `exercise_instructions_title` → "Instructions"
- `exercise_equipment_target` → `"%1$s • %2$s"` (format string, not literal concatenation)
- `action_read_more` → "read more"
- `action_start_workout` → "Start Workout"

(`exercises_title` already exists from an earlier pass — reuse it, do not re-add.)

### 3.6 Colors

Check `AppColorsExtension` first for anything semantically reusable (e.g. an existing
teal-on-light-teal pairing) before adding new tokens. If nothing fits, add a small new
block following the existing per-screen grouping convention (e.g. `// --- Exercise
Instructions Sheet ---`) with only what's genuinely new: sheet background/scrim (Material3
defaults may already cover this via `MaterialTheme.colorScheme.surface` — try that before
adding a custom token) and the circular illustration backdrop color if it differs from
whatever Phase 1 used for the list thumbnails.

## 4. Verification Plan

**Automated:** `ExercisesViewModelTest` additions (JUnit 5 + Turbine, per `SKILL.md` §4):
- `OnExerciseClick` sets `selectedExercise` and resets `isInstructionsExpanded` to false.
- `OnReadMoreClick` flips `isInstructionsExpanded` to true without changing
  `selectedExercise`.
- `OnDismissInstructions` clears `selectedExercise` back to null.
- `OnStartWorkoutClick` emits `NavigateToExerciseWorkout` carrying the currently
  selected exercise.

**Manual:** Compare rendered sheet against `Exercise-bottomsheet.png` (collapsed) and
`Exercise-bottomsheet-seemore.png` (expanded) in light + dark + Arabic (RTL layout of the
equipment/target row and the "read more" link position), for both a `CARDIO` and a
`NORMAL_WORKOUT` mock exercise (confirms the sheet itself doesn't branch on type — only
Phase 3's workout screen should).

# Exercise Workout Screen — Implementation Plan (Phase 3 of 4)

## Goal Description

Build the second screen of the Exercises feature: the **Exercise (workout) screen**
reached after the user taps "Start Workout" in Phase 2's instructions bottom sheet.

Scope of this phase, exactly:
1. `ExerciseWorkoutRoute` navigation entry + `NavGraph.kt` wiring.
2. `ExerciseWorkoutScreen` UI, matching `Exercise.png` / `Cardio-finish.png` /
   `normal-workout-finish.png` pixel-for-pixel in structure, spacing, and color/typography
   tokens (light + dark + RTL/Arabic).
3. Countdown/count-up timer with **Start → Restart/Pause** state machine.
4. "Cancel workout" navigation back to Daily Products (`CaloriesRoute`).
5. On Pause, branch the bottom UI by `ExerciseType` (Cardio vs Normal Workout):
   - Cardio → show `Finish` button directly under the timer (`Cardio-finish.png`).
   - Normal Workout → show `Sets` / `Reps` value cards **above** `Finish`
     (`normal-workout-finish.png`), rendered but **not yet interactive**.
6. `ExerciseWorkoutViewModel` (MVI) + unit tests per `SKILL.md` §4.

**Explicitly out of scope** (Phase 4's job — do not implement, only leave clean seams):
- The +/- steppers and manual-entry dialogs on the Sets/Reps cards.
- The kcal calculation (`kcalPerMin`/`kcalPerRep` math).
- The finish-session result dialog and any update to `ExerciseCard`/`CalorieGoalsCard`
  on the Daily Products screen.
- `Finish` button's `onClick` in this phase is a **no-op placeholder** event
  (`ExerciseWorkoutEvent.OnFinishClick`) that Phase 4 will implement fully.

## User Review Required

- Confirm timer direction: screenshots show a single static value (`02:22`) with no
  visible spec for count-up vs count-down or a target duration. **Assumption (please
  confirm/correct):** the timer **counts up from `00:00`** with no fixed end (open-ended
  workout duration), since no target duration attribute exists in the flow description.
  This total elapsed time in minutes is what Phase 4 will multiply into the kcal formula.
- Confirm the exact circle illustration behavior: the same `person-exercise` asset
  (added in Phase 1) is reused unscaled behind a static teal-tinted circle for both the
  "ready" and "paused" states — no animation — matching `Exercise.png` and
  `Cardio-finish.png`/`normal-workout-finish.png` (please confirm, since no animation was
  described).

## Dependencies on Phase 1 & Phase 2 (please verify at start of this phase)

- `ExerciseUiModel` (Phase 1) must exist with at least: `id`, `name`, `type:
  ExerciseType` (enum `CARDIO` / `NORMAL_WORKOUT`), `kcalPerMin: Double?`,
  `kcalPerRep: Double?`. This phase reads `type` only — never renders `kcalPerMin` /
  `kcalPerRep` (per `PROGRESS.md` global rule).
- Phase 2 hands off via an `ExerciseWorkoutRoute(exerciseId: String)` — id-only, per
  `PROGRESS.md`'s note under Phase 2. This phase's own `ExerciseWorkoutRoute` definition
  in `Route.kt` must match that shape; if Phase 2 already declared it, reuse it as-is
  rather than redeclaring.
- This phase re-fetches the full `ExerciseUiModel` from the same mock data source Phase
  1 created (by `exerciseId`), rather than having Phase 2 pass the whole model through
  navigation — keeps the nav argument small and avoids re-serializing instructions text.
- If any of the above don't match what Phase 1/2 actually built, adapt to what exists
  and note the deviation in the Phase 3 working notes in `PROGRESS.md` — don't block on
  it.

## Proposed Changes

### 1. Navigation (`app/src/main/kotlin/iti/grad/nutriscan/navigation`)

- `Route.kt`: add (if not already present from Phase 2)
  ```kotlin
  @Serializable
  data class ExerciseWorkoutRoute(val exerciseId: String)
  ```
- `NavGraph.kt`: add `composable<ExerciseWorkoutRoute> { backStackEntry -> ... }`
  wiring `ExerciseWorkoutScreen` with:
  - `onNavigateBack = { navController.navigate(CaloriesRoute) { popUpTo(CaloriesRoute) { inclusive = false } } }`
    — "Cancel workout" must land on Daily Products, not just pop one entry, since the
    user reached this screen via Exercises → bottom sheet → workout (popping once would
    land back on the bottom sheet/Exercises screen, not Daily Products, per the flow
    description).
  - Do **not** wire a "finish" navigation callback yet beyond the no-op event — Phase 4
    owns the finish flow's navigation back to `CaloriesRoute` with the result dialog.

### 2. Feature module layout (mirrors `news/`, `saved/` structure)

```
presentation/.../exercise_workout/
  state/
    ExerciseWorkoutState.kt
    ExerciseWorkoutEvent.kt
    ExerciseWorkoutEffect.kt
  viewmodel/
    ExerciseWorkoutViewModel.kt
  view/
    ExerciseWorkoutScreen.kt
    components/
      WorkoutTimerDisplay.kt
      WorkoutIllustrationCircle.kt
      WorkoutControlButtons.kt      // Restart + Pause row (ready/running state)
      SetsRepsValueCard.kt          // static display only this phase — see note below
```

`SetsRepsValueCard` is built as a **read-only display component** in this phase (title +
current value, no +/- buttons, no click handler) so Phase 4 can extend it in place
in `common/components/` if it turns out to be reusable elsewhere, or keep it
feature-local otherwise — leave that placement call to Phase 4 once the interactive
version is built, since it's premature to promote an unfinished component. Note this
decision explicitly in the Phase 3 working notes so Phase 4 doesn't miss it.

### 3. State (MVI)

`ExerciseWorkoutState.kt`:
```kotlin
data class ExerciseWorkoutState(
    val isLoading: Boolean = true,
    val exercise: ExerciseUiModel? = null,
    val timerSeconds: Int = 0,
    val timerStatus: TimerStatus = TimerStatus.READY,   // READY, RUNNING, PAUSED
    val sets: Int = 1,   // display-only seed value in this phase; Phase 4 makes it editable
    val reps: Int = 1,   // display-only seed value in this phase; Phase 4 makes it editable
)

enum class TimerStatus { READY, RUNNING, PAUSED }
```
- `READY`: initial state before first Start tap — screenshots show the timer at a
  non-zero value in the "ready" mock (`02:22`) purely as static Figma content; actual
  running state starts the count at `00:00`. Render `Start` as the button label when
  `timerStatus == READY` and `Restart`/`Pause` once `RUNNING` or `PAUSED` — confirm this
  reading of `Exercise.png` vs `Cardio-finish.png` against the live app rather than
  assuming the mock's `02:22` is a real starting value.
- `PAUSED` is what triggers the Cardio/Normal-Workout branching UI at the bottom.

`ExerciseWorkoutEvent.kt`:
```kotlin
sealed interface ExerciseWorkoutEvent {
    data object OnStartClick : ExerciseWorkoutEvent      // first tap, READY -> RUNNING
    data object OnRestartClick : ExerciseWorkoutEvent    // resets timerSeconds to 0, keeps RUNNING
    data object OnPauseClick : ExerciseWorkoutEvent      // RUNNING -> PAUSED
    data object OnCancelWorkoutClick : ExerciseWorkoutEvent
    data object OnFinishClick : ExerciseWorkoutEvent     // no-op placeholder — Phase 4 implements
}
```
- No sets/reps +/- events in this phase — those belong to Phase 4's `ExerciseWorkoutEvent`
  additions; don't guess their shape here to avoid churn when Phase 4 defines them.

`ExerciseWorkoutEffect.kt`:
```kotlin
sealed interface ExerciseWorkoutEffect {
    data object NavigateToDailyProducts : ExerciseWorkoutEffect
}
```

### 4. ViewModel

- Loads `ExerciseUiModel` by `exerciseId` (from the mock data source) in `init`.
- Runs the timer via a coroutine (`viewModelScope.launch { while(isActive) { delay(1000); ... } }`)
  gated by `timerStatus == RUNNING`, cancelled/relaunched on pause/restart — do not use
  `CountDownTimer`/Android framework timers, to stay consistent with the coroutine-based
  patterns already used elsewhere in `presentation/`.
- `OnCancelWorkoutClick` stops the timer coroutine and emits `NavigateToDailyProducts`.
- `OnFinishClick`, in this phase, only exists so the button is wireable; it can emit
  nothing or a `TODO`-marked effect — leave a clear `// Phase 4: ...` comment rather than
  silently swallowing it, so it's easy to find.

### 5. UI (`ExerciseWorkoutScreen.kt`)

Structure top to bottom, matching the screenshots:
- Top bar: `AppBackButton` (reuse, wired to `OnCancelWorkoutClick`'s same destination —
  confirm whether back-arrow and "Cancel workout" text should behave identically; the
  screenshots don't show a separate back action, so treat both as "leave workout") +
  `"Exercise"` title, `AppTheme.typography.titleMedium` (LexendDeca Medium 20sp — exact
  match already in `AppTypography.kt`, do not redeclare).
- Exercise name (`"Full Body Warm Up"`), `AppTheme.typography.headlineMedium`
  (PlusJakartaSans SemiBold 24sp — exact existing match, reuse).
- Illustration circle: teal-tinted circular background (`AppTheme.colors.Teal200` or
  nearest existing token — check `AppColors.kt` against the screenshot color, do not
  hardcode a hex) containing the `person-exercise` vector asset Phase 1 added — reuse
  that drawable resource by name, do not re-import the SVG.
- Timer: `"Total Time"` label (`AppTheme.typography.bodyMedium`, per existing 14sp
  regular token) + large time value. The `34sp/Bold` timer digits don't match any
  existing `AppTypography` slot (`displaySmall` is 28sp) — add a new one-off style
  following the established per-feature-object pattern (see `HomeTypography`,
  `CaloriesTypography`, `ProductDetailsTypography` in `AppTypography.kt`):
  ```kotlin
  object ExerciseWorkoutTypography {
      /** Running timer value ("02:22") — Plus Jakarta Sans Bold 34sp. */
      val timerValue = TextStyle(
          fontFamily = PlusJakartaSans,
          fontWeight = FontWeight.Bold,
          fontSize = 34.sp,
      )
      /** Restart / Pause / Cancel workout labels — Lexend Deca Medium 18sp. */
      val actionLabel = TextStyle(
          fontFamily = LexendDeca,
          fontWeight = FontWeight.Medium,
          fontSize = 18.sp,
      )
  }
  ```
  Confirm font family against the live Figma/theme before finalizing — the flow
  description gave weight/style/size but not family; `PlusJakartaSans` for numerals and
  `LexendDeca` for the action labels are inferred from which family the rest of the app
  uses for similar-weight roles (headlines vs. body/action text respectively).
- Controls row: two states —
  - `READY`: single full-width `Start` button (reuse `AppButton` shape/style if it fits
    a two-button row, otherwise a `Row` of teal-outlined "Restart"-style + filled
    "Pause"-style buttons matching `Exercise-bottomsheet` button styling conventions).
    Actually per `Exercise.png`, the ready state shows the same Restart/Pause row already
    active-looking — **confirm with the actual screenshots**: `Exercise.png` shows
    Restart+Pause even before any Start tap, meaning there may be no separate "Start"
    label at all and the button simply reads "Pause" once RUNNING starts automatically,
    or Start IS the initial Pause-row's right-hand button before first tap. Resolve this
    ambiguity by checking exact button text in `Exercise.png` vs `Cardio-finish.png`
    pixel content before writing the state machine's label mapping, since the two
    screenshots given for this phase both already show "Restart"/"Pause" — there may be
    no distinct "Start" label in this flow at all (i.e., `Restart` doubles as the initial
    action). Do not guess further — inspect the actual asset text at implementation time.
  - `RUNNING`: `Restart` (outlined, `AppTheme.colors.Teal1000` border/text) + `Pause`
    (filled `AppTheme.colors.Teal1000` background) — 2-button `Row`, per `Exercise.png`.
  - Below the row: `"Cancel workout"` text-only tap target, muted/secondary color,
    `ExerciseWorkoutTypography.actionLabel`.
- On `PAUSED`:
  - Cardio → `Finish` full-width button directly below timer (reuse `AppButton` if its
    puffed-shape styling matches `Cardio-finish.png`'s flat teal button; otherwise this
    is the existing "primary CTA" button already used for "Start Workout" in Phase 2 —
    reuse that exact composable rather than creating a second primary-button component).
  - Normal Workout → `Sets` card + `Reps` card (`SetsRepsValueCard`, read-only this
    phase) in a `Row` above the same `Finish` button, per `normal-workout-finish.png`.
    Card label typography: `AppTheme.typography.titleSmall` (LexendDeca Medium 16sp —
    exact existing match for `"Sets"`/`"Reps"`).

### 6. Strings (localize, EN + AR — no hardcoded text)

Add to `values/strings.xml` and `values-ar/strings.xml`:
`exercise_workout_title`, `exercise_total_time_label`, `exercise_start_button`,
`exercise_restart_button`, `exercise_pause_button`, `exercise_cancel_workout`,
`exercise_finish_button`, `exercise_sets_label`, `exercise_reps_label`.
(Reuse Phase 1/2's existing string keys for anything already added — e.g. don't
re-add a `"Full Body Warm Up"`-style key, that's exercise **data**, not a UI string.)

### 7. Colors

No new hex values. Use existing `AppTheme.colors.*` tokens for: illustration circle
background, Teal1000 primary actions, secondary/muted text for "Cancel workout" and the
timer's "Total Time" label (reuse whatever token the rest of the app uses for muted
labels — check `AppColors.kt` rather than assuming `TextSecondary` exists verbatim).

## Verification Plan

**Automated:**
- `ExerciseWorkoutViewModelTest` (JUnit 5 + Turbine + `StandardTestDispatcher`, per
  `SKILL.md` §4 template exactly):
  - `OnStartClick` transitions `READY -> RUNNING` and timer begins incrementing
    (advance `testScheduler` and assert `timerSeconds`).
  - `OnPauseClick` transitions `RUNNING -> PAUSED` and stops further increments.
  - `OnRestartClick` resets `timerSeconds` to 0 while remaining/returning to `RUNNING`.
  - `OnCancelWorkoutClick` emits `ExerciseWorkoutEffect.NavigateToDailyProducts` via
    `viewModel.effect.test { ... }`.
  - Loading an exercise with `type = CARDIO` vs `type = NORMAL_WORKOUT` is reflected
    correctly in `state.value.exercise?.type` (drives the branching UI — the branching
    condition itself is a Composable-level `when`, so this test only needs to confirm
    the state carries the right type through, not the UI branch).

**Manual (against provided screenshots, light + dark + Arabic/RTL):**
- `Exercise.png` — ready/running row layout, spacing, illustration placement.
- `Cardio-finish.png` — Finish button appears directly under timer, no Sets/Reps cards.
- `normal-workout-finish.png` — Sets/Reps cards render above Finish, correct labels,
  static value `1`/`1` (no working +/- yet — confirm buttons are visibly present per the
  screenshot but non-functional is acceptable for this phase, OR omit the +/- icons
  entirely until Phase 4 if their presence-but-inertness would be a confusing partial
  state — **flag this UX call explicitly** in the Phase 3 working notes for Phase 4 to
  see, don't decide it silently).
- "Cancel workout" from both `RUNNING` and `PAUSED` states returns to Daily Products.
- No hardcoded strings/colors/text styles/dp values anywhere in the new files — grep the
  new package for raw hex codes and string literals in `Text(...)` calls before marking
  this phase done.

# Phase 4 — Sets/Reps Controls, Calorie Calculation & Daily Products Completion

**Feature:** Exercises (Phase 4 of 4 — final phase)
**Depends on:** Phase 1 (`ExerciseUiModel`, mock data, `ExercisesScreen`), Phase 2
(instructions bottom sheet, `Start Workout` nav), Phase 3 (`ExerciseWorkoutScreen`:
timer, Start/Restart/Pause, Cancel workout, Cardio-vs-Normal branching UI on pause).
This phase does **not** modify Phase 1–3 UI beyond the specific hook points listed below.

---

## Goal Description

On the Exercise (workout) screen, after the user pauses and the type-specific UI is
showing (Cardio: Finish button only — `Cardio-finish.png`; Normal Workout: Sets/Reps
cards + Finish button — `normal-workout-finish.png`):

1. Make the **Sets** and **Reps** value cards interactive:
   - `+` / `-` buttons increment/decrement the value.
   - Tapping the card body (not the +/- buttons) opens a dialog to type a value
     manually.
2. On **Finish** tap, compute burnt calories from the mock `kcalPerMin` /
   `kcalPerRep` attribute on the exercise (never rendered to the user — see Global
   Rules) and the elapsed timer, per the formula confirmed in "User Review Required"
   below.
3. Show a result dialog with the burnt-calories value.
4. On dialog dismiss, navigate back to the Daily Products screen (`CaloriesScreen`)
   and reflect the session there:
   - `ExerciseCard` (`common/components/ExerciseCard.kt`): add the session's burnt
     kcal to `exerciseKcal`, add the elapsed minutes to `exerciseMinutes`.
   - `CalorieGoalsCard`'s "Calories Gained" stat: subtract the burnt kcal from
     `caloriesGained` (per the product ask — burning calories reduces net gained).

---

## User Review Required

**Please confirm before implementation — the calorie formula as dictated has an
internal contradiction:**

- The flow description states the backend will eventually return `kcalPerRep` for
  Normal Workout exercises (e.g. `0.3`), implying the calculation must use it.
- But the calculation instruction given is: *"multiply sets × reps × timer_minutes
  (normal workout)"* and *"kcalPerMin × timer_minutes (cardio)"* — this omits
  `kcalPerRep` entirely from the normal-workout side, and instead uses the same
  `timer_minutes` factor as cardio.

Two readings are possible; **Phase 4 will implement reading (A) unless told otherwise**,
since it's the only one that actually uses the `kcalPerRep` attribute the product
description says exists for exactly this purpose:

- **(A) Recommended:** `burntKcal = sets × reps × kcalPerRep` for Normal Workout
  (timer not a factor — mirrors cardio's `kcalPerMin × timer_minutes`, where
  `kcalPerMin` is cardio's "per unit" attribute and `timer_minutes` is cardio's
  "count" attribute; for Normal Workout the analogous "count" is `sets × reps`, and
  the analogous "per unit" is `kcalPerRep`).
- **(B) As literally written:** `burntKcal = sets × reps × timer_minutes` for Normal
  Workout, and `kcalPerRep` is carried on the mock data but unused in Phase 4's math
  (left for a future backend-driven revision).

Also confirm:
- **Rounding:** result dialog should show a whole-number kcal (e.g. round to nearest
  int) — confirm this is acceptable, since `kcalPerMin`/`kcalPerRep` are fractional.
- **`caloriesGained` floor:** should the subtraction be clamped at 0 (never negative),
  given `caloriesGained` is also used for the goals progress bar ratio in
  `CalorieGoalsCard`? Recommended: yes, clamp at 0.
- **Minimum Finish guard:** should Finish be disabled/no-op if `sets == 0` or
  `reps == 0` (Normal Workout) or `timer == 00:00` (both types), to avoid a 0-kcal
  or nonsensical session? Recommended: allow it (burntKcal will just be 0) rather than
  disabling — simplest for a mock-data UI phase, but flag if product wants a guard.

---

## Cross-Screen State Hand-off (architecture note — read first)

`ExerciseWorkoutViewModel` (Phase 3) and `CaloriesViewModel` (existing,
`presentation/main/calories/viewmodel/CaloriesViewModel.kt`) are **separate Hilt
ViewModels with no shared repository** — the whole feature is mock-data/UI-only per
`PROGRESS.md`'s Global Rules, and `CaloriesState.exerciseKcal` /
`exerciseMinutes` are currently static mock defaults (`250`, `45`) with no
persistence layer feeding them.

Since there is no backend/DB in scope for this feature, Phase 4 must pass the
session result (`burntKcal: Int`, `elapsedMinutes: Int`) back across the nav graph
using **`SavedStateHandle` nav-result passing** (the standard Navigation-Compose
pattern: the popped destination sets a value on the previous back-stack entry's
`SavedStateHandle`, which the destination observes). Do not introduce a new
singleton/shared ViewModel or an in-memory repository for this — that would be a
architecture decision bigger than one feature and isn't sanctioned by
`PROGRESS.md`'s Global Rules.

Concretely:
1. `ExerciseWorkoutViewModel`, in its finish-confirmed handler, calls
   `navController.previousBackStackEntry?.savedStateHandle?.set("exercise_session_result", ExerciseSessionResult(burntKcal, elapsedMinutes))`
   (or the ViewModel emits an effect carrying the result and the Composable does the
   `savedStateHandle` write — keep the ViewModel effect-only per `AGENTS.md`/Phase
   3's own pattern of "ViewModel never holds a NavController reference").
2. `CaloriesScreen`/`CaloriesViewModel` reads it once via
   `savedStateHandle.getStateFlow<ExerciseSessionResult?>("exercise_session_result", null)`
   in `CaloriesViewModel`'s `init` (Hilt ViewModel already supports `SavedStateHandle`
   injection), applies it to `exerciseKcal`/`exerciseMinutes`/`caloriesGained`, then
   clears the key so it isn't re-applied on process/config changes or re-entry.
3. Define `ExerciseSessionResult` as a small `@Serializable`/`Parcelable` data class
   (whichever the existing nav-result convention in this codebase uses — check how
   other screens pass results back, e.g. `ProductDetailsRoute`'s `ProductUiModelNavType`
   for a working example of custom nav type-safety in this codebase) with just
   `burntKcal: Int` and `elapsedMinutes: Int`. Do not put `kcalPerMin`/`kcalPerRep`
   in it — those must never leave the exercise-workout layer (Global Rule).

If, on inspecting the actual Phase 3 implementation, a cleaner pattern already exists
(e.g. Phase 3 already added an effect type for "workout finished" that Phase 4 can
extend), prefer reusing it over introducing a parallel mechanism — check Phase 3's
actual code and `ExerciseWorkoutEffect` before building the hand-off from scratch.

---

## Proposed Changes

### 1. Sets/Reps interactive controls
- Locate `SetsRepsValueCard` (built statically/non-interactive in Phase 3 — see
  Phase 3's "Left for Phase 4" notes in `PROGRESS.md` for its current location).
  Decide its final home (`common/components/` vs. feature-local) now that it's
  interactive and confirm/record that decision in this phase's `PROGRESS.md` notes.
- Add `+` / `-` tap handlers wired to new `ExerciseWorkoutEvent`s (e.g.
  `SetsIncremented`, `SetsDecremented`, `RepsIncremented`, `RepsDecremented`) —
  clamp at a minimum of 0 (or 1 — confirm with whatever Phase 3 defaulted the
  starting value to; screenshots show starting value `1`).
- Add a tap-to-edit affordance on the card body (distinct from the +/- buttons'
  click targets) that opens a manual-entry dialog (reuse an existing app dialog
  component if one exists for numeric entry — check `common/components/` before
  building a new one, per Global Rules) pre-filled with the current value, confirming
  writes back via a `SetsManuallyEntered(value: Int)` / `RepsManuallyEntered(value: Int)`
  event.

### 2. Finish → calorie calculation
- In `ExerciseWorkoutViewModel`, on `FinishClicked` (the `// Phase 4:` marked
  placeholder from Phase 3 — see `PROGRESS.md` Phase 3 notes):
  - Read `exercise.kcalPerMin` or `exercise.kcalPerRep` from the mock model
    depending on `exercise.type`, plus current `sets`/`reps`/elapsed timer seconds
    from state.
  - Compute `burntKcal` per the formula confirmed in "User Review Required" above.
  - Round for display per the confirmed rounding rule.
  - Update state to show the result dialog (`ExerciseWorkoutState.resultDialogKcal:
    Int?` or similar) — do not navigate yet.

### 3. Result dialog
- New composable (check for an existing generic result/info dialog in
  `common/components/` first — e.g. something like `ConfirmationDialog` used in
  `CaloriesScreen` may be adaptable, or a new `ExerciseResultDialog` may be
  warranted if the visual doesn't match). Must use `AppTheme.typography`/
  `AppTheme.colors`/`stringResource` exclusively — no hardcoded strings/colors/styles
  (Global Rules). Add string keys for the dialog title/body/confirm button to both
  `values/strings.xml` and `values-ar/strings.xml`.
- On dialog confirm/dismiss, trigger the nav-result hand-off (see architecture note
  above) and navigate back to `CaloriesRoute` (pop back stack to it — it should
  already be on the back stack under `ExercisesRoute` → `ExerciseWorkoutRoute`, so a
  simple `navigateUp()` twice or a `popUpTo<CaloriesRoute>` is likely correct; verify
  the actual back-stack shape Phase 2/3 produced before choosing).

### 4. Daily Products (`CaloriesScreen`) update on return
- In `CaloriesViewModel`, add `SavedStateHandle` to the constructor (Hilt supports
  this out of the box) and, in `init`, collect the nav-result key described above.
- On receiving a result:
  - `exerciseKcal = exerciseKcal + burntKcal`
  - `exerciseMinutes = exerciseMinutes + elapsedMinutes`
  - `caloriesGained = (caloriesGained - burntKcal)` clamped per the confirmed floor
    rule.
  - Clear the `SavedStateHandle` key after applying, so it isn't reapplied.
- No changes needed to `ExerciseCard.kt` or `CalorieGoalsCard.kt` composables
  themselves — they already render off `CaloriesState` fields; this phase only
  changes how those fields get updated.

### 5. Strings / theme / dimens
- Every new string (dialog copy, any content descriptions for new icon buttons)
  goes in both `values/strings.xml` and `values-ar/strings.xml` — no exceptions.
- No new colors — reuse `AppTheme.colors.*`. No new text styles beyond what Phase 3
  already added to the centralized typography file, unless the result dialog needs
  one not yet defined (check first).
- Follow the existing inline-`.dp`-literal convention noted in `PROGRESS.md`'s
  Global Rules (no invented `Dimens` object).

---

## Verification Plan

**Automated:**
- `ExerciseWorkoutViewModelTest` additions (JUnit 5 + Turbine + coroutines-test,
  per `SKILL.md` §4): sets/reps increment, decrement, clamp-at-minimum, manual entry;
  Finish computes correct `burntKcal` for both a Cardio and a Normal Workout mock
  exercise at known timer/sets/reps values; result dialog state populated correctly.
- `CaloriesViewModelTest` additions: receiving a nav-result updates `exerciseKcal`,
  `exerciseMinutes`, and `caloriesGained` correctly, including the clamp-at-0 case,
  and the key is cleared after one application (doesn't double-apply on
  recomposition/rotation).

**Manual:**
- Full flow: Exercises list → tap Cardio exercise (#1) → bottom sheet → Start
  Workout → run timer → Pause → Cardio-finish UI → Finish → result dialog → Daily
  Products shows updated `ExerciseCard` kcal/minutes and reduced Calories Gained.
- Repeat for the Normal Workout exercise (#2), including using +/- and the
  manual-entry dialog on both Sets and Reps before hitting Finish.
- Verify in light + dark + Arabic (RTL) locale.
- Confirm `kcalPerMin`/`kcalPerRep` never appear anywhere in rendered UI (grep the
  new composables to be sure no `Text` accidentally surfaces them, even in debug/
  dev builds).

---

## Handoff

This is the last phase. On completion, fill in this phase's section in
`PROGRESS.md` (format matches Phase 1's), note the final formula/rounding/clamp
decisions actually implemented (in case they differed from this doc after user
review), the final location of `SetsRepsValueCard`, and any deviations from this
plan and why.

