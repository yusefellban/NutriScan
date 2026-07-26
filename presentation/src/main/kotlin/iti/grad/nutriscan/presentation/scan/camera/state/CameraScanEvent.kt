package iti.grad.nutriscan.presentation.scan.camera.state


import java.io.File

sealed interface CameraScanEvent {
    data class PermissionResult(val granted: Boolean) : CameraScanEvent
    data object RequestPermissionClicked : CameraScanEvent
    data object CaptureClicked : CameraScanEvent
    data class ImageCaptured(val file: File) : CameraScanEvent
    data class ImageCaptureFailed(val error: Exception) : CameraScanEvent
    data object BookmarkClicked : CameraScanEvent
    data object RetryClicked : CameraScanEvent
    data object DismissScanClicked : CameraScanEvent
    data object CardClicked : CameraScanEvent
    data object ConfirmDeleteBookmark : CameraScanEvent
    data object DismissDeleteBookmark : CameraScanEvent
}
