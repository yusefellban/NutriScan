package iti.grad.nutriscan.presentation.scan.camera.state

import android.graphics.RectF


data class CameraScanState(
    val selectedMode: ScanInputMode = ScanInputMode.PHOTO,
    val hasCameraPermission: Boolean = false,
    val isScanning: Boolean = true,
    val isProcessingCenterAction: Boolean = false,
    val pendingGalleryImagePath: String? = null,
    val activeScan: ActiveScanUiModel? = null,
    val permissionDenied: Boolean = false,
    val showDeleteDialog: Boolean = false,

    /**
     * The normalised bounding box [0,1] of the last detected barcode within the camera frame,
     * or null when no barcode is currently visible.
     *
     * Stored as normalised coords so the ViewModel is entirely display-size-agnostic.
     * [BarcodeArOverlay] maps these to physical screen pixels at draw time.
     */
    val detectedBarcodeBounds: RectF? = null,

    /**
     * The raw barcode value (digits) currently being tracked by ML Kit.
     * Cleared once submission is in-flight to prevent a second lock on the same scan.
     */
    val trackedBarcodeValue: String? = null,
)
