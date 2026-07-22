package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.NewsApiService
import iti.grad.nutriscan.data.remote.dto.ArticleDto
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val HEALTH_CATEGORY = "health"

class NewsRepositoryImpl @Inject constructor(
    private val newsApiService: NewsApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : INewsRepository {

    override suspend fun getHealthHeadlines(): Result<List<NewsArticle>> = withContext(ioDispatcher) {
        runCatchingCancellable {
            newsApiService.getTopHeadlines(category = HEALTH_CATEGORY).articles.map { it.toDomain() }
        }
    }

    override suspend fun searchArticles(keywords: List<String>): Result<List<NewsArticle>> =
        withContext(ioDispatcher) {
            runCatchingCancellable {
                val query = "(" + keywords.joinToString(" OR ") + ")"
                newsApiService.searchArticles(query = query).articles.map { it.toDomain() }
            }
        }

    private fun ArticleDto.toDomain(): NewsArticle = NewsArticle(
        title = title.orEmpty(),
        description = description,
        url = url.orEmpty(),
        imageUrl = urlToImage,
        sourceName = source?.name ?: "",
        publishedAt = publishedAt,
        author = author,
    )
}
