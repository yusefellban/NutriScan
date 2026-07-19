package iti.grad.nutriscan.presentation.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.home.state.HomeEffect
import iti.grad.nutriscan.presentation.home.state.HomeEvent
import iti.grad.nutriscan.presentation.home.state.HomeHistoryItem
import iti.grad.nutriscan.presentation.home.state.HomeState
import iti.grad.nutriscan.presentation.home.state.VerdictType
import iti.grad.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 *
 * Currently uses dummy data matching the Figma screenshots.
 * In a future sprint this will inject use cases to load real data from the API.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ScanCardClicked -> emitEffect(HomeEffect.NavigateToScan)
            is HomeEvent.ViewAllHistoryClicked -> emitEffect(HomeEffect.NavigateToHistory)
            is HomeEvent.NotificationClicked -> emitEffect(HomeEffect.NavigateToNotifications)
            is HomeEvent.HistoryItemClicked -> emitEffect(
                HomeEffect.NavigateToScanResult(event.itemId)
            )
            is HomeEvent.BottomNavTabClicked -> {
                // Home is the only tab rendered inline; every other tab is a
                // separate destination, so `selectedTab` is intentionally left
                // at HOME — mutating it here would leave this screen's retained
                // ViewModel stuck highlighting the wrong tab when the user
                // navigates back.
                when (event.tab) {
                    BottomNavTab.HOME -> Unit
                    BottomNavTab.HISTORY -> emitEffect(HomeEffect.NavigateToHistory)
                    BottomNavTab.SCAN -> emitEffect(HomeEffect.NavigateToScan)
                    BottomNavTab.SHOPPING -> emitEffect(HomeEffect.NavigateToShopping)
                    BottomNavTab.PROFILE -> emitEffect(HomeEffect.NavigateToProfile)
                }
            }
        }
    }

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun createInitialState(): HomeState = HomeState(
        userName = "Noureldeen",
        recentHistory = persistentListOf(
            HomeHistoryItem(
                id = "scan_001",
                productName = "Orange Juice",
                scanDate = "Today, 9:24 AM",
                verdictLabelResId = R.string.verdict_healthy,
                verdictType = VerdictType.CYAN,
                imageUrl = "https://picsum.photos/seed/juice/200/200"
            ),
            HomeHistoryItem(
                id = "scan_002",
                productName = "Greek Yogurt",
                scanDate = "Yesterday, 4:15 PM",
                verdictLabelResId = R.string.verdict_probiotic,
                verdictType = VerdictType.CYAN,
                imageUrl = "https://picsum.photos/seed/yogurt/200/200"
            ),
            HomeHistoryItem(
                id = "scan_003",
                productName = "Granola Bar",
                scanDate = "Yesterday, 11:30 AM",
                verdictLabelResId = R.string.verdict_high_sugar,
                verdictType = VerdictType.RED,
                imageUrl = "https://picsum.photos/seed/granola/200/200"
            ),
        ),
        selectedTab = BottomNavTab.HOME,
    )
}
