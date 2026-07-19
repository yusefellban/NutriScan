package iti.grad.nutriscan.data.local.datasource

interface ILanguagePreferencesDataSource {
    suspend fun getLanguage(): String
    suspend fun setLanguage(language: String)
}
