package iti.grad.nutriscan.presentation.scan.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.GetScanResultUseCase
import iti.grad.nutriscan.domain.scan.usecase.SaveScanUseCase
import iti.grad.nutriscan.domain.scan.usecase.SubmitScanImageUseCase
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

@HiltViewModel
class CameraScanViewModel @Inject constructor(
    private val submitScanImageUseCase: SubmitScanImageUseCase,
    private val getScanResultUseCase: GetScanResultUseCase,
    private val saveScanUseCase: SaveScanUseCase
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
        viewModelScope.launch {
            _effect.send(CameraScanEffect.TakePicture)
        }
    }

    private fun handleImageCaptured(file: File) {
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
                                    healthTag = scanResult.foodSafetyResponse?.verdict?.name,
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
            delay(3000)
        }
    }

    private fun handleBookmarkClicked() {
        val currentScan = _state.value.activeScan?.fullResult ?: return
        viewModelScope.launch {
            val result = saveScanUseCase(currentScan)
            if (result.isSuccess) {
                _effect.send(CameraScanEffect.ShowSnackBar("Scan saved to Bookmarks"))
            } else {
                _effect.send(CameraScanEffect.ShowSnackBar("Failed to save scan"))
            }
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
