package iti.grad.nutriscan.data.remote.dto

import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FamilyMemberDto(
    val id: String? = null,
    val name: String,
    @SerialName("allergy_ids") val allergyIds: List<Int> = emptyList(),
    @SerialName("disease_ids") val diseaseIds: List<Int> = emptyList(),
)

fun FamilyMemberDto.toEntity(ownerUserId: String): FamilyMemberEntity {
    return FamilyMemberEntity(
        id = id ?: UUID.randomUUID().toString(),
        ownerUserId = ownerUserId,
        name = name,
        allergyIds = allergyIds,
        diseaseIds = diseaseIds,
    )
}
