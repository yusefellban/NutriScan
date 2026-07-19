package iti.grad.nutriscan.data.local.datasource

import kotlinx.coroutines.flow.Flow

interface IThemePreferencesDataSource {
    suspend fun getThemeMode(): String
    suspend fun setThemeMode(mode: String)
    fun observeThemeMode(): Flow<String>
}
