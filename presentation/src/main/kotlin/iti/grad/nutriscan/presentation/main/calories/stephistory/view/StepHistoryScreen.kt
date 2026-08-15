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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import iti.grad.nutriscan.presentation.common.components.shimmerEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import iti.grad.nutriscan.presentation.common.components.stepHistoryCardShadow
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
                    
                    if (state.isLoading) {
                        StepHistoryShimmer()
                    } else if (state.error != null && state.summary == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.error ?: stringResource(id = R.string.alert_error_title))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.onEvent(StepHistoryEvent.Retry) }) {
                                Text(stringResource(id = R.string.alert_button_retry))
                            }
                        }
                    } else {
                        StepHistoryDateRangeRow(summary = state.summary)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        StepHistoryGaugeCard(summary = state.summary)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        StepHistoryBarChart(summary = state.summary)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        StepHistorySummaryRow(summary = state.summary)
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun StepHistoryShimmer() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Date Range Placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .stepHistoryCardShadow(RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTheme.colors.Surface)
                        .padding(16.dp)
                ) {
                    Box(modifier = Modifier.width(60.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.width(80.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Gauge Card Placeholder
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .stepHistoryCardShadow(RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.Surface)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left side: Gauge + Goal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Right side: Period Average Box
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.StepHistoryChartBarBg)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Bar Chart Placeholder
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .stepHistoryCardShadow(RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.Surface)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.3f, 0.8f)
                
                heights.forEach { fillRatio ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fillRatio)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerEffect()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summary Row Placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .stepHistoryCardShadow(RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTheme.colors.Surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}
