package iti.grad.nutriscan.domain.settings.usecase

import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val themeRepository: IThemeRepository
) {
    suspend operator fun invoke(): ThemeMode {
        return themeRepository.getThemeMode()
    }
}
