package iti.grad.nutriscan.data.local.datasource

import kotlinx.coroutines.flow.Flow

interface ILanguagePreferencesDataSource {
    suspend fun getLanguage(): String
    suspend fun setLanguage(language: String)
    fun observeLanguage(): Flow<String>
}
