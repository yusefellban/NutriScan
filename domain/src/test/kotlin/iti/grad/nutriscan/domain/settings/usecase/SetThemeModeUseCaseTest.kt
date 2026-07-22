package iti.grad.nutriscan.domain.settings.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetThemeModeUseCaseTest {

    private val themeRepository: IThemeRepository = mockk()
    private val useCase = SetThemeModeUseCase(themeRepository)

    @Test
    fun `invoke persists the given theme mode via the repository`() = runTest {
        coEvery { themeRepository.setThemeMode(ThemeMode.LIGHT) } returns Unit

        useCase(ThemeMode.LIGHT)

        coVerify { themeRepository.setThemeMode(ThemeMode.LIGHT) }
    }
}
