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

    /** Debounce job for the 1.5 s barcode stability timer. */
    private var barcodeLockJob: Job? = null

    /**
     * The barcode value for which a lock timer is currently running.
     * Used to detect when a different barcode enters the frame mid-timer so we can reset.
     */
    private var lastLockedBarcode: String? = null

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

        // Cancel any in-flight image scan or barcode lock when switching modes.
        if (mode == ScanInputMode.GALLERY || mode == ScanInputMode.PHOTO) {
            currentScanJob?.cancel()
        }
        if (mode != ScanInputMode.BARCODE) {
            cancelBarcodeLock()
        }

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
            ScanInputMode.BARCODE,
            ScanInputMode.PHOTO,
            -> handleCaptureClicked()

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
                        _state.update { state ->
                            state.copy(
                                activeScan = state.activeScan?.copy(
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
                            )
                        }
                        return
                    }
                    ScanStatus.FAILED -> {
                        _state.update { state ->
                            state.copy(
                                activeScan = state.activeScan?.copy(
                                    isProcessing = false,
                                    isFailed = true
                                )
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
        cancelBarcodeLock()
        _state.update {
            val shouldKeepGalleryPreview = it.selectedMode == ScanInputMode.GALLERY
            val previewToKeep = if (shouldKeepGalleryPreview) {
                // Keep only local file path; remote thumbnail URL cannot be uploaded as File.
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
     *
     * Always updates the AR overlay position. If the barcode is new (or changed) starts a
     * [BARCODE_LOCK_DELAY_MS] debounce timer. If the same barcode was already being timed,
     * the timer is left running. A null detection cancels any pending timer.
     */
    private fun handleBarcodeDetected(
        barcode: String?,
        normalizedBounds: android.graphics.RectF?,
    ) {
        // Always refresh the AR overlay, even while a submission is in-flight,
        // so the bracket keeps tracking the product if the camera moves.
        _state.update {
            it.copy(
                detectedBarcodeBounds = normalizedBounds,
                trackedBarcodeValue   = barcode,
            )
        }

        // If a submission is already in-flight, do not start a new lock timer.
        if (_state.value.isProcessingCenterAction) return

        if (barcode == null) {
            cancelBarcodeLock()
            return
        }

        // Same barcode — let the existing timer finish.
        if (barcode == lastLockedBarcode && barcodeLockJob?.isActive == true) return

        // New (or changed) barcode — reset the stability timer.
        cancelBarcodeLock()
        lastLockedBarcode = barcode
        barcodeLockJob = viewModelScope.launch {
            delay(BARCODE_LOCK_DELAY_MS.milliseconds)
            onEvent(CameraScanEvent.BarcodeLocked(barcode))
        }
    }

    /**
     * Called after [BARCODE_LOCK_DELAY_MS] of stable detection.
     * Guards against double-submission if the barcode lock fires while a scan is already running.
     */
    private fun handleBarcodeLocked(barcode: String) {
        if (_state.value.isProcessingCenterAction) return
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

    /** Cancels any pending barcode stability timer and clears associated tracking state. */
    private fun cancelBarcodeLock() {
        barcodeLockJob?.cancel()
        barcodeLockJob = null
        lastLockedBarcode = null
    }

    companion object {
        /**
         * Duration in milliseconds that the same barcode must be continuously detected before
         * we auto-submit it to the backend. 1 500 ms balances responsiveness with accuracy —
         * long enough to avoid accidental triggers during a camera sweep.
         */
        const val BARCODE_LOCK_DELAY_MS = 1_500L
    }
}
