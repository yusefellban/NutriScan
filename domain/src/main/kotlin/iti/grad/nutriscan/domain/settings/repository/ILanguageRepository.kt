package iti.grad.nutriscan.domain.settings.repository

import iti.grad.nutriscan.domain.settings.model.AppLanguage

interface ILanguageRepository {
    suspend fun getLanguage(): AppLanguage
    suspend fun setLanguage(language: AppLanguage)
}
