package iti.grad.nutriscan.presentation.saved

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.usecase.AddFoodEntryUseCase
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.saved.state.SavedEffect
import iti.grad.nutriscan.presentation.saved.state.SavedEvent
import iti.grad.nutriscan.presentation.saved.viewmodel.SavedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SavedViewModelTest {

    private lateinit var addFoodEntryUseCase: AddFoodEntryUseCase
    private lateinit var viewModel: SavedViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addFoodEntryUseCase = mockk()
        viewModel = SavedViewModel(addFoodEntryUseCase)
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        fun `initial state loads the mock product catalog`() {
            val state = viewModel.state.value

            Assertions.assertEquals(6, state.products.size)
            Assertions.assertEquals(6, state.filteredProducts.size)
            Assertions.assertEquals("", state.searchQuery)
        }
    }

    @Nested
    @DisplayName("Search")
    inner class Search {

        @Test
        fun `SearchQueryChanged filters products by name, case-insensitive`() = runTest {
            viewModel.onEvent(SavedEvent.SearchQueryChanged("chocolate"))
            testScheduler.runCurrent()

            Assertions.assertEquals(1, viewModel.state.value.filteredProducts.size)
            Assertions.assertEquals("Chocolate Bar", viewModel.state.value.filteredProducts.first().productName)
        }

        @Test
        fun `blank SearchQueryChanged resets to the full catalog`() = runTest {
            viewModel.onEvent(SavedEvent.SearchQueryChanged("chocolate"))
            viewModel.onEvent(SavedEvent.SearchQueryChanged(""))
            testScheduler.runCurrent()

            Assertions.assertEquals(6, viewModel.state.value.filteredProducts.size)
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        fun `ProductClicked emits NavigateToProductDetail with the product id`() = runTest {
            viewModel.effect.test {
                val mockProduct = iti.grad.nutriscan.presentation.common.model.ProductUiModel(
                    id = "3",
                    productName = "Sample",
                    imageUrl = null,
                    verdict = iti.grad.nutriscan.domain.common.model.ProductVerdict.SAFE,
                    calories = "100"
                )
                viewModel.onEvent(SavedEvent.ProductClicked(mockProduct))
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertTrue(effect is SavedEffect.NavigateToProductDetail)
                Assertions.assertEquals("3", (effect as SavedEffect.NavigateToProductDetail).product.id)
            }
        }

        @Test
        fun `BottomNavTabClicked with HOME emits NavigateToHome`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(SavedEvent.BottomNavTabClicked(BottomNavTab.HOME))
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is SavedEffect.NavigateToHome)
            }
        }

        @Test
        fun `BottomNavTabClicked with SAVED emits no effect`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(SavedEvent.BottomNavTabClicked(BottomNavTab.SAVED))
                testScheduler.runCurrent()

                expectNoEvents()
            }
        }
    }

    @Nested
    @DisplayName("Swipe To Add")
    inner class SwipeToAdd {

        @Test
        fun `SwipeToAddTriggered on a known product calls AddFoodEntryUseCase and shows success snackbar`() = runTest {
            coEvery { addFoodEntryUseCase(any()) } returns Result.success(Unit)

            viewModel.effect.test {
                viewModel.onEvent(SavedEvent.SwipeToAddTriggered("1"))
                testScheduler.runCurrent()

                val effect = awaitItem()
                Assertions.assertTrue(effect is SavedEffect.ShowAddedToFoodLogSnackbar)
                Assertions.assertEquals(
                    "Almarai Milk Full Fat",
                    (effect as SavedEffect.ShowAddedToFoodLogSnackbar).productName,
                )
            }
            coVerify(exactly = 1) { addFoodEntryUseCase(any<FoodLogEntry>()) }
        }

        @Test
        fun `SwipeToAddTriggered failure shows an error snackbar`() = runTest {
            coEvery { addFoodEntryUseCase(any()) } returns Result.failure(IllegalStateException("Not authenticated"))

            viewModel.effect.test {
                viewModel.onEvent(SavedEvent.SwipeToAddTriggered("1"))
                testScheduler.runCurrent()

                Assertions.assertTrue(awaitItem() is SavedEffect.ShowAddErrorSnackbar)
            }
        }

        @Test
        fun `SwipeToAddTriggered with an unknown product id is a no-op`() = runTest {
            viewModel.effect.test {
                viewModel.onEvent(SavedEvent.SwipeToAddTriggered("unknown-id"))
                testScheduler.runCurrent()

                expectNoEvents()
            }
            coVerify(exactly = 0) { addFoodEntryUseCase(any()) }
        }
    }
}
