package iti.grad.nutriscan.presentation.saved.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab

sealed interface SavedEvent {
    data class SearchQueryChanged(val query: String) : SavedEvent
    data class ProductClicked(val productId: String) : SavedEvent
    data class SwipeToAddTriggered(val productId: String) : SavedEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : SavedEvent
}
