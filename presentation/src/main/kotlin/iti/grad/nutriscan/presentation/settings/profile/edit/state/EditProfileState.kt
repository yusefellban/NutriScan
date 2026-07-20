package iti.grad.nutriscan.presentation.settings.profile.edit.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class EditProfileState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val avatarUrl: String? = null,
    val chronicConditions: ImmutableList<String> = persistentListOf(
        "Diabetes",
        "Hypertension",
        "Celiac Disease"
    ),
    val selectedChronicConditions: ImmutableList<String> = persistentListOf(),
    val allergies: ImmutableList<String> = persistentListOf(
        "Peanuts",
        "Gluten",
        "Dairy"
    ),
    val selectedAllergies: ImmutableList<String> = persistentListOf(),
    val isAddingCustomCondition: Boolean = false,
    val isAddingCustomAllergy: Boolean = false,
    val customConditionInput: String = "",
    val customAllergyInput: String = "",
    val showSaveConfirmation: Boolean = false,
    val isLoading: Boolean = false
)
