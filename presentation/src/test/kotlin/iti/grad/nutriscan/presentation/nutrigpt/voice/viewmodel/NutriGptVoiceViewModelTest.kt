package iti.grad.nutriscan.presentation.nutrigpt.voice.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import iti.grad.nutriscan.domain.nutrigpt.manager.IVoiceManager
import iti.grad.nutriscan.domain.nutrigpt.manager.VoiceState
import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import iti.grad.nutriscan.domain.nutrigpt.usecase.SendNutriGptMessageUseCase
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEffect
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NutriGptVoiceViewModelTest {

    private lateinit var sendNutriGptMessageUseCase: SendNutriGptMessageUseCase
    private lateinit var voiceManager: IVoiceManager
    private lateinit var viewModel: NutriGptVoiceViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    private val voiceStateFlow = MutableStateFlow<VoiceState>(VoiceState.Idle)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sendNutriGptMessageUseCase = mockk()
        voiceManager = mockk(relaxed = true)
        every { voiceManager.state } returns voiceStateFlow
        
        viewModel = NutriGptVoiceViewModel(
            sendNutriGptMessageUseCase,
            voiceManager
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ToggleLanguage event toggles chat language`() {
        assertEquals(ChatLanguage.EN, viewModel.state.value.chatLanguage)
        
        viewModel.onEvent(NutriGptVoiceEvent.ToggleLanguage)
        assertEquals(ChatLanguage.AR, viewModel.state.value.chatLanguage)
        
        viewModel.onEvent(NutriGptVoiceEvent.ToggleLanguage)
        assertEquals(ChatLanguage.EN, viewModel.state.value.chatLanguage)
    }

    @Test
    fun `SetListeningState true starts listening via voiceManager`() {
        viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(true))
        verify { voiceManager.startListening("en-US") }
    }

    @Test
    fun `SetListeningState false stops listening via voiceManager`() {
        viewModel.onEvent(NutriGptVoiceEvent.SetListeningState(false))
        verify { voiceManager.stopListening() }
    }

    @Test
    fun `VoiceManager emitting PartialResult updates currentQuery`() = runTest {
        voiceStateFlow.value = VoiceState.PartialResult("Hello")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals("Hello", viewModel.state.value.currentQuery)
    }

    @Test
    fun `VoiceManager emitting FinalResult triggers SendMessage on success`() = runTest {
        val finalQuery = "Is apple healthy?"
        val response = NutriGptMessage(id = "1", text = "Yes, very healthy.", isFromUser = false, sources = emptyList())
        coEvery { sendNutriGptMessageUseCase(finalQuery) } returns Result.success(response)
        
        voiceStateFlow.value = VoiceState.FinalResult(finalQuery)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isGenerating)
        assertTrue(viewModel.state.value.isPlaying)
        assertEquals("Yes, very healthy.", viewModel.state.value.answer)
        verify { voiceManager.speak("Yes, very healthy.", "en") }
    }
    
    @Test
    fun `VoiceManager emitting Error sends ShowError effect`() = runTest {
        viewModel.effect.test {
            voiceStateFlow.value = VoiceState.Error("Network failure")
            testDispatcher.scheduler.advanceUntilIdle()
            
            val effect = awaitItem()
            assertTrue(effect is NutriGptVoiceEffect.ShowError)
            assertEquals("Network failure", (effect as NutriGptVoiceEffect.ShowError).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun `StopPlaying event updates state and stops voiceManager speaking`() {
        viewModel.onEvent(NutriGptVoiceEvent.StopPlaying)
        
        assertFalse(viewModel.state.value.isPlaying)
        verify { voiceManager.stopSpeaking() }
    }
}
