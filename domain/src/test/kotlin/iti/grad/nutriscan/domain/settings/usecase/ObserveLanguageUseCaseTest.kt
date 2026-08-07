package iti.grad.nutriscan.domain.settings.usecase

import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ObserveLanguageUseCaseTest {

    private val languageRepository: ILanguageRepository = mockk()
    private val useCase = ObserveLanguageUseCase(languageRepository)

    @Test
    fun `invoke emits the language from the repository`() = runTest {
        every { languageRepository.observeLanguage() } returns flowOf(AppLanguage.AR)

        val result = useCase().first()

        Assertions.assertEquals(AppLanguage.AR, result)
    }
}
