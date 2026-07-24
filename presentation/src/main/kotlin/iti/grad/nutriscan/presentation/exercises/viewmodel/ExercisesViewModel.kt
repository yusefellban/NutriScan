package iti.grad.nutriscan.presentation.exercises.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.model.ExerciseQuery
import iti.grad.nutriscan.domain.exercises.usecase.GetExerciseCategoriesUseCase
import iti.grad.nutriscan.domain.exercises.usecase.GetExercisesUseCase
import iti.grad.nutriscan.presentation.common.model.ExerciseType
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel
import iti.grad.nutriscan.presentation.exercises.model.ExerciseCategoryUi
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEffect
import iti.grad.nutriscan.presentation.exercises.state.ExercisesEvent
import iti.grad.nutriscan.presentation.exercises.state.ExercisesState
import iti.grad.presentation.R
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getExercisesUseCase: GetExercisesUseCase,
    private val getCategoriesUseCase: GetExerciseCategoriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ExercisesState())
    val state: StateFlow<ExercisesState> = _state.asStateFlow()

    private val _effect = Channel<ExercisesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        loadCategories()
        loadExercises(reset = true)
    }

    fun onEvent(event: ExercisesEvent) {
        when (event) {
            is ExercisesEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
                debounceSearch()
            }
            is ExercisesEvent.OnCategorySelected -> {
                _state.update { it.copy(selectedCategoryId = event.categoryId) }
                loadExercises(reset = true)
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
            ExercisesEvent.OnRetryClick -> {
                loadExercises(reset = true)
            }
            ExercisesEvent.OnLoadMore -> {
                if (!_state.value.isLoading && !_state.value.isLoadingMore && _state.value.hasNextPage) {
                    loadExercises(reset = false)
                }
            }
        }
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadExercises(reset = true)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().fold(
                onSuccess = { categoriesList ->
                    val uiCategories = listOf(
                        ExerciseCategoryUi(id = "all", label = context.getString(R.string.exercises_category_all))
                    ) + categoriesList.map { category ->
                        ExerciseCategoryUi(
                            id = category,
                            label = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                        )
                    }
                    _state.update { it.copy(categories = uiCategories.toImmutableList()) }
                },
                onFailure = {
                    // Fallback to static "All" if category request fails
                    _state.update {
                        it.copy(
                            categories = kotlinx.collections.immutable.persistentListOf(
                                ExerciseCategoryUi(id = "all", label = context.getString(R.string.exercises_category_all))
                            )
                        )
                    }
                }
            )
        }
    }

    private fun loadExercises(reset: Boolean) {
        if (reset) {
            _state.update { it.copy(isLoading = true, currentPage = 1, errorMessageRes = null) }
        } else {
            _state.update { it.copy(isLoadingMore = true, errorMessageRes = null) }
        }

        val currentState = _state.value
        val queryParams = ExerciseQuery(
            page = if (reset) 1 else currentState.currentPage + 1,
            limit = 20,
            bodyPart = if (currentState.selectedCategoryId == "all") null else currentState.selectedCategoryId,
            query = currentState.searchQuery.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            getExercisesUseCase(queryParams).fold(
                onSuccess = { page ->
                    _state.update {
                        val newExercises = if (reset) {
                            page.exercises.map { it.toUiModel() }
                        } else {
                            currentState.exercises + page.exercises.map { it.toUiModel() }
                        }
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            exercises = newExercises.toImmutableList(),
                            visibleExercises = newExercises.toImmutableList(),
                            currentPage = page.currentPage,
                            hasNextPage = page.hasNext,
                            errorMessageRes = null
                        )
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessageRes = R.string.exercises_load_error
                        )
                    }
                }
            )
        }
    }

    private fun Exercise.toUiModel(): ExerciseUiModel = ExerciseUiModel(
        id = id,
        name = name,
        equipment = equipment,
        target = target,
        instructions = instructions,
        type = if (category == "cardio" || minKcal != null) ExerciseType.CARDIO else ExerciseType.NORMAL_WORKOUT,
        imageUrl = imageUrl,
        gifUrl = gifUrl,
        kcalPerMin = minKcal,
        kcalPerRep = repKcal
    )
}
