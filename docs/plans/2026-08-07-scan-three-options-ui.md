# Plan: Scan Screen 3 Modes (QR / Photo / Gallery)

## 1. Feature Summary

The scan experience will be upgraded to support three clear input modes:
- QR
- Photo
- Image from Gallery

The center bottom button behavior will be mode-dependent:
- QR and Photo: Capture action
- Image from Gallery: Upload action

This plan is limited to UI and interaction behavior in the presentation layer, with no implementation started yet.

## 2. Files to Create

### Presentation Module

| Action | File | Purpose |
|---|---|---|
| CREATE | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/ScanInputMode.kt | Define the three modes as enum or sealed type with icon and label resource mapping |
| CREATE | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/ScanModeSelector.kt | Reusable segmented selector for QR, Photo, and Gallery with motion states |

### Presentation Test Source Set

| Action | File | Purpose |
|---|---|---|
| CREATE | presentation/src/test/kotlin/iti/grad/nutriscan/presentation/scan/camera/viewmodel/CameraScanViewModelTest.kt | Required ViewModel coverage for initial state, Event to State, Event to Effect, and failure paths |

## 3. Files to Modify

### Presentation Module

| Action | File | Change |
|---|---|---|
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/CameraScanState.kt | Add selected mode, gallery pick transient state, and mode-specific center-action loading state |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/CameraScanEvent.kt | Add events for mode selection, center action click, gallery result, and gallery cancel |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/state/CameraScanEffect.kt | Add effect for opening gallery picker and mode-specific feedback effects |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/viewmodel/CameraScanViewModel.kt | Route center action by selected mode, keep existing scan flow, and preserve current save or delete behavior |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/CameraScanScreen.kt | Add mode selector UI, add gallery launcher handling, and bind center trigger to selected mode action |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/CameraPreview.kt | Adapt camera work by mode so QR and Photo keep active preview while Gallery minimizes camera activity |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/scan/camera/view/components/ScanFrameOverlay.kt | Apply subtle mode-specific frame motion differences |
| MODIFY | presentation/src/main/kotlin/iti/grad/nutriscan/presentation/main/container/view/MainScreen.kt | Keep existing center trigger mechanism and delegate action semantics to CameraScanScreen mode state |
| MODIFY | presentation/src/main/res/values/strings.xml | Add new English strings |
| MODIFY | presentation/src/main/res/values-ar/strings.xml | Add new Arabic strings |

### Documentation

| Action | File | Change |
|---|---|---|
| MODIFY | README.md | Update feature status after implementation is complete |
| CREATE | docs/ai/2026-08-07-scan-three-options-ui-plan-summary.md | Post-implementation summary file |

## 4. Layer Breakdown

### Domain
- No changes.
- Reason: scope is UI and interaction behavior only.

### Data
- No changes.
- Reason: gallery image will reuse existing submitScanImageUseCase pipeline after local file mapping in presentation.

### Presentation

State additions:
- selectedMode: ScanInputMode = PHOTO
- isProcessingCenterAction: Boolean = false
- hasCameraPermission: Boolean
- permissionDenied: Boolean
- activeScan: ActiveScanUiModel?
- showDeleteDialog: Boolean
- pendingGalleryImageUri: String?

Event additions:
- ModeSelected(mode: ScanInputMode)
- CenterActionClicked
- GalleryImagePicked(uri: String)
- GalleryPickerCancelled
- PermissionResult(granted: Boolean)
- RequestPermissionClicked
- RetryClicked
- BookmarkClicked
- ConfirmDeleteBookmark
- DismissDeleteBookmark
- DismissScanClicked

Effect additions:
- RequestCameraPermission
- TakePicture
- OpenGalleryPicker
- ShowSnackBarRes(messageResId: Int)
- ShowSnackBar(message: String, snackbarType: SnackbarType)
- NavigateToProductDetail(product: ProductUiModel)

ViewModel behavior:
1. ModeSelected updates selected mode and clears incompatible transient states.
2. CenterActionClicked maps to TakePicture for QR and Photo, and OpenGalleryPicker for Gallery.
3. GalleryImagePicked converts uri safely to File and reuses the same submit and polling flow.
4. PermissionResult keeps current permission behavior with clearer messaging.
5. Existing bookmark, delete, and product-detail flows remain unchanged.

UI and motion spec:
- A three-segment mode selector above the center button.
- Animated sliding indicator with spring behavior.
- Selected item scale emphasis and non-selected opacity reduction.
- Center button icon transition between Capture and Upload using AnimatedContent.
- Scan frame differences by mode:
  - QR: faster pulse and stronger corner glow.
  - Photo: calmer scan-line movement.
  - Gallery: softer frame brightness with upload hint.
- Transition between modes uses short crossfade and mild scale to avoid jitter.

## 5. Navigation Changes

- No new routes.
- No NavGraph updates.
- Change is fully internal to the screen while user is on BottomNavTab.SCAN.

## 6. Strings — MANDATORY (Zero Hardcoded Text)

| Key | English Value | Arabic Value |
|---|---|---|
| scan_mode_qr | QR | كيو آر |
| scan_mode_photo | Photo | صورة |
| scan_mode_gallery | Image | صورة من المعرض |
| scan_center_action_capture | Capture | التقاط |
| scan_center_action_upload | Upload | رفع |
| scan_mode_selector_content_desc | Scan mode selector | اختيار وضع المسح |
| scan_gallery_pick_prompt | Choose an image to scan | اختر صورة للفحص |
| scan_gallery_pick_failed | Could not read selected image | تعذر قراءة الصورة المختارة |
| scan_gallery_pick_cancelled | No image selected | لم يتم اختيار صورة |
| scan_mode_qr_hint | Align QR code inside the frame | ضع رمز QR داخل الإطار |
| scan_mode_photo_hint | Align product and capture clearly | ضع المنتج داخل الإطار والتقط بوضوح |
| scan_mode_gallery_hint | Upload a product image from gallery | ارفع صورة منتج من المعرض |
| scan_center_action_content_desc | Scan primary action | الإجراء الرئيسي للمسح |

## 7. Testing Plan

### Build Verification

Run:
./gradlew :presentation:test

### ViewModel Unit Test Cases

| # | Scenario | Expected |
|---|---|---|
| 1 | Initial state | Default mode is PHOTO and baseline state is valid |
| 2 | ModeSelected QR | State updates to QR |
| 3 | ModeSelected PHOTO | State updates to PHOTO |
| 4 | ModeSelected GALLERY | State updates to GALLERY |
| 5 | CenterActionClicked in QR | TakePicture effect emitted |
| 6 | CenterActionClicked in PHOTO | TakePicture effect emitted |
| 7 | CenterActionClicked in GALLERY | OpenGalleryPicker effect emitted |
| 8 | GalleryImagePicked with valid uri | Submit flow starts and active scan state updates |
| 9 | GalleryImagePicked with invalid uri | Error snackbar effect emitted |
| 10 | GalleryPickerCancelled | Informational snackbar effect emitted |
| 11 | PermissionResult false | permissionDenied is true |
| 12 | PermissionResult true | hasCameraPermission is true and permissionDenied is false |
| 13 | BookmarkClicked after mode changes | Existing bookmark behavior remains correct |

### Manual UX Validation

| # | Scenario | Expected |
|---|---|---|
| 1 | Switch across all three modes repeatedly | Smooth transitions without layout jumps |
| 2 | Press center button in QR | Capture flow starts |
| 3 | Press center button in Photo | Capture flow starts |
| 4 | Press center button in Gallery | Gallery picker opens |
| 5 | Return from gallery with no selection | No crash and clear user feedback |
| 6 | Use app in Arabic locale | Selector and hints remain readable and RTL-safe |

## 8. Edge Cases

| Scenario | Expected Behavior |
|---|---|
| User switches mode during active processing job | Previous incompatible transient work is safely cancelled or ignored |
| User rapidly taps center action button | Debounce prevents duplicate capture or picker launches |
| Gallery picker returns unreadable uri | Graceful error feedback, no crash |
| Permission changes after returning from gallery | State re-evaluates correctly and UI remains consistent |
| Device or emulator has no camera | Gallery mode remains fully usable |
| QR mode sees no clear code for long time | No crash and no invalid state transition |

## 9. Definition of Done

- [ ] All listed files are created or updated exactly as scoped.
- [ ] Center button performs Capture in QR and Photo, and Upload in Gallery.
- [ ] Mode selector is clear, responsive, and RTL-compatible.
- [ ] Mode transitions are smooth with no visible jitter.
- [ ] All new user-facing strings are added in English and Arabic resources.
- [ ] No hardcoded user-facing text in Kotlin files.
- [ ] No hardcoded colors outside AppTheme tokens.
- [ ] CameraScanViewModelTest is implemented and passing.
- [ ] README feature status is updated after implementation.
