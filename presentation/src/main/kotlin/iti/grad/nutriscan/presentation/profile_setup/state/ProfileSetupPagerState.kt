package iti.grad.nutriscan.presentation.profile_setup.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.disease.model.Disease
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

    // Page 4: Weight
    val selectedWeightKg: Int = 60,

    // Health Profile fields (migrated from HealthProfileSetupState)
    // Diseases are fetched from the backend (GET /v1/diseases)
    val diseases: ImmutableList<Disease> = persistentListOf(),
    val selectedDiseaseIds: ImmutableList<Int> = persistentListOf(),
    val isDiseasesLoading: Boolean = false,
    val diseasesErrorMessage: String? = null,

    // Allergies are fetched from the backend (GET /v1/allergies)
    val allergies: ImmutableList<Allergy> = persistentListOf(),
    val selectedAllergyIds: ImmutableList<Int> = persistentListOf(),
    val isAllergiesLoading: Boolean = false,
    val allergiesErrorMessage: String? = null,

    val isLoading: Boolean = false
)
