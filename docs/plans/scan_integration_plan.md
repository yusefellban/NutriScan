# Scan Integration — Camera Capture → Backend API → Result Display

Integrate the new backend scan API into the camera screen. Replace barcode scanning with photo capture, upload to `/v1/scans`, poll `/api/v1/scans/{scanId}` every 3 seconds **continuously** until `COMPLETED`, and display the result card. The bookmark button will save the scan locally to a Room database for later integration into the "Saved" tab.

## User Review Required

> [!IMPORTANT]
> **AuthInterceptor Fix Required**: The current `AuthInterceptor` hardcodes `Content-Type: application/json` on **every** request. This breaks multipart (image upload) requests because OkHttp needs to set `Content-Type: multipart/form-data; boundary=...` automatically. The plan removes the hardcoded `Content-Type` header from the interceptor — Retrofit's converter factory already sets it correctly per-request.

> [!WARNING]
> **Barcode scanning disabled**: Per your instruction, the `ImageAnalysis` barcode scanner in `CameraPreview` will be replaced with `ImageCapture` for photo capture.

## Resolved Questions

1. **Bookmark**: The bookmark button will save the `ScanResult` locally to the Room DB (`SavedScanEntity`).
2. **Product Name**: Left empty/hidden per your instruction.
3. **Polling Timeout**: Removed. The app will continue polling every 3 seconds indefinitely until the status is no longer `PROCESSING`.
4. **Nutrition Facts**: Added to models and DTOs according to the exact schema provided.

---

## Proposed Changes

### Domain Layer — New Models

#### [NEW] [ScanStatus.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/model/ScanStatus.kt)
```kotlin
enum class ScanStatus { PROCESSING, COMPLETED, FAILED }
```

#### [NEW] [NutritionFacts.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/model/NutritionFacts.kt)
```kotlin
data class NutritionFacts(
    val calories: Long,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatG: Float,
    val fiberGrams: Float,
    val sugarG: Float,
    val sodiumMg: Float
)
```

#### [NEW] [FoodSafetyResponse.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/model/FoodSafetyResponse.kt)
```kotlin
data class FoodSafetyResponse(
    val verdict: ProductVerdict?, // Mapped from SAFE, CAUTION, UNSAFE
    val flaggedIngredients: List<FlaggedIngredient>,
    val summary: String?,
)
```
*Note: `FlaggedIngredient` will be updated to match `{ ingredient, reason, type, name: List<String> }`.*

#### [NEW] [ScanResult.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/model/ScanResult.kt)
```kotlin
data class ScanResult(
    val scanId: String,
    val status: ScanStatus,
    val scannedAt: String?,
    val imageUrl: String?,
    val foodSafetyResponse: FoodSafetyResponse?,
    val nutritionFacts: NutritionFacts?
)
```

---

### Domain Layer — Repository Interfaces & Use Cases

#### [MODIFY] [IScanRepository.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/repository/IScanRepository.kt)
```kotlin
suspend fun submitScanImage(imageFile: File): Result<ScanResult>
suspend fun getScanResult(scanId: String): Result<ScanResult>
```

#### [NEW] [ISavedScanRepository.kt](file:///c:/Users/DELL/Documents/NutriScan/domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/repository/ISavedScanRepository.kt)
```kotlin
suspend fun saveScan(scanResult: ScanResult): Result<Unit>
// Future: suspend fun getSavedScans(): Flow<List<ScanResult>>
```

#### Use Cases
- **[NEW]** `SubmitScanImageUseCase.kt`: Uploads image, returns initial `ScanResult`.
- **[NEW]** `GetScanResultUseCase.kt`: Gets scan by ID.
- **[NEW]** `SaveScanUseCase.kt`: Saves a completed `ScanResult` to Room.

---

### Data Layer — Remote DTOs

#### [NEW] [FoodSafetyResponseDto.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/FoodSafetyResponseDto.kt)
```kotlin
@Serializable
data class FlaggedIngredientDto(
    val ingredient: String = "",
    val reason: String = "",
    val type: String = "",
    val name: List<String> = emptyList()
)

@Serializable
data class FoodSafetyResponseDto(
    val verdict: String? = null,
    val flaggedIngredients: List<FlaggedIngredientDto> = emptyList(),
    val summary: String? = null,
)

@Serializable
data class NutritionFactsDto(
    val calories: Long = 0,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatG: Float = 0f,
    val fiberGrams: Float = 0f,
    val sugarG: Float = 0f,
    val sodiumMg: Float = 0f
)
```

#### [NEW] [ScanResultResponseDto.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/ScanResultResponseDto.kt)
```kotlin
@Serializable
data class ScanResultResponseDto(
    val scanId: String,
    val status: String,
    val scannedAt: String? = null,
    val imageUrl: String? = null,
    val foodSafetyResponse: FoodSafetyResponseDto? = null,
    val nutritionFacts: NutritionFactsDto? = null
)
```

---

### Data Layer — Local Database (Room)

#### [NEW] [SavedScanEntity.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/db/entity/SavedScanEntity.kt)
Stores the bookmarked scan details locally for the "Saved" tab.

#### [NEW] [SavedScanDao.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/db/dao/SavedScanDao.kt)
```kotlin
@Dao
interface SavedScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: SavedScanEntity)
    // Future queries...
}
```

#### [MODIFY] [NutriScanDatabase.kt](file:///c:/Users/DELL/Documents/NutriScan/data/src/main/kotlin/iti/grad/nutriscan/data/db/NutriScanDatabase.kt)
Add `SavedScanEntity` to `@Database` entities and provide `savedScanDao()`. Add Room TypeConverters for `FoodSafetyResponse` and `NutritionFacts` JSON mapping.

---

### App Layer — DI Wiring

#### [MODIFY] [NetworkModule.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/NetworkModule.kt)
Provide `ScanApiService`.

#### [MODIFY] [DatabaseModule.kt](file:///c:/Users/DELL/Documents/NutriScan/app/src/main/kotlin/iti/grad/nutriscan/di/DatabaseModule.kt)
Provide `SavedScanDao`.

---

### Presentation Layer — Camera Capture & UI

#### [MODIFY] [CameraPreview.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/CameraPreview.kt)
- Replace `ImageAnalysis` (ML Kit barcode) with `ImageCapture`.
- Call `takePicture()` and return `File` when capture is triggered.

#### [NEW] [CaptureButton.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/CaptureButton.kt)
Bottom-center circular shutter button for taking photos.

#### [MODIFY] [ActiveScanCard.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/ActiveScanCard.kt)
- Thumbnail from `imageUrl`.
- Product name is left empty (or showing only the verdict/summary).
- Verdict badge.
- Bookmark button triggers Room save.

#### [MODIFY] [CameraScanViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/viewmodel/CameraScanViewModel.kt)
- **Continuous Polling**: Start polling every 3 seconds using `delay(3000)`. Do not time out. Loop until `status != PROCESSING`.
- **Bookmarking**: `onBookmarkClicked` calls `SaveScanUseCase(currentScan)` and updates UI state to filled bookmark icon.

---

## File Change Summary

| Layer | Component | Action |
|-------|------|--------|
| **Domain** | `ScanStatus`, `NutritionFacts`, `FoodSafetyResponse`, `ScanResult` | NEW |
| **Domain** | `IScanRepository`, `ISavedScanRepository` | MODIFY/NEW |
| **Domain** | `SubmitScanImageUseCase`, `GetScanResultUseCase`, `SaveScanUseCase` | NEW |
| **Data** | `ScanSubmissionResponseDto`, `FoodSafetyResponseDto`, `ScanResultResponseDto` | NEW |
| **Data** | `ScanApiService`, `ScanMapper` | NEW |
| **Data** | `AuthInterceptor` (Fix Content-Type issue) | MODIFY |
| **Data** | `SavedScanEntity`, `SavedScanDao` | NEW |
| **Data** | `NutriScanDatabase` | MODIFY |
| **App** | `NetworkModule`, `DatabaseModule` | MODIFY |
| **Presentation** | `CameraPreview`, `CaptureButton` | MODIFY/NEW |
| **Presentation** | `ActiveScanUiModel`, `CameraScanState`, `Event`, `Effect` | MODIFY |
| **Presentation** | `CameraScanViewModel`, `CameraScanScreen`, `ActiveScanCard` | MODIFY |
| **Presentation** | `strings.xml` | MODIFY |
