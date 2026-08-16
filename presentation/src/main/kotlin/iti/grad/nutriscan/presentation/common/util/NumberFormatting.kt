package iti.grad.nutriscan.presentation.common.util

import java.util.Locale
import kotlin.math.roundToLong

/** Rounds to the nearest 0.5 (e.g. 12.37 -> 12.5, 12.1 -> 12.0). */
fun Float.roundToNearestHalf(): Float = (this * 2).roundToLong() / 2f

/** Formats a value already rounded to the nearest 0.5 as "12" or "12.5" (no other decimals). */
fun Float.formatHalfStep(): String {
    val rounded = roundToNearestHalf()
    return if (rounded % 1f == 0f) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", rounded)
    }
}
