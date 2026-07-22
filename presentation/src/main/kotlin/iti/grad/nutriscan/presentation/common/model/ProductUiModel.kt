package iti.grad.nutriscan.presentation.common.model

import iti.grad.nutriscan.domain.common.model.ProductVerdict

/** Shared card content shown by [iti.grad.nutriscan.presentation.common.components.ProductCard] — used by both the Saved catalog and the food log. */
data class ProductUiModel(
    val id: String,
    val productName: String,
    val imageUrl: String?,
    val verdict: ProductVerdict,
    val calories: String,
)
