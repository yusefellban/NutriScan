package iti.grad.nutriscan.domain.user.model

/**
 * Returned by the backend after a successful DELETE /v1/users/profile request.
 *
 * The account is NOT removed immediately — it enters a grace period of
 * [gracePeriodDays] days. If the user logs in and calls POST /v1/users/profile/restore
 * before [scheduledDeletionAt], the account is fully restored.
 */
data class AccountDeletionInfo(
    /** ISO-8601 date string (e.g. "2026-08-22") when the account will be permanently removed. */
    val scheduledDeletionAt: String,
    /** Number of days the user has to restore the account before permanent deletion. */
    val gracePeriodDays: Int,
)
