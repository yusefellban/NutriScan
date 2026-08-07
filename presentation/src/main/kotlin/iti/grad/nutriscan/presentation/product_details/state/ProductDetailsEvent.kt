package iti.grad.nutriscan.presentation.product_details.state

sealed interface ProductDetailsEvent {
    data object BackClicked : ProductDetailsEvent
    data object BookmarkToggled : ProductDetailsEvent
    data object ConfirmDeleteBookmark : ProductDetailsEvent
    data object DismissDeleteBookmark : ProductDetailsEvent
    data object RetryLoad : ProductDetailsEvent
}
