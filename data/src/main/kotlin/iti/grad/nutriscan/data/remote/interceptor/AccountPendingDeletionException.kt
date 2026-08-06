package iti.grad.nutriscan.data.remote.interceptor

/**
 * Thrown when GET /v1/users/profile returns HTTP 409 with error code
 * "ACCOUNT_PENDING_DELETION".
 *
 * This is a domain-level signal, not a generic network failure.
 * It MUST be propagated to the presentation layer so the user is
 * redirected to the AccountPendingDeletion screen.
 *
 * SAFETY RULE: Never swallow this exception. A suppressed
 * AccountPendingDeletionException could silently show a "Green" verdict
 * or allow normal app usage for an account that is about to be erased.
 *
 * Placed in the interceptor package alongside sibling HTTP exceptions
 * (see [NutriScanHttpException]) so the sealed hierarchy stays together.
 */
class AccountPendingDeletionException(
    /** ISO-8601 date string extracted from the backend error message, e.g. "2026-08-22". */
    val scheduledDeletionAt: String,
) : NutriScanHttpException("409 — account is pending deletion on $scheduledDeletionAt")
