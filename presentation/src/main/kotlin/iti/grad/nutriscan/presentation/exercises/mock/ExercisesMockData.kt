package iti.grad.nutriscan.presentation.exercises.mock

import iti.grad.nutriscan.presentation.common.model.ExerciseType
import iti.grad.nutriscan.presentation.common.model.ExerciseUiModel
import iti.grad.nutriscan.presentation.exercises.model.ExerciseCategory
import iti.grad.presentation.R
import kotlinx.collections.immutable.persistentListOf

object ExercisesMockData {
    val categories = persistentListOf(
        ExerciseCategory("all", R.string.exercises_category_all),
        ExerciseCategory("warm_up", R.string.exercises_category_warm_up),
        ExerciseCategory("yoga", R.string.exercises_category_yoga),
        ExerciseCategory("biceps", R.string.exercises_category_biceps),
        ExerciseCategory("chest", R.string.exercises_category_chest),
        ExerciseCategory("back", R.string.exercises_category_back),
        ExerciseCategory("legs", R.string.exercises_category_legs)
    )

    val exercises = persistentListOf(
        ExerciseUiModel(
            id = "1",
            name = "Warm Up",
            equipment = "None",
            target = "Full Body",
            instructions = "Follow the instructions.",
            type = ExerciseType.CARDIO,
            imageUrl = null,
            kcalPerMin = 0.23,
            kcalPerRep = null
        ),
        ExerciseUiModel(
            id = "2",
            name = "Strength Training",
            equipment = "Dumbbells",
            target = "Arms",
            instructions = "Follow the instructions.",
            type = ExerciseType.NORMAL_WORKOUT,
            imageUrl = null,
            kcalPerMin = null,
            kcalPerRep = 0.3
        ),
        ExerciseUiModel(
            id = "3",
            name = "Side Plank",
            equipment = "Mat",
            target = "Core",
            instructions = "Follow the instructions.",
            type = ExerciseType.CARDIO,
            imageUrl = null,
            kcalPerMin = 0.15,
            kcalPerRep = null
        ),
        ExerciseUiModel(
            id = "4",
            name = "Abs Workout",
            equipment = "None",
            target = "Core",
            instructions = "Follow the instructions.",
            type = ExerciseType.NORMAL_WORKOUT,
            imageUrl = null,
            kcalPerMin = null,
            kcalPerRep = 0.25
        ),
        ExerciseUiModel(
            id = "5",
            name = "Torso Trap",
            equipment = "Machine",
            target = "Back",
            instructions = "Follow the instructions.",
            type = ExerciseType.CARDIO,
            imageUrl = null,
            kcalPerMin = 0.20,
            kcalPerRep = null
        ),
        ExerciseUiModel(
            id = "6",
            name = "Lower Back",
            equipment = "Mat",
            target = "Back",
            instructions = "Follow the instructions.",
            type = ExerciseType.NORMAL_WORKOUT,
            imageUrl = null,
            kcalPerMin = null,
            kcalPerRep = 0.22
        )
    )
}
