package iti.grad.nutriscan.domain.settings.repository

import iti.grad.nutriscan.domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface IThemeRepository {
    suspend fun getThemeMode(): ThemeMode
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeThemeMode(): Flow<ThemeMode>
}
