package iti.grad.nutriscan.presentation.news.state

sealed interface NewsEvent {
    data class ChipClicked(val chipId: String) : NewsEvent
    data class ArticleClicked(val article: NewsUiArticle) : NewsEvent
    data object CloseArticleClicked : NewsEvent
    data class OpenArticleInWeb(val url: String) : NewsEvent
    data class SearchQueryChanged(val query: String) : NewsEvent
    data object FilterClicked : NewsEvent
    data object BackClicked : NewsEvent
    data object RetryClicked : NewsEvent
}
