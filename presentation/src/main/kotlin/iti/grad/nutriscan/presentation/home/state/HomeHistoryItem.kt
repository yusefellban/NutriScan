package iti.grad.nutriscan.presentation.home.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Represents a single item in the Recent History section of the Home screen.
 *
 * All user-visible text fields (productName, scanDate, verdictLabel) are populated
 * from string resources at the ViewModel level to ensure localization compliance.
 */
data class HomeHistoryItem(
    val id: String,
    val productName: String,
    val scanDate: String,
    @StringRes val verdictLabelResId: Int,
    val verdictType: VerdictType,
    val imageUrl: String? = null,
)
