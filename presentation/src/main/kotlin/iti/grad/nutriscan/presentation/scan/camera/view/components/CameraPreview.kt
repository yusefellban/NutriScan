package iti.grad.nutriscan.presentation.scan.camera.view.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@Composable
fun CameraPreview(
    isScanning: Boolean,
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

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

    DisposableEffect(lifecycleOwner, previewView, isScanning) {
        val view = previewView
        if (view == null) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            val barcodeScanner = BarcodeScanning.getClient()
            var bound = false

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = view.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var isProcessingFrame = false
                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (!isScanning) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    if (isProcessingFrame) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    isProcessingFrame = true
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees,
                    )
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstOrNull()?.rawValue?.let(onBarcodeDetected)
                        }
                        .addOnCompleteListener {
                            isProcessingFrame = false
                            imageProxy.close()
                        }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
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
                barcodeScanner.close()
            }
        }
    }
}
