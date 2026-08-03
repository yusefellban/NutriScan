package iti.grad.nutriscan.presentation.calories_history.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEffect
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEvent
import iti.grad.nutriscan.presentation.calories_history.view.components.CaloriesHistoryDayCard
import iti.grad.nutriscan.presentation.calories_history.view.components.CaloriesHistoryTopBar
import iti.grad.nutriscan.presentation.calories_history.viewmodel.CaloriesHistoryViewModel
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CaloriesHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CaloriesHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CaloriesHistoryEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.CaloriesHistoryScreenBg,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.CaloriesHistoryScreenBg)
                .padding(paddingValues)
                .statusBarsPadding(),
        ) {
            CaloriesHistoryTopBar(
                onBackClick = { viewModel.onEvent(CaloriesHistoryEvent.NavigateBack) },
                onCalendarClick = { viewModel.onEvent(CaloriesHistoryEvent.CalendarClicked) },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
            }
        }
    }
}
