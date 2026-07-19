package iti.grad.nutriscan.data.local.datasource

interface IThemePreferencesDataSource {
    suspend fun getThemeMode(): String
    suspend fun setThemeMode(mode: String)
}
