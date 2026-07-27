package iti.grad.nutriscan.domain.family.usecase

import iti.grad.nutriscan.domain.family.model.FamilyMember
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFamilyMembersUseCase @Inject constructor(
    private val familyMemberRepository: IFamilyMemberRepository
) {
    operator fun invoke(): Flow<List<FamilyMember>> {
        return familyMemberRepository.getFamilyMembers()
    }
}
