package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    operator fun invoke(): Flow<User?> {
        return userRepository.getUserData()
    }
}
