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
import iti.grad.nutriscan.domain.user.repository.IUserRepository
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
    private val userRepository: IUserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CaloriesState())
    val state: StateFlow<CaloriesState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var stepsObservationJob: Job? = null

    init {
        observeFoodLog()
        observeDailyTracking()
        observeUserMetrics()
    }

    /** Server-computed TDEE/BMI (see [iti.grad.nutriscan.domain.user.model.User]) — null until
     * the first profile sync completes. The Calories screen shows a placeholder in that gap
     * rather than hiding the BMI page. */
    private fun observeUserMetrics() {
        viewModelScope.launch {
            userRepository.getUserData().collect { user ->
                _state.update {
                    it.copy(
                        tdee = user?.tdee?.toInt() ?: 0,
                        bmi = user?.bmi,
                    )
                }
            }
        }
    }

    /** Collects today's Room-backed water/steps/exercise (offline-first, synced nightly for
     * water/steps only — exercise has no backend field yet) — see DailyTrackingRepositoryImpl. */
    private fun observeDailyTracking() {
        viewModelScope.launch {
            observeTodayDailyTracking().collect { tracking ->
                _state.update {
                    it.copy(
                        waterConsumed = tracking.waterCnt,
                        waterGoal = tracking.targetWaterCnt,
                        steps = tracking.stepsCnt,
                        exerciseKcal = tracking.exerciseKcal,
                        exerciseMinutes = tracking.exerciseMinutes,
                        caloriesBurned = tracking.caloriesBurnedSteps + tracking.exerciseKcal,
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

    /** Collects today's food log (Room, offline-first) and keeps addedFoods/caloriesGained in
     * sync. Entries for the same product are grouped into one card with a quantity badge instead
     * of duplicating the card — see [toGroupedProductUiModel]. */
    private fun observeFoodLog() {
        viewModelScope.launch {
            observeTodayFoodLog().collect { entries ->
                val products = entries
                    .groupBy { it.productId ?: it.id }
                    .values
                    .map { it.toGroupedProductUiModel() }
                    .toImmutableList()
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

    /** Collects the live sensor-backed steps flow and updates local state directly on every
     * emission so the gauge feels instant. The Room mirror (via [updateStepsCnt]) is only read
     * once a night by the sync worker, so it's collected separately and debounced by 30s to
     * avoid a DB read + write per single step (bursts of rapid emissions collapse to one write). */
    private fun startObservingSteps() {
        if (stepsObservationJob?.isActive == true) return
        stepsObservationJob = viewModelScope.launch {
            launch {
                observeTodaySteps().collect { steps ->
                    _state.update { it.copy(steps = steps) }
                }
            }
            launch {
                observeTodaySteps().debounce(30.seconds).collect { steps ->
                    updateStepsCnt(steps)
                }
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

    /** One card per distinct product: [quantity] is the group size, and swiping to remove
     * targets [logEntryId] — the most-recently-added entry — so each swipe removes one instance
     * and decrements the badge instead of deleting every logged copy at once. */
    private fun List<FoodLogEntry>.toGroupedProductUiModel(): ProductUiModel {
        val mostRecent = maxBy { it.addedAt }
        return ProductUiModel(
            id = mostRecent.productId ?: mostRecent.id,
            productName = mostRecent.name,
            imageUrl = mostRecent.imageUrl,
            verdict = mostRecent.verdict,
            calories = mostRecent.calories.toString(),
            quantity = size,
            logEntryId = mostRecent.id,
        )
    }
}
