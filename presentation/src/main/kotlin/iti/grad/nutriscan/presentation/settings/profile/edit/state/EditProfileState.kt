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
    /** Cache-busting token for the avatar image — see [iti.grad.nutriscan.presentation.common.components.rememberAvatarImageRequest]. */
    val avatarUpdatedAt: String? = null,
    val avatarUploadState: AvatarUploadState = AvatarUploadState.Idle,
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

/** Independent state for the avatar upload flow — decoupled from [EditProfileState.isSaving]
 * so a slow/failed picture upload never blocks saving the rest of the profile fields. */
@Immutable
sealed interface AvatarUploadState {
    data object Idle : AvatarUploadState
    data object Uploading : AvatarUploadState
    data object Error : AvatarUploadState
}
