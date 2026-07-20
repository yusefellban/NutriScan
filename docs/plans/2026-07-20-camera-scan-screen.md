# Plan: Camera Scan Screen

## 1. Feature Summary

Implements the first slice of the core scan feature: a full-screen **Camera Scan** destination that opens when the user taps the center Scan FAB on the bottom navigation bar (from Home, Profile, or any screen that hosts `AppBottomNavBar`). The screen matches the provided Figma design — live camera viewfinder, white corner-bracket scan frame, animated scan line, floating dark product card, and teal bottom nav — **without** the top-left back button.

**Scan trigger (this sprint):** Auto-detect barcode/QR codes via **Google ML Kit Barcode Scanning** on a live **CameraX** preview. Supports product barcodes (EAN-13, UPC-A) and QR codes commonly found on food packaging.

**Post-scan (this sprint):** Briefly show the floating "Processing" card, then navigate to the existing `ScanProcessingRoute` placeholder, passing the detected barcode value. Real API/OCR integration is deferred to a follow-up sprint.

Maps to root `AGENTS.md` §13.3 (Core Label Scan — camera entry point) and §1.9 (CameraX).

---

## 2. Files to Create

| File | Purpose |
|------|---------|
| `presentation/.../scan/camera/state/CameraScanState.kt` | Screen state: permission, scanning flag, active card UiModel, selected tab |
| `presentation/.../scan/camera/state/CameraScanEvent.kt` | User/system events: permission result, barcode detected, bottom-nav taps |
| `presentation/.../scan/camera/state/CameraScanEffect.kt` | One-shot effects: navigation, permission snackbar |
| `presentation/.../scan/camera/state/ActiveScanUiModel.kt` | UiModel for the floating processing card |
| `presentation/.../scan/camera/viewmodel/CameraScanViewModel.kt` | MVI ViewModel: debounce, card state, navigation effects |
| `presentation/.../scan/camera/view/CameraScanScreen.kt` | Stateful root + stateless content; permission launcher |
| `presentation/.../scan/camera/view/components/CameraPreview.kt` | CameraX preview via `PreviewView` + ML Kit `ImageAnalysis` |
| `presentation/.../scan/camera/view/components/ScanFrameOverlay.kt` | White L-corner brackets + animated horizontal scan line |
| `presentation/.../scan/camera/view/components/ActiveScanCard.kt` | Floating card: thumbnail, brand, product name, Processing badge, + button |
| `app/.../di/CameraModule.kt` | Hilt `@Provides` for `ListenableFuture<ProcessCameraProvider>` |
| `presentation/src/test/.../scan/camera/CameraScanViewModelTest.kt` | Required ViewModel unit tests |

No new vector drawables — reuses existing `ic_scanner.xml`, `ic_plus.xml`, and bottom-nav icons.

---

## 3. Files to Modify

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add `camerax` and `mlkitBarcode` version aliases + library entries |
| `presentation/build.gradle.kts` | Add CameraX + ML Kit Barcode Scanning dependencies |
| `app/build.gradle.kts` | Add CameraX if needed for `CameraModule` (ProcessCameraProvider lives in app DI) |
| `app/src/main/AndroidManifest.xml` | Add `android.permission.CAMERA`; declare `android.hardware.camera` feature as not required |
| `app/.../navigation/Route.kt` | Extend `ScanProcessingRoute` with `barcode: String`; keep optional `imageUri` for future label capture |
| `app/.../navigation/NavGraph.kt` | Replace `CameraScanRoute` placeholder with real `CameraScanScreen`; wire bottom-nav callbacks; update processing placeholder to display barcode |
| `presentation/.../common/theme/AppColors.kt` | Add scan-specific tokens only if existing tokens cannot cover the floating card surface |
| `presentation/src/main/res/values/strings.xml` | Add scan screen strings (English) |
| `presentation/src/main/res/values-ar/strings.xml` | Add scan screen strings (Arabic) |

---

## 4. Layer Breakdown

### Domain

No changes this sprint. Barcode detection and camera binding are presentation/infrastructure concerns. A future sprint adds:

- `AnalyzeLabelImageUseCase` / barcode lookup UseCase
- `IScanRepository` + API integration per root `AGENTS.md` §2.2

### Data

No changes this sprint.

### Presentation

**State properties:**

```kotlin
data class CameraScanState(
    val hasCameraPermission: Boolean = false,
    val isScanning: Boolean = true,
    val activeScan: ActiveScanUiModel? = null,
    val selectedTab: BottomNavTab = BottomNavTab.SCAN,
    val permissionDenied: Boolean = false,
)

data class ActiveScanUiModel(
    val barcode: String,
    val brand: String?,
    val productName: String,
    val thumbnailUrl: String?,
    val statusResId: Int,
)
```

**Events:**

- `PermissionResult(granted: Boolean)`
- `RequestPermissionClicked`
- `BarcodeDetected(value: String, format: Int)`
- `BottomNavTabClicked(BottomNavTab)`
- `AddToListClicked` (+ button on floating card)

**Effects:**

- `NavigateToProcessing(barcode: String)`
- `NavigateToHome`
- `NavigateToHistory`
- `NavigateToShopping`
- `NavigateToProfile`
- `ShowSnackBarRes(messageResId: Int)`
- `RequestCameraPermission`

**ViewModel logic outline:**

1. On first load, emit `RequestCameraPermission` effect (handled by Composable launcher).
2. On `PermissionResult(true)`: set `hasCameraPermission = true`, start scanning.
3. On `PermissionResult(false)`: set `permissionDenied = true`; show `AppErrorWidget` with retry.
4. On `BarcodeDetected`:
   - Debounce with 2 s cooldown to prevent duplicate navigations.
   - Set `isScanning = false`, populate `activeScan` with barcode + placeholder brand/product name.
   - After ~400 ms delay, emit `NavigateToProcessing(barcode)`.
5. On `BottomNavTabClicked`: mirror `HomeViewModel` pattern — HOME/HISTORY/SHOPPING/PROFILE emit navigation effects; SCAN is no-op.
6. On `AddToListClicked`: emit `ShowSnackBarRes(R.string.scan_add_to_list_coming_soon)`.

**Screen layout (Figma):**

```
Box(fillMaxSize) {
  CameraPreview(Modifier.fillMaxSize)              // full-bleed camera feed
  ScanFrameOverlay(Modifier.align(Center))         // corner brackets + scan line
  Column(Modifier.align(Bottom)) {
    ActiveScanCard(activeScan)                     // visible when non-null
    AppBottomNavBar(selectedTab = SCAN)
  }
  if (permissionDenied) AppErrorWidget(...)       // centered overlay
}
```

- No back button, no top app bar.
- Reuse existing `AppBottomNavBar` with `BottomNavTab.SCAN` selected.

**CameraPreview component:**

- `AndroidView(PreviewView)` bound inside `DisposableEffect(lifecycleOwner)`.
- Use cases: `Preview` + `ImageAnalysis` (back camera, `STRATEGY_KEEP_ONLY_LATEST`).
- ML Kit `BarcodeScanner` processes each frame; forwards first valid result to `onBarcodeDetected`.
- Pause analysis when `isScanning == false`.
- Unbind camera in `onDispose`.

---

## 5. Navigation Changes

**Route update** (`Route.kt`):

```kotlin
@Serializable
data class ScanProcessingRoute(
    val barcode: String,
    val imageUri: String? = null,
)
```

`CameraScanRoute` is already declared — no new route needed.

**NavGraph wiring** (`NavGraph.kt`):

Replace the `CameraScanRoute` placeholder block with:

```kotlin
composable<CameraScanRoute> {
    CameraScanScreen(
        onNavigateToProcessing = { barcode ->
            navController.navigate(ScanProcessingRoute(barcode = barcode)) {
                popUpTo<CameraScanRoute> { inclusive = true }
            }
        },
        onNavigateToHome = { navController.navigate(HomeRoute) },
        onNavigateToHistory = { navController.navigate(ScanHistoryRoute) },
        onNavigateToShopping = { navController.navigate(ShoppingListRoute) },
        onNavigateToProfile = { navController.navigate(UserProfileRoute) },
    )
}
```

Update the `ScanProcessingRoute` placeholder to display the received `barcode` for verification.

**Entry points already wired:**

- Home: Scan FAB + Scan Ready card → `navController.navigate(CameraScanRoute)`
- Profile: bottom nav SCAN tab → `navController.navigate(CameraScanRoute)`

---

## 6. Strings — MANDATORY (Zero Hardcoded Text)

| Key (R.string.xxx) | English Value | Arabic Value |
|--------------------|---------------|--------------|
| `scan_status_processing` | Processing | جاري المعالجة |
| `scan_product_unknown` | Scanned product | منتج ممسوح |
| `scan_brand_unknown` | Unknown brand | ماركة غير معروفة |
| `scan_camera_permission_denied` | Camera access is required to scan products | يلزم الوصول للكاميرا لمسح المنتجات |
| `scan_camera_permission_retry` | Grant permission | منح الإذن |
| `scan_add_to_list_coming_soon` | Add to list coming soon | إضافة للقائمة قريباً |
| `scan_frame_content_description` | Barcode scanner viewfinder | إطار ماسح الباركود |
| `scan_add_to_list_content_description` | Add product to list | إضافة المنتج للقائمة |

---

## 7. Testing Plan

`CameraScanViewModelTest.kt` (JUnit5 + MockK + Turbine + `TestCoroutineRule`):

| Test case | Assert |
|-----------|--------|
| Initial state | `hasCameraPermission = false`, `isScanning = true`, `selectedTab = SCAN`, `activeScan = null` |
| `PermissionResult(true)` | `hasCameraPermission = true`, `permissionDenied = false` |
| `PermissionResult(false)` | `hasCameraPermission = false`, `permissionDenied = true` |
| `BarcodeDetected` | `activeScan` populated, `isScanning = false`; `NavigateToProcessing` effect emitted after delay |
| Duplicate barcode within cooldown | Only one `NavigateToProcessing` effect |
| `BottomNavTabClicked(HOME)` | Effect `NavigateToHome` |
| `BottomNavTabClicked(SCAN)` | No effect emitted |
| `BottomNavTabClicked(HISTORY)` | Effect `NavigateToHistory` |
| `AddToListClicked` | Effect `ShowSnackBarRes(R.string.scan_add_to_list_coming_soon)` |
| `RequestPermissionClicked` | Effect `RequestCameraPermission` |

Camera/ML Kit frame analysis is **not** unit-tested — verified manually on a physical device or emulator with camera support.

---

## 8. Edge Cases

- **Permission permanently denied:** Show `AppErrorWidget` with retry button; optionally deep-link to app settings in a follow-up.
- **No barcode in frame:** Camera keeps scanning indefinitely; no navigation triggered.
- **Multiple barcodes in frame:** Take the first valid ML Kit result; debounce prevents duplicate navigations.
- **User switches bottom-nav tab mid-scan:** `DisposableEffect.onDispose` unbinds CameraX cleanly.
- **Screen rotation:** CameraX rebinding via `LocalLifecycleOwner`.
- **Emulator without camera:** Graceful error state; manual test on physical device recommended.
- **Rapid re-entry to scan screen:** Reset `isScanning = true` and clear cooldown on ViewModel init.

---

## 9. Definition of Done

- [ ] All listed files created
- [ ] Scan screen renders Figma layout (no back button)
- [ ] Bottom nav SCAN opens screen from Home and Profile
- [ ] Camera permission request flow works
- [ ] Barcode/QR auto-detect navigates to `ScanProcessingRoute(barcode)`
- [ ] Floating Processing card appears briefly before navigation
- [ ] All ViewModel test cases passing
- [ ] All strings defined in `strings.xml` (Arabic + English)
- [ ] No hardcoded colors, strings, or dimensions in any Composable
- [ ] `CameraModule` registered in Hilt
- [ ] CameraX + ML Kit dependencies added via version catalog

---

## 10. Library Choice

| Library | Gradle coordinates | Role |
|---------|-------------------|------|
| **CameraX** | `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` | Live preview, lifecycle-aware binding |
| **ML Kit Barcode Scanning** | `com.google.mlkit:barcode-scanning` | Auto-detect QR + product barcodes (EAN-13, UPC-A, etc.) |

**Why ML Kit over ZXing:** Official Google library, actively maintained, integrates natively with CameraX `ImageAnalysis`, and supports the barcode formats common on Egyptian food products.

**Not in scope this sprint:** ML Kit Text Recognition (label OCR), `ImageCapture` (photo capture), real product lookup API, scan processing/result screen implementation.

---

## 11. Out of Scope (Follow-Up Sprints)

- Real product lookup from barcode via REST API
- Label photo capture + on-device OCR preprocessing (ML Kit Text Recognition)
- Full `ScanProcessingScreen` and `ScanResultScreen` implementation
- Domain layer: `IScanRepository`, `AnalyzeLabelImageUseCase`, DTOs, Room scan history
- Proto DataStore auth token + profile IDs passed to scan API
- Bottom-nav scan entry from History / Shopping screens (when those screens get real UI)
