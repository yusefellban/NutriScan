package iti.grad.nutriscan.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Body for PATCH /v1/users/profile.
 * All fields are optional so callers can send a partial update —
 * e.g. the health profile setup screen only sets [diseaseIds].
 */
@Serializable
data class UpdateUserProfileRequestDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val diseaseIds: List<Int>? = null,
    val allergyIds: List<Int>? = null,
    val avatarUrl: String? = null,
    /** Full replace of the family members list — see family-members plan §2. */
    val familyMembers: List<FamilyMemberDto>? = null
)
