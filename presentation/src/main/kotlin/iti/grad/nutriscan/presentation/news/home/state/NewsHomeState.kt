package iti.grad.nutriscan.presentation.news.home.state

import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class NewsHomeState(
    val breakingArticles: ImmutableList<NewsUiArticle> = persistentListOf(),
    val recommendationArticles: ImmutableList<NewsUiArticle> = persistentListOf(),
    val isLoading: Boolean = true,
    val errorMessageResId: Int? = null,
)
