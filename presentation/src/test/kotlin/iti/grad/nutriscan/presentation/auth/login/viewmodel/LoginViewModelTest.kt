package iti.grad.nutriscan.presentation.auth.login.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.domain.auth.usecase.GetOidcAuthConfigUseCase
import iti.grad.nutriscan.domain.auth.usecase.LoginWithEmailUseCase
import iti.grad.nutriscan.domain.auth.usecase.SaveGoogleLoginTokensUseCase
import iti.grad.nutriscan.presentation.auth.login.state.LoginEffect
import iti.grad.nutriscan.presentation.auth.login.state.LoginEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase
    private lateinit var getOidcAuthConfigUseCase: GetOidcAuthConfigUseCase
    private lateinit var saveGoogleLoginTokensUseCase: SaveGoogleLoginTokensUseCase
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        loginWithEmailUseCase = mockk()
        getOidcAuthConfigUseCase = mockk()
        saveGoogleLoginTokensUseCase = mockk()
        viewModel = LoginViewModel(
            loginWithEmailUseCase,
            getOidcAuthConfigUseCase,
            saveGoogleLoginTokensUseCase
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SignInClicked with valid credentials emits NavigateToHome on success`() = runTest {
        val email = "test@example.com"
        val password = "Password123"
        coEvery { loginWithEmailUseCase(email, password) } returns Result.success(Unit)

        viewModel.onEvent(LoginEvent.EmailChanged(email))
        viewModel.onEvent(LoginEvent.PasswordChanged(password))
        
        viewModel.effect.test {
            viewModel.onEvent(LoginEvent.SignInClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SignInClicked with valid credentials emits ShowErrorDialog on failure`() = runTest {
        val email = "test@example.com"
        val password = "Password123"
        val errorMessage = "Invalid credentials"
        coEvery { loginWithEmailUseCase(email, password) } returns Result.failure(Exception(errorMessage))

        viewModel.onEvent(LoginEvent.EmailChanged(email))
        viewModel.onEvent(LoginEvent.PasswordChanged(password))
        
        viewModel.effect.test {
            viewModel.onEvent(LoginEvent.SignInClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.ShowErrorDialog)
            assertEquals(errorMessage, (effect as LoginEffect.ShowErrorDialog).messageStr)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
