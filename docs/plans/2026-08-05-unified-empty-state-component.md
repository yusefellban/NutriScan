# Unified Reusable Empty State Component

## Goal Description

The codebase currently has **7 separate empty/failure state composables** that are nearly identical in structure but copy-pasted across different features with inconsistent sizing, spacing, and typography. This plan creates a **single reusable `AppEmptyStateWidget`** in `common/components/` that accepts the illustration, title, subtitle, and optional button — then migrates all existing callers to use it.

This eliminates ~350 lines of duplicated UI code and guarantees every empty/error/not-found state in the app uses the same icon size, font style, spacing, and button treatment.

---

## User Review Required

> [!IMPORTANT]
> This is a **pure UI refactor** — no behavior changes. Every screen will look and behave exactly as before, but with consistent sizing across all states.

> [!IMPORTANT]
> The old widget files will be **deleted** after migration. All imports in calling screens will change to point to the new shared `AppEmptyStateWidget`.

---

## Current Problem — Inconsistency Audit

| Widget | Location | Icon Size | Title Style | Subtitle Alpha | Spacing | Button |
|--------|----------|-----------|-------------|----------------|---------|--------|
| `EmptyStateWidget` | `common/components/` | 64.dp | `bodyLarge` | 0.5f | 16dp | ❌ None |
| `NotFoundStateWidget` | `common/components/` | unset (intrinsic) | `titleLarge` 32.sp | 0.5f | 24/8/32dp | ✅ |
| `OfflineStateWidget` | `common/components/` | fillMaxWidth | `titleLarge` | 0.5f | 12/32dp | ✅ |
| `SearchNotFoundEmptyStateWidget` | `common/components/` | unset (intrinsic) | `titleLarge` | 0.5f | 24/8/32dp | Optional |
| `ScanHistoryEmptyStateWidget` | `scan_history/components/` | unset (intrinsic) | `titleLarge` | 0.5f | 24/8/32dp | ✅ |
| `SavedEmptyStateWidget` | `saved/components/` | unset (intrinsic) | `titleLarge` | 0.5f | 24/8/32dp | ✅ |
| `CaloriesHistoryEmptyState` | `calories_history/components/` | 306.dp | `titleMedium` SemiBold | secondary color | 24/8/32dp | ✅ |

**Problems:** Icon sizes range from 64dp to 306dp to intrinsic. Title styles mix `bodyLarge`, `titleMedium`, `titleLarge`. Spacing is inconsistent.

---

## Proposed Changes

### 1. New Unified Component

#### [NEW] [AppEmptyStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/AppEmptyStateWidget.kt)

A single `@Composable` function with the following signature:

```kotlin
@Composable
fun AppEmptyStateWidget(
    @DrawableRes lightImageRes: Int,
    @DrawableRes darkImageRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
)
```

**Standardized design tokens:**
- **Image size:** fixed at `220.dp` width/height (fits all illustrations uniformly)
- **Image-to-title spacing:** `24.dp`
- **Title style:** `AppTheme.typography.titleLarge`, `FontWeight.Bold`, `AppTheme.colors.TextPrimary`
- **Title-to-subtitle spacing:** `8.dp`
- **Subtitle style:** `AppTheme.typography.bodyLarge`, `AppTheme.colors.TextSecondary`
- **Subtitle-to-button spacing:** `32.dp`
- **Button:** Uses existing `AppButton` (only rendered when `buttonText != null`)
- **Layout:** `Column` with `fillMaxSize`, `horizontalAlignment = CenterHorizontally`, `verticalArrangement = Center`, `padding(horizontal = 32.dp)`

The composable internally picks `lightImageRes` or `darkImageRes` based on `AppTheme.isDark`.

---

### 2. Migration — Delete Old Widgets & Update Callers

#### [DELETE] [EmptyStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/EmptyStateWidget.kt)

This generic widget with just `message` is unused except potentially internally. Will be replaced by `AppEmptyStateWidget`.

---

#### [DELETE] [NotFoundStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/NotFoundStateWidget.kt)

**Callers to migrate:**
- `ProductDetailsScreen.kt` (line 146)

**Migration:** Replace with:
```kotlin
AppEmptyStateWidget(
    lightImageRes = R.drawable.not_found_light,
    darkImageRes = R.drawable.not_found_dark,
    title = stringResource(R.string.not_found_title),
    subtitle = stringResource(R.string.not_found_subtitle),
    buttonText = stringResource(R.string.try_again),
    onButtonClick = { /* existing retry lambda */ },
)
```

---

#### [DELETE] [OfflineStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/OfflineStateWidget.kt)

**Callers to migrate (8 usages):**
- `SavedScreen.kt` → `onRetry = { onEvent(SavedEvent.RetryLoad) }`
- `ScanHistoryScreen.kt` → `onRetry = { onEvent(ScanHistoryEvent.RetryLoad) }`
- `ProductDetailsScreen.kt` → existing retry lambda
- `NutriGptScreen.kt` → existing retry lambda
- `NewsScreen.kt` → 2 usages
- `HomeScreen.kt` → existing retry lambda
- `NewsHomeScreen.kt` → existing retry lambda
- `ExercisesScreen.kt` → existing retry lambda

**Migration:** Replace each with:
```kotlin
AppEmptyStateWidget(
    lightImageRes = R.drawable.no_network_connection_light,
    darkImageRes = R.drawable.no_network_connection_dark,
    title = stringResource(R.string.offline_state_title),
    subtitle = stringResource(R.string.offline_state_subtitle),
    buttonText = stringResource(R.string.offline_state_retry),
    onButtonClick = { /* existing retry lambda */ },
)
```

---

#### [DELETE] [SearchNotFoundEmptyStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/common/components/SearchNotFoundEmptyStateWidget.kt)

**Callers to migrate (3 usages):**
- `SavedScreen.kt` — with button
- `NewsScreen.kt` — no button
- `ExercisesScreen.kt` — no button

**Migration:** Replace each with:
```kotlin
AppEmptyStateWidget(
    lightImageRes = R.drawable.search_reasult_not_found_light,
    darkImageRes = R.drawable.search_reasult_not_found_dark,
    title = stringResource(R.string.search_not_found_title),
    subtitle = stringResource(R.string.search_not_found_subtitle),
    buttonText = if (showButton) stringResource(R.string.search_not_found_button) else null,
    onButtonClick = if (showButton) onScanNowClick else null,
)
```

---

#### [DELETE] [ScanHistoryEmptyStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/view/components/ScanHistoryEmptyStateWidget.kt)

**Callers to migrate:**
- `ScanHistoryScreen.kt` (line 131)

**Migration:**
```kotlin
AppEmptyStateWidget(
    lightImageRes = R.drawable.no_scans_yet_light,
    darkImageRes = R.drawable.no_scans_yet_dark,
    title = stringResource(R.string.scan_history_empty_title),
    subtitle = stringResource(R.string.scan_history_empty_subtitle),
    buttonText = stringResource(R.string.scan_history_empty_button),
    onButtonClick = onScanNowClick,
)
```

---

#### [DELETE] [SavedEmptyStateWidget.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/saved/view/components/SavedEmptyStateWidget.kt)

**Callers to migrate:**
- `SavedScreen.kt` (line 174)

**Migration:**
```kotlin
AppEmptyStateWidget(
    lightImageRes = R.drawable.saved_not_found_light,
    darkImageRes = R.drawable.saved_not_found_dark,
    title = stringResource(R.string.saved_empty_title),
    subtitle = stringResource(R.string.saved_empty_subtitle),
    buttonText = stringResource(R.string.saved_empty_button),
    onButtonClick = onScanNowClick,
)
```

---

#### [DELETE] [CaloriesHistoryEmptyState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/calories_history/view/components/CaloriesHistoryEmptyState.kt)

**Callers to migrate:**
- `CaloriesHistoryScreen.kt` (line 177)

**Migration:**
```kotlin
AppEmptyStateWidget(
    lightImageRes = R.drawable.ic_no_calories_history_light,
    darkImageRes = R.drawable.ic_no_calories_history_dark,
    title = stringResource(R.string.calories_history_empty_title),
    subtitle = stringResource(R.string.calories_history_empty_subtitle),
    buttonText = stringResource(R.string.calories_history_empty_button),
    onButtonClick = onAddMealsClick,
)
```

---

### 3. Caller Screen Modifications

#### [MODIFY] [ProductDetailsScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/product_details/view/ProductDetailsScreen.kt)
- Replace `NotFoundStateWidget` and `OfflineStateWidget` imports → `AppEmptyStateWidget`

#### [MODIFY] [SavedScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/saved/view/SavedScreen.kt)
- Replace `OfflineStateWidget`, `SearchNotFoundEmptyStateWidget`, `SavedEmptyStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [ScanHistoryScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan_history/view/ScanHistoryScreen.kt)
- Replace `OfflineStateWidget`, `ScanHistoryEmptyStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [NutriGptScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/nutrigpt/chat/view/NutriGptScreen.kt)
- Replace `OfflineStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [NewsScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/view/NewsScreen.kt)
- Replace `OfflineStateWidget`, `SearchNotFoundEmptyStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [NewsHomeScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/news/home/view/NewsHomeScreen.kt)
- Replace `OfflineStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [HomeScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/HomeScreen.kt)
- Replace `OfflineStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [ExercisesScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/exercises/view/ExercisesScreen.kt)
- Replace `OfflineStateWidget`, `SearchNotFoundEmptyStateWidget` → `AppEmptyStateWidget`

#### [MODIFY] [CaloriesHistoryScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/calories_history/view/CaloriesHistoryScreen.kt)
- Replace `CaloriesHistoryEmptyState` → `AppEmptyStateWidget`

---

## Summary of Changes

| Action | Count |
|--------|-------|
| **New files** | 1 (`AppEmptyStateWidget.kt`) |
| **Deleted files** | 7 (old widgets) |
| **Modified files** | 9 (caller screens) |
| **Lines removed (approx)** | ~350 |
| **Lines added (approx)** | ~60 (new component) + ~100 (inline calls) |

---

## Verification Plan

### Build Check
```bash
./gradlew :presentation:compileDebugKotlin
```
- Ensures no broken imports after deletion

### Manual Verification
- Open each affected screen in the app and verify the empty/error/offline state renders correctly with the same illustration and layout
- Test both light and dark themes for each state
