package iti.grad.nutriscan.presentation.main.calories.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.AppBottomNavBar
import iti.grad.nutriscan.presentation.common.components.AppSnackbar
import iti.grad.nutriscan.presentation.common.components.CalorieGoalsCard
import iti.grad.nutriscan.presentation.common.components.DashedActionCard
import iti.grad.nutriscan.presentation.common.components.ExerciseCard
import iti.grad.nutriscan.presentation.common.components.FoodEntryCard
import iti.grad.nutriscan.presentation.common.components.StepsGaugeCard
import iti.grad.nutriscan.presentation.common.components.WaterTrackerCard
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Calories Dashboard ("Daily Products") — second bottom-nav tab.
 *
 * Follows MVI: collects [CaloriesState] from [CaloriesViewModel], dispatches
 * [CaloriesEvent], and handles [CaloriesEffect] for one-shot navigation.
 */
@Composable
fun CaloriesScreen(
    viewModel: CaloriesViewModel = hiltViewModel(),
    onNavigateToSavedProducts: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToShopping: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToExercises: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val stepsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(CaloriesEvent.StepsPermissionResult(granted = granted))
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(CaloriesEvent.StepsCardClicked)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CaloriesEffect.NavigateToSavedProducts -> onNavigateToSavedProducts()
                is CaloriesEffect.NavigateToHome -> onNavigateToHome()
                is CaloriesEffect.NavigateToScan -> onNavigateToScan()
                is CaloriesEffect.NavigateToShopping -> onNavigateToShopping()
                is CaloriesEffect.NavigateToProfile -> onNavigateToProfile()
                is CaloriesEffect.NavigateToExercises -> onNavigateToExercises()
                is CaloriesEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = context.getString(effect.messageResId))
                }

                is CaloriesEffect.RequestStepsPermission -> {
                    stepsPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        }
    }

    CaloriesContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun CaloriesContent(
    state: CaloriesState,
    onEvent: (CaloriesEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        containerColor = AppTheme.colors.Background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                AppSnackbar(message = data.visuals.message)
            }
        },
        bottomBar = {
            AppBottomNavBar(
                selectedTab = state.selectedTab,
                onTabClick = { tab -> onEvent(CaloriesEvent.BottomNavTabClicked(tab)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.Background)
                .padding(innerPadding)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                CaloriesHeader(caloriesGained = state.caloriesGained)
            }

            if (state.addedFoods.isEmpty()) {
                item {
                    DashedActionCard(
                        label = stringResource(R.string.add_food),
                        onClick = { onEvent(CaloriesEvent.AddFoodClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            DashedActionCard(
                                label = stringResource(R.string.add_food),
                                onClick = { onEvent(CaloriesEvent.AddFoodClicked) },
                                contentPadding = 16.dp,
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(130.dp),
                            )
                        }
                        items(state.addedFoods, key = { it.id }) { food ->
                            FoodEntryCard(
                                name = food.name,
                                kcal = food.kcal,
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(130.dp),
                            )
                        }
                    }
                }
            }

            item {
                CalorieGoalsCard(
                    tdee = state.tdee,
                    caloriesGained = state.caloriesGained,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StepsGaugeCard(
                        steps = state.steps,
                        stepsGoal = state.stepsGoal,
                        onClick = { onEvent(CaloriesEvent.StepsCardClicked) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    ExerciseCard(
                        exerciseKcal = state.exerciseKcal,
                        exerciseMinutes = state.exerciseMinutes,
                        onAddClick = { onEvent(CaloriesEvent.AddExerciseClicked) },
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                    )
                }
            }

            item {
                WaterTrackerCard(
                    waterConsumed = state.waterConsumed,
                    waterGoal = state.waterGoal,
                    onAddWater = { onEvent(CaloriesEvent.AddWaterClicked) },
                    onCupClicked = { index -> onEvent(CaloriesEvent.WaterCupClicked(index)) },
                    onCupLongPressed = { index -> onEvent(CaloriesEvent.WaterCupLongPressed(index)) },
                )
            }
        }
    }
}

@Composable
private fun CaloriesHeader(caloriesGained: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.daily_products),
            style = CaloriesTypography.headerTitle,
            color = AppTheme.colors.CaloriesAccentTeal1200,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%,d", caloriesGained),
                style = CaloriesTypography.badgeText,
                color = AppTheme.colors.Teal300,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppTheme.colors.Teal1000)
                    .padding(horizontal = 3.dp),
            )
            Text(
                text = stringResource(R.string.calorie_badge),
                style = CaloriesTypography.badgeText,
                color = AppTheme.colors.Teal1000,
            )
        }
    }
}
