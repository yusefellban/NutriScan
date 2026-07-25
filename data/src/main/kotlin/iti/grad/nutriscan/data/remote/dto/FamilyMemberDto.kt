package iti.grad.nutriscan.data.remote.dto

import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FamilyMemberDto(
    val id: String? = null,
    val name: String,
    val relation: String? = null,
    @SerialName("allergyIds") val allergyIds: List<Int> = emptyList(),
    @SerialName("diseaseIds") val diseaseIds: List<Int> = emptyList(),
    val allergies: List<AllergyDto> = emptyList(),
    val diseases: List<DiseaseDto> = emptyList(),
)

fun FamilyMemberDto.toEntity(ownerUserId: String): FamilyMemberEntity {
    return FamilyMemberEntity(
        id = id ?: UUID.randomUUID().toString(),
        ownerUserId = ownerUserId,
        name = name,
        relation = relation ?: "",
        allergyIds = if (allergyIds.isNotEmpty()) allergyIds else allergies.map { it.id },
        diseaseIds = if (diseaseIds.isNotEmpty()) diseaseIds else diseases.map { it.id },
    )
}
