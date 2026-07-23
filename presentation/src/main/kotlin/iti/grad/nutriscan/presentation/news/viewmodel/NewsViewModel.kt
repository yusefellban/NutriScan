package iti.grad.nutriscan.presentation.news.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.domain.news.usecase.BuildNewsTopicChipsUseCase
import iti.grad.nutriscan.domain.news.usecase.GetHealthHeadlinesUseCase
import iti.grad.nutriscan.domain.news.usecase.SearchNewsArticlesUseCase
import iti.grad.nutriscan.presentation.news.state.NewsEffect
import iti.grad.nutriscan.presentation.news.state.NewsEvent
import iti.grad.nutriscan.presentation.news.state.NewsState
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.presentation.R
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val buildNewsTopicChipsUseCase: BuildNewsTopicChipsUseCase,
    private val getHealthHeadlinesUseCase: GetHealthHeadlinesUseCase,
    private val searchNewsArticlesUseCase: SearchNewsArticlesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewsState())
    val state: StateFlow<NewsState> = _state.asStateFlow()

    private val _effect = Channel<NewsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            buildNewsTopicChipsUseCase().collectLatest { chips ->
                _state.update { it.copy(chips = chips.toPersistentList()) }
            }
        }
        fetchArticles()
    }

    fun onEvent(event: NewsEvent) {
        when (event) {
            is NewsEvent.ChipClicked -> onChipClicked(event.chipId)
            is NewsEvent.ArticleClicked -> emitEffect(NewsEffect.OpenArticle(event.url))
            is NewsEvent.ShareClicked -> emitEffect(NewsEffect.ShareArticle(event.url, event.title))
            is NewsEvent.BackClicked -> emitEffect(NewsEffect.NavigateBack)
            is NewsEvent.RetryClicked -> fetchArticles()
        }
    }

    private fun onChipClicked(chipId: String) {
        val current = _state.value.selectedChipIds
        val newSelection = when {
            chipId == NewsTopicChip.ALL_CHIP_ID -> persistentSetOf(NewsTopicChip.ALL_CHIP_ID)
            chipId in current -> (current - chipId).ifEmpty { persistentSetOf(NewsTopicChip.ALL_CHIP_ID) }
            else -> (current - NewsTopicChip.ALL_CHIP_ID) + chipId
        }
        _state.update { it.copy(selectedChipIds = newSelection.toPersistentSet()) }
        fetchArticles()
    }

    private fun fetchArticles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageResId = null) }
            val selected = _state.value.selectedChipIds
            val result = if (selected.contains(NewsTopicChip.ALL_CHIP_ID) || selected.isEmpty()) {
                getHealthHeadlinesUseCase()
            } else {
                val keywords = _state.value.chips
                    .filter { it.id in selected }
                    .mapNotNull { it.searchKeyword }
                searchNewsArticlesUseCase(keywords)
            }
            result
                .onSuccess { articles ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            articles = articles.map { article -> article.toUiModel() }.toPersistentList(),
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(isLoading = false, errorMessageResId = R.string.news_load_error)
                    }
                }
        }
    }

    private fun emitEffect(effect: NewsEffect) {
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
    )
}
