package iti.grad.nutriscan.presentation.settings.profile.edit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEffect
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileEvent
import iti.grad.nutriscan.presentation.settings.profile.edit.state.EditProfileState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Edit Profile screen.
 *
 * Implements MVI patterns and stores editable profile states in-memory with safety confirmation flows.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _effect = Channel<EditProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.UpdateName -> _state.update { it.copy(name = event.name) }
            is EditProfileEvent.UpdateUsername -> _state.update { it.copy(username = event.username) }
            is EditProfileEvent.UpdateEmail -> _state.update { it.copy(email = event.email) }
            is EditProfileEvent.UpdatePassword -> _state.update { it.copy(password = event.password) }
            is EditProfileEvent.ToggleCondition -> toggleCondition(event.condition)
            is EditProfileEvent.ToggleAllergy -> toggleAllergy(event.allergy)
            EditProfileEvent.StartAddCustomCondition -> _state.update {
                it.copy(isAddingCustomCondition = true, customConditionInput = "")
            }
            is EditProfileEvent.UpdateCustomConditionInput -> _state.update {
                it.copy(customConditionInput = event.value)
            }
            EditProfileEvent.SubmitCustomCondition -> submitCustomCondition()
            EditProfileEvent.CancelAddCustomCondition -> _state.update {
                it.copy(isAddingCustomCondition = false, customConditionInput = "")
            }
            EditProfileEvent.StartAddCustomAllergy -> _state.update {
                it.copy(isAddingCustomAllergy = true, customAllergyInput = "")
            }
            is EditProfileEvent.UpdateCustomAllergyInput -> _state.update {
                it.copy(customAllergyInput = event.value)
            }
            EditProfileEvent.SubmitCustomAllergy -> submitCustomAllergy()
            EditProfileEvent.CancelAddCustomAllergy -> _state.update {
                it.copy(isAddingCustomAllergy = false, customAllergyInput = "")
            }
            EditProfileEvent.SaveClicked -> _state.update { it.copy(showSaveConfirmation = true) }
            EditProfileEvent.ConfirmSave -> saveProfileData()
            EditProfileEvent.DismissSaveConfirmation -> _state.update { it.copy(showSaveConfirmation = false) }
            EditProfileEvent.BackClicked -> emitEffect(EditProfileEffect.NavigateBack)
            is EditProfileEvent.SelectAvatar -> _state.update { it.copy(avatarUrl = event.avatarUrl) }
        }
    }

    private fun toggleCondition(condition: String) {
        _state.update {
            val list = it.selectedChronicConditions
            val newList = if (list.contains(condition)) {
                list - condition
            } else {
                list + condition
            }
            it.copy(selectedChronicConditions = newList.toPersistentList())
        }
    }

    private fun toggleAllergy(allergy: String) {
        _state.update {
            val list = it.selectedAllergies
            val newList = if (list.contains(allergy)) {
                list - allergy
            } else {
                list + allergy
            }
            it.copy(selectedAllergies = newList.toPersistentList())
        }
    }

    private fun submitCustomCondition() {
        val input = _state.value.customConditionInput.trim()
        if (input.isNotEmpty()) {
            _state.update {
                val conditions = if (it.chronicConditions.contains(input)) {
                    it.chronicConditions
                } else {
                    it.chronicConditions + input
                }
                val selected = if (it.selectedChronicConditions.contains(input)) {
                    it.selectedChronicConditions
                } else {
                    it.selectedChronicConditions + input
                }
                it.copy(
                    chronicConditions = conditions.toPersistentList(),
                    selectedChronicConditions = selected.toPersistentList(),
                    isAddingCustomCondition = false,
                    customConditionInput = ""
                )
            }
        } else {
            _state.update { it.copy(isAddingCustomCondition = false) }
        }
    }

    private fun submitCustomAllergy() {
        val input = _state.value.customAllergyInput.trim()
        if (input.isNotEmpty()) {
            _state.update {
                val allergiesList = if (it.allergies.contains(input)) {
                    it.allergies
                } else {
                    it.allergies + input
                }
                val selected = if (it.selectedAllergies.contains(input)) {
                    it.selectedAllergies
                } else {
                    it.selectedAllergies + input
                }
                it.copy(
                    allergies = allergiesList.toPersistentList(),
                    selectedAllergies = selected.toPersistentList(),
                    isAddingCustomAllergy = false,
                    customAllergyInput = ""
                )
            }
        } else {
            _state.update { it.copy(isAddingCustomAllergy = false) }
        }
    }

    private fun saveProfileData() {
        _state.update { it.copy(isLoading = true, showSaveConfirmation = false) }
        // Simulate save API success
        viewModelScope.launch {
            _state.update { it.copy(isLoading = false) }
            _effect.send(EditProfileEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: EditProfileEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun createInitialState(): EditProfileState = EditProfileState(
        name = "",
        username = "",
        email = "",
        password = "",
        avatarUrl = "https://i.pravatar.cc/200?u=yousef-elban",
        selectedChronicConditions = kotlinx.collections.immutable.persistentListOf("Celiac Disease"),
        selectedAllergies = kotlinx.collections.immutable.persistentListOf()
    )
}
