package iti.grad.nutriscan.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream

/**
 * Centralized JPEG downscale/compress helper used before any multipart image
 * upload (avatar, scans, etc.), so every upload pipeline in the app shares
 * the same size/quality behavior instead of duplicating bitmap math.
 */
object ImageCompressor {

    private const val DEFAULT_MAX_DIMENSION = 1024
    private const val DEFAULT_JPEG_QUALITY = 85

    /**
     * Downscales [imageFile] so neither dimension exceeds [maxDimension],
     * re-encodes it as JPEG at [quality], and writes the result next to the
     * original file as "compressed_<name>". Returns [imageFile] unchanged if
     * it can't be decoded as a bitmap.
     */
    fun compress(
        imageFile: File,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): File {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return imageFile

        val ratio = (maxDimension.toFloat() / bitmap.width)
            .coerceAtMost(maxDimension.toFloat() / bitmap.height)

        val compressedBitmap = if (ratio < 1) {
            bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
        } else {
            bitmap
        }

        val outputFile = File(imageFile.parent, "compressed_${imageFile.name}")
        FileOutputStream(outputFile).use { fos ->
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }

        if (compressedBitmap != bitmap) {
            compressedBitmap.recycle()
        }
        bitmap.recycle()

        return outputFile
    }
}
