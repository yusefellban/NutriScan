package iti.grad.nutriscan.presentation.account_deletion.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.auth.usecase.LogoutUseCase
import iti.grad.nutriscan.domain.user.usecase.RestoreAccountUseCase
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionEffect
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionEvent
import iti.grad.nutriscan.presentation.account_deletion.state.AccountPendingDeletionState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class AccountPendingDeletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val restoreAccountUseCase: RestoreAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val scheduledDeletionAt: String =
        checkNotNull(savedStateHandle["scheduledDeletionAt"]) {
            "AccountPendingDeletionViewModel requires a scheduledDeletionAt nav argument"
        }

    private val _state = MutableStateFlow(AccountPendingDeletionState())
    val state: StateFlow<AccountPendingDeletionState> = _state.asStateFlow()

    private val _effect = Channel<AccountPendingDeletionEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        initState()
    }

    private fun initState() {
        val (daysRemaining, formatted) = computeRemainingDays(scheduledDeletionAt)
        _state.update {
            it.copy(
                scheduledDeletionAt = scheduledDeletionAt,
                daysRemaining = daysRemaining,
                formattedDeletionDate = formatted,
            )
        }
    }

    fun onEvent(event: AccountPendingDeletionEvent) {
        when (event) {
            is AccountPendingDeletionEvent.RestoreAccountClicked -> restoreAccount()
            is AccountPendingDeletionEvent.LogoutClicked -> logout()
            is AccountPendingDeletionEvent.ErrorDismissed -> _state.update { it.copy(error = null) }
        }
    }

    private fun restoreAccount() {
        _state.update { it.copy(isRestoring = true, error = null) }
        viewModelScope.launch {
            restoreAccountUseCase()
                .onSuccess {
                    _effect.send(AccountPendingDeletionEffect.NavigateToHome)
                }
                .onFailure { error ->
                    Timber.e(error, "restoreAccount failed")
                    _state.update {
                        it.copy(
                            isRestoring = false,
                            error = error.message ?: "Failed to restore account. Please try again.",
                        )
                    }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _effect.send(AccountPendingDeletionEffect.NavigateToLogin)
        }
    }

    /**
     * Computes the number of days remaining until [scheduledDeletionAt] and formats the date.
     * Falls back gracefully if the date string cannot be parsed.
     *
     * @return Pair of (daysRemaining, formattedDate)
     */
    private fun computeRemainingDays(isoDate: String): Pair<Int, String> {
        return try {
            val target = LocalDate.parse(isoDate)
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, target).coerceAtLeast(0).toInt()
            val formatted = target.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            Pair(days, formatted)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse scheduledDeletionAt: $isoDate")
            Pair(0, isoDate)
        }
    }
}
