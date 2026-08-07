package iti.grad.nutriscan.domain.settings.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetLanguageUseCaseTest {

    private val languageRepository: ILanguageRepository = mockk()
    private val useCase = SetLanguageUseCase(languageRepository)

    @Test
    fun `invoke persists the given language via the repository`() = runTest {
        coEvery { languageRepository.setLanguage(AppLanguage.AR) } returns Unit

        useCase(AppLanguage.AR)

        coVerify { languageRepository.setLanguage(AppLanguage.AR) }
    }
}
