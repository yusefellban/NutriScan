package iti.grad.nutriscan.presentation.main.calories.stephistory.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryEffect
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryEvent
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryBarChart
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryGaugeCard
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryPeriodSelector
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistorySummaryRow
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryTopBar
import iti.grad.nutriscan.presentation.main.calories.stephistory.viewmodel.StepHistoryViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun StepHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: StepHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is StepHistoryEffect.NavigateBack -> onNavigateBack()
                is StepHistoryEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppTheme.colors.StepHistoryScreenBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null && state.summary == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.error ?: "Unknown Error")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onEvent(StepHistoryEvent.Retry) }) {
                        Text("Retry")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    StepHistoryTopBar(
                        onBackClick = { viewModel.onEvent(StepHistoryEvent.NavigateBack) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StepHistoryGaugeCard(summary = state.summary)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    StepHistoryPeriodSelector(
                        selectedPeriod = state.selectedPeriod,
                        onPeriodSelected = { viewModel.onEvent(StepHistoryEvent.SelectPeriod(it)) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    StepHistoryBarChart(summary = state.summary)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    StepHistorySummaryRow(summary = state.summary)
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
