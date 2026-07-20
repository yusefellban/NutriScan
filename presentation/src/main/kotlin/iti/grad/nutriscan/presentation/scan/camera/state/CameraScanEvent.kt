package iti.grad.nutriscan.presentation.scan.camera.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab

sealed interface CameraScanEvent {
    data class PermissionResult(val granted: Boolean) : CameraScanEvent
    data object RequestPermissionClicked : CameraScanEvent
    data class BarcodeDetected(val value: String, val format: Int) : CameraScanEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : CameraScanEvent
    data object AddToListClicked : CameraScanEvent
}
