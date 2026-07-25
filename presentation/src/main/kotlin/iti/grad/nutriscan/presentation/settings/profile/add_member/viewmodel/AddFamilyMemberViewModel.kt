package iti.grad.nutriscan.presentation.settings.profile.add_member.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.family.usecase.AddFamilyMemberUseCase
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEffect
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberEvent
import iti.grad.nutriscan.presentation.settings.profile.add_member.state.AddFamilyMemberState
import iti.grad.presentation.R
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Add Family Member bottom sheet. Scoped to the sheet's
 * composition (not shared with [iti.grad.nutriscan.presentation.settings.profile.viewmodel.UserProfileViewModel])
 * so it owns the allergy/disease-loading concerns the profile screen doesn't
 * otherwise need — cleared automatically when the sheet closes.
 *
 * Loading pattern for diseases/allergies mirrors
 * [iti.grad.nutriscan.presentation.profile_setup.viewmodel.ProfileSetupPagerViewModel]
 * exactly: sync from network, then collect the offline-first Room-backed flow.
 */
@HiltViewModel
class AddFamilyMemberViewModel @Inject constructor(
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
    private val syncDiseasesUseCase: SyncDiseasesUseCase,
    private val syncAllergiesUseCase: SyncAllergiesUseCase,
    private val addFamilyMemberUseCase: AddFamilyMemberUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddFamilyMemberState())
    val state: StateFlow<AddFamilyMemberState> = _state.asStateFlow()

    private val _effect = Channel<AddFamilyMemberEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadDiseases()
        loadAllergies()
    }

    fun onEvent(event: AddFamilyMemberEvent) {
        when (event) {
            is AddFamilyMemberEvent.NameChanged ->
                _state.update { it.copy(name = event.name, nameError = null) }

            is AddFamilyMemberEvent.RelationChanged ->
                _state.update { it.copy(relation = event.relation, relationError = null) }

            is AddFamilyMemberEvent.ToggleDisease -> _state.update {
                it.copy(selectedDiseaseIds = it.selectedDiseaseIds.toggle(event.id).toImmutableList())
            }

            is AddFamilyMemberEvent.ToggleAllergy -> _state.update {
                it.copy(selectedAllergyIds = it.selectedAllergyIds.toggle(event.id).toImmutableList())
            }

            AddFamilyMemberEvent.RetryLoadDiseases -> loadDiseases()
            AddFamilyMemberEvent.RetryLoadAllergies -> loadAllergies()
            AddFamilyMemberEvent.SaveClicked -> save()
            AddFamilyMemberEvent.DismissRequested -> emitEffect(AddFamilyMemberEffect.Dismiss)
        }
    }

    private fun loadDiseases() {
        viewModelScope.launch {
            getDiseasesUseCase().collectLatest { diseases ->
                _state.update { it.copy(diseases = diseases.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isDiseasesLoading = true, diseasesErrorMessage = null) }

            val syncResult = syncDiseasesUseCase()
            _state.update { state ->
                val error = if (state.diseases.isEmpty()) {
                    syncResult.exceptionOrNull()?.message ?: "Failed to load diseases"
                } else null

                state.copy(
                    isDiseasesLoading = false,
                    diseasesErrorMessage = if (syncResult.isFailure) error else null
                )
            }
        }
    }

    private fun loadAllergies() {
        viewModelScope.launch {
            getAllergiesUseCase().collectLatest { allergies ->
                _state.update { it.copy(allergies = allergies.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isAllergiesLoading = true, allergiesErrorMessage = null) }

            val syncResult = syncAllergiesUseCase()
            _state.update { state ->
                val error = if (state.allergies.isEmpty()) {
                    syncResult.exceptionOrNull()?.message ?: "Failed to load allergies"
                } else null

                state.copy(
                    isAllergiesLoading = false,
                    allergiesErrorMessage = if (syncResult.isFailure) error else null
                )
            }
        }
    }

    private fun save() {
        val name = _state.value.name.trim()
        val relation = _state.value.relation.trim()

        var hasError = false
        if (name.isBlank()) {
            _state.update { it.copy(nameError = R.string.error_name_required) }
            hasError = true
        }
        if (relation.isBlank()) {
            _state.update { it.copy(relationError = R.string.error_relation_required) }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = addFamilyMemberUseCase(
                name = name,
                relation = relation,
                allergyIds = _state.value.selectedAllergyIds,
                diseaseIds = _state.value.selectedDiseaseIds,
            )
            _state.update { it.copy(isSaving = false) }
            result
                .onSuccess { emitEffect(AddFamilyMemberEffect.Dismiss) }
                .onFailure { emitEffect(AddFamilyMemberEffect.ShowError(it.message ?: "")) }
        }
    }

    private fun List<Int>.toggle(id: Int) = if (contains(id)) this - id else this + id

    private fun emitEffect(effect: AddFamilyMemberEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
