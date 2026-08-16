# Daily Products card — match iOS dimensions/layout

## Why
iOS `DailyProductsSection.swift` (CalorieMealCard) ships a redesigned card:
160x116dp footprint, always-visible quantity pill, rounded-rect kcal badge,
tinted circular action buttons, 2-line product name. Android's
`FoodLogItemCard` (112x84dp) predates it. Port the iOS visual spec to
Android using existing `AppColors` tokens — no new colors.

## Scope
Single file: `presentation/common/components/FoodLogItemCard.kt`
(`FoodLogItemCard` + `CompactAddFoodCard`). Call site
(`CaloriesScreen.kt`) unchanged — same params, same outer dashed box.

## Target spec (from iOS, mapped to existing AppColors tokens)
- Card: 160x116dp, corner 18dp, padding 10dp, background
  `ProductCardBackground`, border 1dp `Gray300`/`Teal1300` (reuse existing
  `Divider`-style token already in AppColors — no new token).
- Image: 44x44dp, corner 12dp (was 34dp/8dp).
- Name: 2 lines (was 1), `ProductCardNameText`, bold, 11sp.
- Quantity pill: always shown (was gated on qty>1), `Teal200`/`Teal1600` bg/text.
- Kcal badge: rounded-rect corner 8dp (was pill), `ProductCardCaloriesBackground`/`ProductCardCaloriesText`.
- Action buttons: 26dp circle (was 18dp), tinted background + colored icon
  (minus: neutral `Gray200`/`Gray800`; delete: `ErrorBackground`/`Error`) —
  matches iOS soft-tint style, replaces current solid `Warning`/`Error` fill.
  Minus button stays conditional on `quantity > 1` (matches iOS
  `mealCnt > 1` gate).
- `CompactAddFoodCard`: resize to 96x116dp to match iOS `AddFoodCarouselCard`.

## Test impact
No ViewModel/state change — pure Composable visual change. No
`*ViewModelTest.kt` update needed. No new strings.

---

## Addendum — Saved-screen swipe-to-add animation (iOS parity)

Source: root `FavoriteCardView.swift` + `SwipeToActionButton.swift` (iOS
"Favorites" swipe slider). Android's `SwipeActionButton` in
`ProductCard.kt` currently drags, checks a 70% threshold, fires
`onTriggered()` fire-and-forget, and always springs back immediately —
no submitting/success feedback, no signal for whether the add actually
succeeded.

### Scope (4 files)
1. `ProductCardSwipeAction.kt` — `onTriggered` becomes
   `(onResult: (Boolean) -> Unit) -> Unit` so the button can react to the
   real API result instead of guessing.
2. `ProductCard.kt` (`SwipeActionButton`) — add a 3-phase state machine
   (`Idle` / `Submitting` / `Success`), mirroring iOS:
   - Drag past 50% (matching iOS `maxDrag * 0.5`, tightened from Android's
     current 70%) → snap thumb to track end, phase = Submitting, spinner
     icon, label → "Adding…", call `onTriggered(::onResult)`.
   - `onResult(true)` → phase = Success: thumb width animates to fill the
     track, checkmark icon, brief scale-up pulse (`spring`), label →
     "Added", track tint shifts to `Primary.copy(alpha=0.24f)`. Auto-resets
     to Idle after ~900ms.
   - `onResult(false)` → springs straight back to Idle (no success state).
   - All transitions use `spring()` `Animatable`, matching the existing
     `Animatable`-based drag in this file (no new animation primitive).
3. `SavedProductGrid.kt` — thread the `onResult` callback through
   `onSwipeToAdd`.
4. `SavedViewModel.kt` / `SavedEvent.kt` — `SwipeToAddTriggered` gets a new
   `onResult: (Boolean) -> Unit = {}` param (defaulted, existing
   `SwipeToAddTriggered("id")` call sites/tests keep compiling);
   `addToFoodLog` invokes it after `addFoodEntryUseCase` resolves, in
   addition to the existing snackbar effects (kept for accessibility /
   TalkBack, which won't see the inline animation).

### New strings
`product_card_swipe_adding` / `product_card_swipe_added` — English +
Arabic, added to both `strings.xml`.

### Icon
Success checkmark uses `Icons.Default.Check` (core Material icons,
already used elsewhere in this module) — no new drawable, no new
dependency.

### Test impact
`SavedViewModelTest.kt` — add a case asserting `onResult` is invoked with
`true`/`false` matching the use-case result. No new `ViewModelTest` file
needed (existing `SavedViewModelTest` already covers `SwipeToAddTriggered`).
