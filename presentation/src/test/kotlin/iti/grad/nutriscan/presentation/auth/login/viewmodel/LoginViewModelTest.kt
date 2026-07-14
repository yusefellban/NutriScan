package iti.grad.nutriscan.presentation.auth.login.viewmodel

import app.cash.turbine.test
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

    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when SignUpClicked, effect is NavigateToRegister`() = runTest {
        viewModel.effect.test {
            viewModel.onEvent(LoginEvent.SignUpClicked)
            
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.NavigateToRegister)
        }
    }

    @Test
    fun `when EmailChanged, state is updated`() {
        viewModel.onEvent(LoginEvent.EmailChanged("test@test.com"))
        
        val state = viewModel.state.value
        assertEquals("test@test.com", state.email)
    }

    @Test
    fun `when PasswordChanged, state is updated`() {
        viewModel.onEvent(LoginEvent.PasswordChanged("Password123!"))
        
        val state = viewModel.state.value
        assertEquals("Password123!", state.password)
    }

    @Test
    fun `when SignInClicked with valid data, effect is NavigateToHome after delay`() = runTest {
        viewModel.onEvent(LoginEvent.EmailChanged("test@test.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("Password123!"))
        
        viewModel.effect.test {
            viewModel.onEvent(LoginEvent.SignInClicked)
            
            val effect = awaitItem()
            assertTrue(effect is LoginEffect.NavigateToHome)
        }
    }
}
