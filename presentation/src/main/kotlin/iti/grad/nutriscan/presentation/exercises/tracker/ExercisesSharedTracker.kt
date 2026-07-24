package iti.grad.nutriscan.presentation.exercises.tracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Thread-safe in-memory singleton to share exercise workout statistics
 * between ExerciseWorkoutViewModel and CaloriesViewModel reactive flows.
 */
object ExercisesSharedTracker {
    private val _exerciseKcal = MutableStateFlow(250) // Starting baseline
    val exerciseKcal: StateFlow<Int> = _exerciseKcal.asStateFlow()

    private val _exerciseMinutes = MutableStateFlow(45) // Starting baseline
    val exerciseMinutes: StateFlow<Int> = _exerciseMinutes.asStateFlow()

    fun addWorkout(kcal: Int, minutes: Int) {
        _exerciseKcal.update { it + kcal }
        _exerciseMinutes.update { it + minutes }
    }

    fun reset() {
        _exerciseKcal.value = 250
        _exerciseMinutes.value = 45
    }
}
