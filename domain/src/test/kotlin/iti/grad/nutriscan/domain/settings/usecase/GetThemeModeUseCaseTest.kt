package iti.grad.nutriscan.domain.settings.usecase

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GetThemeModeUseCaseTest {

    private val themeRepository: IThemeRepository = mockk()
    private val useCase = GetThemeModeUseCase(themeRepository)

    @Test
    fun `invoke returns the theme mode from the repository`() = runTest {
        coEvery { themeRepository.getThemeMode() } returns ThemeMode.DARK

        val result = useCase()

        Assertions.assertEquals(ThemeMode.DARK, result)
    }
}
