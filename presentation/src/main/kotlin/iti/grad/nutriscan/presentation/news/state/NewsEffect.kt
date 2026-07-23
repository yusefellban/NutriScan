package iti.grad.nutriscan.presentation.news.state

sealed interface NewsEffect {
    data object NavigateBack : NewsEffect
    data class OpenArticle(val url: String) : NewsEffect
    data class ShareArticle(val url: String, val title: String) : NewsEffect
}
