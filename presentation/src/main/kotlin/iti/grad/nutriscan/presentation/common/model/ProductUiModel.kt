package iti.grad.nutriscan.presentation.common.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict

import kotlinx.serialization.Serializable

/** Shared card content shown by [iti.grad.nutriscan.presentation.common.components.ProductCard] — used by both the Saved catalog and the food log. */
@Serializable
data class ProductUiModel(
    val id: String,
    val productName: String,
    val imageUrl: String?,
    val verdict: ProductVerdict,
    val calories: String,
    /** True when the scan itself failed (status == "FAILED") — shows a "Failed" badge instead of verdict. */
    val isFailed: Boolean = false,
    /** How many times this product was logged today — shown as an "x2"-style badge instead of
     * duplicating the card. 1 means no badge. */
    val quantity: Int = 1,
    /** The single most-recently-added [iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry.id]
     * behind this grouped card — swiping to remove deletes just this one entry, decrementing
     * [quantity] rather than removing the whole product. Null outside the food log (e.g. the
     * Saved catalog, which has no log entry to target). */
    val logEntryId: String? = null,
)
