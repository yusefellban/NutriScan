package iti.grad.nutriscan.presentation.scan.camera.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
import iti.grad.nutriscan.presentation.scan.camera.state.ScanInputMode
import iti.grad.nutriscan.presentation.scan.camera.view.components.ActiveScanCard
import iti.grad.nutriscan.presentation.scan.camera.view.components.BarcodeArOverlay
import iti.grad.nutriscan.presentation.scan.camera.view.components.BarcodeScanAnalyzer
import iti.grad.nutriscan.presentation.scan.camera.view.components.CameraPreview
import iti.grad.nutriscan.presentation.scan.camera.view.components.ScanFrameOverlay
import iti.grad.nutriscan.presentation.scan.camera.view.components.ScanModeSelector
import iti.grad.nutriscan.presentation.scan.camera.viewmodel.CameraScanViewModel
import iti.grad.presentation.R
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CameraScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState,
    captureTrigger: Int = 0,
    onCenterActionUploadModeChanged: (Boolean) -> Unit = {},
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

    // Create the ML Kit barcode analyzer and run it alongside ImageCapture in PHOTO mode.
    // Keyed on selectedMode so it is re-created (and the old one discarded) on mode switches.
    val barcodeAnalyzer: ImageAnalysis.Analyzer? = remember(state.selectedMode) {
        if (state.selectedMode == ScanInputMode.PHOTO) {
            BarcodeScanAnalyzer { barcode, bounds ->
                viewModel.onEvent(CameraScanEvent.BarcodeDetected(barcode, bounds))
            }
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            mediaActionSound.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.onEvent(CameraScanEvent.PermissionResult(true))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var permissionRequestedFromButton by remember { mutableStateOf(false) }
    var permissionRequestedAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(CameraScanEvent.PermissionResult(granted))
        if (!granted && permissionRequestedFromButton) {
            val timeElapsed = System.currentTimeMillis() - permissionRequestedAt
            if (timeElapsed < 350) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
        }
        permissionRequestedFromButton = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.onEvent(CameraScanEvent.GalleryPickCancelled)
            return@rememberLauncherForActivityResult
        }

        val galleryFile = runCatching { copyUriToCacheFile(uri, context) }.getOrNull()
        if (galleryFile == null) {
            viewModel.onEvent(CameraScanEvent.GalleryPickFailed)
        } else {
            viewModel.onEvent(CameraScanEvent.GalleryImageSelected(galleryFile))
        }
    }

    LaunchedEffect(state.selectedMode) {
        onCenterActionUploadModeChanged(state.selectedMode == ScanInputMode.GALLERY)
    }

    var previousTrigger by remember { mutableIntStateOf(captureTrigger) }

    LaunchedEffect(captureTrigger) {
        if (captureTrigger > previousTrigger) {
            previousTrigger = captureTrigger
            viewModel.onEvent(CameraScanEvent.CenterActionClicked)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CameraScanEffect.ShowSnackBarRes ->
                    snackbarHostState.showAppSnackbar(
                        message = context.getString(effect.messageResId),
                        type = SnackbarType.WARNING,
                    )

                is CameraScanEffect.ShowSnackBar ->
                    snackbarHostState.showAppSnackbar(
                        message = effect.message,
                        type = effect.snackbarType,
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
                        },
                    )
                }

                is CameraScanEffect.OpenGalleryPicker -> {
                    galleryLauncher.launch("image/*")
                }

                is CameraScanEffect.NavigateToProductDetail -> {
                    onNavigateToProductDetail(effect.product)
                }
            }
        }
    }

    val handleEvent: (CameraScanEvent) -> Unit = { event ->
        if (event == CameraScanEvent.RequestPermissionClicked) {
            permissionRequestedFromButton = true
            permissionRequestedAt = System.currentTimeMillis()
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            viewModel.onEvent(event)
        }
    }

    CameraScanContent(
        state = state,
        imageCapture = imageCapture,
        barcodeAnalyzer = barcodeAnalyzer,
        onEvent = handleEvent,
        bottomPadding = bottomPadding,
        flashAlpha = flashAlpha.value,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScanContent(
    state: CameraScanState,
    imageCapture: ImageCapture,
    barcodeAnalyzer: ImageAnalysis.Analyzer?,
    onEvent: (CameraScanEvent) -> Unit,
    bottomPadding: Dp,
    flashAlpha: Float,
) {
    val context = LocalContext.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onEvent(CameraScanEvent.DismissScanClicked)
                true
            } else {
                false
            }
        },
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

    val isGalleryMode = state.selectedMode == ScanInputMode.GALLERY
    val galleryPreviewPath = state.pendingGalleryImagePath ?: state.activeScan?.thumbnailUrl
    val optionsBottomOffset = bottomPadding + 64.dp
    val activeScanBottomOffset = bottomPadding + 164.dp
    val separatorBottomOffset = bottomPadding + 126.dp

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.hasCameraPermission && !isGalleryMode -> {
                CameraPreview(
                    isScanning = state.isScanning,
                    imageCapture = imageCapture,
                    barcodeAnalyzer = barcodeAnalyzer,
                    isPreviewActive = true,
                    modifier = Modifier.fillMaxSize(),
                )
                if (flashAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha)),
                    )
                }
                // AR overlay renders in PHOTO mode whenever a barcode is detected in-frame.
                if (state.selectedMode == ScanInputMode.PHOTO) {
                    BarcodeArOverlay(
                        normalizedBounds      = state.detectedBarcodeBounds,
                        barcodeValue          = state.trackedBarcodeValue,
                        isLocked              = state.isProcessingCenterAction,
                        onBarcodeChipClicked  = { onEvent(CameraScanEvent.BarcodeChipClicked) },
                        modifier              = Modifier.fillMaxSize(),
                    )
                }
            }

            state.permissionDenied && !isGalleryMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    CameraPermissionDeniedContent(
                        onRetry = {
                            onEvent(CameraScanEvent.RequestPermissionClicked)
                        },
                    )
                }
            }

            else -> {
                if (isGalleryMode && !galleryPreviewPath.isNullOrBlank()) {
                    AsyncImage(
                        model = galleryPreviewPath,
                        contentDescription = stringResource(R.string.scan_gallery_selected_image_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppTheme.colors.Background.copy(alpha = 0.94f)),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 78.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Crossfade(targetState = state.selectedMode, label = "scanModeHint") { mode ->
                Text(
                    text = stringResource(mode.hintResId),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.OnPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        state.activeScan?.let { scan ->
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = activeScanBottomOffset),
            ) {
                ActiveScanCard(
                    scan = scan,
                    onBookmarkClick = { onEvent(CameraScanEvent.BookmarkClicked) },
                    onCardClick = { onEvent(CameraScanEvent.CardClicked) },
                    onRetryClick = { onEvent(CameraScanEvent.DismissScanClicked) },
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = separatorBottomOffset)
                    .width(72.dp)
                    .height(4.dp)
                    .background(
                        color = AppTheme.colors.OnPrimary.copy(alpha = 0.32f),
                        shape = RoundedCornerShape(50),
                    ),
            )
        }

        ScanModeSelector(
            selectedMode = state.selectedMode,
            hasSelectedGalleryImage = !state.pendingGalleryImagePath.isNullOrBlank() ||
                (state.selectedMode == ScanInputMode.GALLERY && !state.activeScan?.thumbnailUrl.isNullOrBlank()),
            onModeSelected = { onEvent(CameraScanEvent.ModeSelected(it)) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = optionsBottomOffset),
        )

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

private fun copyUriToCacheFile(uri: Uri, context: Context): File {
    val file = File(context.cacheDir, "gallery_scan_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: error("Failed to open selected image")
    return file
}
