package iti.grad.nutriscan.presentation.home

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.dailytracking.usecase.ReconcileTodayUseCase
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.GetRecentScansUseCase
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.presentation.common.model.VerdictType
import iti.grad.nutriscan.presentation.home.viewmodel.HomeViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelHistoryStatusTest {

    private val testDispatcher = StandardTestDispatcher()

    private val userRepository: IUserRepository = mockk()
    private val getRecentScansUseCase: GetRecentScansUseCase = mockk()
    private val reconcileTodayUseCase: ReconcileTodayUseCase = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed recent scan is displayed as Failed not Safe`() = runTest {
        every { userRepository.getUserData() } returns MutableStateFlow(null)
        coEvery { userRepository.fetchAndSyncProfile() } returns Result.success(Unit)
        coEvery { reconcileTodayUseCase.invoke() } returns Result.success(Unit)
        coEvery {
            getRecentScansUseCase(page = 0, size = 3, date = null, verdict = null)
        } returns Result.success(
            listOf(
                ScanHistoryEntry(
                    scanId = "failed_home_1",
                    imageUrl = null,
                    verdict = ProductVerdict.SAFE,
                    scannedAt = null,
                    productName = "Any product",
                    calories = null,
                    status = ScanStatus.FAILED,
                )
            )
        )

        val viewModel = HomeViewModel(
            userRepository = userRepository,
            getRecentScansUseCase = getRecentScansUseCase,
            reconcileTodayUseCase = reconcileTodayUseCase,
        )
        advanceUntilIdle()

        val item = viewModel.state.value.recentHistory.first()
        assertEquals(R.string.scan_status_failed, item.verdictLabelResId)
        assertEquals(VerdictType.RED, item.verdictType)
    }

    @Test
    fun `completed recent scan with null verdict is displayed as Unknown`() = runTest {
        every { userRepository.getUserData() } returns MutableStateFlow(null)
        coEvery { userRepository.fetchAndSyncProfile() } returns Result.success(Unit)
        coEvery { reconcileTodayUseCase.invoke() } returns Result.success(Unit)
        coEvery {
            getRecentScansUseCase(page = 0, size = 3, date = null, verdict = null)
        } returns Result.success(
            listOf(
                ScanHistoryEntry(
                    scanId = "unknown_home_1",
                    imageUrl = null,
                    verdict = null,
                    scannedAt = null,
                    productName = "Any product",
                    calories = null,
                    status = ScanStatus.COMPLETED,
                )
            )
        )

        val viewModel = HomeViewModel(
            userRepository = userRepository,
            getRecentScansUseCase = getRecentScansUseCase,
            reconcileTodayUseCase = reconcileTodayUseCase,
        )
        advanceUntilIdle()

        val item = viewModel.state.value.recentHistory.first()
        assertEquals(R.string.verdict_unknown, item.verdictLabelResId)
        assertEquals(VerdictType.CYAN, item.verdictType)
    }
}
