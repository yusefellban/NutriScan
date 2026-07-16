package iti.grad.nutriscan.presentation.auth.profile_setup.state

import androidx.annotation.StringRes

sealed interface HealthProfileSetupEffect {
    data object NavigateToHome : HealthProfileSetupEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : HealthProfileSetupEffect
}
