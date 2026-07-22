package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.ILanguagePreferencesDataSource
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override fun observeLanguage(): Flow<AppLanguage> {
        return languageDataSource.observeLanguage().map { rawLanguage ->
            runCatching { AppLanguage.valueOf(rawLanguage) }.getOrDefault(AppLanguage.EN)
        }
    }
}
