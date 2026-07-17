package iti.grad.nutriscan.presentation.profile_setup.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class Gender {
    FEMALE, MALE
}

@Immutable
data class ProfileSetupPagerState(
    // Pager navigation
    val currentPage: Int = 0,
    val pageCount: Int = 5,

    // Page 1: Gender
    val selectedGender: Gender? = null,

    // Page 2: Date of Birth
    val selectedDateOfBirthMillis: Long? = null,

    // Page 3: Height
    val selectedHeightCm: Int = 170,

    // Health Profile fields (migrated from HealthProfileSetupState)
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
