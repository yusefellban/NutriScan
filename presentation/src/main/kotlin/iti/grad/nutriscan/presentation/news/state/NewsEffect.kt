package iti.grad.nutriscan.presentation.news.state

sealed interface NewsEffect {
    data object NavigateBack : NewsEffect
    data class NavigateToDetail(val article: NewsUiArticle) : NewsEffect
}
