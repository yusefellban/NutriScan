package iti.grad.nutriscan.presentation.auth.profile_setup.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HealthProfileSetupState(
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
    val isLoading: Boolean = false
)
