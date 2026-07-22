package iti.grad.nutriscan.presentation.main.calories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEffect
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesEvent
import iti.grad.nutriscan.presentation.main.calories.state.CaloriesState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaloriesViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(CaloriesState())
    val state: StateFlow<CaloriesState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: CaloriesEvent) {
        when (event) {
            CaloriesEvent.AddFoodClicked -> navigate(CaloriesEffect.NavigateToSavedProducts)
            CaloriesEvent.AddExerciseClicked -> Unit
            CaloriesEvent.AddWaterClicked -> addWaterCup()
            is CaloriesEvent.WaterCupClicked -> toggleWaterCup(event.index)
            is CaloriesEvent.WaterCupLongPressed -> removeWaterCup(event.index)
            is CaloriesEvent.BottomNavTabClicked -> handleTabClick(event.tab)
        }
    }

    private fun addWaterCup() {
        _state.update { it.copy(waterGoal = it.waterGoal + 1) }
    }

    /**
     * Only the boundary cups respond: tapping the next empty cup fills it,
     * tapping the last filled cup unfills it — every other index is a no-op,
     * so cups always fill/unfill strictly in order.
     */
    private fun toggleWaterCup(index: Int) {
        _state.update { state ->
            when {
                index == state.waterConsumed && index < state.waterGoal ->
                    state.copy(waterConsumed = state.waterConsumed + 1)
                index == state.waterConsumed - 1 && state.waterConsumed > 0 ->
                    state.copy(waterConsumed = state.waterConsumed - 1)
                else -> state
            }
        }
    }

    /** Only the last cup can be deleted, same ordering rule as [toggleWaterCup]. */
    private fun removeWaterCup(index: Int) {
        _state.update { state ->
            if (index == state.waterGoal - 1) {
                val newGoal = state.waterGoal - 1
                state.copy(waterGoal = newGoal, waterConsumed = state.waterConsumed.coerceAtMost(newGoal))
            } else {
                state
            }
        }
    }

    private fun handleTabClick(tab: BottomNavTab) {
        // Calories is the only tab rendered inline here — every other tab is
        // a separate destination reached via effect, mirroring HomeViewModel.
        when (tab) {
            BottomNavTab.CALORIES -> Unit
            BottomNavTab.HOME -> navigate(CaloriesEffect.NavigateToHome)
            BottomNavTab.SCAN -> navigate(CaloriesEffect.NavigateToScan)
            BottomNavTab.SHOPPING -> navigate(CaloriesEffect.NavigateToShopping)
            BottomNavTab.PROFILE -> navigate(CaloriesEffect.NavigateToProfile)
        }
    }

    private fun navigate(effect: CaloriesEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
