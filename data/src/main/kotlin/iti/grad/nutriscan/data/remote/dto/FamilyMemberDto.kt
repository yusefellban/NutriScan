package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape for a family member inside [UserDto.familyMembers] /
 * [UpdateUserProfileRequestDto.familyMembers].
 *
 * NOTE: per the family-members implementation plan, the Postman collection's
 * `familyMembers` bodies use camelCase keys (`allergyIds`/`diseaseIds`)
 * while the rest of the user payload uses snake_case via `@SerialName` —
 * this is the backend's existing inconsistency, not a bug here. [id] is
 * null when sending a newly-created (not yet synced) member.
 */
@Serializable
data class FamilyMemberDto(
    val id: String? = null,
    val name: String,
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
