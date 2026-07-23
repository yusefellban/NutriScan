package iti.grad.nutriscan.presentation.saved.state

import iti.grad.nutriscan.presentation.common.model.ProductUiModel

sealed interface SavedEffect {
    data class NavigateToProductDetail(val product: ProductUiModel) : SavedEffect
    data class ShowAddedToFoodLogSnackbar(val productName: String) : SavedEffect
    data object ShowAddErrorSnackbar : SavedEffect
}
