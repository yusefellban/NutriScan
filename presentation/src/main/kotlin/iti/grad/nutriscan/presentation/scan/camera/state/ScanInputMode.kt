package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes
import iti.grad.presentation.R

enum class ScanInputMode(
    @StringRes val labelResId: Int,
    @StringRes val hintResId: Int,
) {
    BARCODE(
        labelResId = R.string.scan_mode_barcode,
        hintResId = R.string.scan_mode_barcode_hint,
    ),
    PHOTO(
        labelResId = R.string.scan_mode_photo,
        hintResId = R.string.scan_mode_photo_hint,
    ),
    GALLERY(
        labelResId = R.string.scan_mode_gallery,
        hintResId = R.string.scan_mode_gallery_hint,
    ),
}