package iti.grad.nutriscan.presentation.nutrigpt.voice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.nutrigpt.manager.IVoiceManager
import iti.grad.nutriscan.domain.nutrigpt.manager.VoiceState
import iti.grad.nutriscan.domain.nutrigpt.usecase.SendNutriGptMessageUseCase
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEffect
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceEvent
import iti.grad.nutriscan.presentation.nutrigpt.voice.state.NutriGptVoiceState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

@HiltViewModel
class NutriGptVoiceViewModel @Inject constructor(
    private val sendNutriGptMessageUseCase: SendNutriGptMessageUseCase,
    private val voiceManager: IVoiceManager
) : ViewModel() {

    private val _state = MutableStateFlow(NutriGptVoiceState())
    val state: StateFlow<NutriGptVoiceState> = _state.asStateFlow()

    private val _effect = Channel<NutriGptVoiceEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            voiceManager.state.collect { voiceState ->
                when (voiceState) {
                    is VoiceState.Idle -> {
                        _state.update { it.copy(isListening = false) }
                    }
                    is VoiceState.Listening -> {
                        _state.update { it.copy(isListening = true) }
                    }
                    is VoiceState.PartialResult -> {
                        _state.update { it.copy(currentQuery = voiceState.text) }
                    }
                    is VoiceState.FinalResult -> {
                        _state.update { it.copy(currentQuery = voiceState.text, isListening = false) }
                        if (voiceState.text.isNotBlank()) {
                            sendMessage(voiceState.text)
                        }
                    }
                    is VoiceState.Error -> {
                        _state.update { it.copy(isListening = false) }
                        _effect.send(NutriGptVoiceEffect.ShowError(voiceState.message))
                    }
                    is VoiceState.DoneSpeaking -> {
                        _state.update { it.copy(isPlaying = false) }
                    }
                }
            }
        }
    }

    fun onEvent(event: NutriGptVoiceEvent) {
        when (event) {
            is NutriGptVoiceEvent.ToggleLanguage -> {
                _state.update {
                    val newLang = if (it.chatLanguage == ChatLanguage.EN) ChatLanguage.AR else ChatLanguage.EN
                    it.copy(chatLanguage = newLang)
                }
            }
            is NutriGptVoiceEvent.SetListeningState -> {
                if (event.isListening) {
                    val langCode = if (state.value.chatLanguage == ChatLanguage.AR) "ar-EG" else "en-US"
                    voiceManager.startListening(langCode)
                } else {
                    voiceManager.stopListening()
                }
            }
            is NutriGptVoiceEvent.UpdateQuery -> {
                _state.update { it.copy(currentQuery = event.text) }
            }
            is NutriGptVoiceEvent.SubmitQuery -> {
                if (event.query.isNotBlank()) {
                    sendMessage(event.query)
                }
            }
            is NutriGptVoiceEvent.StopPlaying -> {
                _state.update { it.copy(isPlaying = false) }
                voiceManager.stopSpeaking()
            }
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _effect.send(NutriGptVoiceEffect.NavigateBack)
        }
    }

    private fun sendMessage(query: String) {
        _state.update {
            it.copy(
                isGenerating = true,
                isListening = false,
                currentQuery = "",
                isPlaying = false,
                error = null
            )
        }

        viewModelScope.launch {
            val result = sendNutriGptMessageUseCase(query)
            _state.update { it.copy(isGenerating = false) }

            result.onSuccess { data ->
                _state.update {
                    it.copy(
                        sources = data.sources.toImmutableList(),
                        answer = data.text,
                        isPlaying = true 
                    )
                }
                
                val langCode = if (state.value.chatLanguage == ChatLanguage.AR) "ar" else "en"
                voiceManager.speak(data.text, langCode)
                
            }.onFailure { error ->
                _effect.send(NutriGptVoiceEffect.ShowError(error.message ?: "Unknown Error"))
                
                val fallbackReply = getRandomErrorReply(state.value.chatLanguage)
                _state.update {
                    it.copy(
                        answer = fallbackReply,
                        isPlaying = true,
                        sources = persistentListOf()
                    )
                }
                
                val langCode = if (state.value.chatLanguage == ChatLanguage.AR) "ar" else "en"
                voiceManager.speak(fallbackReply, langCode)
            }
        }
    }

    private fun getRandomErrorReply(language: ChatLanguage): String {
        return if (language == ChatLanguage.AR) {
            fallbackErrorsAr.random()
        } else {
            fallbackErrorsEn.random()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }

    companion object {
        private val fallbackErrorsAr = listOf(
            "عذراً، حدث خطأ ما. يرجى المحاولة مرة أخرى.",
            "لم أتمكن من معالجة طلبك، هل يمكنك إعادة المحاولة؟",
            "للأسف واجهت مشكلة، حاول مرة أخرى.",
            "عفواً، هناك خطأ في الشبكة، برجاء المحاولة لاحقاً."
        )

        private val fallbackErrorsEn = listOf(
            "Sorry, an error occurred. Please try again.",
            "I couldn't process your request, could you try again?",
            "Unfortunately I encountered an issue, please try again.",
            "Oops, there's a network error. Please try again later."
        )
    }
}
