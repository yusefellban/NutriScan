package iti.grad.nutriscan.presentation.auth.register

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEffect
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEvent
import iti.grad.nutriscan.presentation.auth.register.viewmodel.RegisterViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: RegisterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and not loading`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertFalse(state.passwordVisible)
        assertFalse(state.confirmPasswordVisible)
        assertNull(state.emailErrorResId)
        assertNull(state.passwordErrorResId)
        assertNull(state.confirmPasswordErrorResId)
    }

    @Test
    fun `EmailChanged updates email state and resets error`() = runTest(testDispatcher) {
        viewModel.state.test {
            // Initial state
            var state = awaitItem()
            assertNull(state.emailErrorResId)

            // Trigger action that causes validation error first to test error reset
            viewModel.onEvent(RegisterEvent.SignUpClicked)
            state = awaitItem() // error empty field
            assertEquals(R.string.error_empty_field, state.emailErrorResId)

            // Type valid email
            viewModel.onEvent(RegisterEvent.EmailChanged("test@example.com"))
            state = awaitItem()
            assertEquals("test@example.com", state.email)
            assertNull(state.emailErrorResId)
        }
    }

    @Test
    fun `PasswordChanged updates password state and resets error`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()
            assertNull(state.passwordErrorResId)

            viewModel.onEvent(RegisterEvent.SignUpClicked)
            state = awaitItem() // error empty field
            assertEquals(R.string.error_empty_field, state.passwordErrorResId)

            viewModel.onEvent(RegisterEvent.PasswordChanged("pass123"))
            state = awaitItem()
            assertEquals("pass123", state.password)
            assertNull(state.passwordErrorResId)
        }
    }

    @Test
    fun `ConfirmPasswordChanged updates confirmPassword state and resets error`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()
            assertNull(state.confirmPasswordErrorResId)

            // Trigger password mismatch
            viewModel.onEvent(RegisterEvent.EmailChanged("test@example.com"))
            viewModel.onEvent(RegisterEvent.PasswordChanged("pass123"))
            viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("pass456"))
            awaitItem() // EmailChanged
            awaitItem() // PasswordChanged
            awaitItem() // ConfirmPasswordChanged

            viewModel.onEvent(RegisterEvent.SignUpClicked)
            state = awaitItem() // mismatch error
            assertEquals(R.string.error_password_mismatch, state.confirmPasswordErrorResId)

            // Fix password
            viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("pass123"))
            state = awaitItem()
            assertEquals("pass123", state.confirmPassword)
            assertNull(state.confirmPasswordErrorResId)
        }
    }

    @Test
    fun `TogglePasswordVisibility toggles password visible state`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()
            assertFalse(state.passwordVisible)

            viewModel.onEvent(RegisterEvent.TogglePasswordVisibility)
            state = awaitItem()
            assertTrue(state.passwordVisible)

            viewModel.onEvent(RegisterEvent.TogglePasswordVisibility)
            state = awaitItem()
            assertFalse(state.passwordVisible)
        }
    }

    @Test
    fun `ToggleConfirmPasswordVisibility toggles confirm password visible state`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()
            assertFalse(state.confirmPasswordVisible)

            viewModel.onEvent(RegisterEvent.ToggleConfirmPasswordVisibility)
            state = awaitItem()
            assertTrue(state.confirmPasswordVisible)

            viewModel.onEvent(RegisterEvent.ToggleConfirmPasswordVisibility)
            state = awaitItem()
            assertFalse(state.confirmPasswordVisible)
        }
    }

    @Test
    fun `SignUpClicked validations show empty field errors`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()

            viewModel.onEvent(RegisterEvent.SignUpClicked)
            state = awaitItem()

            assertEquals(R.string.error_empty_field, state.emailErrorResId)
            assertEquals(R.string.error_empty_field, state.passwordErrorResId)
            assertNull(state.confirmPasswordErrorResId)
        }
    }

    @Test
    fun `SignUpClicked validation shows invalid email error`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()

            viewModel.onEvent(RegisterEvent.EmailChanged("invalidemail"))
            state = awaitItem()

            viewModel.onEvent(RegisterEvent.SignUpClicked)
            state = awaitItem()

            assertEquals(R.string.error_invalid_email, state.emailErrorResId)
        }
    }

    @Test
    fun `SignUpClicked success flow launches loading and triggers NavigateToHome`() = runTest(testDispatcher) {
        viewModel.state.test {
            var state = awaitItem()

            viewModel.onEvent(RegisterEvent.EmailChanged("user@example.com"))
            awaitItem()
            viewModel.onEvent(RegisterEvent.PasswordChanged("password123"))
            awaitItem()
            viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged("password123"))
            awaitItem()

            // Trigger SignUp
            viewModel.onEvent(RegisterEvent.SignUpClicked)
            state = awaitItem() // loading starts
            assertTrue(state.isLoading)

            // Let the coroutine progress and assert state transitions
            testScheduler.advanceTimeBy(1500)
            state = awaitItem() // loading ends
            assertFalse(state.isLoading)
        }

        viewModel.effect.test {
            // Since NavigateToHome is sent after the delay, it should be emitted here
            assertEquals(RegisterEffect.NavigateToHome, awaitItem())
        }
    }

    @Test
    fun `SignInClicked triggers NavigateToSignIn side effect`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(RegisterEvent.SignInClicked)
            assertEquals(RegisterEffect.NavigateToSignIn, awaitItem())
        }
    }
}
