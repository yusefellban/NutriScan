package iti.grad.nutriscan.domain.settings.usecase

import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val themeRepository: IThemeRepository
) {
    operator fun invoke(): Flow<ThemeMode> {
        return themeRepository.observeThemeMode()
    }
}
