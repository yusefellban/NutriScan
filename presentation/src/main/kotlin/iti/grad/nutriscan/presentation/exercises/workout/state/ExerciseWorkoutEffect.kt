package iti.grad.nutriscan.presentation.exercises.workout.state

sealed interface ExerciseWorkoutEffect {
    data object NavigateBack : ExerciseWorkoutEffect
    data object NavigateToCaloriesDashboard : ExerciseWorkoutEffect
}
