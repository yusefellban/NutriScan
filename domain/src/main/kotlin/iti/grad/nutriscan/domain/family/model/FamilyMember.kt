package iti.grad.nutriscan.domain.family.model

/**
 * A family member linked to the current user's profile, backed by Room and
 * synced to the backend as part of the user's profile (see
 * [iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository]).
 */
data class FamilyMember(
    val id: String,
    val name: String,
    val relation: String = "",
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
