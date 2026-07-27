package iti.grad.nutriscan.domain.family.model

/**
 * Payload for creating a new family member — no [FamilyMember.id], since the
 * id is either generated locally (optimistic insert) or assigned by the
 * backend on sync.
 */
data class FamilyMemberInput(
    val name: String,
    val relation: String = "",
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
