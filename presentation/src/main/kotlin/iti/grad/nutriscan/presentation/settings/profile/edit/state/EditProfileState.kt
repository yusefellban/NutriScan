package iti.grad.nutriscan.presentation.settings.profile.edit.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState.None
import iti.grad.nutriscan.presentation.settings.profile.state.ProfileAlertState
import iti.grad.nutriscan.domain.disease.model.Disease
import androidx.compose.runtime.Immutable

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
    val diseases: ImmutableList<Disease> = persistentListOf(),
    val selectedDiseaseIds: ImmutableList<Int> = persistentListOf(),
    val isDiseasesLoading: Boolean = false,
    val diseasesErrorMessage: String? = null,
    
    val allergies: ImmutableList<Allergy> = persistentListOf(),
    val selectedAllergyIds: ImmutableList<Int> = persistentListOf(),
    val isAllergiesLoading: Boolean = false,
    val allergiesErrorMessage: String? = null,

    val showSaveConfirmation: Boolean = false,
    val isLoading: Boolean = false,
    val alertState: ProfileAlertState = None
)
