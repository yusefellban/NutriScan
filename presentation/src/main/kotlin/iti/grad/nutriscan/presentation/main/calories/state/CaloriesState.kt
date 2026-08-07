package iti.grad.nutriscan.presentation.main.calories.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CaloriesState(
    val tdee: Int = 0,
    val bmi: Double? = null,
    val caloriesGained: Int = 0,
    /** steps + exercise kcal burned today — see CaloriesViewModel.observeDailyTracking. */
    val caloriesBurned: Int = 0,
    val addedFoods: ImmutableList<ProductUiModel> = persistentListOf(),
    /** Food-log entry pending user confirmation before removal — gates the ConfirmationDialog. */
    val pendingRemoveFoodId: String? = null,
    val steps: Int = 0,
    val stepsGoal: Int = 10000,
    val stepsPermissionGranted: Boolean = false,
    val exerciseKcal: Int = 0,
    val exerciseMinutes: Int = 0,
    val waterConsumed: Int = 0,
    val waterGoal: Int = 8,
    val isLoading: Boolean = false,
    /** Drives the pull-to-refresh indicator while [CaloriesEvent.Refreshed]'s backend pull is in
     * flight — see CaloriesViewModel.refresh. */
    val isRefreshing: Boolean = false,
)
