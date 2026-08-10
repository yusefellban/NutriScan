# Feature Plan: Barcode Scan Tab with Real-Time AR Tracking Overlay

| Field | Value |
|---|---|
| **Feature** | Barcode Scan tab with AR bounding-box tracking overlay |
| **Module(s) touched** | `data`, `domain`, `presentation` |
| **Author** | NutriScan AI Team |
| **Status** | ✅ Approved — ready for implementation |

---

## 1. Problem Statement

The existing `CameraScanScreen` supports **Photo** and **Gallery** modes for food-label scanning. A **Barcode** scan mode is needed so users can point the camera at a product barcode and have it analysed automatically — without pressing a shutter button.

To maximise UX quality, the camera feed should display a real-time **AR overlay** that draws a bracket frame directly over the detected barcode's bounding box, tracks the barcode smoothly as the camera or product moves, and transitions to a "locked" green state immediately before auto-submission.

---

## 2. Goals

- ✅ Add a fully-functional **Barcode** tab to the existing `ScanModeSelector` (repurposing the previously disabled `QR` tab).
- ✅ Implement real-time barcode detection using **Google ML Kit Barcode Scanning** (already declared as a dependency).
- ✅ Draw an **AR bounding-box overlay** using Jetpack Compose `Canvas` with spring-animated coordinates that elastically track the barcode in real-time.
- ✅ Auto-submit the detected barcode after **1.5 s of stability** (debounced lock), hitting the new backend endpoint.
- ✅ Feed the barcode submission into the **existing polling + `ActiveScanCard` pipeline** — identical to the photo flow.

---

## 3. Non-Goals

- ❌ A separate QR code mode (the QR tab is **completely repurposed** as Barcode).
- ❌ Manual shutter tap to confirm a barcode — auto-lock handles submission.
- ❌ Offline barcode lookup — the full AI pipeline is always used.

---

## 4. Architecture Overview

This feature follows **Clean Architecture + MVI** exactly as enforced by the project rules.

```
ML Kit ImageAnalysis (CameraX)
        │
        │  Barcode + BoundingBox (on analysis thread)
        ▼
BarcodeScanAnalyzer  ──►  CameraScanEvent.BarcodeDetected  ──►  ViewModel
                                                                       │
                                    ┌──────────────────────────────────┘
                                    │ after 1.5 s stability
                                    ▼ CameraScanEvent.BarcodeLocked
                             SubmitBarcodeScanUseCase
                                    │
                                    ▼
                             IScanRepository.submitBarcodeScan()
                                    │
                                    ▼
                             ScanApiService  POST /v1/scans/barcode
                                    │
                                    ▼
                             pollScanResult() ← existing, unchanged
                                    │
                                    ▼
                             ActiveScanCard (same photo-flow UI)
```

### Module dependency rules — unchanged

```
presentation  →  domain  only
data          →  domain  only
app           →  presentation + data
domain        →  nothing
```

---

## 5. API Contract

### 5.1 Submit Barcode Scan

```
POST https://nutriscan.dev/api/v1/scans/barcode
Content-Type: application/json

{ "barcode": "5922157657516" }
```

**Response (202 Accepted):**
```json
{
  "scanId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "PROCESSING"
}
```

The `scanId` is then polled via the **existing** `GET /v1/scans/{scanId}` endpoint — no changes to polling logic.

---

## 6. Detailed Implementation Plan

### 6.1 Data Layer

#### 6.1.1 NEW — `BarcodeScanRequestDto.kt`

```
data/src/main/kotlin/iti/grad/nutriscan/data/remote/dto/BarcodeScanRequestDto.kt
```

A minimal `@Serializable` data class wrapping the `barcode` string for the POST body.

#### 6.1.2 MODIFY — `ScanApiService.kt`

Add one new suspend function:

```kotlin
@POST("v1/scans/barcode")
suspend fun submitBarcodeScan(@Body body: BarcodeScanRequestDto): ScanSubmissionResponseDto
```

Reuses the existing `ScanSubmissionResponseDto` — no new DTO needed for the response.

#### 6.1.3 MODIFY — `ScanRepositoryImpl.kt`

Implement `IScanRepository.submitBarcodeScan(barcode)`:
- Delegates to `scanApiService.submitBarcodeScan(BarcodeScanRequestDto(barcode))`.
- Maps the response via the existing `ScanSubmissionResponseDto.toDomain()` mapper.
- Wraps in `Result<ScanResult>` following the same try/catch pattern as `submitScanImage`.
- All work performed on `@IoDispatcher` via `withContext`.

---

### 6.2 Domain Layer

#### 6.2.1 MODIFY — `IScanRepository.kt`

Add:
```kotlin
/**
 * Submit a barcode string for AI analysis via POST /v1/scans/barcode.
 *
 * ⚠️ Health-critical: Returns a [ScanResult] in PROCESSING status.
 * The caller MUST poll until COMPLETED before surfacing any verdict.
 * Never infer safety from the initial PROCESSING status.
 */
suspend fun submitBarcodeScan(barcode: String): Result<ScanResult>
```

#### 6.2.2 NEW — `SubmitBarcodeScanUseCase.kt`

```
domain/src/main/kotlin/iti/grad/nutriscan/domain/scan/usecase/SubmitBarcodeScanUseCase.kt
```

Simple delegation use case — identical structure to `SubmitScanImageUseCase`.

---

### 6.3 Presentation Layer

#### 6.3.1 MODIFY — `ScanInputMode.kt`

Rename `QR` → `BARCODE`. Update `labelResId` and `hintResId` to new string resources.

#### 6.3.2 MODIFY — `CameraScanState.kt`

Add:
```kotlin
val detectedBarcodeBounds: RectF? = null   // normalised [0,1]; null = no barcode in frame
val trackedBarcodeValue: String?  = null   // raw barcode digits being tracked
```

> Storing **normalised** coords keeps the ViewModel display-size-agnostic (Clean Architecture).

#### 6.3.3 MODIFY — `CameraScanEvent.kt`

Add:
```kotlin
data class BarcodeDetected(val barcode: String?, val normalizedBounds: RectF?) : CameraScanEvent
data class BarcodeLocked(val barcode: String) : CameraScanEvent
```

#### 6.3.4 NEW — `BarcodeScanAnalyzer.kt`

```
presentation/.../scan/camera/view/components/BarcodeScanAnalyzer.kt
```

Implements `ImageAnalysis.Analyzer`:
- Configures ML Kit for EAN-13, EAN-8, UPC-A, UPC-B, Code-128, and QR formats.
- Normalises `BoundingBox` pixel coordinates to `[0,1]` relative to image dimensions.
- Calls `onResult(barcode, normalizedBounds)` on every frame; passes `null` when no barcode is found.
- Always calls `imageProxy.close()` in `addOnCompleteListener` to prevent frame stalls.

#### 6.3.5 NEW — `BarcodeArOverlay.kt`

```
presentation/.../scan/camera/view/components/BarcodeArOverlay.kt
```

Composable drawn over the camera preview in BARCODE mode:

| Concern | Approach |
|---|---|
| **Coordinate mapping** | Normalised bounds × `BoxWithConstraints` size → display pixels |
| **Smooth tracking** | Four `Animatable<Float>` (left/top/right/bottom) animated with `Spring.StiffnessMediumLow` |
| **Appear/disappear** | `Animatable<Float>` alpha, `tween(200)` |
| **Lock colour transition** | `animateColorAsState` white → green on `isLocked` |
| **Visual** | Corner brackets (same style as existing `ScanFrameOverlay`) + pill badge showing barcode digits |

#### 6.3.6 MODIFY — `CameraPreview.kt`

Accept optional `barcodeAnalyzer: ImageAnalysis.Analyzer?` parameter. When non-null, build and bind an `ImageAnalysis` use case with `STRATEGY_KEEP_ONLY_LATEST` alongside the existing `Preview` + `ImageCapture` bindings.

#### 6.3.7 MODIFY — `CameraScanScreen.kt`

- `remember` a `BarcodeScanAnalyzer` when `selectedMode == BARCODE`, dispose it when switching away.
- Pass `barcodeAnalyzer` to `CameraPreview`.
- Render `BarcodeArOverlay` above the preview in BARCODE mode.
- Route `BarcodeDetected` and `BarcodeLocked` events to `onEvent`.

#### 6.3.8 MODIFY — `CameraScanViewModel.kt`

- Inject `SubmitBarcodeScanUseCase`.
- `handleBarcodeDetected`: update state bounds/value; debounce with a `barcodeLockJob` (`delay(1_500)` → `BarcodeLocked`); cancel on `null` or new barcode value.
- `handleBarcodeLocked`: guard against re-submission; call `submitBarcode()`.
- `submitBarcode()`: mirrors `submitImageFile()` — sets processing state, calls use case, on success updates `activeScan.scanId`, then calls existing `pollScanResult()`.
- Remove the early-return guard blocking `QR` (now `BARCODE`) in `handleModeSelected`.

#### 6.3.9 MODIFY — `ScanModeSelector.kt`

- Enable all modes (`isModeEnabled = true`).
- Update icon for `BARCODE` to a barcode-specific vector.

---

## 7. String Resources

| Key | English value |
|---|---|
| `scan_mode_barcode` | `Barcode` |
| `scan_mode_barcode_hint` | `Point at a product barcode` |

---

## 8. End-to-End User Flow

```
User taps "Barcode" tab
    │
    ▼
CameraX binds:  Preview + ImageCapture + ImageAnalysis (BarcodeScanAnalyzer)
    │
    │  Every frame (STRATEGY_KEEP_ONLY_LATEST):
    ▼
BarcodeScanAnalyzer → BarcodeDetected(barcode, normalizedBounds)
    │
    ▼
ViewModel: updates state → BarcodeArOverlay tracks barcode with spring animation
    │
    │  Same barcode stable for 1.5 s:
    ▼
BarcodeLocked(barcode) → submitBarcode() → POST /v1/scans/barcode
    │
    ▼
Response { scanId, status: "PROCESSING" }
    │
    ▼
pollScanResult(scanId)  ← existing loop, unchanged
    │
    ▼
ActiveScanCard shows verdict (same as photo flow)
    │
    ▼
User taps card → NavigateToProductDetail
```

---

## 9. Verification Plan

### Build check
```bash
./gradlew :data:compileDebugKotlin :domain:compileDebugKotlin :presentation:compileDebugKotlin
```

### Unit tests

| Test class | What it covers |
|---|---|
| `SubmitBarcodeScanUseCaseTest` | Delegates to repository; propagates `Result.success` and `Result.failure` |
| `CameraScanViewModelBarcodeTest` | Debounce logic — same barcode ×2 within 1.5 s fires one `BarcodeLocked`; `null` cancels job; processing guard prevents double-submit |

### Manual verification checklist

- [ ] Barcode tab visible and selectable in `ScanModeSelector`.
- [ ] `BarcodeArOverlay` appears and tracks barcode bounding box in real-time.
- [ ] Bracket turns green after 1.5 s of stability.
- [ ] `ActiveScanCard` appears with "Processing" immediately after submission.
- [ ] Card resolves to correct product verdict (same as photo flow).
- [ ] Moving camera away mid-lock cancels job; new detection restarts timer.
- [ ] No double-submission if second barcode appears while polling.
- [ ] Switching to Photo/Gallery mode while barcode is locked cancels job cleanly.

---

## 10. Files Changed Summary

| File | Operation |
|---|---|
| `data/remote/dto/BarcodeScanRequestDto.kt` | **NEW** |
| `data/remote/api/ScanApiService.kt` | MODIFY — add 1 endpoint |
| `data/repository/ScanRepositoryImpl.kt` | MODIFY — add 1 method |
| `domain/scan/repository/IScanRepository.kt` | MODIFY — add 1 contract method |
| `domain/scan/usecase/SubmitBarcodeScanUseCase.kt` | **NEW** |
| `presentation/.../state/ScanInputMode.kt` | MODIFY — rename QR → BARCODE |
| `presentation/.../state/CameraScanState.kt` | MODIFY — 2 new fields |
| `presentation/.../state/CameraScanEvent.kt` | MODIFY — 2 new events |
| `presentation/.../components/BarcodeScanAnalyzer.kt` | **NEW** |
| `presentation/.../components/BarcodeArOverlay.kt` | **NEW** |
| `presentation/.../components/CameraPreview.kt` | MODIFY — optional analyzer param |
| `presentation/.../view/CameraScanScreen.kt` | MODIFY — wire analyzer + overlay |
| `presentation/.../viewmodel/CameraScanViewModel.kt` | MODIFY — barcode events + UseCase |
| `presentation/.../components/ScanModeSelector.kt` | MODIFY — enable BARCODE tab |
| `presentation/res/values/strings.xml` | MODIFY — 2 new strings |
