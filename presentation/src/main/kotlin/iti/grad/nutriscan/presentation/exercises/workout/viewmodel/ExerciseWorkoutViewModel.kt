package iti.grad.nutriscan.presentation.exercises.workout.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.dailytracking.usecase.AddExerciseWorkoutUseCase
import iti.grad.nutriscan.domain.exercises.model.Exercise
import iti.grad.nutriscan.domain.exercises.usecase.GetExerciseByIdUseCase
import iti.grad.nutriscan.presentation.common.model.ExerciseType
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel
import iti.grad.nutriscan.presentation.exercises.mock.ExercisesMockData
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEffect
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutEvent
import iti.grad.nutriscan.presentation.exercises.workout.state.ExerciseWorkoutState
import iti.grad.presentation.R
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
class ExerciseWorkoutViewModel @Inject constructor(
    private val getExerciseByIdUseCase: GetExerciseByIdUseCase,
    private val addExerciseWorkout: AddExerciseWorkoutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseWorkoutState())
    val state: StateFlow<ExerciseWorkoutState> = _state.asStateFlow()

    private val _effect = Channel<ExerciseWorkoutEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var timerJob: Job? = null
    private var exerciseId: String? = null

    fun onEvent(event: ExerciseWorkoutEvent) {
        when (event) {
            is ExerciseWorkoutEvent.InitExercise -> {
                if (exerciseId == event.id && _state.value.exercise != null) {
                    return
                }
                _state.update {
                    it.copy(
                        isLoading = true,
                        exercise = null,
                        secondsElapsed = 0,
                        isTimerRunning = false,
                        hasStarted = false,
                        sets = 1,
                        reps = 1,
                        showCongratsDialog = false,
                        errorMessageRes = null
                    )
                }
                exerciseId = event.id
                loadExercise(event.id)
            }
            ExerciseWorkoutEvent.OnStartResumeClick -> {
                _state.update {
                    it.copy(
                        isTimerRunning = true,
                        hasStarted = true
                    )
                }
                startTimer()
            }
            ExerciseWorkoutEvent.OnPauseClick -> {
                _state.update { it.copy(isTimerRunning = false) }
                stopTimer()
            }
            ExerciseWorkoutEvent.OnRestartClick -> {
                _state.update {
                    it.copy(
                        secondsElapsed = 0,
                        isTimerRunning = true
                    )
                }
                startTimer()
            }
            ExerciseWorkoutEvent.OnCancelClick -> {
                stopTimer()
                viewModelScope.launch {
                    _effect.send(ExerciseWorkoutEffect.NavigateToCaloriesDashboard)
                }
            }
            ExerciseWorkoutEvent.OnFinishClick -> {
                stopTimer()
                calculateAndFinish()
            }
            ExerciseWorkoutEvent.OnSetIncrement -> {
                _state.update { it.copy(sets = it.sets + 1) }
            }
            ExerciseWorkoutEvent.OnSetDecrement -> {
                _state.update { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) }
            }
            ExerciseWorkoutEvent.OnRepIncrement -> {
                _state.update { it.copy(reps = it.reps + 1) }
            }
            ExerciseWorkoutEvent.OnRepDecrement -> {
                _state.update { it.copy(reps = (it.reps - 1).coerceAtLeast(1)) }
            }
            is ExerciseWorkoutEvent.OnSetChange -> {
                _state.update { it.copy(sets = event.sets.coerceAtLeast(1)) }
            }
            is ExerciseWorkoutEvent.OnRepChange -> {
                _state.update { it.copy(reps = event.reps.coerceAtLeast(1)) }
            }
            ExerciseWorkoutEvent.OnTimerTick -> {
                _state.update { it.copy(secondsElapsed = it.secondsElapsed + 1) }
            }
            ExerciseWorkoutEvent.OnCongratsDialogConfirm -> {
                _state.update { it.copy(showCongratsDialog = false) }
                viewModelScope.launch {
                    _effect.send(ExerciseWorkoutEffect.NavigateToCaloriesDashboard)
                }
            }
            ExerciseWorkoutEvent.OnBackClick -> {
                stopTimer()
                viewModelScope.launch {
                    _effect.send(ExerciseWorkoutEffect.NavigateBack)
                }
            }
            ExerciseWorkoutEvent.OnRetryInitClick -> {
                exerciseId?.let { id ->
                    _state.update { it.copy(isLoading = true, errorMessageRes = null) }
                    loadExercise(id)
                }
            }
        }
    }

    private fun loadExercise(id: String) {
        viewModelScope.launch {
            getExerciseByIdUseCase(id).fold(
                onSuccess = { exercise ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            exercise = exercise.toUiModel(),
                            errorMessageRes = null
                        )
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = R.string.exercises_load_error
                        )
                    }
                }
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                onEvent(ExerciseWorkoutEvent.OnTimerTick)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun calculateAndFinish() {
        val currentState = _state.value
        val exercise = currentState.exercise ?: return
        
        val durationMins = currentState.secondsElapsed.toDouble() / 60.0
        val calories = if (exercise.type == ExerciseType.CARDIO) {
            durationMins * (exercise.kcalPerMin ?: 0.0)
        } else {
            currentState.reps.toDouble() * currentState.sets.toDouble() * (exercise.kcalPerRep ?: 0.0)
        }

        val roundedCalories = Math.round(calories).toInt().coerceAtLeast(1)
        val workoutMinutes = (currentState.secondsElapsed / 60).coerceAtLeast(1)

        viewModelScope.launch { addExerciseWorkout(roundedCalories, workoutMinutes) }

        _state.update {
            it.copy(
                caloriesBurned = roundedCalories,
                showCongratsDialog = true,
                isTimerRunning = false
            )
        }
    }

    private fun Exercise.toUiModel(): ExerciseUiModel = ExerciseUiModel(
        id = id,
        name = name,
        equipment = equipment,
        target = target,
        instructions = run {
            val lang = java.util.Locale.getDefault().language.lowercase()
            instructions[lang] ?: instructions["en"].orEmpty()
        },
        type = if (category == "cardio" || minKcal != null) ExerciseType.CARDIO else ExerciseType.NORMAL_WORKOUT,
        imageUrl = imageUrl,
        gifUrl = gifUrl,
        kcalPerMin = minKcal,
        kcalPerRep = repKcal
    )

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
