# Saved Screen — UI Implementation Plan

## Goal Description
Implement the **Saved (Bookmarks) screen** for NutriScan AI. This screen displays a grid of products the user has bookmarked/saved, with a search bar at the top and a swipe-to-add-to-shopping-list affordance on each item. The product card should be a **reusable component** living in `presentation/common/components/` so it can be reused by the History screen, Shopping List, and any other feature that shows product cards.

This is a **UI-only** implementation — no backend or Room integration. The screen will be populated with mock data from the ViewModel.

The implementation strictly adheres to Clean Architecture, MVI, and the rules defined in `AGENTS.md`.

---

## Technical Constraints & Safety Rules
- **Localization**: No hardcoded string literals in composables. All texts via `stringResource(R.string.xxx)` with both `values/strings.xml` and `values-ar/strings.xml`.
- **Theming**: No hardcoded colors. All colors via `AppTheme.colors.*`.
- **MVI Contract & Immutability**: All collections in UI states use `kotlinx.collections.immutable` (`ImmutableList`).
- **Reusability**: The product item card must be a generic `ProductCard` composable in `common/components/`, not coupled to the Saved screen's state model.

---

## Design Reference (from screenshots)

The screen has:
1. **Search bar** at the top — rounded, with a teal search icon button on the right.
2. **Product grid** — 2-column `LazyVerticalGrid` of product cards.
3. **Each product card** contains:
   - Product image (rounded top corners, fills the card width).
   - Product name (e.g., "Milk Product").
   - A health verdict badge (e.g., "Safe" in green/teal).
   - Calorie count (e.g., "180 Kcal").
   - A "Swipe right to add" indicator row with an arrow icon.
4. **Bottom navigation bar** — reuses `AppBottomNavBar` with `SHOPPING` tab selected (the bookmark icon tab).

---

## Proposed Changes

### Resources

#### [MODIFY] `presentation/src/main/res/values/strings.xml`
Add:
```xml
<!-- Saved Screen -->
<string name="saved_screen_title">Saved</string>
<string name="saved_search_hint">Search here</string>
<string name="saved_empty_state">No saved products yet</string>
<string name="product_card_swipe_hint">Swipe right to add</string>
<string name="product_card_kcal_unit">Kcal</string>
```

#### [MODIFY] `presentation/src/main/res/values-ar/strings.xml`
Add Arabic translations for all the above strings.

#### [NEW] `presentation/src/main/res/drawable/ic_search.xml`
Search icon vector drawable for the search bar button.

---

### Presentation — Reusable Component

#### [NEW] `presentation/.../common/components/ProductCard.kt`
A **reusable** product card composable:

```kotlin
@Composable
fun ProductCard(
    imageUrl: String?,
    productName: String,
    verdictLabel: String,
    calories: String,
    @StringRes swipeHintResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

**Design details:**
- Rounded corners (`16.dp`), white/surface background, teal shadow.
- Product image at the top using `AsyncImage` (Coil 3), with `ContentScale.Crop` and rounded top corners.
- Below image: product name text (`titleMedium`, `PrimaryVariant` color).
- Row with verdict badge (teal background pill, white text) and calorie text (teal box, bold).
- Bottom row: arrow icon + "Swipe right to add" text in subtle gray.
- The entire card is clickable.

This composable is **decoupled from any feature-specific model** — it takes raw primitives so any feature can call it with whatever data it has.

---

### Presentation — Saved Feature

#### [NEW] `presentation/.../saved/state/SavedProductUiModel.kt`
UI model for a single saved product:
```kotlin
data class SavedProductUiModel(
    val id: String,
    val productName: String,
    val imageUrl: String?,
    val verdictLabel: String,
    val calories: String,
)
```

#### [NEW] `presentation/.../saved/state/SavedState.kt`
```kotlin
data class SavedState(
    val searchQuery: String = "",
    val products: ImmutableList<SavedProductUiModel> = persistentListOf(),
    val filteredProducts: ImmutableList<SavedProductUiModel> = persistentListOf(),
    val selectedTab: BottomNavTab = BottomNavTab.SHOPPING,
)
```

#### [NEW] `presentation/.../saved/state/SavedEvent.kt`
```kotlin
sealed interface SavedEvent {
    data class SearchQueryChanged(val query: String) : SavedEvent
    data class ProductClicked(val productId: String) : SavedEvent
    data class SwipeToAddClicked(val productId: String) : SavedEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : SavedEvent
}
```

#### [NEW] `presentation/.../saved/state/SavedEffect.kt`
```kotlin
sealed interface SavedEffect {
    data object NavigateToHome : SavedEffect
    data object NavigateToHistory : SavedEffect
    data object NavigateToScan : SavedEffect
    data object NavigateToProfile : SavedEffect
    data class NavigateToProductDetail(val productId: String) : SavedEffect
    data class ShowAddedToListSnackbar(val productName: String) : SavedEffect
}
```

#### [NEW] `presentation/.../saved/viewmodel/SavedViewModel.kt`
- `@HiltViewModel` with mock data for now.
- Handles `SearchQueryChanged` by filtering the products list (case-insensitive match on product name).
- Handles `BottomNavTabClicked` by emitting navigation effects.
- Handles `SwipeToAddClicked` by emitting `ShowAddedToListSnackbar`.
- Initializes with 6 mock products (matching the screenshot design).

#### [NEW] `presentation/.../saved/view/SavedScreen.kt`
Stateful MVI composable:
- Collects `SavedState` from `SavedViewModel`.
- Handles `SavedEffect` for navigation and snackbar.
- Delegates to `SavedScreenContent` (stateless).

#### [NEW] `presentation/.../saved/view/components/SavedSearchBar.kt`
Custom search bar with:
- Rounded text field with placeholder "Search here".
- Teal circular search icon button on the right.
- Emits `SearchQueryChanged` on text change.

#### [NEW] `presentation/.../saved/view/components/SavedProductGrid.kt`
`LazyVerticalGrid` (2 columns) rendering `ProductCard` for each item. Uses the reusable `ProductCard` from `common/components/`.

---

### Navigation

#### [NEW] `app/.../navigation/Route.kt` — add `SavedRoute`
```kotlin
@Serializable
data object SavedRoute
```

#### [MODIFY] `app/.../navigation/NavGraph.kt`
Wire `SavedScreen` into the NavHost. Replace the `ShoppingListRoute` placeholder at comment `// 18.` with a real `SavedScreen` composable, connecting:
- `onNavigateToHome` → `HomeRoute`
- `onNavigateToScan` → `CameraScanRoute`
- `onNavigateToHistory` → `ScanHistoryRoute`
- `onNavigateToProfile` → `UserProfileRoute`

Also update the `HomeScreen` wiring so that the Shopping/Bookmark bottom nav tab navigates to `SavedRoute` instead of `ShoppingListRoute`.

---

## Open Questions

> [!IMPORTANT]
> 1. **Bottom nav tab naming**: The bottom nav currently labels the bookmark tab as `SHOPPING` in the `BottomNavTab` enum. Should I rename it to `SAVED` (since this is the Saved screen), or keep `SHOPPING` to match the existing AGENTS.md folder structure? I recommend renaming to `SAVED` since that matches the Figma design, but want your call.

> [!IMPORTANT]
> 2. **Swipe-to-add gesture**: The screenshots show "Swipe right to add" text. For now (UI-only), should I implement an actual swipe gesture (e.g., `SwipeToDismiss` or `AnchoredDraggable`) or just make it a tap button with the text as a visual hint? I recommend starting with a simple tap for now and adding the swipe gesture later when we wire up the shopping list backend.

---

## Verification Plan

### Automated Tests
#### [NEW] `presentation/src/test/.../saved/SavedViewModelTest.kt`
- Verify initial mock data populates `filteredProducts`.
- Verify `SearchQueryChanged` filters products correctly.
- Verify `BottomNavTabClicked` emits correct navigation effects.

### Manual Verification
- Deploy to emulator/device.
- Toggle between light/dark modes — all colors from `AppTheme.colors`.
- Switch to Arabic locale — all text translates correctly and layout mirrors.
- Verify bottom nav highlights the bookmark tab.
- Verify search bar filters the grid in real-time.
