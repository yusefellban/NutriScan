package iti.grad.nutriscan.presentation.news.state

sealed interface NewsEvent {
    data class ChipClicked(val chipId: String) : NewsEvent
    data class ArticleClicked(val url: String) : NewsEvent
    data class ShareClicked(val url: String, val title: String) : NewsEvent
    data object BackClicked : NewsEvent
    data object RetryClicked : NewsEvent
}
