package iti.grad.nutriscan.presentation.settings.profile.edit.state

import android.net.Uri

sealed interface EditProfileEvent {
    object EditClicked : EditProfileEvent
    data class UpdateFirstName(val firstName: String) : EditProfileEvent
    data class UpdateLastName(val lastName: String) : EditProfileEvent
    data class UpdateDateOfBirth(val dateOfBirth: String) : EditProfileEvent
    data class UpdateHeight(val heightCm: Double?) : EditProfileEvent
    data class UpdateWeight(val weightKg: Double?) : EditProfileEvent
    data class ToggleDisease(val diseaseId: Int) : EditProfileEvent
    data class ToggleAllergy(val allergyId: Int) : EditProfileEvent
    object RetryLoadDiseases : EditProfileEvent
    object RetryLoadAllergies : EditProfileEvent
    object SaveClicked : EditProfileEvent
    object ConfirmSave : EditProfileEvent
    object DismissSaveConfirmation : EditProfileEvent
    object BackClicked : EditProfileEvent
    /** User picked a new photo from the picker — the raw content [uri] is uploaded immediately. */
    data class SelectAvatar(val uri: Uri) : EditProfileEvent
    object RetryAvatarUpload : EditProfileEvent
    object DismissAvatarUploadError : EditProfileEvent
    object DismissAlert : EditProfileEvent
    object RetryAction : EditProfileEvent
}
