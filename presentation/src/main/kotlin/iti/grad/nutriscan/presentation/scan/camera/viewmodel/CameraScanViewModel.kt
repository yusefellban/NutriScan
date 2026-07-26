package iti.grad.nutriscan.presentation.scan.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.DeleteSavedScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.GetScanResultUseCase
import iti.grad.nutriscan.domain.scan.usecase.SaveScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.SubmitScanImageUseCase
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.scan.camera.state.ActiveScanUiModel
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
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
    private val getScanResultUseCase: GetScanResultUseCase,
    private val saveScanUseCase: SaveScanUseCase,
    private val deleteSavedScanUseCase: DeleteSavedScanUseCase,
    private val getSavedScansUseCase: iti.grad.nutriscan.domain.scan.usecase.GetSavedScansUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CameraScanState())
    val state: StateFlow<CameraScanState> = _state.asStateFlow()

    private val _effect = Channel<CameraScanEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentScanJob: Job? = null

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
            is CameraScanEvent.PermissionResult -> handlePermissionResult(event.granted)
            is CameraScanEvent.RequestPermissionClicked -> requestPermission()
            is CameraScanEvent.CaptureClicked -> handleCaptureClicked()
            is CameraScanEvent.ImageCaptured -> handleImageCaptured(event.file)
            is CameraScanEvent.ImageCaptureFailed -> handleImageCaptureFailed(event.error)
            is CameraScanEvent.BookmarkClicked -> handleBookmarkClicked()
            is CameraScanEvent.RetryClicked -> handleRetryClicked()
            is CameraScanEvent.DismissScanClicked -> handleDismissScanClicked()
            is CameraScanEvent.CardClicked -> handleCardClicked()
            is CameraScanEvent.ConfirmDeleteBookmark -> handleConfirmDeleteBookmark()
            is CameraScanEvent.DismissDeleteBookmark -> handleDismissDeleteBookmark()
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

    private fun handleCaptureClicked() {
        _state.update {
            it.copy(
                isScanning = true,
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

    private fun handleImageCaptured(file: File) {
        currentScanJob?.cancel()
        currentScanJob = viewModelScope.launch {
            val submitResult = submitScanImageUseCase(file)
            submitResult.onSuccess { scanResult ->
                val scanId = scanResult.scanId
                _state.update { state ->
                    state.copy(activeScan = state.activeScan?.copy(scanId = scanId))
                }
                pollScanResult(scanId)
            }.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        activeScan = state.activeScan?.copy(
                            isProcessing = false,
                            isFailed = true
                        )
                    )
                }
            }
        }
    }

    private fun handleImageCaptureFailed(error: Exception) {
        _state.update {
            it.copy(
                isScanning = false,
                activeScan = null
            )
        }
        viewModelScope.launch {
            _effect.send(CameraScanEffect.ShowSnackBar("Image capture failed: ${error.message}"))
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
                    _effect.send(CameraScanEffect.ShowSnackBar("Scan saved to Bookmarks"))
                } else {
                    _effect.send(CameraScanEffect.ShowSnackBar("Failed to save scan"))
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
                activeScan = null
            )
        }
    }

    private fun handleDismissScanClicked() {
        currentScanJob?.cancel()
        _state.update {
            it.copy(
                isScanning = true,
                activeScan = null
            )
        }
    }
}
