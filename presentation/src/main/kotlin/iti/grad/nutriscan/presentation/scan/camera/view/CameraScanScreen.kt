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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.util.tick
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
import kotlin.math.roundToInt
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
    val haptics = LocalHapticFeedback.current
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mediaActionSound = remember {
        MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }
    }
    val flashAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Create the ML Kit barcode analyzer and run it alongside ImageCapture in PHOTO mode.
    // Keyed on selectedMode so it is re-created (and the old one discarded) on mode switches.
    val barcodeAnalyzer: ImageAnalysis.Analyzer? =
            remember(state.selectedMode) {
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
                val granted =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.onEvent(CameraScanEvent.PermissionResult(true))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var permissionRequestedFromButton by remember { mutableStateOf(false) }
    var permissionRequestedAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val permissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                viewModel.onEvent(CameraScanEvent.PermissionResult(granted))
                if (!granted && permissionRequestedFromButton) {
                    val timeElapsed = System.currentTimeMillis() - permissionRequestedAt
                    if (timeElapsed < 350) {
                        val intent =
                                Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                )
                        context.startActivity(intent)
                    }
                }
                permissionRequestedFromButton = false
            }

    val cropLauncher =
            rememberLauncherForActivityResult(
                    contract = CropImageContract(),
            ) { result ->
                if (result.isSuccessful) {
                    val uriContent = result.uriContent
                    if (uriContent == null) {
                        viewModel.onEvent(CameraScanEvent.GalleryPickFailed)
                        return@rememberLauncherForActivityResult
                    }
                    val galleryFile =
                            runCatching { copyUriToCacheFile(uriContent, context) }.getOrNull()
                    if (galleryFile == null) {
                        viewModel.onEvent(CameraScanEvent.GalleryPickFailed)
                    } else {
                        viewModel.onEvent(CameraScanEvent.GalleryImageSelected(galleryFile))
                    }
                } else {
                    // User cancelled cropping, we treat it as if they cancelled picking
                    viewModel.onEvent(CameraScanEvent.GalleryPickCancelled)
                }
            }

    val galleryLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent(),
            ) { uri: Uri? ->
                if (uri == null) {
                    viewModel.onEvent(CameraScanEvent.GalleryPickCancelled)
                    return@rememberLauncherForActivityResult
                }

                cropLauncher.launch(
                        CropImageContractOptions(
                                uri = uri,
                                cropImageOptions =
                                        CropImageOptions(
                                                imageSourceIncludeGallery = false,
                                                imageSourceIncludeCamera = false,
                                                guidelines =
                                                        com.canhub.cropper.CropImageView.Guidelines
                                                                .ON,
                                                showIntentChooser = false,
                                        )
                        )
                )
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
                    val granted =
                            ContextCompat.checkSelfPermission(
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
                    haptics.tick()
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
                                override fun onImageSaved(
                                        outputFileResults: ImageCapture.OutputFileResults
                                ) {
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
    // True for the whole submit → poll window, not just the initial submit call — used to
    // lock every entry point that would otherwise hit the backend again mid-scan.
    val isBusy = state.isProcessingCenterAction || state.activeScan?.isProcessing == true

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
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(Color.White.copy(alpha = flashAlpha)),
                    )
                }
                // AR overlay renders in PHOTO mode whenever a barcode is detected in-frame.
                if (state.selectedMode == ScanInputMode.PHOTO) {
                    BarcodeArOverlay(
                            normalizedBounds = state.detectedBarcodeBounds,
                            barcodeValue = state.trackedBarcodeValue,
                            isLocked = isBusy,
                            onBarcodeChipClicked = { onEvent(CameraScanEvent.BarcodeChipClicked) },
                            modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            state.permissionDenied && !isGalleryMode -> {
                Box(
                        modifier = Modifier.fillMaxSize().background(AppTheme.colors.Background),
                        contentAlignment = Alignment.Center,
                ) {
                    CameraPermissionDeniedContent(
                            onRetry = { onEvent(CameraScanEvent.RequestPermissionClicked) },
                    )
                }
            }
            else -> {
                if (isGalleryMode && !galleryPreviewPath.isNullOrBlank()) {
                    AsyncImage(
                            model = galleryPreviewPath,
                            contentDescription =
                                    stringResource(
                                            R.string.scan_gallery_selected_image_content_description
                                    ),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(
                                                    AppTheme.colors.Background.copy(alpha = 0.94f)
                                            ),
                    )
                }
            }
        }

        // Scan beam overlay — shown only in camera (PHOTO) mode while actively scanning
        if (!state.permissionDenied && !isGalleryMode && state.isScanning) {
            ScanFrameOverlay(
                    selectedMode = state.selectedMode,
                    modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
                modifier =
                        Modifier.align(Alignment.TopCenter)
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
            // key(scanId) forces a full recomposition — and a brand-new drag offset —
            // every time a different scan arrives, so the sheet never carries over a stale
            // dismissed offset from the previous swipe.
            key(scan.scanId) {
                Box(
                        modifier =
                                Modifier.align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        // No bottom padding here — the sheet reaches all the way
                                        // to the screen edge, sitting "behind" the nav bar / center
                                        // capture button the same way the Figma sheet does.
                ) {
                    ActiveScanCard(
                            scan = scan,
                            bottomSafeInset = bottomPadding,
                            onBookmarkClick = { onEvent(CameraScanEvent.BookmarkClicked) },
                            onCardClick = { onEvent(CameraScanEvent.CardClicked) },
                            onRetryClick = { onEvent(CameraScanEvent.RetryClicked) },
                    )
                }
            }
        }

        // The camera/gallery mode row only shows when there's no result sheet on screen —
        // in the Figma, once the sheet is up, all that's left on top of it is the center
        // capture button + nav bar, not this row as an extra layer.
        if (state.activeScan == null) {
            ScanModeSelector(
                    selectedMode = state.selectedMode,
                    hasSelectedGalleryImage = !state.pendingGalleryImagePath.isNullOrBlank(),
                    onModeSelected = { onEvent(CameraScanEvent.ModeSelected(it)) },
                    enabled = !isBusy,
                    modifier =
                            Modifier.align(Alignment.BottomCenter)
                                    .padding(bottom = optionsBottomOffset),
            )
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

private fun copyUriToCacheFile(uri: Uri, context: Context): File {
    val file = File(context.cacheDir, "gallery_scan_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
            ?: error("Failed to open selected image")
    return file
}