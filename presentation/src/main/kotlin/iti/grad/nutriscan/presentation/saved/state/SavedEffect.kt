package iti.grad.nutriscan.presentation.saved.state

sealed interface SavedEffect {
    data object NavigateToHome : SavedEffect
    data object NavigateToHistory : SavedEffect
    data object NavigateToScan : SavedEffect
    data object NavigateToProfile : SavedEffect
    data class NavigateToProductDetail(val productId: String) : SavedEffect
    data class ShowAddedToListSnackbar(val productName: String) : SavedEffect
}
