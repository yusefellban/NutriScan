package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.NewsDao
import iti.grad.nutriscan.data.db.entity.NewsArticleEntity
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
private const val HEADLINES_FEED_KEY = "headlines"
private const val SEARCH_FEED_KEY_PREFIX = "search:"

class NewsRepositoryImpl @Inject constructor(
    private val newsApiService: NewsApiService,
    private val newsDao: NewsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : INewsRepository {

    override suspend fun getHealthHeadlines(): Result<List<NewsArticle>> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val articles = newsApiService.getTopHeadlines(category = HEALTH_CATEGORY).articles.map { it.toDomain() }
            newsDao.replaceFeed(HEADLINES_FEED_KEY, articles.toEntities(HEADLINES_FEED_KEY))
            articles
        }.recoverCatching { throwable ->
            val cached = newsDao.getArticles(HEADLINES_FEED_KEY)
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw throwable
        }
    }

    override suspend fun searchArticles(keywords: List<String>): Result<List<NewsArticle>> =
        withContext(ioDispatcher) {
            val feedKey = SEARCH_FEED_KEY_PREFIX + keywords.sorted().joinToString(",")
            runCatchingCancellable {
                val query = "(" + keywords.joinToString(" OR ") + ")"
                val articles = newsApiService.searchArticles(query = query).articles.map { it.toDomain() }
                newsDao.replaceFeed(feedKey, articles.toEntities(feedKey))
                articles
            }.recoverCatching { throwable ->
                val cached = newsDao.getArticles(feedKey)
                if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw throwable
            }
        }

    private fun List<NewsArticle>.toEntities(feedKey: String): List<NewsArticleEntity> =
        mapIndexed { index, article ->
            NewsArticleEntity(
                url = article.url,
                feedKey = feedKey,
                title = article.title,
                description = article.description,
                imageUrl = article.imageUrl,
                sourceName = article.sourceName,
                publishedAt = article.publishedAt,
                author = article.author,
                position = index,
            )
        }

    private fun NewsArticleEntity.toDomain(): NewsArticle = NewsArticle(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sourceName = sourceName,
        publishedAt = publishedAt,
        author = author,
    )

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
