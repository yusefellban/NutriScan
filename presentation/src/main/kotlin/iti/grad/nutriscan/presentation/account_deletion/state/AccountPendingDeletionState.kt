package iti.grad.nutriscan.presentation.account_deletion.state

import androidx.compose.runtime.Immutable

@Immutable
data class AccountPendingDeletionState(
    /** Raw ISO-8601 date string from backend, e.g. "2026-08-22". */
    val scheduledDeletionAt: String = "",
    /** Human-readable formatted date, e.g. "22 Aug 2026". */
    val formattedDeletionDate: String = "",
    /** Number of full days remaining until deletion. */
    val daysRemaining: Int = 0,
    val isRestoring: Boolean = false,
    val error: String? = null,
)
