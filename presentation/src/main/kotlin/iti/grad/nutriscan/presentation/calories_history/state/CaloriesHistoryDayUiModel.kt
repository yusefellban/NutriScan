package iti.grad.nutriscan.presentation.calories_history.state

import androidx.compose.runtime.Immutable

@Immutable
data class CaloriesHistoryDayUiModel(
    val dateLabel: String,
    val totalMealsKcal: Int,
    val waterCups: Int,
    val waterTarget: Int,
    val steps: Int,
    val stepsKcal: Int,
    val exerciseMinutes: Int,
    val exerciseKcal: Int,
)
