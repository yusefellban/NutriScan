package iti.grad.nutriscan.presentation.news

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.news.model.NewsArticle
import iti.grad.nutriscan.domain.news.model.NewsTopicChip
import iti.grad.nutriscan.domain.news.usecase.BuildNewsTopicChipsUseCase
import iti.grad.nutriscan.domain.news.usecase.GetHealthHeadlinesUseCase
import iti.grad.nutriscan.domain.news.usecase.SearchNewsArticlesUseCase
import iti.grad.nutriscan.presentation.news.state.NewsEffect
import iti.grad.nutriscan.presentation.news.state.NewsEvent
import iti.grad.nutriscan.presentation.news.viewmodel.NewsViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val diabetesChip = NewsTopicChip(id = "disease:1", label = "Diabetes", searchKeyword = "Diabetes")
    private val allergyChip = NewsTopicChip(id = "allergy:2", label = "Peanuts", searchKeyword = "Peanuts")
    private val defaultChips = listOf(
        NewsTopicChip(id = NewsTopicChip.ALL_CHIP_ID, label = "All", searchKeyword = null),
        diabetesChip,
        allergyChip,
    )

    private fun article(title: String = "Title") = NewsArticle(
        title = title,
        description = "Description",
        url = "https://example.com/$title",
        imageUrl = null,
        sourceName = "Source",
        publishedAt = "2026-07-22T00:00:00Z",
        author = "Author",
    )

    private lateinit var buildNewsTopicChipsUseCase: BuildNewsTopicChipsUseCase
    private lateinit var getHealthHeadlinesUseCase: GetHealthHeadlinesUseCase
    private lateinit var searchNewsArticlesUseCase: SearchNewsArticlesUseCase
    private lateinit var viewModel: NewsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        buildNewsTopicChipsUseCase = mockk()
        getHealthHeadlinesUseCase = mockk()
        searchNewsArticlesUseCase = mockk()
        every { buildNewsTopicChipsUseCase() } returns flowOf(defaultChips)
        coEvery { getHealthHeadlinesUseCase() } returns Result.success(listOf(article("A"), article("B")))
        coEvery { searchNewsArticlesUseCase(any()) } returns Result.success(listOf(article("Matched")))
        viewModel = NewsViewModel(buildNewsTopicChipsUseCase, getHealthHeadlinesUseCase, searchNewsArticlesUseCase)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `initial state loads chips and default health headlines`() = runTest {
            testScheduler.runCurrent()

            val state = viewModel.state.value
            Assertions.assertEquals(3, state.chips.size)
            Assertions.assertEquals(setOf(NewsTopicChip.ALL_CHIP_ID), state.selectedChipIds)
            Assertions.assertEquals(2, state.articles.size)
            Assertions.assertFalse(state.isLoading)
            Assertions.assertNull(state.errorMessageResId)
        }
    }

    @Nested
    @DisplayName("Chip selection")
    inner class ChipSelection {

        @Test
        fun `selecting a non-All chip clears All and searches by its keyword`() = runTest {
            testScheduler.runCurrent()

            viewModel.onEvent(NewsEvent.ChipClicked(diabetesChip.id))
            testScheduler.runCurrent()

            Assertions.assertEquals(setOf(diabetesChip.id), viewModel.state.value.selectedChipIds)
            coVerify { searchNewsArticlesUseCase(listOf("Diabetes")) }
        }

        @Test
        fun `selecting a second non-All chip multi-selects and ORs both keywords`() = runTest {
            testScheduler.runCurrent()
            viewModel.onEvent(NewsEvent.ChipClicked(diabetesChip.id))
            testScheduler.runCurrent()

            viewModel.onEvent(NewsEvent.ChipClicked(allergyChip.id))
            testScheduler.runCurrent()

            Assertions.assertEquals(setOf(diabetesChip.id, allergyChip.id), viewModel.state.value.selectedChipIds)
            coVerify { searchNewsArticlesUseCase(match { it.size == 2 && it.containsAll(listOf("Diabetes", "Peanuts")) }) }
        }

        @Test
        fun `selecting All while others are selected clears them and fetches headlines`() = runTest {
            testScheduler.runCurrent()
            viewModel.onEvent(NewsEvent.ChipClicked(diabetesChip.id))
            testScheduler.runCurrent()

            viewModel.onEvent(NewsEvent.ChipClicked(NewsTopicChip.ALL_CHIP_ID))
            testScheduler.runCurrent()

            Assertions.assertEquals(setOf(NewsTopicChip.ALL_CHIP_ID), viewModel.state.value.selectedChipIds)
            coVerify(exactly = 2) { getHealthHeadlinesUseCase() }
        }

        @Test
        fun `deselecting the only selected chip falls back to All`() = runTest {
            testScheduler.runCurrent()
            viewModel.onEvent(NewsEvent.ChipClicked(diabetesChip.id))
            testScheduler.runCurrent()

            viewModel.onEvent(NewsEvent.ChipClicked(diabetesChip.id))
            testScheduler.runCurrent()

            Assertions.assertEquals(setOf(NewsTopicChip.ALL_CHIP_ID), viewModel.state.value.selectedChipIds)
        }
    }

    @Nested
    @DisplayName("Errors and retry")
    inner class ErrorsAndRetry {

        @Test
        fun `failed fetch surfaces news_load_error`() = runTest {
            coEvery { getHealthHeadlinesUseCase() } returns Result.failure(RuntimeException("network error"))
            val vm = NewsViewModel(buildNewsTopicChipsUseCase, getHealthHeadlinesUseCase, searchNewsArticlesUseCase)
            testScheduler.runCurrent()

            Assertions.assertEquals(R.string.news_load_error, vm.state.value.errorMessageResId)
            Assertions.assertTrue(vm.state.value.articles.isEmpty())
        }

        @Test
        fun `RetryClicked after failure clears error once the fetch succeeds`() = runTest {
            coEvery { getHealthHeadlinesUseCase() } returns Result.failure(RuntimeException("network error"))
            val vm = NewsViewModel(buildNewsTopicChipsUseCase, getHealthHeadlinesUseCase, searchNewsArticlesUseCase)
            testScheduler.runCurrent()
            Assertions.assertEquals(R.string.news_load_error, vm.state.value.errorMessageResId)

            coEvery { getHealthHeadlinesUseCase() } returns Result.success(listOf(article("A")))
            vm.onEvent(NewsEvent.RetryClicked)
            testScheduler.runCurrent()

            Assertions.assertNull(vm.state.value.errorMessageResId)
            Assertions.assertEquals(1, vm.state.value.articles.size)
        }
    }

    @Nested
    @DisplayName("One-shot effects and preview state")
    inner class Effects {

        @Test
        fun `ArticleClicked updates selectedArticle in state`() = runTest {
            testScheduler.runCurrent()
            val stateArticle = viewModel.state.value.articles.first()
            viewModel.onEvent(NewsEvent.ArticleClicked(stateArticle))
            Assertions.assertEquals(stateArticle, viewModel.state.value.selectedArticle)
        }

        @Test
        fun `CloseArticleClicked clears selectedArticle in state`() = runTest {
            testScheduler.runCurrent()
            val stateArticle = viewModel.state.value.articles.first()
            viewModel.onEvent(NewsEvent.ArticleClicked(stateArticle))
            Assertions.assertEquals(stateArticle, viewModel.state.value.selectedArticle)

            viewModel.onEvent(NewsEvent.CloseArticleClicked)
            Assertions.assertNull(viewModel.state.value.selectedArticle)
        }

        @Test
        fun `BackClicked emits NavigateBack`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(NewsEvent.BackClicked)
                Assertions.assertTrue(awaitItem() is NewsEffect.NavigateBack)
            }
        }
    }
}
