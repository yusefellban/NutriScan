package iti.grad.nutriscan.presentation.news.state

import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

data class NewsUiArticle(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAtLabel: String,
    val author: String?,
    val category: String,
)

data class NewsState(
    val chips: ImmutableList<NewsTopicChip> = persistentListOf(),
    val selectedChipIds: ImmutableSet<String> = persistentSetOf(NewsTopicChip.ALL_CHIP_ID),
    val articles: ImmutableList<NewsUiArticle> = persistentListOf(),
    val selectedArticle: NewsUiArticle? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessageResId: Int? = null,
)
