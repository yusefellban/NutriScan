# Continuous Scanning & OpenFoodFacts Integration

**Date:** 2026-07-21
This plan outlines the integration of the OpenFoodFacts API to display scanned product data in real-time without stopping the camera. We'll also update the UI to match the requested design and add navigation to a placeholder screen.

## Proposed Changes

### 1. Domain Layer (`domain` module)
- **[NEW]** `domain/scan/model/ProductResult.kt`: Model representing the product data (name, brand, image URL, health tag).
- **[NEW]** `domain/scan/repository/IScanRepository.kt`: Repository interface containing `suspend fun getProductByBarcode(barcode: String): Result<ProductResult>`.
- **[NEW]** `domain/scan/usecase/GetProductByBarcodeUseCase.kt`: UseCase to execute the network request.

### 2. Data Layer (`data` module)
- **[NEW]** `data/remote/api/OpenFoodFactsApiService.kt`: Retrofit service with `@GET("https://world.openfoodfacts.org/api/v2/product/{barcode}")`.
- **[NEW]** `data/remote/dto/OpenFoodFactsResponseDto.kt`: DTO to parse the JSON response.
- **[NEW]** `data/repository/ScanRepositoryImpl.kt`: Implementation of the repository.
- **[MODIFY]** `app/di/NetworkModule.kt` & `app/di/RepositoryModule.kt`: Add DI bindings for the new API service and repository.

### 3. Presentation Layer (`presentation` module)
- **[MODIFY]** `presentation/scan/camera/viewmodel/CameraScanViewModel.kt`: 
  - Inject `GetProductByBarcodeUseCase`.
  - Handle `BarcodeDetected` by launching a coroutine to fetch data.
  - Keep `isScanning = true` to allow continuous scanning.
  - Avoid redundant calls by tracking the `lastScannedBarcode`.
  - Cancel the previous API request if a new barcode is detected.
- **[MODIFY]** `presentation/scan/camera/state/CameraScanState.kt`: Update `ActiveScanUiModel` to hold the new product data.
- **[MODIFY]** `presentation/scan/camera/view/components/ActiveScanCard.kt`: Re-design the card to match the screenshot (teal background, product image, title, health tag, and a large "+" button).
- **[NEW]** `presentation/scan/placeholder/view/PlaceholderScreen.kt`: A simple placeholder screen displaying a message.
- **[MODIFY]** `app/navigation/Route.kt` & `app/navigation/NavGraph.kt`: Add the `PlaceholderRoute`.

## User Feedback Integrated
- The scanner will instantly cancel the previous request and fetch the new one if a different barcode is detected.
- The "Health" tag will be mapped from `ecoscore_grade` or `nutrition_grades_tags` as provided by OpenFoodFacts.
