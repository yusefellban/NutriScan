package iti.grad.nutriscan.presentation.scan.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.DeleteSavedScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetSavedScansUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetScanResultUseCase
import iti.grad.nutriscan.domain.scan.usecase.SaveScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.SubmitBarcodeScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.SubmitScanImageUseCase
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.scan.camera.state.ActiveScanUiModel
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.presentation.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CameraScanViewModel @Inject constructor(
    private val submitScanImageUseCase: SubmitScanImageUseCase,
    private val submitBarcodeScanUseCase: SubmitBarcodeScanUseCase,
    private val getScanResultUseCase: GetScanResultUseCase,
    private val saveScanUseCase: SaveScanUseCase,
    private val deleteSavedScanUseCase: DeleteSavedScanUseCase,
    private val getSavedScansUseCase: GetSavedScansUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CameraScanState())
    val state: StateFlow<CameraScanState> = _state.asStateFlow()

    private val _effect = Channel<CameraScanEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentScanJob: Job? = null

    /**
     * Debounce job for clearing the barcode state. ML Kit often drops detection for a few
     * frames. Instead of instantly hiding the AR overlay (causing flashing/trembling),
     * we wait 500ms before clearing.
     */
    private var barcodeLossJob: Job? = null

    /** 
     * Fixed dimensions to prevent the AR overlay from trembling/breathing in size. 
     * Once caught, the size remains constant but the center follows the barcode. 
     */
    private var lockedBoundsWidth: Float? = null
    private var lockedBoundsHeight: Float? = null
    private var lastTrackedBarcodeForSize: String? = null

    /** Used by the dead-band filter to prevent micro-jitter in the center point. */
    private var lastEmittedCenterX: Float? = null
    private var lastEmittedCenterY: Float? = null

    init {
        viewModelScope.launch {
            _effect.send(CameraScanEffect.RequestCameraPermission)
        }
        viewModelScope.launch {
            getSavedScansUseCase().collect { savedScans ->
                val savedIds = savedScans.map { it.scanId }.toSet()
                _state.update { state ->
                    val active = state.activeScan
                    if (active != null) {
                        state.copy(activeScan = active.copy(isSaved = savedIds.contains(active.scanId)))
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun onEvent(event: CameraScanEvent) {
        when (event) {
            is CameraScanEvent.ModeSelected         -> handleModeSelected(event.mode)
            is CameraScanEvent.PermissionResult     -> handlePermissionResult(event.granted)
            is CameraScanEvent.RequestPermissionClicked -> requestPermission()
            is CameraScanEvent.CenterActionClicked  -> handleCenterActionClicked()
            is CameraScanEvent.GalleryImageSelected -> handleGalleryImageSelected(event.file)
            is CameraScanEvent.ImageCaptured        -> handleImageCaptured(event.file)
            is CameraScanEvent.ImageCaptureFailed   -> handleImageCaptureFailed(event.error)
            is CameraScanEvent.GalleryPickCancelled -> handleGalleryPickCancelled()
            is CameraScanEvent.GalleryPickFailed    -> handleGalleryPickFailed()
            is CameraScanEvent.BookmarkClicked      -> handleBookmarkClicked()
            is CameraScanEvent.RetryClicked         -> handleRetryClicked()
            is CameraScanEvent.DismissScanClicked   -> handleDismissScanClicked()
            is CameraScanEvent.CardClicked          -> handleCardClicked()
            is CameraScanEvent.ConfirmDeleteBookmark -> handleConfirmDeleteBookmark()
            is CameraScanEvent.DismissDeleteBookmark -> handleDismissDeleteBookmark()
            is CameraScanEvent.BarcodeDetected      -> handleBarcodeDetected(event.barcode, event.normalizedBounds)
            is CameraScanEvent.BarcodeLocked        -> handleBarcodeLocked(event.barcode)
            is CameraScanEvent.BarcodeChipClicked   -> handleBarcodeChipClicked()
        }
    }

    private fun handleModeSelected(mode: ScanInputMode) {
        val currentMode = _state.value.selectedMode
        if (currentMode == mode) {
            if (mode == ScanInputMode.GALLERY) {
                openGalleryPicker()
            }
            return
        }

        // Cancel any in-flight scan job when switching modes.
        currentScanJob?.cancel()
        // Clear barcode tracking state when leaving PHOTO mode.
        clearBarcodeState()

        _state.update {
            it.copy(
                selectedMode = mode,
                isProcessingCenterAction = false,
                pendingGalleryImagePath = if (mode == ScanInputMode.GALLERY) it.pendingGalleryImagePath else null,
                detectedBarcodeBounds = null,
                trackedBarcodeValue = null,
            )
        }

        if (mode == ScanInputMode.GALLERY && _state.value.pendingGalleryImagePath == null) {
            openGalleryPicker()
        }
    }

    private fun handlePermissionResult(granted: Boolean) {
        _state.update {
            it.copy(
                hasCameraPermission = granted,
                permissionDenied = !granted,
            )
        }
    }

    private fun requestPermission() {
        viewModelScope.launch {
            _effect.send(CameraScanEffect.RequestCameraPermission)
        }
    }

    private fun handleCenterActionClicked() {
        val selectedMode = _state.value.selectedMode
        when (selectedMode) {
            ScanInputMode.PHOTO -> handleCaptureClicked()

            ScanInputMode.GALLERY -> {
                val pendingPath = _state.value.pendingGalleryImagePath
                if (pendingPath.isNullOrBlank()) {
                    openGalleryPicker()
                } else {
                    submitImageFile(File(pendingPath))
                }
            }
        }
    }

    private fun handleCaptureClicked() {
        _state.update {
            it.copy(
                isScanning = true,
                isProcessingCenterAction = true,
                activeScan = ActiveScanUiModel(
                    scanId = "",
                    thumbnailUrl = null,
                    isProcessing = true,
                ),
            )
        }
        viewModelScope.launch {
            _effect.send(CameraScanEffect.TakePicture)
        }
    }

    private fun openGalleryPicker() {
        _state.update { it.copy(isProcessingCenterAction = true) }
        viewModelScope.launch {
            _effect.send(CameraScanEffect.OpenGalleryPicker)
        }
    }

    private fun handleImageCaptured(file: File) {
        submitImageFile(file)
    }

    private fun handleGalleryImageSelected(file: File) {
        currentScanJob?.cancel()
        _state.update { state ->
            state.copy(
                isScanning = true,
                isProcessingCenterAction = false,
                pendingGalleryImagePath = file.absolutePath,
                // Keep only gallery preview state; do not show scan card until upload is pressed.
                activeScan = null,
            )
        }
    }

    private fun submitImageFile(file: File) {
        currentScanJob?.cancel()
        _state.update { state ->
            state.copy(
                isScanning = true,
                isProcessingCenterAction = true,
                activeScan = (state.activeScan ?: ActiveScanUiModel(
                    scanId = "",
                    thumbnailUrl = file.absolutePath,
                    isProcessing = true,
                )).copy(
                    thumbnailUrl = file.absolutePath,
                    isProcessing = true,
                    isFailed = false,
                    fullResult = null,
                ),
            )
        }
        currentScanJob = viewModelScope.launch {
            val submitResult = submitScanImageUseCase(file)
            submitResult.onSuccess { scanResult ->
                val scanId = scanResult.scanId
                _state.update { state ->
                    state.copy(
                        isProcessingCenterAction = false,
                        // Keep the local gallery path so user can re-upload the same image.
                        pendingGalleryImagePath = state.pendingGalleryImagePath,
                        activeScan = state.activeScan?.copy(scanId = scanId),
                    )
                }
                pollScanResult(scanId)
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        isProcessingCenterAction = false,
                        activeScan = state.activeScan?.copy(
                            isProcessing = false,
                            isFailed = true,
                        ),
                    )
                }
            }
        }
    }

    private fun handleImageCaptureFailed(error: Exception) {
        _state.update {
            it.copy(
                isScanning = false,
                isProcessingCenterAction = false,
                activeScan = null,
            )
        }
        viewModelScope.launch {
            _effect.send(
                CameraScanEffect.ShowSnackBarRes(
                    messageResId = R.string.scan_capture_failed_generic,
                ),
            )
        }
    }

    private fun handleGalleryPickCancelled() {
        val hasExistingGalleryImage = !_state.value.pendingGalleryImagePath.isNullOrBlank()
        _state.update {
            if (hasExistingGalleryImage) {
                it.copy(
                    selectedMode = ScanInputMode.GALLERY,
                    isProcessingCenterAction = false,
                )
            } else {
                it.copy(
                    selectedMode = ScanInputMode.PHOTO,
                    isProcessingCenterAction = false,
                    pendingGalleryImagePath = null,
                )
            }
        }
    }

    private fun handleGalleryPickFailed() {
        val hasExistingGalleryImage = !_state.value.pendingGalleryImagePath.isNullOrBlank()
        _state.update {
            if (hasExistingGalleryImage) {
                it.copy(
                    selectedMode = ScanInputMode.GALLERY,
                    isProcessingCenterAction = false,
                )
            } else {
                it.copy(
                    selectedMode = ScanInputMode.PHOTO,
                    isProcessingCenterAction = false,
                    pendingGalleryImagePath = null,
                )
            }
        }
        viewModelScope.launch {
            _effect.send(CameraScanEffect.ShowSnackBarRes(R.string.scan_gallery_pick_failed))
        }
    }

    private suspend fun pollScanResult(scanId: String) {
        while (true) {
            val result = getScanResultUseCase(scanId)
            result.onSuccess { scanResult ->
                when (scanResult.status) {
                    ScanStatus.COMPLETED -> {
                        val finalScan = _state.value.activeScan?.copy(
                            isProcessing = false,
                            thumbnailUrl = scanResult.imageUrl,
                            healthTagResId = scanResult.foodSafetyResponse?.verdict?.let {
                                when (it) {
                                    ProductVerdict.SAFE -> R.string.verdict_safe
                                    ProductVerdict.CAUTION -> R.string.verdict_caution
                                    ProductVerdict.UNSAFE -> R.string.verdict_unsafe
                                }
                            },
                            fullResult = scanResult
                        )
                        _state.update {
                            it.copy(
                                activeScan = finalScan,
                                isProcessingCenterAction = false,
                                isScanning = false
                            )
                        }
                        return
                    }
                    ScanStatus.FAILED -> {
                        val finalScan = _state.value.activeScan?.copy(
                            isProcessing = false,
                            isFailed = true
                        )
                        _state.update {
                            it.copy(
                                activeScan = finalScan,
                                isProcessingCenterAction = false,
                            )
                        }
                        return
                    }
                    ScanStatus.PROCESSING -> {
                        // Continue polling
                    }
                }
            }
            delay(3000.milliseconds)
        }
    }

    private fun handleBookmarkClicked() {
        val currentScan = _state.value.activeScan ?: return
        val fullResult = currentScan.fullResult ?: return

        if (currentScan.isSaved) {
            _state.update { it.copy(showDeleteDialog = true) }
        } else {
            viewModelScope.launch {
                val result = saveScanUseCase(fullResult)
                if (result.isSuccess) {
                    _state.update { state ->
                        state.copy(
                            activeScan = state.activeScan?.copy(isSaved = true)
                        )
                    }
                } else {
                    _effect.send(
                        CameraScanEffect.ShowSnackBarRes(
                            messageResId = R.string.scan_save_failed,
                        ),
                    )
                }
            }
        }
    }

    private fun handleConfirmDeleteBookmark() {
        val currentScan = _state.value.activeScan ?: return
        viewModelScope.launch {
            deleteSavedScanUseCase(currentScan.scanId)
            _state.update { state ->
                state.copy(
                    showDeleteDialog = false,
                    activeScan = state.activeScan?.copy(isSaved = false)
                )
            }
        }
    }

    private fun handleDismissDeleteBookmark() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    private fun handleCardClicked() {
        val currentScan = _state.value.activeScan ?: return
        if (currentScan.isProcessing || currentScan.isFailed) return
        
        val uiModel = ProductUiModel(
            id = currentScan.fullResult?.scanId ?: "",
            productName = currentScan.fullResult?.productName ?: "",
            imageUrl = currentScan.fullResult?.imageUrl,
            verdict = currentScan.fullResult?.foodSafetyResponse?.verdict ?: ProductVerdict.SAFE,
            calories = currentScan.fullResult?.nutritionFacts?.calories?.toString() ?: "0"
        )
        viewModelScope.launch {
            _effect.send(CameraScanEffect.NavigateToProductDetail(uiModel))
        }
    }

    private fun handleRetryClicked() {
        currentScanJob?.cancel()
        _state.update {
            it.copy(
                isScanning = true,
                isProcessingCenterAction = false,
                activeScan = null,
            )
        }
    }

    private fun handleDismissScanClicked() {
        currentScanJob?.cancel()
        clearBarcodeState()
        _state.update {
            // Keep gallery image path only when currently in GALLERY mode so the user
            // can re-upload the same image without picking again.
            val previewToKeep = if (it.selectedMode == ScanInputMode.GALLERY) {
                it.pendingGalleryImagePath
            } else {
                null
            }
            it.copy(
                isScanning = true,
                isProcessingCenterAction = false,
                pendingGalleryImagePath = previewToKeep,
                activeScan = null,
                detectedBarcodeBounds = null,
                trackedBarcodeValue = null,
            )
        }
    }

    // ── Barcode scan helpers ───────────────────────────────────────────────────────

    /**
     * Called on every analysis frame by [BarcodeScanAnalyzer] via [CameraScanEvent.BarcodeDetected].
     * Updates the AR overlay position only — does NOT auto-submit the barcode.
     * The user must tap the barcode chip explicitly to trigger submission.
     *
     * Resolves AR trembling/flashing via three strategies:
     * 1. **Frame-drop tolerance**: ML Kit often misses a frame. We wait 500ms before clearing.
     * 2. **Fixed size**: We lock the width/height on first detection so it doesn't breathe.
     * 3. **Dead-band filter**: The center only updates if it moves > 5% of the screen.
     */
    private fun handleBarcodeDetected(
        barcode: String?,
        normalizedBounds: android.graphics.RectF?,
    ) {
        if (barcode == null || normalizedBounds == null) {
            // ML Kit missed a frame. Don't clear instantly to avoid flickering.
            if (barcodeLossJob == null && _state.value.detectedBarcodeBounds != null) {
                barcodeLossJob = viewModelScope.launch {
                    delay(500.milliseconds)
                    clearBarcodeState()
                }
            }
            return
        }

        // Barcode detected! Cancel any pending loss job.
        barcodeLossJob?.cancel()
        barcodeLossJob = null

        // Reset the cached size if tracking a new/different barcode.
        if (barcode != lastTrackedBarcodeForSize) {
            lockedBoundsWidth = null
            lockedBoundsHeight = null
            lastEmittedCenterX = null
            lastEmittedCenterY = null
            lastTrackedBarcodeForSize = barcode
        }

        // Lock the width and height to the first detected bounds.
        val w = lockedBoundsWidth ?: normalizedBounds.width().also { lockedBoundsWidth = it }
        val h = lockedBoundsHeight ?: normalizedBounds.height().also { lockedBoundsHeight = it }

        val cx = normalizedBounds.centerX()
        val cy = normalizedBounds.centerY()
        val prevCx = lastEmittedCenterX
        val prevCy = lastEmittedCenterY

        // Dead-band filter: Only update the center if it moved more than 5% (0.05f).
        // This makes the AR overlay rock-solid and extremely stable, ignoring all minor shifts.
        val shouldUpdateCenter = prevCx == null || prevCy == null ||
            kotlin.math.abs(cx - prevCx) > 0.05f ||
            kotlin.math.abs(cy - prevCy) > 0.05f

        if (shouldUpdateCenter) {
            lastEmittedCenterX = cx
            lastEmittedCenterY = cy

            val stableBounds = android.graphics.RectF(
                cx - w / 2f,
                cy - h / 2f,
                cx + w / 2f,
                cy + h / 2f,
            )

            _state.update {
                it.copy(
                    detectedBarcodeBounds = stableBounds,
                    trackedBarcodeValue   = barcode,
                )
            }
        }
    }

    /**
     * No-op stub kept to avoid breaking the event sealed interface.
     * Auto-lock is disabled in the unified Photo mode — users tap the chip instead.
     */
    private fun handleBarcodeLocked(barcode: String) = Unit

    /**
     * Called when the user taps the barcode chip shown by [BarcodeArOverlay] in PHOTO mode.
     * Guards against submission while another scan is already in-flight.
     */
    private fun handleBarcodeChipClicked() {
        if (_state.value.isProcessingCenterAction) return
        val barcode = _state.value.trackedBarcodeValue ?: return
        if (barcode.isBlank()) return
        submitBarcode(barcode)
    }

    /**
     * Submits [barcode] to the backend via [SubmitBarcodeScanUseCase], then polls for the result.
     * Mirrors [submitImageFile] in structure — identical state transitions and error handling.
     */
    private fun submitBarcode(barcode: String) {
        currentScanJob?.cancel()
        _state.update { state ->
            state.copy(
                isScanning = false, // FREEZE the camera and AR overlay immediately
                isProcessingCenterAction = true,
                trackedBarcodeValue = barcode,
                activeScan = ActiveScanUiModel(
                    scanId = "",
                    thumbnailUrl = null,
                    isProcessing = true,
                ),
            )
        }
        currentScanJob = viewModelScope.launch {
            val result = submitBarcodeScanUseCase(barcode)
            result.onSuccess { scanResult ->
                val scanId = scanResult.scanId
                _state.update { state ->
                    state.copy(
                        isProcessingCenterAction = false,
                        activeScan = state.activeScan?.copy(scanId = scanId),
                    )
                }
                pollScanResult(scanId)
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        isProcessingCenterAction = false,
                        activeScan = state.activeScan?.copy(
                            isProcessing = false,
                            isFailed = true,
                        ),
                    )
                }
                _effect.send(
                    CameraScanEffect.ShowSnackBarRes(
                        messageResId = R.string.scan_capture_failed_generic,
                    ),
                )
            }
        }
    }

    /** Fully clears all barcode tracking state, instantly hiding the AR overlay. */
    private fun clearBarcodeState() {
        barcodeLossJob?.cancel()
        barcodeLossJob = null
        lockedBoundsWidth = null
        lockedBoundsHeight = null
        lastTrackedBarcodeForSize = null
        lastEmittedCenterX = null
        lastEmittedCenterY = null
        _state.update { it.copy(detectedBarcodeBounds = null, trackedBarcodeValue = null) }
    }
}
