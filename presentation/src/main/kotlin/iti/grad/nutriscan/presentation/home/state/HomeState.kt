package iti.grad.nutriscan.presentation.home.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable UI state for the Home screen.
 *
 * All collections use `kotlinx.collections.immutable` to ensure Compose
 * stability and avoid unnecessary recompositions.
 */
data class HomeState(
    val userName: String = "",
    val recentHistory: ImmutableList<HomeHistoryItem> = persistentListOf(),
    val selectedTab: BottomNavTab = BottomNavTab.HOME,
)
