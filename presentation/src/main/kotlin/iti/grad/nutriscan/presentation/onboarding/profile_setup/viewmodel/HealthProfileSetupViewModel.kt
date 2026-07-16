package iti.grad.nutriscan.presentation.onboarding.profile_setup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.onboarding.usecase.CompleteOnboardingUseCase
import iti.grad.nutriscan.presentation.onboarding.profile_setup.state.HealthProfileSetupEffect
import iti.grad.nutriscan.presentation.onboarding.profile_setup.state.HealthProfileSetupEvent
import iti.grad.nutriscan.presentation.onboarding.profile_setup.state.HealthProfileSetupState
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
class HealthProfileSetupViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HealthProfileSetupState())
    val state: StateFlow<HealthProfileSetupState> = _state.asStateFlow()

    private val _effect = Channel<HealthProfileSetupEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: HealthProfileSetupEvent) {
        when (event) {
            is HealthProfileSetupEvent.ToggleCondition -> toggleCondition(event.condition)
            is HealthProfileSetupEvent.ToggleAllergy -> toggleAllergy(event.allergy)
            HealthProfileSetupEvent.StartAddCustomCondition -> _state.update {
                it.copy(isAddingCustomCondition = true, customConditionInput = "")
            }
            is HealthProfileSetupEvent.UpdateCustomConditionInput -> _state.update {
                it.copy(customConditionInput = event.input)
            }
            HealthProfileSetupEvent.SubmitCustomCondition -> submitCustomCondition()
            HealthProfileSetupEvent.CancelAddCustomCondition -> _state.update {
                it.copy(isAddingCustomCondition = false, customConditionInput = "")
            }
            HealthProfileSetupEvent.StartAddCustomAllergy -> _state.update {
                it.copy(isAddingCustomAllergy = true, customAllergyInput = "")
            }
            is HealthProfileSetupEvent.UpdateCustomAllergyInput -> _state.update {
                it.copy(customAllergyInput = event.input)
            }
            HealthProfileSetupEvent.SubmitCustomAllergy -> submitCustomAllergy()
            HealthProfileSetupEvent.CancelAddCustomAllergy -> _state.update {
                it.copy(isAddingCustomAllergy = false, customAllergyInput = "")
            }
            HealthProfileSetupEvent.SaveProfile -> saveProfile()
        }
    }

    private fun toggleCondition(condition: String) {
        _state.update { state ->
            val updatedSelected = if (state.selectedChronicConditions.contains(condition)) {
                state.selectedChronicConditions.filter { it != condition }
            } else {
                state.selectedChronicConditions + condition
            }
            state.copy(selectedChronicConditions = updatedSelected.toImmutableList())
        }
    }

    private fun toggleAllergy(allergy: String) {
        _state.update { state ->
            val updatedSelected = if (state.selectedAllergies.contains(allergy)) {
                state.selectedAllergies.filter { it != allergy }
            } else {
                state.selectedAllergies + allergy
            }
            state.copy(selectedAllergies = updatedSelected.toImmutableList())
        }
    }

    private fun submitCustomCondition() {
        val input = _state.value.customConditionInput.trim()
        if (input.isNotEmpty()) {
            _state.update { state ->
                val updatedConditions = if (state.chronicConditions.contains(input)) {
                    state.chronicConditions
                } else {
                    state.chronicConditions + input
                }
                val updatedSelected = if (state.selectedChronicConditions.contains(input)) {
                    state.selectedChronicConditions
                } else {
                    state.selectedChronicConditions + input
                }
                state.copy(
                    chronicConditions = updatedConditions.toImmutableList(),
                    selectedChronicConditions = updatedSelected.toImmutableList(),
                    isAddingCustomCondition = false,
                    customConditionInput = ""
                )
            }
        } else {
            _state.update {
                it.copy(isAddingCustomCondition = false, customConditionInput = "")
            }
        }
    }

    private fun submitCustomAllergy() {
        val input = _state.value.customAllergyInput.trim()
        if (input.isNotEmpty()) {
            _state.update { state ->
                val updatedAllergies = if (state.allergies.contains(input)) {
                    state.allergies
                } else {
                    state.allergies + input
                }
                val updatedSelected = if (state.selectedAllergies.contains(input)) {
                    state.selectedAllergies
                } else {
                    state.selectedAllergies + input
                }
                state.copy(
                    allergies = updatedAllergies.toImmutableList(),
                    selectedAllergies = updatedSelected.toImmutableList(),
                    isAddingCustomAllergy = false,
                    customAllergyInput = ""
                )
            }
        } else {
            _state.update {
                it.copy(isAddingCustomAllergy = false, customAllergyInput = "")
            }
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                completeOnboardingUseCase()
                _effect.send(HealthProfileSetupEffect.NavigateToHome)
            } catch (e: Exception) {
                _effect.send(HealthProfileSetupEffect.ShowSnackbar(messageStr = e.message ?: "Failed to save profile"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
