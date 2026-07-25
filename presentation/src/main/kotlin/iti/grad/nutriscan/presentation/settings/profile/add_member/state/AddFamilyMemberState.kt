package iti.grad.nutriscan.presentation.settings.profile.add_member.state

import androidx.annotation.StringRes
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.disease.model.Disease
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Local, transient form state for the Add Family Member bottom sheet — kept
 * separate from [iti.grad.nutriscan.presentation.settings.profile.state.UserProfileState]
 * so it doesn't pollute the profile screen's state with form-editing
 * concerns (same separation used between EditProfileViewModel and
 * UserProfileViewModel).
 */
data class AddFamilyMemberState(
    val name: String = "",
    @StringRes val nameError: Int? = null,
    val relation: String = "",
    @StringRes val relationError: Int? = null,
    val diseases: ImmutableList<Disease> = persistentListOf(),
    val selectedDiseaseIds: ImmutableList<Int> = persistentListOf(),
    val isDiseasesLoading: Boolean = false,
    val diseasesErrorMessage: String? = null,
    val allergies: ImmutableList<Allergy> = persistentListOf(),
    val selectedAllergyIds: ImmutableList<Int> = persistentListOf(),
    val isAllergiesLoading: Boolean = false,
    val allergiesErrorMessage: String? = null,
    val isSaving: Boolean = false,
)
