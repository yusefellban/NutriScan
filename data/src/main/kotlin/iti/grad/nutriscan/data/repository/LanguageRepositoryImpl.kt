package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.ILanguagePreferencesDataSource
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import javax.inject.Inject

class LanguageRepositoryImpl @Inject constructor(
    private val languageDataSource: ILanguagePreferencesDataSource
) : ILanguageRepository {

    override suspend fun getLanguage(): AppLanguage {
        return runCatching { AppLanguage.valueOf(languageDataSource.getLanguage()) }
            .getOrDefault(AppLanguage.EN)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        languageDataSource.setLanguage(language.name)
    }
}
