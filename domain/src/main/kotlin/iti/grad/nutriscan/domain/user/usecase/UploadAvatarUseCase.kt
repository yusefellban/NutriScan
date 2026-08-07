package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import java.io.File
import javax.inject.Inject

/**
 * Uploads a new profile picture. Compression and the actual multipart
 * request happen in the data layer; this use case only forwards the call.
 */
class UploadAvatarUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(imageFile: File): Result<Unit> =
        userRepository.uploadAvatar(imageFile)
}
