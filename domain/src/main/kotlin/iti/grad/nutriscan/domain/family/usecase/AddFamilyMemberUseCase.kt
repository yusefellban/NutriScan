package iti.grad.nutriscan.domain.family.usecase

import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import javax.inject.Inject

class AddFamilyMemberUseCase @Inject constructor(
    private val familyMemberRepository: IFamilyMemberRepository
) {
    suspend operator fun invoke(
        name: String,
        allergyIds: List<Int>,
        diseaseIds: List<Int>,
    ): Result<Unit> = familyMemberRepository.addFamilyMember(
        FamilyMemberInput(name = name, allergyIds = allergyIds, diseaseIds = diseaseIds)
    )
}
