package iti.grad.nutriscan.presentation.saved.state

import iti.grad.nutriscan.presentation.common.model.ProductVerdict

data class SavedProductUiModel(
    val id: String,
    val productName: String,
    val imageUrl: String?,
    val verdict: ProductVerdict,
    val calories: String,
)
