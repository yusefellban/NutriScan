package iti.grad.nutriscan.presentation.scan.camera.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
import iti.grad.nutriscan.presentation.scan.camera.view.components.ActiveScanCard
import iti.grad.nutriscan.presentation.scan.camera.view.components.CameraPreview
import iti.grad.nutriscan.presentation.scan.camera.view.components.CaptureButton
import iti.grad.nutriscan.presentation.scan.camera.view.components.ScanFrameOverlay
import iti.grad.nutriscan.presentation.scan.camera.viewmodel.CameraScanViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.util.concurrent.Executors

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CameraScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(CameraScanEvent.PermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CameraScanEffect.ShowSnackBarRes ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageResId))
                is CameraScanEffect.ShowSnackBar ->
                    snackbarHostState.showSnackbar(effect.message)
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
            }
        }
    }

    CameraScanContent(
        state = state,
        imageCapture = imageCapture,
        onEvent = viewModel::onEvent,
        bottomPadding = bottomPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScanContent(
    state: CameraScanState,
    imageCapture: ImageCapture,
    onEvent: (CameraScanEvent) -> Unit,
    bottomPadding: Dp,
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
                ScanFrameOverlay(
                    modifier = Modifier.fillMaxSize(),
                )
                
                if (state.isScanning) {
                    CaptureButton(
                        onClick = { onEvent(CameraScanEvent.CaptureClicked) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomPadding + 32.dp)
                    )
                }
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
                    .padding(bottom = bottomPadding + 16.dp),
            ) {
                ActiveScanCard(
                    scan = scan,
                    onBookmarkClick = { onEvent(CameraScanEvent.BookmarkClicked) },
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
