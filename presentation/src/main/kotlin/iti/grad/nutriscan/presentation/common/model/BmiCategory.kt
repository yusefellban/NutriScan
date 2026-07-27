package iti.grad.nutriscan.presentation.common.model

import androidx.compose.runtime.Composable
import iti.grad.nutriscan.presentation.common.theme.AppTheme
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

@Composable
fun BmiCategory.pillColor() = when (this) {
    BmiCategory.UNDERWEIGHT -> AppTheme.colors.Teal200
    BmiCategory.NORMAL -> AppTheme.colors.VerdictGreen.copy(alpha = 0.15f)
    BmiCategory.OVERWEIGHT -> AppTheme.colors.VerdictYellow.copy(alpha = 0.20f)
    BmiCategory.OBESE -> AppTheme.colors.VerdictRed.copy(alpha = 0.15f)
}

@Composable
fun BmiCategory.textColor() = when (this) {
    BmiCategory.UNDERWEIGHT -> AppTheme.colors.Teal1300
    BmiCategory.NORMAL -> AppTheme.colors.VerdictGreen
    BmiCategory.OVERWEIGHT -> AppTheme.colors.VerdictYellow
    BmiCategory.OBESE -> AppTheme.colors.VerdictRed
}
