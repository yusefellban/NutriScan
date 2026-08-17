package iti.grad.nutriscan.presentation.main.calories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.dailytracking.usecase.ObserveTodayDailyTrackingUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.ReconcileTodayUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateTargetWaterCntUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateWaterCntUseCase
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.ObserveTodayFoodLogUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryCompletelyUseCase
import iti.grad.nutriscan.domain.foodlog.usecase.RemoveFoodEntryUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import iti.grad.presentation.R
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CaloriesViewModel
@Inject
constructor(
        private val checkStepsPermission: CheckStepsPermissionUseCase,
        private val observeTodaySteps: ObserveTodayStepsUseCase,
        private val observeTodayFoodLog: ObserveTodayFoodLogUseCase,
        private val removeFoodEntry: RemoveFoodEntryUseCase,
        private val removeFoodEntryCompletely: RemoveFoodEntryCompletelyUseCase,
        private val observeTodayDailyTracking: ObserveTodayDailyTrackingUseCase,
        private val updateWaterCnt: UpdateWaterCntUseCase,
        private val updateTargetWaterCnt: UpdateTargetWaterCntUseCase,
        private val updateStepsCnt: UpdateStepsCntUseCase,
        private val reconcileToday: ReconcileTodayUseCase,
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

    /**
     * Server-computed TDEE (see [iti.grad.nutriscan.domain.user.model.User]) — 0 until the first
     * profile sync completes.
     */
    private fun observeUserMetrics() {
        viewModelScope.launch {
            userRepository.getUserData().catch { /* Ignored */}.collect { user ->
                _state.update { it.copy(tdee = user?.tdee?.toInt() ?: 0) }
            }
        }
    }

    /**
     * Collects today's Room-backed water/steps/exercise (offline-first, synced nightly for
     * water/steps only — exercise has no backend field yet) — see DailyTrackingRepositoryImpl.
     */
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
            CaloriesEvent.RemoveWaterClicked -> removeWaterCup()
            CaloriesEvent.StepsCardClicked -> checkStepsAccess()
            is CaloriesEvent.StepsPermissionResult -> handleStepsPermissionResult(event.granted)
            is CaloriesEvent.FoodItemDeleteClicked -> {
                _state.update { it.copy(pendingRemoveFoodId = event.entryId) }
            }
            CaloriesEvent.RemoveFoodConfirmed -> confirmRemoveFood()
            CaloriesEvent.RemoveFoodDismissed -> {
                _state.update { it.copy(pendingRemoveFoodId = null) }
            }
            is CaloriesEvent.FoodItemClicked ->
                    navigate(CaloriesEffect.NavigateToProductDetail(event.product))
            is CaloriesEvent.FoodItemMinusClicked -> minusFoodItem(event.entryId)
            CaloriesEvent.Refreshed -> refresh()
        }
    }

    /**
     * Re-pulls today's backend state on demand. [reconcileToday] seeds water/steps *and* inserts
     * any backend-known meal missing locally, so this is what makes a reinstall — or a second
     * device on the same account — catch up without waiting for the next app start. The Room flows
     * this ViewModel already collects push the new data into state on their own.
     */
    private fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            reconcileToday().onFailure {
                navigate(CaloriesEffect.ShowSnackbar(R.string.calories_refresh_error))
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Collects today's food log (Room, offline-first). One row per product per day — the repository
     * increments/decrements mealCnt in place (see FoodLogRepositoryImpl) — so no grouping is needed
     * here, unlike before that guarantee existed.
     */
    private fun observeFoodLog() {
        viewModelScope.launch {
            observeTodayFoodLog().collect { entries ->
                val products = entries.map { it.toProductUiModel() }.toImmutableList()
                _state.update {
                    it.copy(
                            addedFoods = products,
                            caloriesGained =
                                    entries.sumOf { entry -> entry.calories * entry.mealCnt },
                    )
                }
            }
        }
    }

    /** Delete button — removes every serving of the entry, regardless of its current count. */
    private fun confirmRemoveFood() {
        val entryId = _state.value.pendingRemoveFoodId ?: return
        viewModelScope.launch {
            removeFoodEntryCompletely(entryId).onFailure {
                navigate(CaloriesEffect.ShowSnackbar(R.string.food_log_remove_error))
            }
            _state.update { it.copy(pendingRemoveFoodId = null) }
        }
    }

    /** Minus button — decrements one serving immediately, no confirmation. */
    private fun minusFoodItem(entryId: String) {
        viewModelScope.launch {
            removeFoodEntry(entryId).onFailure {
                navigate(CaloriesEffect.ShowSnackbar(R.string.food_log_remove_error))
            }
        }
    }

    /**
     * Checks the step-counter permission and either starts live tracking or asks the screen to
     * request it.
     */
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

    /**
     * Collects the live sensor-backed steps flow and updates local state directly on every emission
     * so the gauge feels instant. The Room mirror (via [updateStepsCnt]) is only read once a night
     * by the sync worker, so it's collected separately and debounced by 30s to avoid a DB read +
     * write per single step (bursts of rapid emissions collapse to one write).
     */
    private fun startObservingSteps() {
        if (stepsObservationJob?.isActive == true) return
        stepsObservationJob =
                viewModelScope.launch {
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
     * Only the boundary cups respond: tapping the next empty cup fills it, tapping the last filled
     * cup unfills it — every other index is a no-op, so cups always fill/unfill strictly in order.
     */
    private fun toggleWaterCup(index: Int) {
        val state = _state.value
        val newWaterCnt =
                when {
                    // Tapping any empty cup jumps straight to it — fills every cup up to and
                    // including
                    // it in one update; the cascading fill-by-fill look is purely a UI animation
                    // (see
                    // WaterTrackerCard.WaterCup), not a series of backend writes.
                    index >= state.waterConsumed && index < state.waterGoal -> index + 1
                    index == state.waterConsumed - 1 && state.waterConsumed > 0 ->
                            state.waterConsumed - 1
                    else -> return
                }
        viewModelScope.launch { updateWaterCnt(newWaterCnt) }
    }

    /** Removes the last cup, dropping [CaloriesState.waterConsumed] to match if it was filled. */
    private fun removeWaterCup() {
        val state = _state.value
        if (state.waterGoal <= 0) return
        val newGoal = state.waterGoal - 1
        val newWaterCnt = state.waterConsumed.coerceAtMost(newGoal)
        viewModelScope.launch {
            val targetResult = updateTargetWaterCnt(newGoal)
            val countResult = updateWaterCnt(newWaterCnt)
            if (targetResult.isFailure || countResult.isFailure) {
                navigate(CaloriesEffect.ShowSnackbar(R.string.water_cup_remove_error))
            }
        }
    }

    private fun navigate(effect: CaloriesEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun FoodLogEntry.toProductUiModel(): ProductUiModel =
            ProductUiModel(
                    id = productId ?: id,
                    productName = name,
                    imageUrl = imageUrl,
                    verdict = verdict,
                    calories = calories.toString(),
                    quantity = mealCnt,
                    logEntryId = id,
            )
}
