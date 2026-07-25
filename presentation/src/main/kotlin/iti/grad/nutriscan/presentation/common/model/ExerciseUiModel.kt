package iti.grad.nutriscan.presentation.common.model

import androidx.annotation.StringRes

data class ExerciseUiModel(
    val id: String,
    val name: String,
    val equipment: String,
    val target: String,
    val instructions: String,
    val type: ExerciseType,
    val imageUrl: String?,
    val gifUrl: String? = null,
    val kcalPerMin: Double?,
    val kcalPerRep: Double?,
)
