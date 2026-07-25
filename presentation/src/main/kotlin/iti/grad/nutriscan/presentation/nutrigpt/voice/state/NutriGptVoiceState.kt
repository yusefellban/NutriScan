package iti.grad.nutriscan.presentation.nutrigpt.voice.state

import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptSource
import iti.grad.nutriscan.presentation.nutrigpt.chat.state.ChatLanguage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class NutriGptVoiceState(
    val chatLanguage: ChatLanguage = ChatLanguage.EN,
    val isListening: Boolean = false,
    val isGenerating: Boolean = false,
    val isPlaying: Boolean = false,
    val currentQuery: String = "",
    val answer: String = "",
    val sources: ImmutableList<NutriGptSource> = persistentListOf(),
    val error: String? = null
)
