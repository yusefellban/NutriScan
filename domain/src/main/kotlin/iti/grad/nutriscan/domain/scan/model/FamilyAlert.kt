package iti.grad.nutriscan.domain.scan.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict

/**
 * A safety alert for a specific family member regarding the scanned product.
 *
 * ⚠️ Health-critical: This alert must never be silently dropped or defaulted.
 * A missing alert for a member with a nut allergy when the product contains nuts
 * is a direct health risk. Always propagate this list in full through all layers.
 *
 * @param targetProfile The display name of the at-risk family member (e.g. "Ahmed (Son)").
 * @param severity The verdict for this specific member — may differ from the product's overall verdict.
 * @param reason Human-readable explanation of why this member is at risk.
 */
data class FamilyAlert(
    val targetProfile: String,
    val severity: ProductVerdict,
    val reason: String,
    val targetImageUrl: String? = null,
)
