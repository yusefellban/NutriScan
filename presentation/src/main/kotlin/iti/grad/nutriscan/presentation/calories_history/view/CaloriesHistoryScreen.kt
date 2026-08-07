package iti.grad.nutriscan.presentation.calories_history.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEffect
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEvent
import iti.grad.nutriscan.presentation.calories_history.view.components.CaloriesHistoryDayCard
import iti.grad.nutriscan.presentation.common.components.AppEmptyStateWidget
import iti.grad.nutriscan.presentation.calories_history.view.components.CaloriesHistoryTopBar
import iti.grad.nutriscan.presentation.calories_history.viewmodel.CaloriesHistoryViewModel
import iti.grad.nutriscan.presentation.common.components.AppButton
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val LOAD_MORE_THRESHOLD = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloriesHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddMeals: () -> Unit,
    viewModel: CaloriesHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Trigger LoadMore when user scrolls close to the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
            totalItems > 0 && lastVisibleIndex >= totalItems - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.onEvent(CaloriesHistoryEvent.LoadMore)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CaloriesHistoryEffect.NavigateBack -> onNavigateBack()
                is CaloriesHistoryEffect.NavigateToAddMeals -> onNavigateToAddMeals()
            }
        }
    }

    // ── Date Picker Dialog ──
    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate
                ?.atStartOfDay()
                ?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = {
                viewModel.onEvent(CaloriesHistoryEvent.DismissDatePicker)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.onEvent(CaloriesHistoryEvent.DateSelected(picked))
                        }
                    },
                ) {
                    Text(stringResource(R.string.calories_history_date_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(CaloriesHistoryEvent.DismissDatePicker)
                    },
                ) {
                    Text(stringResource(R.string.calories_history_date_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.ScreenSurfaceBackground,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            ) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.ScreenSurfaceBackground)
                .padding(paddingValues)
                .statusBarsPadding(),
        ) {
            CaloriesHistoryTopBar(
                onBackClick = { viewModel.onEvent(CaloriesHistoryEvent.NavigateBack) },
                onCalendarClick = { viewModel.onEvent(CaloriesHistoryEvent.CalendarClicked) },
                isFilterActive = state.selectedDate != null,
                onClearFilterClick = { viewModel.onEvent(CaloriesHistoryEvent.ClearDateFilter) },
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Full-screen loading on first page
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = AppTheme.colors.Teal1000)
                        }
                    }

                    // Empty state (no errors, no entries, not loading)
                    !state.isLoading && state.errorMessage == null && state.entries.isEmpty() -> {
                        AppEmptyStateWidget(
                            lightImageRes = R.drawable.ic_no_calories_history_light,
                            darkImageRes = R.drawable.ic_no_calories_history_dark,
                            title = stringResource(R.string.calories_history_empty_title),
                            subtitle = stringResource(R.string.calories_history_empty_subtitle),
                            buttonText = stringResource(R.string.calories_history_empty_button),
                            onButtonClick = { viewModel.onEvent(CaloriesHistoryEvent.NavigateToAddMeals) },
                        )
                    }

                    // Full-screen error with retry (only when list is empty)
                    state.errorMessage != null && state.entries.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.calories_history_error),
                                style = AppTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AppTheme.colors.CaloriesHistoryStatLabel,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AppButton(
                                textResId = R.string.calories_history_retry,
                                isLoading = false,
                                onClick = { viewModel.onEvent(CaloriesHistoryEvent.Retry) },
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            items(
                                items = state.entries,
                                key = { it.dateLabel },
                            ) { entry ->
                                CaloriesHistoryDayCard(entry = entry)
                            }

                            // Loading indicator at bottom while loading next page
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = AppTheme.colors.Teal1000,
                                            strokeWidth = 2.dp,
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
}


