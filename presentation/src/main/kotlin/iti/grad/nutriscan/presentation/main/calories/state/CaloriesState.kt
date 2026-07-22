package iti.grad.nutriscan.presentation.main.calories.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.presentation.common.model.BottomNavTab

@Immutable
data class CaloriesState(
    val tdee: Int = 2350,
    val caloriesGained: Int = 2100,
    val steps: Int = 0,
    val stepsGoal: Int = 10000,
    val stepsPermissionGranted: Boolean = false,
    val exerciseKcal: Int = 250,
    val exerciseMinutes: Int = 45,
    val waterConsumed: Int = 4,
    val waterGoal: Int = 8,
    val isLoading: Boolean = false,
    val selectedTab: BottomNavTab = BottomNavTab.CALORIES,
)
