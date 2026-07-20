package iti.grad.nutriscan.domain.settings.usecase

import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLanguageUseCase @Inject constructor(
    private val languageRepository: ILanguageRepository
) {
    operator fun invoke(): Flow<AppLanguage> {
        return languageRepository.observeLanguage()
    }
}
