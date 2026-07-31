package iti.grad.nutriscan.presentation.news.home.state

import iti.grad.nutriscan.presentation.news.state.NewsUiArticle

sealed interface NewsHomeEffect {
    data object NavigateBack : NewsHomeEffect
    data object NavigateToDiscover : NewsHomeEffect
    data class NavigateToDetail(val article: NewsUiArticle) : NewsHomeEffect
}
