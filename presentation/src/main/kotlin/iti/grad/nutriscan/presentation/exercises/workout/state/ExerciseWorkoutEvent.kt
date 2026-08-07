package iti.grad.nutriscan.presentation.exercises.workout.state

sealed interface ExerciseWorkoutEvent {
    data class InitExercise(val id: String) : ExerciseWorkoutEvent
    data object OnStartResumeClick : ExerciseWorkoutEvent
    data object OnPauseClick : ExerciseWorkoutEvent
    data object OnRestartClick : ExerciseWorkoutEvent
    data object OnCancelClick : ExerciseWorkoutEvent
    data object OnFinishClick : ExerciseWorkoutEvent
    data object OnSetIncrement : ExerciseWorkoutEvent
    data object OnSetDecrement : ExerciseWorkoutEvent
    data object OnRepIncrement : ExerciseWorkoutEvent
    data object OnRepDecrement : ExerciseWorkoutEvent
    data class OnSetChange(val sets: Int) : ExerciseWorkoutEvent
    data class OnRepChange(val reps: Int) : ExerciseWorkoutEvent
    data object OnCongratsDialogConfirm : ExerciseWorkoutEvent
    data object OnTimerTick : ExerciseWorkoutEvent
    data object OnBackClick : ExerciseWorkoutEvent
    data object OnRetryInitClick : ExerciseWorkoutEvent
}
