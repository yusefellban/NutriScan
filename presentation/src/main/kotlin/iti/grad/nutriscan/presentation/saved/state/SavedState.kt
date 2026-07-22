package iti.grad.nutriscan.presentation.saved.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SavedState(
    val searchQuery: String = "",
    val products: ImmutableList<SavedProductUiModel> = persistentListOf(),
    val filteredProducts: ImmutableList<SavedProductUiModel> = persistentListOf(),
    val selectedTab: BottomNavTab = BottomNavTab.SAVED,
)
