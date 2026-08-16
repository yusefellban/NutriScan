package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes
import iti.grad.presentation.R

enum class ScanInputMode(
    @StringRes val labelResId: Int,
    @StringRes val hintResId: Int,
) {
    PHOTO(
        labelResId = R.string.scan_mode_photo,
        hintResId = R.string.scan_mode_photo_hint,
    ),
    GALLERY(
        labelResId = R.string.scan_mode_gallery,
        hintResId = R.string.scan_mode_gallery_hint,
    ),
}