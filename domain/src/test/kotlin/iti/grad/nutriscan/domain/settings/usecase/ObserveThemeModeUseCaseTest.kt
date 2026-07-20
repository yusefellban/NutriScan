package iti.grad.nutriscan.domain.settings.usecase

import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ObserveThemeModeUseCaseTest {

    private val themeRepository: IThemeRepository = mockk()
    private val useCase = ObserveThemeModeUseCase(themeRepository)

    @Test
    fun `invoke emits the theme mode from the repository`() = runTest {
        every { themeRepository.observeThemeMode() } returns flowOf(ThemeMode.DARK)

        val result = useCase().first()

        Assertions.assertEquals(ThemeMode.DARK, result)
    }
}
