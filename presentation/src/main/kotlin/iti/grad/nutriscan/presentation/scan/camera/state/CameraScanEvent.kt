package iti.grad.nutriscan.presentation.scan.camera.state


sealed interface CameraScanEvent {
    data class PermissionResult(val granted: Boolean) : CameraScanEvent
    data object RequestPermissionClicked : CameraScanEvent
    data class BarcodeDetected(val value: String, val format: Int) : CameraScanEvent
    data object AddToListClicked : CameraScanEvent
}
