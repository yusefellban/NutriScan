package iti.grad.nutriscan.presentation.product_details.state

sealed interface ProductDetailsEffect {
    data object NavigateBack : ProductDetailsEffect
}
