package iti.grad.nutriscan.presentation.profile_setup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.onboarding.usecase.CompleteOnboardingUseCase
import iti.grad.nutriscan.domain.user.usecase.UpdateHealthProfileUseCase
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEffect
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerEvent
import iti.grad.nutriscan.presentation.profile_setup.state.ProfileSetupPagerState
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
class ProfileSetupPagerViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
    private val updateHealthProfileUseCase: UpdateHealthProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupPagerState())
    val state: StateFlow<ProfileSetupPagerState> = _state.asStateFlow()

    private val _effect = Channel<ProfileSetupPagerEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadDiseases()
        loadAllergies()
    }

    fun onEvent(event: ProfileSetupPagerEvent) {
        when (event) {
            // Pager navigation
            ProfileSetupPagerEvent.NextClicked -> handleNextClicked()
            ProfileSetupPagerEvent.BackClicked -> handleBackClicked()
            is ProfileSetupPagerEvent.PageChanged -> _state.update {
                it.copy(currentPage = event.page)
            }

            // Page 1: Gender
            is ProfileSetupPagerEvent.SelectGender -> _state.update {
                it.copy(selectedGender = event.gender)
            }

            // Page 2: Date of Birth
            is ProfileSetupPagerEvent.SelectDateOfBirth -> _state.update {
                it.copy(selectedDateOfBirthMillis = event.dateMillis)
            }

            // Page 3: Height
            is ProfileSetupPagerEvent.SelectHeight -> _state.update {
                it.copy(selectedHeightCm = event.heightCm)
            }

            // Page 4: Weight
            is ProfileSetupPagerEvent.SelectWeight -> _state.update {
                it.copy(selectedWeightKg = event.weightKg)
            }

            // Health Profile events
            is ProfileSetupPagerEvent.ToggleDisease -> toggleDisease(event.diseaseId)
            ProfileSetupPagerEvent.RetryLoadDiseases -> loadDiseases()
            is ProfileSetupPagerEvent.ToggleAllergy -> toggleAllergy(event.allergyId)
            ProfileSetupPagerEvent.RetryLoadAllergies -> loadAllergies()
            ProfileSetupPagerEvent.SaveProfile -> saveProfile()
        }
    }

    private fun handleNextClicked() {
        val currentPage = _state.value.currentPage
        val lastPage = _state.value.pageCount - 1
        if (currentPage < lastPage) {
            viewModelScope.launch {
                _effect.send(ProfileSetupPagerEffect.ScrollToPage(currentPage + 1))
            }
        }
    }

    private fun handleBackClicked() {
        val currentPage = _state.value.currentPage
        if (currentPage > 0) {
            viewModelScope.launch {
                _effect.send(ProfileSetupPagerEffect.ScrollToPage(currentPage - 1))
            }
        } else {
            viewModelScope.launch {
                _effect.send(ProfileSetupPagerEffect.NavigateBack)
            }
        }
    }

    private fun loadDiseases() {
        viewModelScope.launch {
            _state.update { it.copy(isDiseasesLoading = true, diseasesErrorMessage = null) }
            getDiseasesUseCase()
                .onSuccess { diseases ->
                    _state.update {
                        it.copy(
                            diseases = diseases.toImmutableList(),
                            isDiseasesLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isDiseasesLoading = false,
                            diseasesErrorMessage = throwable.message ?: "Failed to load diseases"
                        )
                    }
                }
        }
    }

    private fun loadAllergies() {
        viewModelScope.launch {
            _state.update { it.copy(isAllergiesLoading = true, allergiesErrorMessage = null) }
            getAllergiesUseCase()
                .onSuccess { allergies ->
                    _state.update {
                        it.copy(
                            allergies = allergies.toImmutableList(),
                            isAllergiesLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isAllergiesLoading = false,
                            allergiesErrorMessage = throwable.message ?: "Failed to load allergies"
                        )
                    }
                }
        }
    }

    private fun toggleDisease(diseaseId: Int) {
        _state.update { state ->
            val updatedSelected = if (state.selectedDiseaseIds.contains(diseaseId)) {
                state.selectedDiseaseIds.filter { it != diseaseId }
            } else {
                state.selectedDiseaseIds + diseaseId
            }
            state.copy(selectedDiseaseIds = updatedSelected.toImmutableList())
        }
    }

    private fun toggleAllergy(allergyId: Int) {
        _state.update { state ->
            val updatedSelected = if (state.selectedAllergyIds.contains(allergyId)) {
                state.selectedAllergyIds.filter { it != allergyId }
            } else {
                state.selectedAllergyIds + allergyId
            }
            state.copy(selectedAllergyIds = updatedSelected.toImmutableList())
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            updateHealthProfileUseCase(
                diseaseIds = _state.value.selectedDiseaseIds,
                allergyIds = _state.value.selectedAllergyIds
            )
                .onSuccess {
                    completeOnboardingUseCase()
                    _effect.send(ProfileSetupPagerEffect.NavigateToHome)
                }
                .onFailure { throwable ->
                    _effect.send(
                        ProfileSetupPagerEffect.ShowSnackbar(
                            messageStr = throwable.message ?: "Failed to save profile"
                        )
                    )
                }
            _state.update { it.copy(isLoading = false) }
        }
    }
}
