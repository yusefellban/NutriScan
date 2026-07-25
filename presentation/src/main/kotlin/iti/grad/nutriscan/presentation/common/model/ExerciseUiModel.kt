package iti.grad.nutriscan.presentation.common.model

import androidx.annotation.StringRes

data class ExerciseUiModel(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val equipmentRes: Int,
    @StringRes val targetRes: Int,
    @StringRes val instructionsRes: Int,
    val type: ExerciseType,
    val exerciseCount: Int = 0,
    val durationMin: Int = 0,
    val kcalPerMin: Double? = null,
    val kcalPerRep: Double? = null,
)
