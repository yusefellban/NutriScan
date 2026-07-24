package iti.grad.nutriscan.presentation.main.calories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.domain.water.usecase.LogWaterGlassUseCase
import iti.grad.nutriscan.domain.water.usecase.ObserveTodayWaterUseCase
import iti.grad.nutriscan.domain.water.usecase.SetWaterGoalUseCase
import iti.grad.nutriscan.domain.water.usecase.UnlogWaterGlassUseCase
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import iti.grad.presentation.R
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
    private val observeTodayWater: ObserveTodayWaterUseCase,
    private val logWaterGlass: LogWaterGlassUseCase,
    private val unlogWaterGlass: UnlogWaterGlassUseCase,
    private val setWaterGoal: SetWaterGoalUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CaloriesState())
    val state: StateFlow<CaloriesState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var stepsObservationJob: Job? = null

    init {
        observeFoodLog()
        observeWater()
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

    /** Collects the live sensor-backed steps flow so the gauge updates as the user walks. */
    private fun startObservingSteps() {
        if (stepsObservationJob?.isActive == true) return
        stepsObservationJob = viewModelScope.launch {
            observeTodaySteps().collect { steps -> _state.update { it.copy(steps = steps) } }
        }
    }

    /** Collects today's water log (Room, offline-first) and keeps waterConsumed/waterGoal in sync. */
    private fun observeWater() {
        viewModelScope.launch {
            observeTodayWater().collect { water ->
                _state.update { it.copy(waterConsumed = water.glassCount, waterGoal = water.goalGlasses) }
            }
        }
    }

    private fun addWaterCup() {
        val newGoal = _state.value.waterGoal + 1
        _state.update { it.copy(waterGoal = newGoal) }
        viewModelScope.launch { setWaterGoal(newGoal) }
    }

    /**
     * Only the boundary cups respond: tapping the next empty cup fills it,
     * tapping the last filled cup unfills it — every other index is a no-op,
     * so cups always fill/unfill strictly in order.
     */
    private fun toggleWaterCup(index: Int) {
        val state = _state.value
        when {
            index == state.waterConsumed && index < state.waterGoal -> {
                _state.update { it.copy(waterConsumed = it.waterConsumed + 1) }
                viewModelScope.launch { logWaterGlass() }
            }
            index == state.waterConsumed - 1 && state.waterConsumed > 0 -> {
                _state.update { it.copy(waterConsumed = it.waterConsumed - 1) }
                viewModelScope.launch { unlogWaterGlass() }
            }
        }
    }

    /** Only the last cup can be deleted, same ordering rule as [toggleWaterCup]. */
    private fun removeWaterCup(index: Int) {
        if (index != _state.value.waterGoal - 1) return
        val newGoal = _state.value.waterGoal - 1
        _state.update { state ->
            state.copy(waterGoal = newGoal, waterConsumed = state.waterConsumed.coerceAtMost(newGoal))
        }
        viewModelScope.launch { setWaterGoal(newGoal) }
        navigate(CaloriesEffect.ShowSnackbar(R.string.cup_removed))
    }


    private fun navigate(effect: CaloriesEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun FoodLogEntry.toProductUiModel() = ProductUiModel(
        id = id,
        productName = name,
        imageUrl = imageUrl,
        verdict = verdict,
        calories = calories.toString(),
    )
}
