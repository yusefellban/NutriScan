package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for DELETE /v1/users/profile.
 * The account is NOT removed immediately — it is scheduled for the date in [scheduledDeletionAt].
 */
@Serializable
data class AccountDeletionResponseDto(
    @SerialName("scheduledDeletionAt") val scheduledDeletionAt: String,
    @SerialName("gracePeriodDays") val gracePeriodDays: Int,
)

/**
 * Response body for POST /v1/users/profile/restore.
 */
@Serializable
data class RestoreAccountResponseDto(
    @SerialName("message") val message: String = "",
    @SerialName("restoredAt") val restoredAt: String = "",
)
