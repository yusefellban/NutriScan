package iti.grad.nutriscan.presentation.scan.camera.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import android.media.MediaActionSound
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
import iti.grad.nutriscan.presentation.scan.camera.view.components.ActiveScanCard
import iti.grad.nutriscan.presentation.scan.camera.view.components.CameraPreview
import iti.grad.nutriscan.presentation.scan.camera.view.components.ScanFrameOverlay
import iti.grad.nutriscan.presentation.scan.camera.viewmodel.CameraScanViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CameraScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState,
    captureTrigger: Int = 0,
    onNavigateToProductDetail: (ProductUiModel) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build() 
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mediaActionSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    val flashAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(CameraScanEvent.PermissionResult(granted))
    }

    var previousTrigger by remember { mutableIntStateOf(captureTrigger) }

    LaunchedEffect(captureTrigger) {
        if (captureTrigger > previousTrigger) {
            previousTrigger = captureTrigger
            if (state.isScanning) {
                viewModel.onEvent(CameraScanEvent.CaptureClicked)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CameraScanEffect.ShowSnackBarRes ->
                    snackbarHostState.showAppSnackbar(
                        message = context.getString(effect.messageResId),
                        type = SnackbarType.WARNING
                    )
                is CameraScanEffect.ShowSnackBar ->
                    snackbarHostState.showAppSnackbar(
                        message = effect.message,
                        type = effect.snackbarType
                    )
                is CameraScanEffect.RequestCameraPermission -> {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.onEvent(CameraScanEvent.PermissionResult(true))
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                is CameraScanEffect.TakePicture -> {
                    coroutineScope.launch {
                        mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                        flashAlpha.animateTo(1f, animationSpec = tween(50))
                        flashAlpha.animateTo(0f, animationSpec = tween(400))
                    }
                    val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                viewModel.onEvent(CameraScanEvent.ImageCaptured(photoFile))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                viewModel.onEvent(CameraScanEvent.ImageCaptureFailed(exception))
                            }
                        }
                    )
                }
                is CameraScanEffect.NavigateToProductDetail -> {
                    onNavigateToProductDetail(effect.product)
                }
            }
        }
    }

    CameraScanContent(
        state = state,
        imageCapture = imageCapture,
        onEvent = viewModel::onEvent,
        bottomPadding = bottomPadding,
        flashAlpha = flashAlpha.value,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScanContent(
    state: CameraScanState,
    imageCapture: ImageCapture,
    onEvent: (CameraScanEvent) -> Unit,
    bottomPadding: Dp,
    flashAlpha: Float,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onEvent(CameraScanEvent.DismissScanClicked)
                true
            } else {
                false
            }
        }
    )

    if (state.showDeleteDialog) {
        DeleteWarningAlert(
            title = stringResource(id = R.string.alert_remove_saved_title),
            message = stringResource(id = R.string.alert_remove_saved_message),
            confirmText = stringResource(id = R.string.action_remove),
            cancelText = stringResource(id = R.string.action_cancel),
            onConfirm = { onEvent(CameraScanEvent.ConfirmDeleteBookmark) },
            onDismiss = { onEvent(CameraScanEvent.DismissDeleteBookmark) },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when {
            state.hasCameraPermission -> {
                CameraPreview(
                    isScanning = state.isScanning,
                    imageCapture = imageCapture,
                    modifier = Modifier.fillMaxSize(),
                )
                if (flashAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha))
                    )
                }
                ScanFrameOverlay(
                    modifier = Modifier.fillMaxSize(),
                )
            }
            state.permissionDenied -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    CameraPermissionDeniedContent(
                        onRetry = { onEvent(CameraScanEvent.RequestPermissionClicked) },
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.Background),
                )
            }
        }

        state.activeScan?.let { scan ->
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding + 64.dp),
            ) {
                ActiveScanCard(
                    scan = scan,
                    onBookmarkClick = { onEvent(CameraScanEvent.BookmarkClicked) },
                    onCardClick = { onEvent(CameraScanEvent.CardClicked) },
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionDeniedContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.scan_camera_permission_denied),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            textResId = R.string.scan_camera_permission_retry,
            isLoading = false,
            onClick = onRetry,
        )
    }
}
