package iti.grad.nutriscan.presentation.exercises.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel
import iti.grad.nutriscan.presentation.exercises.model.ExerciseCategoryUi
import iti.grad.nutriscan.presentation.exercises.model.ExerciseCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ExercisesState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val categories: ImmutableList<ExerciseCategoryUi> = persistentListOf(),
    val selectedCategoryId: String = "all",
    val exercises: ImmutableList<ExerciseUiModel> = persistentListOf(),
    val visibleExercises: ImmutableList<ExerciseUiModel> = persistentListOf(),
    val selectedExercise: ExerciseUiModel? = null,
    val isInstructionsExpanded: Boolean = false,
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessageRes: Int? = null
)
