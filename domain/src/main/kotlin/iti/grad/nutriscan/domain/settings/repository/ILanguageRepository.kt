package iti.grad.nutriscan.domain.settings.repository

import iti.grad.nutriscan.domain.settings.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface ILanguageRepository {
    suspend fun getLanguage(): AppLanguage
    suspend fun setLanguage(language: AppLanguage)
    fun observeLanguage(): Flow<AppLanguage>
}
