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
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.components.SelectableChip
import iti.grad.nutriscan.presentation.scan_history.state.HistoryFilter
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEffect
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEvent
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryState
import iti.grad.nutriscan.presentation.common.components.HistoryItemShimmerCard
import iti.grad.nutriscan.presentation.common.components.AppEmptyStateWidget
import iti.grad.nutriscan.presentation.scan_history.viewmodel.ScanHistoryViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import iti.grad.nutriscan.presentation.scan_history.state.*
import iti.grad.nutriscan.presentation.common.theme.AppTheme

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
                onBackClick = { onEvent(ScanHistoryEvent.BackClicked) },
                actionIconResId = R.drawable.ic_date,
                actionIconContentDescription = "Filter by date",
                onActionClick = { onEvent(ScanHistoryEvent.ShowDatePicker(true)) }
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filters
                FilterRow(
                    selectedFilter = state.selectedFilter,
                    selectedDate = state.selectedDate,
                    onFilterSelected = { onEvent(ScanHistoryEvent.FilterSelected(it)) },
                    onReset = { onEvent(ScanHistoryEvent.ResetFilters) }
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
                    AppEmptyStateWidget(
                        lightImageRes = R.drawable.no_network_connection_light,
                        darkImageRes = R.drawable.no_network_connection_dark,
                        title = stringResource(R.string.offline_state_title),
                        subtitle = stringResource(R.string.offline_state_subtitle),
                        buttonText = stringResource(R.string.offline_state_retry),
                        onButtonClick = { onEvent(ScanHistoryEvent.RetryLoad) },
                    )
                } else if (state.displayedHistoryItems.isEmpty()) {
                    AppEmptyStateWidget(
                        lightImageRes = R.drawable.no_scans_yet_light,
                        darkImageRes = R.drawable.no_scans_yet_dark,
                        title = stringResource(R.string.scan_history_empty_title),
                        subtitle = stringResource(R.string.scan_history_empty_subtitle),
                        buttonText = stringResource(R.string.scan_history_empty_button),
                        onButtonClick = { onEvent(ScanHistoryEvent.BackClicked) },
                    )
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

    if (state.showDatePicker) {
        ScanDatePickerDialog(
            onDateSelected = { dateMillis -> onEvent(ScanHistoryEvent.DateSelected(dateMillis)) },
            onDismiss = { onEvent(ScanHistoryEvent.ShowDatePicker(false)) }
        )
    }
}

@Composable
private fun FilterRow(
    selectedFilter: HistoryFilter,
    selectedDate: String?,
    onFilterSelected: (HistoryFilter) -> Unit,
    onReset: () -> Unit
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedFilter != HistoryFilter.ALL || selectedDate != null) {
            item {
                SelectableChip(
                    text = "Reset",
                    isSelected = false,
                    onClick = onReset,
                    selectedBgColor = Color.Transparent,
                    unselectedBgColor = Color.Transparent,
                    selectedBorderColor = Color.Red,
                    unselectedBorderColor = Color.Red,
                    selectedTextColor = Color.Red,
                    unselectedTextColor = Color.Red
                )
            }
        }
        
        items(filters) { (filter, label) ->
            SelectableChip(
                text = label,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                selectedBgColor = AppTheme.colors.ExerciseChipSelectedBg,
                unselectedBgColor = AppTheme.colors.ExerciseChipUnselectedBg,
                selectedBorderColor = AppTheme.colors.ExerciseChipSelectedBorder,
                unselectedBorderColor = AppTheme.colors.ExerciseChipUnselectedBorder,
                selectedTextColor = AppTheme.colors.ExerciseChipSelectedText,
                unselectedTextColor = AppTheme.colors.ExerciseChipUnselectedText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
