package iti.grad.nutriscan.presentation.news.home.state

import iti.grad.nutriscan.presentation.news.state.NewsUiArticle

sealed interface NewsHomeEvent {
    data object BackClicked : NewsHomeEvent
    data object SearchClicked : NewsHomeEvent
    data class BreakingArticleClicked(val article: NewsUiArticle) : NewsHomeEvent
    data class RecommendationArticleClicked(val article: NewsUiArticle) : NewsHomeEvent
    data object RetryClicked : NewsHomeEvent
}
