package iti.grad.nutriscan.presentation.main.calories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateTargetWaterCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateWaterCntUseCase
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.exercises.tracker.ExercisesSharedTracker
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaloriesViewModel @Inject constructor(
    private val checkStepsPermission: CheckStepsPermissionUseCase,
    private val observeTodaySteps: ObserveTodayStepsUseCase,
    private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
    private val removeFoodEntry: RemoveFoodEntryUseCase,
    private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
    private val updateWaterCnt: UpdateWaterCntUseCase,
    private val updateTargetWaterCnt: UpdateTargetWaterCntUseCase,
    private val updateStepsCnt: UpdateStepsCntUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CaloriesState())
    val state: StateFlow<CaloriesState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var stepsObservationJob: Job? = null

    init {
        observeFoodLog()
        observeWorkoutStats()
        observeDailyTracking()
    }

    private fun observeWorkoutStats() {
        viewModelScope.launch {
            ExercisesSharedTracker.exerciseKcal.collect { kcal ->
                _state.update { it.copy(exerciseKcal = kcal) }
            }
        }
        viewModelScope.launch {
            ExercisesSharedTracker.exerciseMinutes.collect { mins ->
                _state.update { it.copy(exerciseMinutes = mins) }
            }
        }
    }

    /** Collects today's Room-backed water/steps (offline-first, synced nightly) — see
     * DailyTrackingRepositoryImpl. Replaces the previous pure in-memory mutation. */
    private fun observeDailyTracking() {
        viewModelScope.launch {
            observeTodayDailyTracking().collect { tracking ->
                _state.update {
                    it.copy(
                        waterConsumed = tracking.waterCnt,
                        waterGoal = tracking.targetWaterCnt,
                        steps = tracking.stepsCnt,
                    )
                }
            }
        }
    }

    fun onEvent(event: CaloriesEvent) {
        when (event) {
            CaloriesEvent.AddFoodClicked -> navigate(CaloriesEffect.NavigateToSavedProducts)
            CaloriesEvent.AddExerciseClicked -> navigate(CaloriesEffect.NavigateToExercises)
            CaloriesEvent.AddWaterClicked -> addWaterCup()
            is CaloriesEvent.WaterCupClicked -> toggleWaterCup(event.index)
            is CaloriesEvent.WaterCupLongPressed -> removeWaterCup(event.index)
            CaloriesEvent.StepsCardClicked -> checkStepsAccess()
            is CaloriesEvent.StepsPermissionResult -> handleStepsPermissionResult(event.granted)
            is CaloriesEvent.FoodItemSwipedToRemove -> {
                _state.update { it.copy(pendingRemoveFoodId = event.entryId) }
            }
            CaloriesEvent.RemoveFoodConfirmed -> confirmRemoveFood()
            CaloriesEvent.RemoveFoodDismissed -> {
                _state.update { it.copy(pendingRemoveFoodId = null) }
            }
            is CaloriesEvent.FoodItemClicked -> navigate(CaloriesEffect.NavigateToProductDetail(event.product))
        }
    }

    /** Collects today's food log (Room, offline-first) and keeps addedFoods/caloriesGained in sync. */
    private fun observeFoodLog() {
        viewModelScope.launch {
            observeTodayFoodLog().collect { entries ->
                val products = entries.map { it.toProductUiModel() }.toImmutableList()
                _state.update {
                    it.copy(
                        addedFoods = products,
                        caloriesGained = entries.sumOf { entry -> entry.calories },
                    )
                }
            }
        }
    }

    private fun confirmRemoveFood() {
        val entryId = _state.value.pendingRemoveFoodId ?: return
        viewModelScope.launch {
            removeFoodEntry(entryId)
                .onFailure { navigate(CaloriesEffect.ShowSnackbar(R.string.food_log_remove_error)) }
            _state.update { it.copy(pendingRemoveFoodId = null) }
        }
    }

    /** Checks the step-counter permission and either starts live tracking or asks the screen to request it. */
    private fun checkStepsAccess() {
        viewModelScope.launch {
            if (checkStepsPermission()) {
                handleStepsPermissionResult(granted = true)
            } else {
                navigate(CaloriesEffect.RequestStepsPermission)
            }
        }
    }

    private fun handleStepsPermissionResult(granted: Boolean) {
        _state.update { it.copy(stepsPermissionGranted = granted) }
        if (granted) startObservingSteps()
    }

    /** Collects the live sensor-backed steps flow, mirrors each new value into Room via
     * [updateStepsCnt] so it's there for the nightly sync job, and still updates local state
     * directly (rather than waiting on [observeDailyTracking]'s round-trip) so the gauge feels
     * instant. */
    private fun startObservingSteps() {
        if (stepsObservationJob?.isActive == true) return
        stepsObservationJob = viewModelScope.launch {
            observeTodaySteps().collect { steps ->
                _state.update { it.copy(steps = steps) }
                updateStepsCnt(steps)
            }
        }
    }

    private fun addWaterCup() {
        viewModelScope.launch { updateTargetWaterCnt(_state.value.waterGoal + 1) }
    }

    /**
     * Only the boundary cups respond: tapping the next empty cup fills it,
     * tapping the last filled cup unfills it — every other index is a no-op,
     * so cups always fill/unfill strictly in order.
     */
    private fun toggleWaterCup(index: Int) {
        val state = _state.value
        val newWaterCnt = when {
            index == state.waterConsumed && index < state.waterGoal -> state.waterConsumed + 1
            index == state.waterConsumed - 1 && state.waterConsumed > 0 -> state.waterConsumed - 1
            else -> return
        }
        viewModelScope.launch { updateWaterCnt(newWaterCnt) }
    }

    /** Only the last cup can be deleted, same ordering rule as [toggleWaterCup]. */
    private fun removeWaterCup(index: Int) {
        val state = _state.value
        if (index != state.waterGoal - 1) return
        val newGoal = state.waterGoal - 1
        val newWaterCnt = state.waterConsumed.coerceAtMost(newGoal)
        viewModelScope.launch {
            updateTargetWaterCnt(newGoal)
            updateWaterCnt(newWaterCnt)
        }
        navigate(CaloriesEffect.ShowSnackbar(R.string.cup_removed))
    }


    private fun navigate(effect: CaloriesEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun FoodLogEntry.toProductUiModel() = ProductUiModel(
        id = productId ?: id,
        productName = name,
        imageUrl = imageUrl,
        verdict = verdict,
        calories = calories.toString(),
    )
}
