package iti.grad.nutriscan.presentation.main.calories.view
import androidx.compose.ui.graphics.Color

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import androidx.compose.runtime.LaunchedEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import androidx.compose.foundation.layout.IntrinsicSize
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.common.components.StepsGaugeCard
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.components.ProductCard
import iti.grad.nutriscan.presentation.common.theme.CaloriesTypography
import iti.grad.nutriscan.presentation.common.components.ProductCardSwipeAction
import androidx.compose.ui.Alignment
import iti.grad.nutriscan.presentation.common.components.WaterTrackerCard

import androidx.activity.result.contract.ActivityResultContracts
import iti.grad.nutriscan.presentation.common.components.ConfirmationDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import iti.grad.nutriscan.presentation.common.components.SectionHeroHeader
import iti.grad.nutriscan.presentation.common.components.HeroHeaderTitle
import androidx.compose.ui.platform.LocalContext
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import androidx.compose.foundation.lazy.LazyColumn
import iti.grad.nutriscan.presentation.common.components.PullToRefreshShimmerBox
import iti.grad.nutriscan.presentation.common.components.CaloriesScreenShimmer
import iti.grad.nutriscan.presentation.common.components.ExerciseCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.SnackbarHostState
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.components.showAppSnackbar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import iti.grad.nutriscan.presentation.common.components.DashedActionCard
import iti.grad.nutriscan.presentation.main.calories.viewmodel.CaloriesViewModel
import androidx.compose.ui.platform.LocalLocale
import iti.grad.nutriscan.presentation.common.components.CalorieGoalsPager
import iti.grad.nutriscan.presentation.common.components.CustomAlertDialog
import androidx.compose.ui.Modifier
import iti.grad.nutriscan.presentation.common.components.AlertButton
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text

/**
 * Calories Dashboard ("Daily Products") — second bottom-nav tab.
 *
 * Follows MVI: collects [CaloriesState] from [CaloriesViewModel], dispatches
 * [CaloriesEvent], and handles [CaloriesEffect] for one-shot navigation.
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
                is CaloriesEffect.NavigateToExercises -> onNavigateToExercises()
                is CaloriesEffect.NavigateToSavedProducts -> onNavigateToSavedProducts()
                is CaloriesEffect.NavigateToProductDetail -> onNavigateToProductDetail(effect.product)
                is CaloriesEffect.ShowSnackbar -> {
                    snackbarScope.launch {
                        snackbarHostState.showAppSnackbar(
                            message = context.getString(effect.messageResId),
                            type = SnackbarType.ERROR
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
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.ProfileHeaderBackground),
    ) {
        SectionHeroHeader {
            CaloriesHeroContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 24.dp),
            )
        }

        PullToRefreshShimmerBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(CaloriesEvent.Refreshed) },
            shimmer = { CaloriesScreenShimmer(contentPadding = PaddingValues(top = 20.dp, bottom = bottomPadding + 24.dp)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AppTheme.colors.Background),
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = bottomPadding + 24.dp),
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DashedActionCard(
                            label = stringResource(R.string.add_food),
                            onClick = { onEvent(CaloriesEvent.AddFoodClicked) },
                            contentPadding = 16.dp,
                            modifier = Modifier
                                .width(140.dp)
                                .fillMaxHeight(),
                        )
                        for (food in state.addedFoods) {
                            key(food.id) {
                                ProductCard(
                                    imageUrl = food.imageUrl,
                                    productName = food.productName,
                                    verdict = null,
                                    calories = food.calories,
                                    quantity = food.quantity,
                                    onClick = { onEvent(CaloriesEvent.FoodItemClicked(food)) },
                                    swipeAction = ProductCardSwipeAction.Remove(
                                        hintResId = R.string.food_log_swipe_remove_hint,
                                        onTriggered = {
                                            onEvent(CaloriesEvent.FoodItemSwipedToRemove(food.logEntryId ?: food.id))
                                        },
                                    ),
                                    caloriesOverlayOnImage = true,
                                    modifier = Modifier
                                        .width(140.dp)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }

            item {
                CalorieGoalsPager(
                    tdee = state.tdee,
                    caloriesGained = (state.caloriesGained - state.exerciseKcal).coerceAtLeast(0),
                    caloriesBurned = state.caloriesBurned,
                    bmi = state.bmi,
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
                        onClick = onNavigateToStepHistory,
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

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        }
    }

        if (state.pendingRemoveFoodId != null) {
            CustomAlertDialog(
                title = stringResource(R.string.food_log_remove_confirm_title),
                message = stringResource(R.string.food_log_remove_confirm_message),
                icon = androidx.compose.ui.res.painterResource(id = R.drawable.ic_trash),
                iconBackgroundColor = AppTheme.colors.ErrorBackground,
                iconContentColor = AppTheme.colors.Error,
                onDismiss = { onEvent(CaloriesEvent.RemoveFoodDismissed) }
            ) {
                AlertButton(
                    text = stringResource(R.string.action_cancel),
                    backgroundColor = AppTheme.colors.SurfaceVariant,
                    textColor = AppTheme.colors.TextPrimary,
                    onClick = { onEvent(CaloriesEvent.RemoveFoodDismissed) }
                )
                AlertButton(
                    text = stringResource(R.string.action_remove),
                    backgroundColor = AppTheme.colors.Error,
                    textColor = Color.White,
                    onClick = { onEvent(CaloriesEvent.RemoveFoodConfirmed) }
                )
            }
        }
}

@SuppressLint("NonObservableLocale")
@Composable
private fun CaloriesHeader(caloriesGained: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                text = String.format(LocalLocale.current.platformLocale, "%,d", caloriesGained),
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

@Composable
private fun CaloriesHeroContent(modifier: Modifier = Modifier) {
    val today = remember {
        java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"),
        )
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroHeaderTitle(text = stringResource(R.string.calories_tracking_title))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
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
