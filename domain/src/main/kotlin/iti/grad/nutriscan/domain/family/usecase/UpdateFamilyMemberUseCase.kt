package iti.grad.nutriscan.domain.family.usecase

import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import javax.inject.Inject

class UpdateFamilyMemberUseCase @Inject constructor(
    private val familyMemberRepository: IFamilyMemberRepository
) {
    suspend operator fun invoke(
        memberId: String,
        name: String,
        relation: String,
        allergyIds: List<Int>,
        diseaseIds: List<Int>,
    ): Result<Unit> = familyMemberRepository.updateFamilyMember(
        memberId = memberId,
        input = FamilyMemberInput(name = name, relation = relation, allergyIds = allergyIds, diseaseIds = diseaseIds)
    )
}
