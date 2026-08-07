package iti.grad.nutriscan.domain.family.usecase

import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import java.io.File
import javax.inject.Inject

class UploadFamilyMemberImageUseCase @Inject constructor(
    private val familyMemberRepository: IFamilyMemberRepository
) {
    suspend operator fun invoke(memberId: String, imageFile: File): Result<Unit> =
        familyMemberRepository.uploadFamilyMemberImage(memberId, imageFile)
}
