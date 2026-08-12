package iti.grad.nutriscan.presentation.saved.state

import iti.grad.nutriscan.presentation.common.model.ProductUiModel

sealed interface SavedEvent {
    data class SearchQueryChanged(val query: String) : SavedEvent
    data class ProductClicked(val product: ProductUiModel) : SavedEvent
    data class SwipeToAddTriggered(val productId: String) : SavedEvent
    data object RetryLoad : SavedEvent
    data object Refreshed : SavedEvent
}
