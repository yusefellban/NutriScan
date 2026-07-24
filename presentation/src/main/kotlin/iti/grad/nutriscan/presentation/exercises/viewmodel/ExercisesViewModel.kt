package iti.grad.nutriscan.presentation.exercises.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.presentation.exercises.mock.ExercisesMockData
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEffect
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEvent
import iti.grad.nutriscan.presentation.exercises.state.ExercisesState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ExercisesState())
    val state: StateFlow<ExercisesState> = _state.asStateFlow()

    private val _effect = Channel<ExercisesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        _state.update {
            it.copy(
                categories = ExercisesMockData.categories,
                exercises = ExercisesMockData.exercises,
                visibleExercises = ExercisesMockData.exercises
            )
        }
    }

    fun onEvent(event: ExercisesEvent) {
        when (event) {
            is ExercisesEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
                filterExercises()
            }
            is ExercisesEvent.OnCategorySelected -> {
                _state.update { it.copy(selectedCategoryId = event.categoryId) }
                filterExercises()
            }
            is ExercisesEvent.OnExerciseClick -> {
                val exercise = _state.value.exercises.firstOrNull { it.id == event.exerciseId }
                _state.update {
                    it.copy(
                        selectedExercise = exercise,
                        isInstructionsExpanded = false
                    )
                }
            }
            ExercisesEvent.OnDismissInstructions -> {
                _state.update { it.copy(selectedExercise = null, isInstructionsExpanded = false) }
            }
            ExercisesEvent.OnReadMoreClick -> {
                _state.update { it.copy(isInstructionsExpanded = true) }
            }
            ExercisesEvent.OnStartWorkoutClick -> {
                val selected = _state.value.selectedExercise
                if (selected != null) {
                    _state.update { it.copy(selectedExercise = null, isInstructionsExpanded = false) }
                    viewModelScope.launch {
                        _effect.send(ExercisesEffect.NavigateToExerciseWorkout(selected.id))
                    }
                }
            }
            ExercisesEvent.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(ExercisesEffect.NavigateBack)
                }
            }
        }
    }

    private fun filterExercises() {
        val query = _state.value.searchQuery.trim().lowercase()
        val categoryId = _state.value.selectedCategoryId

        val filtered = _state.value.exercises.filter { exercise ->
            // Filter by Category
            val matchesCategory = if (categoryId == "all") {
                true
            } else {
                when (categoryId) {
                    "warm_up" -> exercise.id == "1"
                    "biceps" -> exercise.id == "4"
                    else -> false // news/yoga/etc. can show empty state for testing
                }
            }

            // Filter by Query
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                val name = context.getString(exercise.nameRes).lowercase()
                val equipment = context.getString(exercise.equipmentRes).lowercase()
                val target = context.getString(exercise.targetRes).lowercase()
                name.contains(query) || equipment.contains(query) || target.contains(query)
            }

            matchesCategory && matchesQuery
        }.toImmutableList()

        _state.update { it.copy(visibleExercises = filtered) }
    }
}
