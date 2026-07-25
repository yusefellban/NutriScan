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
            nameRes = R.string.exercise_warm_up_name,
            equipmentRes = R.string.exercise_warm_up_equipment,
            targetRes = R.string.exercise_warm_up_target,
            instructionsRes = R.string.exercise_warm_up_instructions,
            type = ExerciseType.CARDIO,
            exerciseCount = 20,
            durationMin = 22,
            kcalPerMin = 0.23
        ),
        ExerciseUiModel(
            id = "2",
            nameRes = R.string.exercise_strength_name,
            equipmentRes = R.string.exercise_strength_equipment,
            targetRes = R.string.exercise_strength_target,
            instructionsRes = R.string.exercise_strength_instructions,
            type = ExerciseType.NORMAL_WORKOUT,
            exerciseCount = 12,
            durationMin = 14,
            kcalPerRep = 0.3
        ),
        ExerciseUiModel(
            id = "3",
            nameRes = R.string.exercise_side_plank_name,
            equipmentRes = R.string.exercise_side_plank_equipment,
            targetRes = R.string.exercise_side_plank_target,
            instructionsRes = R.string.exercise_side_plank_instructions,
            type = ExerciseType.CARDIO,
            exerciseCount = 15,
            durationMin = 18,
            kcalPerMin = 0.15
        ),
        ExerciseUiModel(
            id = "4",
            nameRes = R.string.exercise_abs_workout_name,
            equipmentRes = R.string.exercise_abs_workout_equipment,
            targetRes = R.string.exercise_abs_workout_target,
            instructionsRes = R.string.exercise_abs_workout_instructions,
            type = ExerciseType.NORMAL_WORKOUT,
            exerciseCount = 10,
            durationMin = 12,
            kcalPerRep = 0.25
        ),
        ExerciseUiModel(
            id = "5",
            nameRes = R.string.exercise_torso_trap_name,
            equipmentRes = R.string.exercise_torso_trap_equipment,
            targetRes = R.string.exercise_torso_trap_target,
            instructionsRes = R.string.exercise_torso_trap_instructions,
            type = ExerciseType.CARDIO,
            exerciseCount = 8,
            durationMin = 10,
            kcalPerMin = 0.20
        ),
        ExerciseUiModel(
            id = "6",
            nameRes = R.string.exercise_lower_back_name,
            equipmentRes = R.string.exercise_lower_back_equipment,
            targetRes = R.string.exercise_lower_back_target,
            instructionsRes = R.string.exercise_lower_back_instructions,
            type = ExerciseType.NORMAL_WORKOUT,
            exerciseCount = 6,
            durationMin = 8,
            kcalPerRep = 0.22
        )
    )
}
