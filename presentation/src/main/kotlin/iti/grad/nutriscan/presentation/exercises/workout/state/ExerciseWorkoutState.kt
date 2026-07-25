package iti.grad.nutriscan.presentation.exercises.workout.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel

@Immutable
data class ExerciseWorkoutState(
    val isLoading: Boolean = false,
    val exercise: ExerciseUiModel? = null,
    val secondsElapsed: Int = 0,
    val isTimerRunning: Boolean = false,
    val hasStarted: Boolean = false,
    val sets: Int = 1,
    val reps: Int = 1,
    val showCongratsDialog: Boolean = false,
    val caloriesBurned: Int = 0
)
