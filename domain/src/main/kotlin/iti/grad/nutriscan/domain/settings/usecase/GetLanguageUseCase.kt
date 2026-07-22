package iti.grad.nutriscan.domain.settings.usecase

import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import javax.inject.Inject

class GetLanguageUseCase @Inject constructor(
    private val languageRepository: ILanguageRepository
) {
    suspend operator fun invoke(): AppLanguage {
        return languageRepository.getLanguage()
    }
}
