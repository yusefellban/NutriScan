package iti.grad.nutriscan.presentation.home.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import iti.grad.nutriscan.presentation.common.model.HistoryItemUiModel

/**
 * Immutable UI state for the Home screen.
 *
 * All collections use `kotlinx.collections.immutable` to ensure Compose
 * stability and avoid unnecessary recompositions.
 */
data class HomeState(
    val userName: String = "",
    val firstName: String = "",
    val avatarUrl: String? = null,
    val avatarUpdatedAt: String? = null,
    val isLoading: Boolean = false,
    val isHistoryLoading: Boolean = false,
    /** Drives the pull-to-refresh indicator; [isHistoryLoading] covers the first load only. */
    val isRefreshing: Boolean = false,
    val historyError: String? = null,
    val recentHistory: ImmutableList<HistoryItemUiModel> = persistentListOf(),
)
