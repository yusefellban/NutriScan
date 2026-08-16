package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.NewsDao
import iti.grad.nutriscan.data.db.entity.NewsArticleEntity
import iti.grad.nutriscan.data.remote.api.NewsApiService
import iti.grad.nutriscan.data.remote.dto.ArticleDto
import iti.grad.nutriscan.data.remote.dto.NewsResponseDto
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Covers [NewsRepositoryImpl]'s offline caching: headlines/search results are written to Room on
 * a successful fetch and served from Room when the network call fails. */
class NewsRepositoryImplTest {

    private lateinit var newsApiService: NewsApiService
    private lateinit var newsDao: NewsDao
    private lateinit var repository: NewsRepositoryImpl

    @BeforeEach
    fun setup() {
        newsApiService = mockk()
        newsDao = mockk()
        repository = NewsRepositoryImpl(newsApiService, newsDao, UnconfinedTestDispatcher())
    }

    @Test
    fun `a successful headlines fetch caches the articles for offline reuse`() = runTest {
        coEvery { newsApiService.getTopHeadlines(category = "health") } returns NewsResponseDto(
            status = "ok",
            articles = listOf(ArticleDto(title = "Eat well", url = "https://a")),
        )
        coEvery { newsDao.replaceFeed(any(), any()) } returns Unit

        val result = repository.getHealthHeadlines().getOrThrow()

        assertEquals("Eat well", result.single().title)
        coVerify(exactly = 1) { newsDao.replaceFeed("headlines", any()) }
    }

    @Test
    fun `headlines with no connectivity falls back to the cached feed`() = runTest {
        coEvery { newsApiService.getTopHeadlines(category = "health") } throws java.io.IOException("no network")
        coEvery { newsDao.getArticles("headlines") } returns listOf(
            NewsArticleEntity(
                url = "https://a",
                feedKey = "headlines",
                title = "Cached article",
                description = null,
                imageUrl = null,
                sourceName = "Source",
                publishedAt = null,
                author = null,
                position = 0,
            )
        )

        val result = repository.getHealthHeadlines().getOrThrow()

        assertEquals("Cached article", result.single().title)
    }

    @Test
    fun `headlines with no connectivity and no cache fails`() = runTest {
        coEvery { newsApiService.getTopHeadlines(category = "health") } throws java.io.IOException("no network")
        coEvery { newsDao.getArticles("headlines") } returns emptyList()

        val result = repository.getHealthHeadlines()

        assertTrue(result.isFailure)
    }

    @Test
    fun `a search fetch caches under a feed key scoped to the keywords`() = runTest {
        coEvery { newsApiService.searchArticles(query = "(sugar)") } returns NewsResponseDto(
            status = "ok",
            articles = listOf(ArticleDto(title = "Sugar risks", url = "https://b")),
        )
        coEvery { newsDao.replaceFeed(any(), any()) } returns Unit

        val result = repository.searchArticles(listOf("sugar")).getOrThrow()

        assertEquals("Sugar risks", result.single().title)
        coVerify(exactly = 1) { newsDao.replaceFeed("search:sugar", any()) }
    }
}
