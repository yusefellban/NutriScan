package iti.grad.nutriscan.presentation.common.model

import iti.grad.presentation.R

enum class BmiCategory(val labelRes: Int) {
    UNDERWEIGHT(R.string.user_profile_bmi_underweight),
    NORMAL(R.string.user_profile_bmi_normal),
    OVERWEIGHT(R.string.user_profile_bmi_overweight),
    OBESE(R.string.user_profile_bmi_obese),
}

fun bmiCategory(bmi: Double): BmiCategory = when {
    bmi < 18.5 -> BmiCategory.UNDERWEIGHT
    bmi < 25.0 -> BmiCategory.NORMAL
    bmi < 30.0 -> BmiCategory.OVERWEIGHT
    else -> BmiCategory.OBESE
}
