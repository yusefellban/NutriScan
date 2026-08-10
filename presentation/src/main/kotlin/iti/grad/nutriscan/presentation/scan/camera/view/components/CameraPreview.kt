package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * CameraX preview composable that optionally binds a live [ImageAnalysis] use case
 * for real-time barcode detection.
 *
 * @param isScanning       Controls whether the camera pipeline is active.
 * @param imageCapture     The [ImageCapture] use case for photo mode.
 * @param isPreviewActive  Additional gate — when false, camera is unbound immediately.
 * @param barcodeAnalyzer  When non-null, a [ImageAnalysis] use case is created and bound
 *                         alongside [imageCapture]. Use [STRATEGY_KEEP_ONLY_LATEST] so
 *                         frames the analyzer cannot keep up with are dropped and the
 *                         overlay always reflects the current camera view.
 *                         Pass null in PHOTO / GALLERY modes (zero overhead).
 */
@Composable
fun CameraPreview(
    isScanning: Boolean,
    imageCapture: ImageCapture,
    isPreviewActive: Boolean = true,
    barcodeAnalyzer: ImageAnalysis.Analyzer? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Dedicated single-thread executor for image analysis.
    // Created once per barcodeAnalyzer instance; shut down when the analyzer changes
    // or when the composable leaves the composition.
    val analysisExecutor = remember(barcodeAnalyzer) {
        if (barcodeAnalyzer != null) Executors.newSingleThreadExecutor() else null
    }

    DisposableEffect(analysisExecutor) {
        onDispose {
            analysisExecutor?.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        },
        modifier = modifier,
        update = { view -> previewView = view },
    )

    DisposableEffect(lifecycleOwner, previewView, isScanning, isPreviewActive, barcodeAnalyzer) {
        val view = previewView
        if (view == null || !isScanning || !isPreviewActive) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            var bound = false

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = view.surfaceProvider
                }

                // Build ImageAnalysis use case only when a live analyzer is provided.
                val imageAnalysis = if (barcodeAnalyzer != null && analysisExecutor != null) {
                    ImageAnalysis.Builder()
                        // Drop frames the analyzer cannot keep up with — ensures the overlay
                        // always reflects the latest camera view, never a stale old frame.
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, barcodeAnalyzer) }
                } else {
                    null
                }

                try {
                    cameraProvider.unbindAll()
                    val useCases = buildList {
                        add(preview)
                        add(imageCapture)
                        if (imageAnalysis != null) add(imageAnalysis)
                    }
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        *useCases.toTypedArray(),
                    )
                    bound = true
                } catch (_: Exception) {
                    // Camera binding failed — e.g. emulator without camera hardware.
                }
            }, executor)

            onDispose {
                if (bound) {
                    runCatching {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    }
                }
            }
        }
    }
}
