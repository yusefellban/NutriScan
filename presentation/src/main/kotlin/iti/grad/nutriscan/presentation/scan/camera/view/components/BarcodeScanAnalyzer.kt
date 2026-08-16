package iti.grad.nutriscan.presentation.scan.camera.view.components

import android.graphics.RectF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX [ImageAnalysis.Analyzer] that uses Google ML Kit Barcode Scanning to detect
 * barcodes in every incoming camera frame.
 *
 * Coordinate normalisation:
 * ML Kit reports [Barcode.boundingBox] in image-sensor pixel space. This class normalises
 * those values to the range [0, 1] relative to the analysis-image dimensions before passing
 * them to [onResult]. This makes the caller (ViewModel + UI overlay) completely independent
 * of the physical sensor resolution.
 *
 * Threading:
 * [analyze] is called on the CameraX analysis thread. [onResult] is invoked on that same
 * thread. The ViewModel's [Channel.send] is safe to call from any thread, so callers should
 * simply forward the result to [onEvent] without any extra `withContext` wrapper.
 *
 * Frame management:
 * [ImageProxy.close] is always called inside [addOnCompleteListener] — not in a try/finally
 * inside [analyze] — because ML Kit's processing is asynchronous. Closing early would corrupt
 * the YUV buffer before ML Kit finishes reading it.
 *
 * @param onResult Callback delivering (rawBarcodeValue, normalisedBoundingBox).
 *                 Both are null when no barcode is detected in the frame.
 */
class BarcodeScanAnalyzer(
    private val onResult: (barcode: String?, normalizedBounds: RectF?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                // Product barcodes (retail)
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                // Industrial / logistics barcodes commonly found on product packaging
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                // QR codes — some Egyptian products use these for traceability
                Barcode.FORMAT_QR_CODE,
            )
            .build(),
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val imageWidth  = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val (rotatedWidth, rotatedHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageHeight to imageWidth
        } else {
            imageWidth to imageHeight
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            rotationDegrees,
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                // Pick the first barcode with a non-blank raw value.
                // If multiple barcodes are visible we always prefer the first detected — the
                // ViewModel's debounce logic will stabilise the selection across frames.
                val best = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }

                if (best != null) {
                    val box = best.boundingBox
                    val normalizedBounds = box?.let { r ->
                        // Clamp to [0,1] to guard against rounding-error coordinates
                        // that would place the overlay fractionally outside the preview.
                        RectF(
                            (r.left   / rotatedWidth ).coerceIn(0f, 1f),
                            (r.top    / rotatedHeight).coerceIn(0f, 1f),
                            (r.right  / rotatedWidth ).coerceIn(0f, 1f),
                            (r.bottom / rotatedHeight).coerceIn(0f, 1f),
                        )
                    }
                    onResult(best.rawValue, normalizedBounds)
                } else {
                    onResult(null, null)
                }
            }
            .addOnFailureListener {
                // Analysis error — treat as no detection; do not crash the scanner loop.
                onResult(null, null)
            }
            .addOnCompleteListener {
                // MUST be called here, not in a try/finally around scanner.process(),
                // because ML Kit processing is asynchronous.
                imageProxy.close()
            }
    }
}
