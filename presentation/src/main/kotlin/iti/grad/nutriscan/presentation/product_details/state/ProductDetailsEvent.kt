package iti.grad.nutriscan.presentation.product_details.state

sealed interface ProductDetailsEvent {
    data object BackClicked : ProductDetailsEvent
    data object BookmarkToggled : ProductDetailsEvent
}
