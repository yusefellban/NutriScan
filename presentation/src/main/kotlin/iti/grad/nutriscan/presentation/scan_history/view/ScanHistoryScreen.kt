package iti.grad.nutriscan.presentation.scan_history.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppTopHeader

import iti.grad.nutriscan.presentation.common.components.HistoryItemCard
import iti.grad.nutriscan.presentation.common.components.HistoryItemShimmerCard
import iti.grad.nutriscan.presentation.common.components.OfflineStateWidget
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.scan_history.state.HistoryFilter
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEffect
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEvent
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryState
import iti.grad.nutriscan.presentation.scan_history.viewmodel.ScanHistoryViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ScanHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    viewModel: ScanHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ScanHistoryEffect.NavigateBack -> onNavigateBack()
                is ScanHistoryEffect.NavigateToProductDetails -> onNavigateToProductDetails(effect.scanId)
            }
        }
    }

    ScanHistoryContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun ScanHistoryContent(
    state: ScanHistoryState,
    onEvent: (ScanHistoryEvent) -> Unit
) {
    val listState = rememberLazyListState()

    // Pagination trigger
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null) {
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (totalItems > 0 && lastVisibleIndex >= totalItems - 2 && !state.isPaginationLoading && !state.isLastPage) {
                        onEvent(ScanHistoryEvent.LoadMore)
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.Background)
    ) {
        // ── Custom Top Header (like ProductDetails) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.Primary)
        ) {
            AppTopHeader(
                title = stringResource(R.string.scan_history_title),
                onBackClick = { onEvent(ScanHistoryEvent.BackClicked) }
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filters
                FilterRow(
                    selectedFilter = state.selectedFilter,
                    onFilterSelected = { onEvent(ScanHistoryEvent.FilterSelected(it)) }
                )

                if (state.isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(8) {
                            HistoryItemShimmerCard(modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                } else if (state.error != null && state.allHistoryItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        OfflineStateWidget(onRetry = { onEvent(ScanHistoryEvent.RetryLoad) })
                    }
                } else if (state.displayedHistoryItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.scan_history_empty),
                            color = AppTheme.colors.TextSecondary,
                            style = AppTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // padding for pagination loader
                    ) {
                        items(
                            items = state.displayedHistoryItems,
                            key = { it.id }
                        ) { item ->
                            HistoryItemCard(
                                item = item,
                                onClick = { onEvent(ScanHistoryEvent.ItemClicked(item.id)) },
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        
                        if (state.isPaginationLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AppTheme.colors.Primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    val filters = listOf(
        HistoryFilter.ALL to stringResource(R.string.filter_all),
        HistoryFilter.SAFE to stringResource(R.string.filter_safe),
        HistoryFilter.CAUTION to stringResource(R.string.filter_caution),
        HistoryFilter.UNSAFE to stringResource(R.string.filter_unsafe)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label) ->
            val isSelected = selectedFilter == filter
            val backgroundColor = if (isSelected) AppTheme.colors.Primary else AppTheme.colors.Surface
            val textColor = if (isSelected) Color.White else AppTheme.colors.TextSecondary
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    style = AppTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}
