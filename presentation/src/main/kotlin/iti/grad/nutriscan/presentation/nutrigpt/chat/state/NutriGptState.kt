package iti.grad.nutriscan.presentation.nutrigpt.chat.state

import iti.grad.nutriscan.domain.nutrigpt.model.NutriGptMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class NutriGptState(
    val messages: ImmutableList<NutriGptMessage> = persistentListOf(),
    val currentQuery: String = "",
    val isLoading: Boolean = false,
    val areSourcesExpanded: Boolean = false,
    val chatLanguage: ChatLanguage = ChatLanguage.EN,
    val isListening: Boolean = false,
    val errorMessageResId: Int? = null
)

enum class ChatLanguage {
    EN, AR
}
