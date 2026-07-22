package iti.grad.nutriscan.presentation.scan.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.scan.camera.state.ActiveScanUiModel
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
import iti.grad.presentation.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import iti.grad.nutriscan.domain.scan.usecase.GetProductByBarcodeUseCase
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CameraScanViewModel @Inject constructor(
    private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CameraScanState())
    val state: StateFlow<CameraScanState> = _state.asStateFlow()

    private val _effect = Channel<CameraScanEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentScanJob: Job? = null
    private var lastScannedBarcode: String? = null

    init {
        viewModelScope.launch {
            _effect.send(CameraScanEffect.RequestCameraPermission)
        }
    }

    fun onEvent(event: CameraScanEvent) {
        when (event) {
            is CameraScanEvent.PermissionResult -> handlePermissionResult(event.granted)
            is CameraScanEvent.RequestPermissionClicked -> requestPermission()
            is CameraScanEvent.BarcodeDetected -> handleBarcodeDetected(event.value)
            is CameraScanEvent.BottomNavTabClicked -> handleBottomNavTab(event.tab)
            is CameraScanEvent.AddToListClicked -> handleAddToList()
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

    private fun handleBarcodeDetected(barcode: String) {
        if (!_state.value.isScanning || barcode == lastScannedBarcode) return

        lastScannedBarcode = barcode
        currentScanJob?.cancel()

        _state.update {
            it.copy(
                activeScan = ActiveScanUiModel(
                    barcode = barcode,
                    brand = null,
                    productName = null,
                    thumbnailUrl = null,
                    statusResId = R.string.scan_status_processing,
                ),
            )
        }

        currentScanJob = viewModelScope.launch {
            val result = getProductByBarcodeUseCase(barcode)
            result.onSuccess { product ->
                _state.update { state ->
                    state.copy(
                        activeScan = state.activeScan?.copy(
                            brand = product.brand,
                            productName = product.productName ?: "Unknown Product",
                            thumbnailUrl = product.imageUrl,
                            healthTag = product.healthTag,
                            statusResId = null
                        )
                    )
                }
            }.onFailure { error ->
                val errorMessage = when (error) {
                    is UnknownHostException,
                    is ConnectException,
                    is SocketException,
                    is SocketTimeoutException,
                    is IOException -> "No Internet Connection"
                    else -> "Product Not Found"
                }
                _state.update { state ->
                    state.copy(
                        activeScan = state.activeScan?.copy(
                            productName = errorMessage,
                            statusResId = null
                        )
                    )
                }
            }
        }
    }

    private fun handleBottomNavTab(tab: BottomNavTab) {
        viewModelScope.launch {
            when (tab) {
                BottomNavTab.HOME -> _effect.send(CameraScanEffect.NavigateToHome)
                BottomNavTab.HISTORY -> _effect.send(CameraScanEffect.NavigateToHistory)
                BottomNavTab.SAVED -> _effect.send(CameraScanEffect.NavigateToSaved)
                BottomNavTab.PROFILE -> _effect.send(CameraScanEffect.NavigateToProfile)
                BottomNavTab.SCAN -> Unit
            }
        }
    }

    private fun handleAddToList() {
        viewModelScope.launch {
            _effect.send(CameraScanEffect.ShowSnackBarRes(R.string.scan_add_to_list_coming_soon))
        }
    }

    private companion object {
        const val NAVIGATION_DELAY_MS = 400L
    }
}
