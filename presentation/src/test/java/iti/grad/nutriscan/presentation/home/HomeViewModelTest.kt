package iti.grad.nutriscan.presentation.home

import app.cash.turbine.test
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.home.state.HomeEffect
import iti.grad.nutriscan.presentation.home.state.HomeEvent
import iti.grad.nutriscan.presentation.home.state.VerdictType
import iti.grad.nutriscan.presentation.home.viewmodel.HomeViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct dummy data`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals("Noureldeen", state.userName)
        assertEquals(3, state.recentHistory.size)
        assertEquals(BottomNavTab.HOME, state.selectedTab)

        // Verify first history item details
        val firstItem = state.recentHistory[0]
        assertEquals("scan_001", firstItem.id)
        assertEquals("Orange Juice", firstItem.productName)
        assertEquals("Today, 9:24 AM", firstItem.scanDate)
        assertEquals(R.string.verdict_healthy, firstItem.verdictLabelResId)
        assertEquals(VerdictType.CYAN, firstItem.verdictType)
    }

    @Test
    fun `when ScanCardClicked, effect is NavigateToScan`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.ScanCardClicked)
            assertEquals(HomeEffect.NavigateToScan, awaitItem())
        }
    }

    @Test
    fun `when ViewAllHistoryClicked, effect is NavigateToHistory`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.ViewAllHistoryClicked)
            assertEquals(HomeEffect.NavigateToHistory, awaitItem())
        }
    }

    @Test
    fun `when NotificationClicked, effect is NavigateToNotifications`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.NotificationClicked)
            assertEquals(HomeEffect.NavigateToNotifications, awaitItem())
        }
    }

    @Test
    fun `when HistoryItemClicked, effect is NavigateToScanResult with correct id`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.HistoryItemClicked("scan_002"))
            assertEquals(HomeEffect.NavigateToScanResult("scan_002"), awaitItem())
        }
    }

    @Test
    fun `when BottomNavTabClicked to CALORIES, effect is NavigateToCalories and selectedTab stays HOME`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.BottomNavTabClicked(BottomNavTab.CALORIES))
            assertEquals(HomeEffect.NavigateToCalories, awaitItem())
        }
        // Home is the only tab rendered inline; the retained ViewModel must keep
        // highlighting HOME so returning here doesn't show a stale tab.
        assertEquals(BottomNavTab.HOME, viewModel.state.value.selectedTab)
    }

    @Test
    fun `when BottomNavTabClicked to SCAN, effect is NavigateToScan and selectedTab stays HOME`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.BottomNavTabClicked(BottomNavTab.SCAN))
            assertEquals(HomeEffect.NavigateToScan, awaitItem())
        }
        assertEquals(BottomNavTab.HOME, viewModel.state.value.selectedTab)
    }

    @Test
    fun `when BottomNavTabClicked to SHOPPING, selectedTab is updated and no effect is emitted`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.BottomNavTabClicked(BottomNavTab.SHOPPING))
            expectNoEvents()
        }
        assertEquals(BottomNavTab.HOME, viewModel.state.value.selectedTab)
    }

    @Test
    fun `when BottomNavTabClicked to PROFILE, selectedTab is updated and no effect is emitted`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.BottomNavTabClicked(BottomNavTab.PROFILE))
            expectNoEvents()
        }
        assertEquals(BottomNavTab.HOME, viewModel.state.value.selectedTab)
    }

    @Test
    fun `when BottomNavTabClicked to HOME, selectedTab remains HOME and no effect is emitted`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.BottomNavTabClicked(BottomNavTab.HOME))
            expectNoEvents()
        }
        assertEquals(BottomNavTab.HOME, viewModel.state.value.selectedTab)
    }
}
