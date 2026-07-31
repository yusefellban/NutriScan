package iti.grad.nutriscan.presentation.news.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.usecase.GetHealthHeadlinesUseCase
import iti.grad.nutriscan.presentation.news.home.state.NewsHomeEffect
import iti.grad.nutriscan.presentation.news.home.state.NewsHomeEvent
import iti.grad.nutriscan.presentation.news.home.state.NewsHomeState
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val BREAKING_NEWS_COUNT = 5

@HiltViewModel
class NewsHomeViewModel @Inject constructor(
    private val getHealthHeadlinesUseCase: GetHealthHeadlinesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewsHomeState())
    val state: StateFlow<NewsHomeState> = _state.asStateFlow()

    private val _effect = Channel<NewsHomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        fetchHeadlines()
    }

    fun onEvent(event: NewsHomeEvent) {
        when (event) {
            is NewsHomeEvent.BackClicked -> emitEffect(NewsHomeEffect.NavigateBack)
            is NewsHomeEvent.SearchClicked -> emitEffect(NewsHomeEffect.NavigateToDiscover)
            is NewsHomeEvent.BreakingArticleClicked -> emitEffect(
                NewsHomeEffect.NavigateToDetail(event.article),
            )
            is NewsHomeEvent.RecommendationArticleClicked -> emitEffect(
                NewsHomeEffect.NavigateToDetail(event.article),
            )
            is NewsHomeEvent.RetryClicked -> fetchHeadlines()
        }
    }

    private fun fetchHeadlines() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageResId = null) }

            getHealthHeadlinesUseCase()
                .onSuccess { articles ->
                    val uiArticles = articles.map { it.toUiModel() }
                    val breaking = uiArticles.take(BREAKING_NEWS_COUNT)
                    val recommendations = uiArticles.drop(BREAKING_NEWS_COUNT)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            breakingArticles = breaking.toPersistentList(),
                            recommendationArticles = recommendations.toPersistentList(),
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageResId = R.string.news_load_error,
                        )
                    }
                }
        }
    }

    private fun emitEffect(effect: NewsHomeEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun NewsArticle.toUiModel(): NewsUiArticle = NewsUiArticle(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sourceName = sourceName,
        publishedAtLabel = publishedAt.orEmpty(),
        author = author,
        category = inferCategory(),
    )

    private fun NewsArticle.inferCategory(): String = if (
        sourceName.contains("health", ignoreCase = true) ||
        sourceName.contains("science", ignoreCase = true) ||
        sourceName.contains("medical", ignoreCase = true) ||
        title.contains("disease", ignoreCase = true) ||
        title.contains("virus", ignoreCase = true)
    ) {
        "Health"
    } else {
        "News"
    }
}
