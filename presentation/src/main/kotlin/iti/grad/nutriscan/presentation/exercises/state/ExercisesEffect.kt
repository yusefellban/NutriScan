package iti.grad.nutriscan.presentation.exercises.state

sealed interface ExercisesEffect {
    data object NavigateBack : ExercisesEffect
    data class NavigateToExerciseWorkout(val exerciseId: String) : ExercisesEffect
}
