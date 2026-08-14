# Plan: Unified Scan — Merge Barcode Detection into Photo Mode

## 1. Problem Statement

Currently the scan screen has **three separate tabs** (Barcode / Photo / Gallery). The user wants to **merge Barcode and Photo** into a single unified camera experience:

- When the user opens the camera (Photo mode), the live preview runs **both** `ImageCapture` and the ML Kit `ImageAnalysis` barcode analyzer simultaneously.
- If a barcode is detected in the frame, the barcode value (digits) appears below the AR overlay as a **tappable chip**.
- Tapping the shutter button → calls the **image scan endpoint** (`POST /v1/scans` — multipart image upload), same as today.
- Tapping the barcode chip → calls the **barcode scan endpoint** (`POST /v1/scans/barcode` — JSON body), same as the current BARCODE tab flow.
- The separate `BARCODE` tab is **removed** from `ScanInputMode`; the mode selector becomes **Photo / Gallery** only.

This gives users the best of both worlds in one view: a quick barcode tap when one is visible, and a shutter tap for label photos when no barcode is present.

---

## 2. User Review Required

> [!IMPORTANT]
> **BARCODE tab removal**: The `ScanInputMode.BARCODE` enum entry will be removed. All code referencing it (ViewModel `handleModeSelected`, `ScanModeSelector`, `CameraScanScreen`) will be updated. The barcode functionality is **not lost** — it is moved into the Photo mode as an always-on background scanner.

> [!IMPORTANT]
> **UX change**: In the current BARCODE tab, after 1.5 s of stable detection the barcode is **auto-submitted**. In the new merged mode, barcode detection will **not auto-submit**. Instead, the barcode chip is shown and the user must **tap it explicitly** to submit. This gives users full control and avoids accidental barcode submissions while they intended to take a photo.

> [!WARNING]
> **AR overlay behaviour**: The `BarcodeArOverlay` with spring-animated bounding-box tracking will continue to render in Photo mode when a barcode is detected. The existing anti-trembling strategies (frame-drop tolerance, fixed size, dead-band filter) are preserved unchanged.

---

## 3. Open Questions

> [!IMPORTANT]
> **Auto-lock timer**: The current BARCODE mode auto-submits after 1.5 s of stable detection. In the unified mode, should we:
> - **(A)** Remove auto-lock entirely — barcode chip is always tap-to-submit only (recommended, avoids accidental submissions while user is trying to photograph a label)?
> - **(B)** Keep the 1.5 s auto-lock as an alternative submission path alongside the tap?

---

## 4. Proposed Changes

### Scan State (Presentation Layer)

---

#### [MODIFY] [ScanInputMode.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/ScanInputMode.kt)

Remove the `BARCODE` entry. The enum becomes:
```kotlin
enum class ScanInputMode(
    @StringRes val labelResId: Int,
    @StringRes val hintResId: Int,
) {
    PHOTO(
        labelResId = R.string.scan_mode_photo,
        hintResId  = R.string.scan_mode_photo_hint,
    ),
    GALLERY(
        labelResId = R.string.scan_mode_gallery,
        hintResId  = R.string.scan_mode_gallery_hint,
    ),
}
```

#### [MODIFY] [CameraScanState.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/CameraScanState.kt)

- Change default `selectedMode` from `ScanInputMode.PHOTO` to `ScanInputMode.PHOTO` (no change, but confirm).
- The `detectedBarcodeBounds` and `trackedBarcodeValue` fields remain — they are now active in PHOTO mode instead of BARCODE mode.

#### [MODIFY] [CameraScanEvent.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/CameraScanEvent.kt)

- Add: `data object BarcodeChipClicked : CameraScanEvent` — fired when the user taps the barcode chip under the AR overlay.
- `BarcodeDetected` and `BarcodeLocked` remain — `BarcodeLocked` is now only used if auto-lock is kept (see Open Question).

---

### ViewModel

---

#### [MODIFY] [CameraScanViewModel.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/viewmodel/CameraScanViewModel.kt)

Key changes:
1. **`handleModeSelected`**: Remove all `ScanInputMode.BARCODE` references. The barcode analyzer is now always active in PHOTO mode, so no mode-specific clearing needed.
2. **`handleCenterActionClicked`**: Remove `ScanInputMode.BARCODE` from the `when` block — PHOTO always fires `TakePicture`.
3. **`handleBarcodeDetected`**: Remove the auto-lock timer logic (`barcodeLockJob`, `lastLockedBarcode`, `BARCODE_LOCK_DELAY_MS`). Barcode detection only updates `detectedBarcodeBounds` and `trackedBarcodeValue` for the AR overlay.
4. **Add `handleBarcodeChipClicked`**: Takes `trackedBarcodeValue` from state, calls existing `submitBarcode()` method. Guards against null/blank barcode and against `isProcessingCenterAction`.
5. **`onEvent` dispatch**: Add `CameraScanEvent.BarcodeChipClicked -> handleBarcodeChipClicked()`.
6. **`handleDismissScanClicked`**: Remove mode check for `ScanInputMode.BARCODE` in the keepGalleryPreview logic.
7. **Remove `BARCODE_LOCK_DELAY_MS` companion object constant** (if auto-lock is removed per Q3).

---

### Screen & Components (Presentation Layer)

---

#### [MODIFY] [CameraScanScreen.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/CameraScanScreen.kt)

1. **Barcode analyzer**: Change from `remember(state.selectedMode)` keyed creation to always-on when in PHOTO mode:
   ```kotlin
   val barcodeAnalyzer: ImageAnalysis.Analyzer? = remember(state.selectedMode) {
       if (state.selectedMode == ScanInputMode.PHOTO) {
           BarcodeScanAnalyzer { barcode, bounds ->
               viewModel.onEvent(CameraScanEvent.BarcodeDetected(barcode, bounds))
           }
       } else {
           null
       }
   }
   ```
2. **AR overlay**: Change the condition from `state.selectedMode == ScanInputMode.BARCODE` to `state.selectedMode == ScanInputMode.PHOTO` inside `CameraScanContent`.
3. **Barcode chip tap**: Pass an `onBarcodeChipClicked` callback to `BarcodeArOverlay` that fires `CameraScanEvent.BarcodeChipClicked`.
4. **Scan frame overlay**: The `ScanFrameOverlay` is now always rendered in PHOTO mode (it already is for PHOTO). It and the `BarcodeArOverlay` can coexist.

#### [MODIFY] [BarcodeArOverlay.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/BarcodeArOverlay.kt)

1. Add an `onBarcodeChipClicked: () -> Unit` callback parameter.
2. The barcode value pill/chip that currently shows the barcode digits should be made **tappable** (wrap in `.clickable { onBarcodeChipClicked() }`).
3. Add a visual affordance (e.g., a subtle tap icon or "Tap to scan" label next to the barcode digits) to indicate it's interactive.

#### [MODIFY] [ScanModeSelector.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/ScanModeSelector.kt)

- `ScanInputMode.entries` now only has PHOTO and GALLERY — no code change needed beyond what `ScanInputMode` removal does. The `icon()` function should remove the `ScanInputMode.BARCODE` branch.

#### [MODIFY] [CameraPreview.kt](file:///c:/Users/DELL/Documents/NutriScan/presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/CameraPreview.kt)

No changes needed — it already accepts optional `barcodeAnalyzer`. In PHOTO mode it will now receive a non-null analyzer where before it received null.

---

### String Resources

---

#### [MODIFY] `presentation/src/main/res/values/strings.xml`

| Key | English value |
|---|---|
| `scan_barcode_chip_tap_hint` | `Tap to scan barcode` |

#### [MODIFY] `presentation/src/main/res/values-ar/strings.xml`

| Key | Arabic value |
|---|---|
| `scan_barcode_chip_tap_hint` | `اضغط لمسح الباركود` |

Remove or keep the following strings (they become unused if BARCODE tab is removed):
- `scan_mode_barcode` — **remove**
- `scan_mode_barcode_hint` — **remove** (or repurpose hint text)

Update `scan_mode_photo_hint`:
| Key | English value | Arabic value |
|---|---|---|
| `scan_mode_photo_hint` | `Capture a label or point at a barcode` | `التقط صورة للملصق أو وجّه الكاميرا نحو الباركود` |

---

## 5. Files Changed Summary

| File | Operation |
|---|---|
| `presentation/.../state/ScanInputMode.kt` | MODIFY — remove BARCODE entry |
| `presentation/.../state/CameraScanEvent.kt` | MODIFY — add BarcodeChipClicked |
| `presentation/.../state/CameraScanState.kt` | MODIFY — minor, default stays PHOTO |
| `presentation/.../viewmodel/CameraScanViewModel.kt` | MODIFY — remove BARCODE mode logic, add chip click handler, remove auto-lock |
| `presentation/.../view/CameraScanScreen.kt` | MODIFY — always-on barcode analyzer in PHOTO, update overlay condition |
| `presentation/.../components/BarcodeArOverlay.kt` | MODIFY — add tap callback to barcode chip |
| `presentation/.../components/ScanModeSelector.kt` | MODIFY — remove BARCODE icon branch |
| `presentation/res/values/strings.xml` | MODIFY — add/update strings |
| `presentation/res/values-ar/strings.xml` | MODIFY — add/update strings |

No data layer or domain layer changes — both `submitScanImage` and `submitBarcodeScan` use cases and endpoints remain exactly as they are.

---

## 6. End-to-End User Flow (After Change)

```
User opens Scan screen (defaults to Photo mode)
    │
    ├── CameraX binds:  Preview + ImageCapture + ImageAnalysis (BarcodeScanAnalyzer)
    │
    │  Every frame (STRATEGY_KEEP_ONLY_LATEST):
    ▼
BarcodeScanAnalyzer → BarcodeDetected(barcode, normalizedBounds)
    │
    ▼
ViewModel: updates state → BarcodeArOverlay shows bracket + barcode chip
    │
    ├── User taps SHUTTER button:
    │       → CenterActionClicked → TakePicture → submitScanImage()
    │       → POST /v1/scans (multipart image)
    │       → pollScanResult() → ActiveScanCard shows verdict
    │
    └── User taps BARCODE CHIP:
            → BarcodeChipClicked → submitBarcode()
            → POST /v1/scans/barcode (JSON)
            → pollScanResult() → ActiveScanCard shows verdict
```

---

## 7. Verification Plan

### Build Verification
```bash
./gradlew :presentation:compileDebugKotlin :data:compileDebugKotlin :domain:compileDebugKotlin
```

### Manual Verification Checklist

- [ ] Photo mode is the default — no BARCODE tab visible.
- [ ] Camera preview shows both capture and barcode detection simultaneously.
- [ ] Barcode AR overlay appears when a barcode enters the frame in Photo mode.
- [ ] Barcode chip shows the detected digits and is tappable.
- [ ] Tapping barcode chip → `POST /v1/scans/barcode` → polling → verdict card.
- [ ] Tapping shutter button → `POST /v1/scans` (image) → polling → verdict card.
- [ ] Gallery mode still works unchanged.
- [ ] Mode selector shows only Photo / Gallery.
- [ ] No crash when switching between Photo and Gallery mid-detection.
- [ ] AR overlay does not flicker/tremble (existing anti-jitter strategies preserved).

---

## 8. Edge Cases

| Scenario | Expected Behavior |
|---|---|
| Barcode appears briefly then disappears | AR overlay stays for 500ms (existing frame-drop tolerance), then fades |
| User taps shutter while barcode is visible | Image scan runs, barcode chip ignored |
| User taps barcode chip while image scan is processing | Guard prevents double-submission |
| Multiple barcodes in frame | ML Kit picks first valid result; single chip shown |
| User switches to Gallery while barcode is tracking | Barcode state cleared, analyzer disposed |
| No barcode in frame | No chip shown, shutter button works normally |

---

## 9. Definition of Done

- [ ] `ScanInputMode.BARCODE` removed from enum.
- [ ] Barcode analyzer active in PHOTO mode.
- [ ] Barcode chip is tappable and triggers `POST /v1/scans/barcode`.
- [ ] Shutter button triggers `POST /v1/scans` (image upload).
- [ ] AR overlay renders in PHOTO mode.
- [ ] Mode selector shows only Photo / Gallery.
- [ ] All strings defined in English and Arabic.
- [ ] No hardcoded strings or colors.
- [ ] Build compiles without errors.
