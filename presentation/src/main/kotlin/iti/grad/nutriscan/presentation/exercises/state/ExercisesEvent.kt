package iti.grad.nutriscan.presentation.exercises.state

sealed interface ExercisesEvent {
    data class OnSearchQueryChange(val query: String) : ExercisesEvent
    data class OnCategorySelected(val categoryId: String) : ExercisesEvent
    data class OnExerciseClick(val exerciseId: String) : ExercisesEvent
    data object OnDismissInstructions : ExercisesEvent
    data object OnReadMoreClick : ExercisesEvent
    data object OnStartWorkoutClick : ExercisesEvent
    data object OnBackClick : ExercisesEvent
}
