# Refactor ProductDetailsScreen to use scanId

Currently, the `ProductDetailsScreen` expects a full `ProductUiModel` to be passed via navigation arguments, serialized as a JSON string. This violates the principle of passing minimal IDs between screens and makes it impossible to open the details screen from the Recent Scans list (which only provides a `scanId`).

This plan outlines the refactoring needed to make `ProductDetailsScreen` take a `scanId` and fetch the data on its own.

## Proposed Changes

### Navigation (`app` module)

#### [MODIFY] `app/src/main/kotlin/iti/grad/nutriscan/navigation/Route.kt`
- Change `ProductDetailsRoute` from `data class ProductDetailsRoute(val product: ProductUiModel)` to `data class ProductDetailsRoute(val scanId: String)`.
- Delete `ProductUiModelNavType` as it will no longer be used.

#### [MODIFY] `app/src/main/kotlin/iti/grad/nutriscan/navigation/NavGraph.kt`
- Remove `typeMap` from the `ProductDetailsRoute` composable declaration.
- Remove `ScanResultRoute` and replace its usages with `ProductDetailsRoute` if applicable (since `ProductDetailsScreen` acts as the final scan result screen).

### Presentation Container

#### [MODIFY] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/container/view/MainScreen.kt`
- Update `onNavigateToProductDetail` signature from `(ProductUiModel) -> Unit` to `(String) -> Unit`.
- Update invocations inside `MainScreen` to map the `ProductUiModel` from child screens to its ID (e.g., `onNavigateToProductDetail = { uiModel -> onNavigateToProductDetail(uiModel.id) }`).
- Wire `HomeScreen`'s `onNavigateToScanResult` directly to `onNavigateToProductDetail`.

### Product Details Feature

#### [MODIFY] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/product_details/viewmodel/ProductDetailsViewModel.kt`
- Update `SavedStateHandle` reading from `savedStateHandle["product"]` to `savedStateHandle["scanId"]`.
- Remove the `buildFallbackDetail` logic, which relied on the previously injected `ProductUiModel`.
- Update `loadProductDetail` to fetch data via `getSavedScanByIdUseCase(scanId)`. If not found locally, fetch from API via `getScanResultUseCase(scanId)`. If both fail, push an error state to the UI.

### Home Screen Feature

#### [MODIFY] `presentation/src/main/kotlin/iti/grad/nutriscan/presentation/home/view/HomeScreen.kt`
- Replace `HomeEffect.NavigateToScanResult` handling to trigger the new `onNavigateToProductDetail` (via the existing `onNavigateToScanResult` lambda in `HomeScreen`).

## Verification Plan

### Automated Tests
- Gradle build: `./gradlew :app:assembleDebug`

### Manual Verification
- Click a history item on the Home Screen. Verify that the Product Details Screen opens and properly fetches the details from the backend/DB.
- Navigate to the Saved tab and click an item. Verify it still opens properly using its ID.
