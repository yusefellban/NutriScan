package iti.grad.nutriscan.presentation.exercises.model

import androidx.annotation.StringRes

data class ExerciseCategory(
    val id: String,
    @StringRes val labelRes: Int
)
