package iti.grad.nutriscan.domain.scan.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict

/**
 * A single ingredient flagged as potentially problematic.
 * Maps to the backend `flaggedIngredients` array element.
 */
data class ScanFlaggedIngredient(
    /** The ingredient text extracted from the label (e.g. "skimmed milk powder"). */
    val ingredient: String,
    /** Human-readable reason why this ingredient was flagged. */
    val reason: String,
    /** Flag type from the backend — e.g. "ALLERGY", "CONDITION". */
    val type: String,
    /** Alternative names or matched profile tags. */
    val name: List<String>,
)

/**
 * The AI food-safety evaluation for a scanned product.
 * Contains the overall verdict, the list of flagged ingredients,
 * and per-family-member alerts.
 *
 * ⚠️ Health-critical: verdict must never be silently defaulted —
 * a null verdict means analysis is still in progress or failed.
 * familyAlerts must never be truncated or discarded.
 */
data class FoodSafetyResponse(
    val verdict: ProductVerdict?,
    val flaggedIngredients: List<ScanFlaggedIngredient>,
    val summary: String?,
    val familyAlerts: List<FamilyAlert> = emptyList(),
)
