package iti.grad.nutriscan.presentation.settings.help

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.settings.help.state.HelpEffect
import iti.grad.nutriscan.presentation.settings.help.state.HelpEvent
import iti.grad.nutriscan.presentation.settings.help.viewmodel.HelpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HelpViewModel()

    @Test
    fun `initial state has ten faq items and nothing expanded`() {
        val viewModel = createViewModel()
        Assertions.assertEquals(10, viewModel.state.value.faqItems.size)
        Assertions.assertNull(viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `FaqItemClicked expands the clicked item`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        Assertions.assertEquals(3, viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `FaqItemClicked on already-expanded item collapses it`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        Assertions.assertNull(viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `FaqItemClicked on a different item switches which one is expanded`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FaqItemClicked(3))
        viewModel.onEvent(HelpEvent.FaqItemClicked(5))
        Assertions.assertEquals(5, viewModel.state.value.expandedFaqId)
    }

    @Test
    fun `ContactSupportClicked emits OpenEmail with support address and empty body`() = runTest {
        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.onEvent(HelpEvent.ContactSupportClicked)
            val effect = awaitItem()
            Assertions.assertTrue(effect is HelpEffect.OpenEmail)
            effect as HelpEffect.OpenEmail
            Assertions.assertEquals("ahmedtayseer424@gmail.com", effect.recipient)
            Assertions.assertEquals("", effect.body)
            Assertions.assertFalse(effect.isFeedback)
        }
    }

    @Test
    fun `SendFeedbackClicked shows feedback dialog`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.SendFeedbackClicked)
        Assertions.assertTrue(viewModel.state.value.showFeedbackDialog)
    }

    @Test
    fun `FeedbackTextChanged updates feedback text`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.FeedbackTextChanged("great app"))
        Assertions.assertEquals("great app", viewModel.state.value.feedbackText)
    }

    @Test
    fun `FeedbackSubmitClicked emits OpenEmail with feedback text as body and closes dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.SendFeedbackClicked)
        viewModel.onEvent(HelpEvent.FeedbackTextChanged("great app"))
        viewModel.effect.test {
            viewModel.onEvent(HelpEvent.FeedbackSubmitClicked)
            val effect = awaitItem()
            Assertions.assertTrue(effect is HelpEffect.OpenEmail)
            effect as HelpEffect.OpenEmail
            Assertions.assertEquals("great app", effect.body)
            Assertions.assertTrue(effect.isFeedback)
        }
        Assertions.assertFalse(viewModel.state.value.showFeedbackDialog)
        Assertions.assertEquals("", viewModel.state.value.feedbackText)
    }

    @Test
    fun `FeedbackDismissed hides dialog and clears text`() {
        val viewModel = createViewModel()
        viewModel.onEvent(HelpEvent.SendFeedbackClicked)
        viewModel.onEvent(HelpEvent.FeedbackTextChanged("draft"))
        viewModel.onEvent(HelpEvent.FeedbackDismissed)
        Assertions.assertFalse(viewModel.state.value.showFeedbackDialog)
        Assertions.assertEquals("", viewModel.state.value.feedbackText)
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.onEvent(HelpEvent.BackClicked)
            Assertions.assertTrue(awaitItem() is HelpEffect.NavigateBack)
        }
    }
}
