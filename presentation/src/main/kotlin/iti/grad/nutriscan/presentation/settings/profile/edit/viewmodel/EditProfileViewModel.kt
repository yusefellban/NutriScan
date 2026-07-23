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

import iti.grad.nutriscan.domain.user.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.flow.collectLatest

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

import iti.grad.nutriscan.domain.disease.usecase.GetDiseasesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.GetAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.user.usecase.GetUserProfileUseCase
import kotlinx.collections.immutable.toImmutableList

/**
 * ViewModel for the Edit Profile screen.
 *
 * Implements MVI patterns and stores editable profile states in-memory with safety confirmation flows.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val getDiseasesUseCase: GetDiseasesUseCase,
    private val getAllergiesUseCase: GetAllergiesUseCase,
    private val syncDiseasesUseCase: SyncDiseasesUseCase,
    private val syncAllergiesUseCase: SyncAllergiesUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _effect = Channel<EditProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadDiseasesOffline()
        loadAllergiesOffline()
        viewModelScope.launch {
            getUserProfileUseCase().collectLatest { user ->
                if (user != null) {
                    _state.update {
                        it.copy(
                            firstName = user.firstName,
                            lastName = user.lastName ?: "",
                            dateOfBirth = user.dateOfBirth ?: "",
                            email = user.email ?: "",
                            heightCm = user.heightCm,
                            weightKg = user.weightKg,
                            avatarUrl = user.avatarUrl,
                            selectedDiseaseIds = user.diseaseIds.toPersistentList(),
                            selectedAllergyIds = user.allergyIds.toPersistentList()
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            EditProfileEvent.EditClicked -> {
                _state.update { it.copy(isEditMode = true) }
                syncData()
            }
            is EditProfileEvent.UpdateFirstName -> _state.update { it.copy(firstName = event.firstName) }
            is EditProfileEvent.UpdateLastName -> _state.update { it.copy(lastName = event.lastName) }
            is EditProfileEvent.UpdateDateOfBirth -> _state.update { it.copy(dateOfBirth = event.dateOfBirth) }
            is EditProfileEvent.ToggleDisease -> toggleDisease(event.diseaseId)
            is EditProfileEvent.ToggleAllergy -> toggleAllergy(event.allergyId)
            EditProfileEvent.RetryLoadDiseases -> syncData() // now retry syncs data
            EditProfileEvent.RetryLoadAllergies -> syncData()
            EditProfileEvent.SaveClicked -> _state.update { it.copy(showSaveConfirmation = true) }
            EditProfileEvent.ConfirmSave -> saveProfileData()
            EditProfileEvent.DismissSaveConfirmation -> _state.update { it.copy(showSaveConfirmation = false) }
            EditProfileEvent.BackClicked -> emitEffect(EditProfileEffect.NavigateBack)
            is EditProfileEvent.SelectAvatar -> _state.update { it.copy(avatarUrl = event.avatarUrl) }
            is EditProfileEvent.UpdateHeight -> _state.update { it.copy(heightCm = event.heightCm) }
            is EditProfileEvent.UpdateWeight -> _state.update { it.copy(weightKg = event.weightKg) }
            EditProfileEvent.DismissAlert -> {
                val wasSuccess = _state.value.alertState is iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Success
                _state.update { it.copy(alertState = iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None) }
                if (wasSuccess) {
                    emitEffect(EditProfileEffect.NavigateBack)
                }
            }
            EditProfileEvent.RetryAction -> saveProfileData()
        }
    }

    private fun loadDiseasesOffline() {
        viewModelScope.launch {
            getDiseasesUseCase().collectLatest { diseases ->
                _state.update {
                    it.copy(
                        diseases = diseases.toImmutableList(),
                        isDiseasesLoading = false
                    )
                }
            }
        }
    }

    private fun loadAllergiesOffline() {
        viewModelScope.launch {
            getAllergiesUseCase().collectLatest { allergies ->
                _state.update {
                    it.copy(
                        allergies = allergies.toImmutableList(),
                        isAllergiesLoading = false
                    )
                }
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            _state.update { it.copy(isDiseasesLoading = true, isAllergiesLoading = true, diseasesErrorMessage = null, allergiesErrorMessage = null) }
            
            val diseasesResult = syncDiseasesUseCase()
            val allergiesResult = syncAllergiesUseCase()
            
            _state.update {
                it.copy(
                    isDiseasesLoading = false,
                    isAllergiesLoading = false,
                    diseasesErrorMessage = diseasesResult.exceptionOrNull()?.message,
                    allergiesErrorMessage = allergiesResult.exceptionOrNull()?.message
                )
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

    private fun saveProfileData() {
        _state.update { it.copy(isSaving = true, showSaveConfirmation = false) }
        viewModelScope.launch {
            val currentState = _state.value
            var finalAvatarUrl = currentState.avatarUrl
            
            // If the user selected a new image from the PhotoPicker, it will be a content:// URI.
            // These URIs lose permission after app restart, so we must copy the file to internal storage.
            if (finalAvatarUrl != null && finalAvatarUrl.startsWith("content://")) {
                try {
                    val uri = android.net.Uri.parse(finalAvatarUrl)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    // Save to a static filename so we don't leak space with multiple edits
                    val file = File(context.filesDir, "profile_avatar.jpg")
                    inputStream?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Coil and standard URI parsers handle file:// schemes perfectly
                    finalAvatarUrl = "file://" + file.absolutePath
                } catch (e: Exception) {
                    // If copy fails, fallback to what we had (or ignore)
                }
            }
            
            updateUserProfileUseCase(
                firstName = currentState.firstName,
                lastName = currentState.lastName.takeIf { it.isNotBlank() },
                gender = null, // Or handle if gender exists
                dateOfBirth = currentState.dateOfBirth.takeIf { it.isNotBlank() },
                heightCm = currentState.heightCm,
                weightKg = currentState.weightKg,
                diseaseIds = currentState.selectedDiseaseIds,
                allergyIds = currentState.selectedAllergyIds,
                avatarUrl = finalAvatarUrl
            ).onSuccess {
                _state.update { 
                    it.copy(
                        isSaving = false, 
                        isEditMode = false,
                        alertState = iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Success(
                            messageResId = iti.grad.presentation.R.string.alert_success_title // You can provide a specific string for profile updated
                        )
                    ) 
                }
            }.onFailure { error ->
                val newAlertState = when {
                    error is java.io.IOException -> iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.InternetError
                    else -> iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.Error(messageStr = "Failed to update profile. Please try again.")
                }
                _state.update { it.copy(isSaving = false, alertState = newAlertState) }
            }
        }
    }

    private fun emitEffect(effect: EditProfileEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }


}
