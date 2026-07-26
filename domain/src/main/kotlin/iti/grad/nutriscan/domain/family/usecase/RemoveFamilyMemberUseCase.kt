package iti.grad.nutriscan.domain.family.usecase

import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import javax.inject.Inject

class RemoveFamilyMemberUseCase @Inject constructor(
    private val familyMemberRepository: IFamilyMemberRepository
) {
    suspend operator fun invoke(memberId: String): Result<Unit> =
        familyMemberRepository.removeFamilyMember(memberId)
}
