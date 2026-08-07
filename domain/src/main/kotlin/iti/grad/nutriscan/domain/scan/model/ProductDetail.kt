package iti.grad.nutriscan.domain.scan.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict
import java.time.LocalDate

/**
 * Full product detail used by the Product Details screen.
 *
 * Holds everything shown on the detail page: header info, verdict,
 * flagged ingredients with allergy/condition matches, and nutritional macros.
 */
data class ProductDetail(
    val id: String,
    val productName: String,
    val brand: String?,
    val imageUrl: String?,
    val verdict: ProductVerdict,
    val scanDate: LocalDate?,
    val safetyReasonText: String?,
    val flaggedIngredients: List<FlaggedIngredient>,
    val calories: String?,
    val servingSize: String?,
    val protein: String?,
    val carbs: String?,
    val fat: String?,
    val fiber: String?,
    val sugar: String?,
    val sodium: String?,
    val isBookmarked: Boolean,
)
