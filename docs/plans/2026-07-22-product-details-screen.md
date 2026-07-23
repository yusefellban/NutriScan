# Product Details Screen — Implementation Plan

## Goal

Build the **Product Details** screen that displays when a user taps any product card in the **Saved Screen** or **Calories Screen**. The screen shows detailed nutritional information, safety verdict, flagged ingredients with allergy/condition reasons, and macro breakdown — matching the provided Figma mockup.

---

## Figma Reference (Mockup Summary)

The screen consists of these visual sections, top-to-bottom:

1. **Top Bar**: Back button (← existing `AppBackButton`) + "Product Details" title + Bookmark icon
2. **Product Image**: Full-width rounded card showing product photo (Coil `AsyncImage`)
3. **Product Name + Scan Date**: Left-aligned name, right-aligned "Scanned at YYYY-MM-DD"
4. **Verdict Badge Row**: "Unsafe" / "Caution" / "Safe" badge + "For you" label
5. **Safety Reason**: Bullet text explaining why the product is flagged (e.g., "Contains hazelnuts and milk, both of which match allergies on your profile.")
6. **"Why It's Unsafe?" Section**: Section header + horizontally scrollable cards — each card shows:
   - Flagged ingredient name (e.g. "Hazelnuts")
   - Matching allergy/condition tag (e.g. "Tree Nuts Allergy")
   - Reason text (e.g. "Matches allergy in your profile")
7. **Nutritional Info Row**: Horizontally scrollable row of macro badges: Calories, Serving, Sugar, Fat, Sat. Fat — each with teal pill

---

## Entry Points (Navigation)

Products can be tapped from two screens:

| Source | Current callback | Current behavior |
|---|---|---|
| **SavedScreen** | `onNavigateToProductDetail(productId: String)` | `/* No-op for now */` in NavGraph |
| **CaloriesScreen** | Product card `onClick = {}` | No-op lambda — no nav event wired |

**IMPORTANT:** The **CaloriesScreen** currently passes `onClick = {}` for food items (line 182 of `CaloriesScreen.kt`). We need to add a `ProductClicked` event to `CaloriesEvent` and wire it through the ViewModel + NavGraph.

---

## Open Questions

**Q1: Data source for full product details?**
Currently `ProductUiModel` only has `id`, `productName`, `imageUrl`, `verdict`, and `calories`. The mockup requires additional fields: **serving size, sugar, fat, saturated fat, flagged ingredients with allergy tags, scan date, and safety reason text**.

**Proposed approach (mock-first):** For this iteration, we'll create a `ProductDetail` domain model with all these fields and populate the ViewModel with **mock/hardcoded data** (same pattern as the existing `SavedViewModel.loadMockData()`). This lets us build and validate the complete UI now, then swap in real API data later when the backend is ready.

Do you agree with mock-first, or would you prefer to extend the OpenFoodFacts DTO to fetch nutritional data too?

**Q2: Bookmark functionality.** The mockup shows a bookmark icon in the top bar. Should tapping it save/unsave the product to the Saved catalog? For now I'll wire it as a visual toggle in state (no persistence), unless you want full Room persistence.

---

## Proposed Changes

### Domain Layer — `domain/scan/model/`

**[NEW] ProductDetail.kt**

New domain model holding everything needed for the detail screen:

```kotlin
data class ProductDetail(
    val id: String,
    val productName: String,
    val brand: String?,
    val imageUrl: String?,
    val verdict: ProductVerdict,
    val scanDate: LocalDate?,
    val safetyReasonText: String?,           // "Contains hazelnuts and milk..."
    val flaggedIngredients: List<FlaggedIngredient>,
    val calories: String?,
    val servingSize: String?,
    val sugar: String?,
    val fat: String?,
    val saturatedFat: String?,
    val isBookmarked: Boolean,
)

data class FlaggedIngredient(
    val name: String,                        // "Hazelnuts"
    val matchTag: String,                    // "Tree Nuts Allergy"
    val reason: String,                      // "Matches allergy in your profile"
)
```

---

### Presentation Layer — Product Details Feature

**[NEW] `presentation/product_details/state/ProductDetailsState.kt`**

```kotlin
@Immutable
data class ProductDetailsState(
    val isLoading: Boolean = true,
    val productDetail: ProductDetail? = null,
    val error: String? = null,
)
```

**[NEW] `presentation/product_details/state/ProductDetailsEvent.kt`**

```kotlin
sealed interface ProductDetailsEvent {
    data object BackClicked : ProductDetailsEvent
    data object BookmarkToggled : ProductDetailsEvent
}
```

**[NEW] `presentation/product_details/state/ProductDetailsEffect.kt`**

```kotlin
sealed interface ProductDetailsEffect {
    data object NavigateBack : ProductDetailsEffect
}
```

**[NEW] `presentation/product_details/viewmodel/ProductDetailsViewModel.kt`**

- `@HiltViewModel`, injected with `SavedStateHandle` to read the `productId` route arg
- Uses mock data initially (same approach as `SavedViewModel`)
- Exposes `StateFlow<ProductDetailsState>` + `Channel<ProductDetailsEffect>`

**[NEW] `presentation/product_details/view/ProductDetailsScreen.kt`**

Top-level composable with:
- Top bar (AppBackButton + title + bookmark icon)
- `LazyColumn` body with all sections
- Effect collection for navigation

**[NEW] `presentation/product_details/view/components/` — Sub-components:**

| Component | Purpose |
|---|---|
| `ProductDetailsTopBar.kt` | Back button + "Product Details" title + bookmark icon |
| `ProductImageCard.kt` | Full-width rounded image card |
| `ProductInfoHeader.kt` | Name + scan date + verdict badge row + safety text |
| `FlaggedIngredientCard.kt` | Single ingredient card in the "Why It's Unsafe?" section |
| `FlaggedIngredientsRow.kt` | Horizontal scrollable row of `FlaggedIngredientCard` |
| `NutritionFactsRow.kt` | Horizontal row of macro pills (Calories, Serving, Sugar, Fat, Sat. Fat) |

---

### Navigation Layer

**[MODIFY] Route.kt** (`app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt`)

- **Replace** the existing `ProductDetailsPlaceholderRoute(barcode: String)` with a proper `ProductDetailsRoute(productId: String)` 
- Keep the barcode-based route as an alias or remove the placeholder

**[MODIFY] NavGraph.kt** (`app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`)

- Replace the `PlaceholderScreen` for `ProductDetailsPlaceholderRoute` with the real `ProductDetailsScreen`
- Wire the **SavedScreen** `onNavigateToProductDetail` callback → `navController.navigate(ProductDetailsRoute(productId))`
- Add `onNavigateToProductDetail` callback to **CaloriesScreen** wiring

---

### Calories Screen Integration

**[MODIFY] CaloriesEvent.kt**

Add: `data class FoodItemClicked(val productId: String) : CaloriesEvent`

**[MODIFY] CaloriesEffect.kt**

Add: `data class NavigateToProductDetail(val productId: String) : CaloriesEffect`

**[MODIFY] CaloriesViewModel.kt**

Handle `FoodItemClicked` → emit `NavigateToProductDetail`

**[MODIFY] CaloriesScreen.kt**

- Add `onNavigateToProductDetail: (String) -> Unit` parameter
- Change `onClick = {}` (line 182) to dispatch `CaloriesEvent.FoodItemClicked(food.id)`
- Handle `CaloriesEffect.NavigateToProductDetail` in the effect collector

---

### Theme & Colors

**[MODIFY] AppColors.kt — AppColorsExtension**

Add Product Details-specific semantic colors (both light and dark):

| Token | Light | Dark | Usage |
|---|---|---|---|
| `ProductDetailImageCardBg` | Gray100 | Teal1500 | Image card background |
| `ProductDetailScanDate` | Gray700 | Teal400 | "Scanned at" text |
| `ProductDetailSafetyText` | Gray1400 | Teal100 | Safety reason bullet text |
| `ProductDetailIngredientCardBg` | Teal100 | Teal1500 | Flagged ingredient card bg |
| `ProductDetailIngredientCardBorder` | Teal1000 | Teal1300 | Card border |
| `ProductDetailIngredientName` | Error | Error | Red ingredient name text |
| `ProductDetailMatchTag` | Teal1000 | Teal400 | Allergy/condition pill |
| `ProductDetailNutritionPill` | Teal1000 | Teal1000 | Macro pill background |
| `ProductDetailNutritionText` | OnPrimary | Teal100 | Macro pill text |
| `ProductDetailBookmarkTint` | Teal1000 | Teal400 | Bookmark icon tint |

---

### String Resources

**[MODIFY] `presentation/src/main/res/values/strings.xml`**

Add new entries:

```xml
<string name="product_details_title">Product Details</string>
<string name="product_details_scanned_at">Scanned at</string>
<string name="product_details_for_you">For you</string>
<string name="product_details_why_unsafe">Why It\'s Unsafe?</string>
<string name="product_details_why_caution">Why Caution?</string>
<string name="product_details_why_safe">Why It\'s Safe?</string>
<string name="product_details_calories">Calories</string>
<string name="product_details_serving">Serving</string>
<string name="product_details_sugar">Sugar</string>
<string name="product_details_fat">Fat</string>
<string name="product_details_sat_fat">Sat. Fat</string>
<string name="product_details_bookmark_add">Save product</string>
<string name="product_details_bookmark_remove">Remove from saved</string>
```

**[MODIFY] `presentation/src/main/res/values-ar/strings.xml`**

Arabic translations for above strings.

---

### Drawable Assets

**[NEW] `ic_bookmark_outlined.xml`** — Outlined bookmark icon (top bar, unsaved state)
**[NEW] `ic_bookmark_filled.xml`** — Filled bookmark icon (top bar, saved state)

*(Vector drawables — can be created from Material Icons or from the Figma export)*

---

## File Summary

| Layer | Action | File |
|---|---|---|
| Domain | NEW | `domain/scan/model/ProductDetail.kt` |
| Presentation | NEW | `presentation/product_details/state/ProductDetailsState.kt` |
| Presentation | NEW | `presentation/product_details/state/ProductDetailsEvent.kt` |
| Presentation | NEW | `presentation/product_details/state/ProductDetailsEffect.kt` |
| Presentation | NEW | `presentation/product_details/viewmodel/ProductDetailsViewModel.kt` |
| Presentation | NEW | `presentation/product_details/view/ProductDetailsScreen.kt` |
| Presentation | NEW | `presentation/product_details/view/components/ProductDetailsTopBar.kt` |
| Presentation | NEW | `presentation/product_details/view/components/ProductImageCard.kt` |
| Presentation | NEW | `presentation/product_details/view/components/ProductInfoHeader.kt` |
| Presentation | NEW | `presentation/product_details/view/components/FlaggedIngredientCard.kt` |
| Presentation | NEW | `presentation/product_details/view/components/FlaggedIngredientsRow.kt` |
| Presentation | NEW | `presentation/product_details/view/components/NutritionFactsRow.kt` |
| Presentation | MODIFY | `CaloriesEvent.kt`, `CaloriesEffect.kt`, `CaloriesViewModel.kt`, `CaloriesScreen.kt` |
| Theme | MODIFY | `AppColors.kt` (extension — add Product Detail tokens) |
| Strings | MODIFY | `strings.xml`, `strings.xml (ar)` |
| Navigation | MODIFY | `Route.kt`, `NavGraph.kt` |
| Drawables | NEW | `ic_bookmark_outlined.xml`, `ic_bookmark_filled.xml` |

---

## Verification Plan

### Build Check
```bash
./gradlew assembleDebug
```

### Manual Verification
1. Launch app → navigate to **Saved Screen** → tap any product card → verify Product Details opens with correct data
2. Navigate to **Calories Screen** → add a food → tap the food card → verify Product Details opens
3. Tap back button → verify navigation returns to source screen
4. Tap bookmark icon → verify visual toggle
5. Verify dark mode renders correctly
6. Verify RTL layout (Arabic) renders correctly
