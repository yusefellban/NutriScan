package iti.grad.nutriscan.domain.settings.usecase

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GetLanguageUseCaseTest {

    private val languageRepository: ILanguageRepository = mockk()
    private val useCase = GetLanguageUseCase(languageRepository)

    @Test
    fun `invoke returns the language from the repository`() = runTest {
        coEvery { languageRepository.getLanguage() } returns AppLanguage.AR

        val result = useCase()

        Assertions.assertEquals(AppLanguage.AR, result)
    }
}
