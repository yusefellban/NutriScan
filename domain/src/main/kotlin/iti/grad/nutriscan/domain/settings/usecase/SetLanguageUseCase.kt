package iti.grad.nutriscan.domain.settings.usecase

import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import javax.inject.Inject

class SetLanguageUseCase @Inject constructor(
    private val languageRepository: ILanguageRepository
) {
    suspend operator fun invoke(language: AppLanguage) {
        languageRepository.setLanguage(language)
    }
}
