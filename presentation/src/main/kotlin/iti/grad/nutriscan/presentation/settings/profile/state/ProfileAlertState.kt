package iti.grad.nutriscan.presentation.settings.profile.state

import androidx.annotation.StringRes

/**
 * Shared UI state representing all alert variations in the profile flows.
 * Enables the ViewModel to control exactly which Custom Alert is shown
 * and what message it contains.
 */
sealed interface ProfileAlertState {
    data object None : ProfileAlertState
    
    data object InternetError : ProfileAlertState
    
    data class Error(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ProfileAlertState
    
    data class Warning(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ProfileAlertState
    
    data class Success(
        @StringRes val messageResId: Int? = null,
        val messageStr: String? = null
    ) : ProfileAlertState
}
