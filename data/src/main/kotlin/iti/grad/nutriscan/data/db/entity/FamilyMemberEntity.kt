package iti.grad.nutriscan.data.db.entity

import kotlinx.serialization.Serializable

/**
 * Embedded family member row stored as serialized JSON inside UserEntity.
 */
@Serializable
data class FamilyMemberEntity(
    val id: String,
    val name: String,
    val relation: String = "",
    val imageUrl: String? = null,
    val allergyIds: List<Int> = emptyList(),
    val diseaseIds: List<Int> = emptyList(),
)
