package iti.grad.nutriscan.presentation.home

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.GetRecentScansUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.ReconcileTodayUseCase
import iti.grad.nutriscan.domain.user.model.User
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import iti.grad.nutriscan.presentation.common.model.VerdictType
import iti.grad.nutriscan.presentation.home.state.HomeEffect
import iti.grad.nutriscan.presentation.home.state.HomeEvent
import iti.grad.nutriscan.presentation.home.viewmodel.HomeViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val userData = MutableStateFlow<User?>(null)
    private val userRepository: IUserRepository = mockk {
        coEvery { fetchAndSyncProfile() } returns Result.success(Unit)
        every { getUserData() } returns userData
    }
    private val getRecentScansUseCase: GetRecentScansUseCase = mockk()
    private val reconcileTodayUseCase: ReconcileTodayUseCase = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getRecentScansUseCase(page = 0, size = 3) } returns Result.success(emptyList())
        coEvery { reconcileTodayUseCase() } returns Result.success(Unit)
        viewModel = HomeViewModel(userRepository, getRecentScansUseCase, reconcileTodayUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state before scans load has empty history and no user name`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertEquals("", state.userName)
        assertTrue(state.recentHistory.isEmpty())
    }

    @Test
    fun `when recent scans load, history is mapped with correct verdict info`() = runTest(testDispatcher) {
        coEvery { getRecentScansUseCase(page = 0, size = 3) } returns Result.success(
            listOf(
                ScanHistoryEntry(
                    scanId = "scan_001",
                    imageUrl = null,
                    verdict = null,
                    scannedAt = null,
                    productName = "Orange Juice",
                    calories = null,
                    status = ScanStatus.COMPLETED,
                )
            )
        )
        viewModel = HomeViewModel(userRepository, getRecentScansUseCase, reconcileTodayUseCase)
        testScheduler.advanceUntilIdle()

        val firstItem = viewModel.state.value.recentHistory[0]
        assertEquals("scan_001", firstItem.id)
        assertEquals("Orange Juice", firstItem.productName)
        assertEquals(R.string.verdict_safe, firstItem.verdictLabelResId)
        assertEquals(VerdictType.CYAN, firstItem.verdictType)
    }

    @Test
    fun `when repository emits a user, userName and avatarUrl update`() = runTest(testDispatcher) {
        userData.value = User(
            id = "1",
            firstName = "Noureldeen",
            lastName = null,
            email = "noureldeen@example.com",
            gender = null,
            dateOfBirth = null,
            heightCm = null,
            weightKg = null,
            diseaseIds = emptyList(),
            allergyIds = emptyList(),
            avatarUrl = "https://example.com/avatar.png",
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Noureldeen", state.userName)
        assertEquals("https://example.com/avatar.png", state.avatarUrl)
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
    fun `when HealthNewsClicked, effect is NavigateToNews`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.HealthNewsClicked)
            assertEquals(HomeEffect.NavigateToNews, awaitItem())
        }
    }

    @Test
    fun `when ChatWithAiClicked, effect is NavigateToChatWithAi`() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.ChatWithAiClicked)
            assertEquals(HomeEffect.NavigateToChatWithAi, awaitItem())
        }
    }

    @Test
    fun `Refreshed re-pulls the feed and clears isRefreshing when it finishes`() = runTest(testDispatcher) {
        coEvery { getRecentScansUseCase(page = 0, size = 3) } returns Result.success(emptyList())

        viewModel.onEvent(HomeEvent.Refreshed)
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isRefreshing)
        coVerify { reconcileTodayUseCase() }
    }

    @Test
    fun `Refreshed surfaces a failure as historyError instead of leaving the spinner up`() = runTest(testDispatcher) {
        coEvery { getRecentScansUseCase(page = 0, size = 3) } returns Result.failure(RuntimeException("offline"))

        viewModel.onEvent(HomeEvent.Refreshed)
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isRefreshing)
        assertEquals("offline", viewModel.state.value.historyError)
    }
}
