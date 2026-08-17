package iti.grad.nutriscan.presentation.main.calories.view

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iti.grad.nutriscan.presentation.common.components.CalorieGoalsCard
import iti.grad.nutriscan.presentation.common.components.CaloriesScreenShimmer
import iti.grad.nutriscan.presentation.common.components.CompactAddFoodCard
import iti.grad.nutriscan.presentation.common.components.DeleteWarningAlert
import iti.grad.nutriscan.presentation.common.components.ExerciseCard
import iti.grad.nutriscan.presentation.common.components.FoodLogItemCard
import iti.grad.nutriscan.presentation.common.components.HeroHeaderTitle
import iti.grad.nutriscan.presentation.common.components.PullToRefreshShimmerBox
import iti.grad.nutriscan.presentation.common.components.SectionHeroHeader
import iti.grad.nutriscan.presentation.common.components.StepsGaugeCard
import iti.grad.nutriscan.presentation.common.components.WaterTrackerCard
import iti.grad.nutriscan.presentation.common.components.dashedBorder
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Calories Dashboard ("Daily Products") — second bottom-nav tab.
 *
 * Follows MVI: collects [CaloriesState] from [CaloriesViewModel], dispatches [CaloriesEvent], and
 * handles [CaloriesEffect] for one-shot navigation.
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CaloriesScreen(
        viewModel: CaloriesViewModel = hiltViewModel(),
        bottomPadding: Dp = 0.dp,
        snackbarHostState: SnackbarHostState,
        onNavigateToProductDetail: (ProductUiModel) -> Unit = {},
        onNavigateToSavedProducts: () -> Unit = {},
        onNavigateToExercises: () -> Unit = {},
        onNavigateToStepHistory: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current

    val stepsPermissionLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                viewModel.onEvent(CaloriesEvent.StepsPermissionResult(granted = granted))
            }

    LaunchedEffect(Unit) { viewModel.onEvent(CaloriesEvent.StepsCardClicked) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CaloriesEffect.NavigateToExercises -> onNavigateToExercises()
                is CaloriesEffect.NavigateToSavedProducts -> onNavigateToSavedProducts()
                is CaloriesEffect.NavigateToProductDetail ->
                        onNavigateToProductDetail(effect.product)
                is CaloriesEffect.ShowSnackbar -> {
                    snackbarScope.launch {
                        snackbarHostState.showAppSnackbar(
                                message = context.getString(effect.messageResId),
                                type = effect.type
                        )
                    }
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
            bottomPadding = bottomPadding,
            onNavigateToStepHistory = onNavigateToStepHistory,
    )
}

@Composable
private fun CaloriesContent(
        state: CaloriesState,
        onEvent: (CaloriesEvent) -> Unit,
        bottomPadding: Dp,
        onNavigateToStepHistory: () -> Unit,
) {
    Column(
            modifier = Modifier.fillMaxSize().background(AppTheme.colors.ProfileHeaderBackground),
    ) {
        SectionHeroHeader {
            CaloriesHeroContent(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 56.dp, bottom = 24.dp),
            )
        }

        PullToRefreshShimmerBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(CaloriesEvent.Refreshed) },
                shimmer = {
                    CaloriesScreenShimmer(
                            contentPadding =
                                    PaddingValues(top = 20.dp, bottom = bottomPadding + 24.dp)
                    )
                },
                modifier =
                        Modifier.weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .background(AppTheme.colors.Background),
        ) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = bottomPadding + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { CaloriesHeader(caloriesGained = state.caloriesGained) }

                item {
                    // Same dashed-box spec as the Family Members section (Teal500, 2dp stroke,
                    // 7.5dp dash / 5dp gap, 22dp corner) — added foods scroll horizontally inside
                    // it,
                    // "Add Food" is always the first card, both sized like FamilyMemberCard
                    // (112x84dp).
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .background(
                                                    AppTheme.colors.FoodLogDashedBoxBackground,
                                                    RoundedCornerShape(22.dp)
                                            )
                                            .dashedBorder(
                                                    2.dp,
                                                    AppTheme.colors.Teal500,
                                                    22.dp,
                                                    dashLength = 7.5.dp,
                                                    gapLength = 5.dp
                                            )
                                            .clip(RoundedCornerShape(22.dp))
                                            .padding(16.dp),
                    ) {
                        LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(key = "add_food_card") {
                                CompactAddFoodCard(
                                        onClick = { onEvent(CaloriesEvent.AddFoodClicked) }
                                )
                            }
                            items(state.addedFoods, key = { it.id }) { food ->
                                FoodLogItemCard(
                                        imageUrl = food.imageUrl,
                                        productName = food.productName,
                                        calories = food.calories,
                                        quantity = food.quantity,
                                        onMinusClick = {
                                            onEvent(
                                                    CaloriesEvent.FoodItemMinusClicked(
                                                            food.logEntryId ?: food.id
                                                    )
                                            )
                                        },
                                        onDeleteClick = {
                                            onEvent(
                                                    CaloriesEvent.FoodItemDeleteClicked(
                                                            food.logEntryId ?: food.id
                                                    )
                                            )
                                        },
                                        modifier =
                                                Modifier.clickable {
                                                    onEvent(CaloriesEvent.FoodItemClicked(food))
                                                },
                                )
                            }
                        }
                    }
                }

                item {
                    CalorieGoalsCard(
                            tdee = state.tdee,
                            caloriesGained =
                                    (state.caloriesGained - state.exerciseKcal).coerceAtLeast(0),
                            caloriesBurned = state.caloriesBurned,
                    )
                }

                item {
                    Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StepsGaugeCard(
                                steps = state.steps,
                                stepsGoal = state.stepsGoal,
                                onClick = onNavigateToStepHistory,
                                modifier = Modifier.width(126.dp).fillMaxHeight(),
                        )
                        ExerciseCard(
                                exerciseKcal = state.exerciseKcal,
                                exerciseMinutes = state.exerciseMinutes,
                                onAddClick = { onEvent(CaloriesEvent.AddExerciseClicked) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }

                item {
                    WaterTrackerCard(
                            waterConsumed = state.waterConsumed,
                            waterGoal = state.waterGoal,
                            onAddWater = { onEvent(CaloriesEvent.AddWaterClicked) },
                            onRemoveWater = { onEvent(CaloriesEvent.RemoveWaterClicked) },
                            onCupClicked = { index ->
                                onEvent(CaloriesEvent.WaterCupClicked(index))
                            },
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (state.pendingRemoveFoodId != null) {
        DeleteWarningAlert(
                title = stringResource(R.string.food_log_remove_confirm_title),
                message = stringResource(R.string.food_log_remove_confirm_message),
                confirmText = stringResource(R.string.action_remove),
                cancelText = stringResource(R.string.action_cancel),
                onConfirm = { onEvent(CaloriesEvent.RemoveFoodConfirmed) },
                onDismiss = { onEvent(CaloriesEvent.RemoveFoodDismissed) }
        )
    }
}

@SuppressLint("NonObservableLocale")
@Composable
private fun CaloriesHeader(
        caloriesGained: Int,
        modifier: Modifier = Modifier,
) {
    Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
                text = stringResource(R.string.daily_products),
                style = CaloriesTypography.headerTitle,
                color = AppTheme.colors.SectionSubtitle,
        )
        Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                        text =
                                String.format(
                                        LocalLocale.current.platformLocale,
                                        "%,d",
                                        caloriesGained
                                ),
                        style = CaloriesTypography.badgeText,
                        color = AppTheme.colors.Teal300,
                        modifier =
                                Modifier.clip(RoundedCornerShape(6.dp))
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
}

@Composable
private fun CaloriesHeroContent(modifier: Modifier = Modifier) {
    val today = remember {
        java.time.LocalDate.now()
                .format(
                        java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"),
                )
    }
    Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroHeaderTitle(text = stringResource(R.string.calories_tracking_title))
        Box(
                modifier =
                        Modifier.clip(RoundedCornerShape(50))
                                .background(AppTheme.colors.ProfileStreakBadgeBackground)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                    text = today,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.Teal400,
            )
        }
    }
}
