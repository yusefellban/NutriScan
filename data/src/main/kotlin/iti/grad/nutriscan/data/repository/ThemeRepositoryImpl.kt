package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.IThemePreferencesDataSource
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val themeDataSource: IThemePreferencesDataSource
) : IThemeRepository {

    override suspend fun getThemeMode(): ThemeMode {
        return runCatching { ThemeMode.valueOf(themeDataSource.getThemeMode()) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeDataSource.setThemeMode(mode.name)
    }
}
