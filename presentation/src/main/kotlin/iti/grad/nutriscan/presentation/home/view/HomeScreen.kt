package iti.grad.nutriscan.presentation.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.home.state.HomeEffect
import iti.grad.nutriscan.presentation.home.state.HomeEvent
import iti.grad.nutriscan.presentation.home.state.HomeState
import iti.grad.nutriscan.presentation.home.view.components.DailyHealthTipCard
import iti.grad.nutriscan.presentation.home.view.components.HistoryItemCard
import iti.grad.nutriscan.presentation.home.view.components.HomeGreetingHeader
import iti.grad.nutriscan.presentation.home.view.components.ScanReadyCard
import iti.grad.nutriscan.presentation.home.viewmodel.HomeViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

/**
 * Home Screen — main entry point after authentication.
 *
 * Follows MVI: collects [HomeState] from [HomeViewModel], dispatches [HomeEvent],
 * and handles [HomeEffect] for one-shot navigation.
 *
 * All text comes from string resources. All colors from [AppTheme].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToScan: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToCalories: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToScanResult: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToScan -> onNavigateToScan()
                is HomeEffect.NavigateToHistory -> onNavigateToHistory()
                is HomeEffect.NavigateToCalories -> onNavigateToCalories()
                is HomeEffect.NavigateToSaved -> onNavigateToSaved()
                is HomeEffect.NavigateToProfile -> onNavigateToProfile()
                is HomeEffect.NavigateToNotifications -> onNavigateToNotifications()
                is HomeEffect.NavigateToScanResult -> onNavigateToScanResult(effect.scanId)
            }
        }
    }

    HomeScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    Scaffold(
        containerColor = AppTheme.colors.Background,
        bottomBar = {
            AppBottomNavBar(
                selectedTab = state.selectedTab,
                onTabClick = { tab -> onEvent(HomeEvent.BottomNavTabClicked(tab)) },
            )
        },
    ) { innerPadding ->
        // Home is the only tab rendered inline — every other bottom-nav tab
        // navigates to its own destination (see HomeViewModel.onEvent).
        HomeFeedContent(state, onEvent, innerPadding)
    }
}

@Composable
private fun HomeFeedContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.Background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Greeting Header ──
            item {
                HomeGreetingHeader(
                    userName = state.userName,
                    avatarUrl = state.avatarUrl,
                    onNotificationClick = { onEvent(HomeEvent.NotificationClicked) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ── Daily Health Tip ──
            item {
                DailyHealthTipCard()
            }

            // ── Scan Ready Card ──
            item {
                Spacer(modifier = Modifier.height(10.dp))
                ScanReadyCard(
                    onClick = { onEvent(HomeEvent.ScanCardClicked) },
                )
            }

            // ── Recent History Header ──
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_recent_history),
                        style = AppTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.PrimaryVariant,
                    )
                    Text(
                        text = stringResource(R.string.home_view_all),
                        style = AppTheme.typography.titleMedium,
                        color = AppTheme.colors.Teal800,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onEvent(HomeEvent.ViewAllHistoryClicked) },
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ── History Items ──
            items(
                items = state.recentHistory,
                key = { it.id },
            ) { historyItem ->
                HistoryItemCard(
                    item = historyItem,
                    onClick = { onEvent(HomeEvent.HistoryItemClicked(historyItem.id)) },
                )
            }

            // Bottom spacing to account for the bottom nav bar overflow
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
