package iti.grad.nutriscan.presentation.scan.camera.state

import android.graphics.RectF
import java.io.File

sealed interface CameraScanEvent {
    data class ModeSelected(val mode: ScanInputMode) : CameraScanEvent
    data class PermissionResult(val granted: Boolean) : CameraScanEvent
    data object RequestPermissionClicked : CameraScanEvent
    data object CenterActionClicked : CameraScanEvent
    data class GalleryImageSelected(val file: File) : CameraScanEvent
    data class ImageCaptured(val file: File) : CameraScanEvent
    data class ImageCaptureFailed(val error: Exception) : CameraScanEvent
    data object GalleryPickCancelled : CameraScanEvent
    data object GalleryPickFailed : CameraScanEvent
    data object BookmarkClicked : CameraScanEvent
    data object RetryClicked : CameraScanEvent
    data object DismissScanClicked : CameraScanEvent
    data object CardClicked : CameraScanEvent
    data object ConfirmDeleteBookmark : CameraScanEvent
    data object DismissDeleteBookmark : CameraScanEvent

    /**
     * Fired when the user taps the barcode chip shown by [BarcodeArOverlay] in PHOTO mode.
     * Triggers a backend call to POST /v1/scans/barcode with the currently tracked barcode value.
     */
    data object BarcodeChipClicked : CameraScanEvent

    /**
     * Fired by [BarcodeScanAnalyzer] on every CameraX analysis frame.
     * [barcode] and [normalizedBounds] are both null when no barcode is visible in frame.
     */
    data class BarcodeDetected(
        val barcode: String?,
        val normalizedBounds: RectF?,
    ) : CameraScanEvent

    /**
     * Fired internally by the ViewModel after [barcode] has been stably detected for
     * [CameraScanViewModel.BARCODE_LOCK_DELAY_MS] milliseconds. Triggers backend submission.
     */
    data class BarcodeLocked(val barcode: String) : CameraScanEvent
}
