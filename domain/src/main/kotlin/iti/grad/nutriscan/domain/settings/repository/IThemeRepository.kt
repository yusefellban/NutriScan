package iti.grad.nutriscan.domain.settings.repository

import iti.grad.nutriscan.domain.settings.model.ThemeMode

interface IThemeRepository {
    suspend fun getThemeMode(): ThemeMode
    suspend fun setThemeMode(mode: ThemeMode)
}
