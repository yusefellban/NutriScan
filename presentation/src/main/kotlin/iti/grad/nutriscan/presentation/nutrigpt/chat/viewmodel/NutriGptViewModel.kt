package iti.grad.nutriscan.presentation.nutrigpt.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import iti.grad.nutriscan.domain.nutrigpt.repository.INutriGptRepository
import iti.grad.nutriscan.domain.nutrigpt.usecase.SendNutriGptMessageUseCase
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.NutriGptEffect
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.NutriGptEvent
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.NutriGptState
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NutriGptViewModel @Inject constructor(
    private val sendNutriGptMessageUseCase: SendNutriGptMessageUseCase,
    private val nutriGptRepository: INutriGptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NutriGptState())
    val state: StateFlow<NutriGptState> = _state.asStateFlow()

    private val _effect = Channel<NutriGptEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            nutriGptRepository.messages.collectLatest { repoMessages ->
                _state.update { it.copy(messages = repoMessages.toImmutableList()) }
            }
        }
    }

    fun onEvent(event: NutriGptEvent) {
        when (event) {
            is NutriGptEvent.SendMessage -> sendMessage(event.query)
            is NutriGptEvent.UpdateQuery -> {
                _state.update { it.copy(currentQuery = event.text) }
            }
            is NutriGptEvent.SetListeningState -> {
                _state.update { it.copy(isListening = event.isListening) }
            }
            NutriGptEvent.ToggleSources -> {
                _state.update { it.copy(areSourcesExpanded = !it.areSourcesExpanded) }
            }
            NutriGptEvent.ToggleLanguage -> {
                _state.update {
                    val newLang = if (it.chatLanguage == ChatLanguage.EN) ChatLanguage.AR else ChatLanguage.EN
                    it.copy(chatLanguage = newLang)
                }
            }
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _effect.send(NutriGptEffect.NavigateBack)
        }
    }

    private fun sendMessage(query: String) {
        if (query.isBlank()) return

        val userMessage = NutriGptMessage(
            id = UUID.randomUUID().toString(),
            text = query,
            isFromUser = true
        )

        // Store user message in repository
        nutriGptRepository.addMessage(userMessage)

        _state.update {
            it.copy(
                currentQuery = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            val result = sendNutriGptMessageUseCase(query)
            _state.update { it.copy(isLoading = false) }

            result.onSuccess { botMessage ->
                nutriGptRepository.addMessage(botMessage)
            }.onFailure { error ->
                _effect.send(NutriGptEffect.ShowError(error.message ?: "Unknown Error"))
            }
        }
    }
}
