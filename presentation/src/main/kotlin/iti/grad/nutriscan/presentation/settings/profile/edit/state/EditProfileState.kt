package iti.grad.nutriscan.presentation.settings.profile.edit.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class EditProfileState(
    val isEditMode: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val email: String = "",
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val isSaving: Boolean = false,
    val avatarUrl: String? = null,
    val diseases: ImmutableList<iti.grad.nutriscan.domain.disease.model.Disease> = persistentListOf(),
    val selectedDiseaseIds: ImmutableList<Int> = persistentListOf(),
    val isDiseasesLoading: Boolean = false,
    val diseasesErrorMessage: String? = null,
    
    val allergies: ImmutableList<iti.grad.nutriscan.domain.allergy.model.Allergy> = persistentListOf(),
    val selectedAllergyIds: ImmutableList<Int> = persistentListOf(),
    val isAllergiesLoading: Boolean = false,
    val allergiesErrorMessage: String? = null,

    val showSaveConfirmation: Boolean = false,
    val isLoading: Boolean = false,
    val alertState: iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState = iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None
)
