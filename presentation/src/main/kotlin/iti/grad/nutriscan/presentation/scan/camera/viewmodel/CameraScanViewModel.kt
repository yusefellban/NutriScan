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
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class CameraScanViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(CameraScanState())
    val state: StateFlow<CameraScanState> = _state.asStateFlow()

    private val _effect = Channel<CameraScanEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var scanCooldownActive = false

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
        if (scanCooldownActive || !_state.value.isScanning) return

        scanCooldownActive = true
        _state.update {
            it.copy(
                isScanning = false,
                activeScan = ActiveScanUiModel(
                    barcode = barcode,
                    brand = null,
                    productName = null,
                    thumbnailUrl = null,
                    statusResId = R.string.scan_status_processing,
                ),
            )
        }

        viewModelScope.launch {
            delay(NAVIGATION_DELAY_MS.milliseconds)
            _effect.send(CameraScanEffect.NavigateToProcessing(barcode))
        }
    }

    private fun handleBottomNavTab(tab: BottomNavTab) {
        viewModelScope.launch {
            when (tab) {
                BottomNavTab.HOME -> _effect.send(CameraScanEffect.NavigateToHome)
                BottomNavTab.HISTORY -> _effect.send(CameraScanEffect.NavigateToHistory)
                BottomNavTab.SHOPPING -> _effect.send(CameraScanEffect.NavigateToShopping)
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
