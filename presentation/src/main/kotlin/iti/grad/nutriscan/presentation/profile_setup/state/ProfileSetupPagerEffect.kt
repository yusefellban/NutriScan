package iti.grad.nutriscan.presentation.profile_setup.state

import androidx.annotation.StringRes

sealed interface ProfileSetupPagerEffect {
    data class ScrollToPage(val page: Int) : ProfileSetupPagerEffect
    data object NavigateBack : ProfileSetupPagerEffect
    data object NavigateToHome : ProfileSetupPagerEffect
    data class ShowSnackbar(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ProfileSetupPagerEffect
}
