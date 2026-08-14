package iti.grad.nutriscan.presentation.common.model

import androidx.annotation.StringRes

/**
 * Represents the type of health verdict badge shown on a scan history item.
 */
enum class VerdictType {
    GREEN,
    CYAN,
    YELLOW,
    RED,
    FAILED,
}

/**
 * Represents a single item in the Recent History or Scan History lists.
 *
 * All user-visible text fields (productName, scanDate, verdictLabel) are populated
 * from string resources at the ViewModel level to ensure localization compliance.
 */
data class HistoryItemUiModel(
    val id: String,
    val productName: String,
    val scanDate: UiText,
    @StringRes val verdictLabelResId: Int,
    val verdictType: VerdictType,
    val imageUrl: String? = null,
)
