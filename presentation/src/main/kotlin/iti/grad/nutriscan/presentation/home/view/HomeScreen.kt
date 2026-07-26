package iti.grad.nutriscan.presentation.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.EmptyStateWidget
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.home.state.HomeEffect
import iti.grad.nutriscan.presentation.home.state.HomeEvent
import iti.grad.nutriscan.presentation.home.state.HomeState
import iti.grad.nutriscan.presentation.home.view.components.DailyHealthTipCard
import iti.grad.nutriscan.presentation.home.view.components.ExploreItemRow
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
    bottomPadding: Dp = 0.dp,
    onNavigateToScanResult: (String) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToNews: () -> Unit = {},
    onNavigateToChatWithAi: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToNotifications -> onNavigateToNotifications()
                is HomeEffect.NavigateToScanResult -> onNavigateToScanResult(effect.scanId)
                is HomeEffect.NavigateToNews -> onNavigateToNews()
                is HomeEffect.NavigateToChatWithAi -> onNavigateToChatWithAi()
                is HomeEffect.NavigateToScan -> onNavigateToScan()
                is HomeEffect.NavigateToHistory -> onNavigateToHistory()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.RefreshHistorySilently)
    }

    HomeScreenContent(
        state = state,
        bottomPadding = bottomPadding,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun HomeScreenContent(
    state: HomeState,
    bottomPadding: Dp,
    onEvent: (HomeEvent) -> Unit,
) {
    HomeFeedContent(state, onEvent, bottomPadding)
}

@Composable
private fun HomeFeedContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    bottomPadding: Dp
) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.Background)
                .padding(top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()),
            contentPadding = PaddingValues(
                bottom = bottomPadding,
            ),
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

            // ── Explore Section ──
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.home_explore),
                    style = AppTheme.typography.headlineMedium,
                    color = AppTheme.colors.PrimaryVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExploreItemRow(
                        iconResId = R.drawable.ic_health_news,
                        label = stringResource(R.string.home_health_news),
                        onClick = { onEvent(HomeEvent.HealthNewsClicked) },
                    )
                    ExploreItemRow(
                        iconResId = R.drawable.ic_chat_ai,
                        label = stringResource(R.string.home_chat_with_ai),
                        onClick = { onEvent(HomeEvent.ChatWithAiClicked) },
                    )
                }
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
                        style = AppTheme.typography.headlineMedium,
                        color = AppTheme.colors.PrimaryVariant,
                    )
                    Text(
                        text = stringResource(R.string.home_view_all),
                        style = AppTheme.typography.bodyLarge,
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
            when {
                state.isHistoryLoading -> {
                    item {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppTheme.colors.Primary)
                        }
                    }
                }
                state.historyError != null -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.historyError, color = AppTheme.colors.VerdictRedText)
                            Spacer(modifier = Modifier.height(16.dp))
                            AppButton(
                                textResId = R.string.common_retry,
                                isLoading = false,
                                onClick = { onEvent(HomeEvent.RetryLoadHistory) }
                            )
                        }
                    }
                }
                state.recentHistory.isEmpty() -> {
                    item {
                        EmptyStateWidget(
                            message = "No recent scans found",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    items(
                        items = state.recentHistory,
                        key = { it.id },
                    ) { historyItem ->
                        HistoryItemCard(
                            item = historyItem,
                            onClick = { onEvent(HomeEvent.HistoryItemClicked(historyItem.id)) },
                        )
                    }
                }
            }

            // Bottom spacing to account for the bottom nav bar overflow
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
