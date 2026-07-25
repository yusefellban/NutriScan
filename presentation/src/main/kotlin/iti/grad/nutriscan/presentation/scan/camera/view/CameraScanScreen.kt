package iti.grad.nutriscan.presentation.scan.camera.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import iti.grad.nutriscan.presentation.scan.camera.viewmodel.CameraScanViewModel
import androidx.compose.foundation.layout.WindowInsets
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.domain.common.model.ProductVerdict.SAFE
import iti.grad.presentation.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEvent
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanEffect
import androidx.compose.ui.platform.LocalContext
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.scan.camera.view.components.CameraPreview
import iti.grad.nutriscan.presentation.scan.camera.view.components.ScanFrameOverlay
import iti.grad.nutriscan.presentation.scan.camera.state.CameraScanState
import androidx.compose.material3.SnackbarHostState
import iti.grad.nutriscan.presentation.scan.camera.view.components.ActiveScanCard
import androidx.compose.runtime.Composable
import iti.grad.nutriscan.presentation.common.components.AppButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CameraScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState,
    onNavigateToProcessing: (String) -> Unit = {},
    onNavigateToProductDetail: (ProductUiModel) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(CameraScanEvent.PermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CameraScanEffect.NavigateToProcessing ->
                    onNavigateToProcessing(effect.barcode)
                is CameraScanEffect.ShowSnackBarRes ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageResId))
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
            }
        }
    }

    CameraScanContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToProductDetail = { barcode ->
            // Use dummy product until CameraScanScreen has access to full ProductUiModel
            onNavigateToProductDetail(ProductUiModel(id = barcode, productName = "", imageUrl = null, verdict = SAFE, calories = "0"))
        },
        bottomPadding = bottomPadding,
    )
}

@Composable
private fun CameraScanContent(
    state: CameraScanState,
    onEvent: (CameraScanEvent) -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    bottomPadding: Dp,
) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            when {
                state.hasCameraPermission -> {
                    CameraPreview(
                        isScanning = state.isScanning,
                        onBarcodeDetected = { barcode ->
                            onEvent(CameraScanEvent.BarcodeDetected(barcode, format = 0))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
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
                ActiveScanCard(
                    scan = scan,
                    onClick = { onNavigateToProductDetail(scan.barcode) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomPadding + 16.dp),
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
