package iti.grad.nutriscan.presentation.main.calories.stephistory.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
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
import androidx.compose.ui.res.stringResource
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.settings.app.view.components.AppSettingsHeader
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryEffect
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryEvent
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryBarChart
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryGaugeCard
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryPeriodSelector
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistorySummaryRow
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.components.StepHistoryDateRangeRow
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
                    snackbarHostState.showAppSnackbar(
                        message = effect.message,
                        type = SnackbarType.ERROR
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) { data ->
                AppSnackbar(snackbarData = data)
            }
        },
        containerColor = AppTheme.colors.ScreenSurfaceBackground,
        contentWindowInsets = WindowInsets(0)
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
                    Text(text = state.error ?: stringResource(id = R.string.alert_error_title))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onEvent(StepHistoryEvent.Retry) }) {
                        Text(stringResource(id = R.string.alert_button_retry))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AppSettingsHeader(
                        title = stringResource(id = R.string.step_history_title),
                        onBackClick = { viewModel.onEvent(StepHistoryEvent.NavigateBack) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        StepHistoryPeriodSelector(
                            selectedPeriod = state.selectedPeriod,
                            onPeriodSelected = { viewModel.onEvent(StepHistoryEvent.SelectPeriod(it)) }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        StepHistoryDateRangeRow(summary = state.summary)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        StepHistoryGaugeCard(summary = state.summary)
                        
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
}

