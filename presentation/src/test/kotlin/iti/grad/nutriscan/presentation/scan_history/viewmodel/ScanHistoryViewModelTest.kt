package iti.grad.nutriscan.presentation.scan_history.viewmodel

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.usecase.GetRecentScansUseCase
import iti.grad.nutriscan.presentation.common.model.VerdictType
import iti.grad.nutriscan.presentation.scan_history.state.HistoryFilter
import iti.grad.nutriscan.presentation.scan_history.state.ScanHistoryEvent
import iti.grad.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ScanHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getRecentScansUseCase: GetRecentScansUseCase = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed scan is mapped to Failed label instead of Safe`() = runTest {
        coEvery {
            getRecentScansUseCase(page = 0, size = 8, date = null, verdict = null)
        } returns Result.success(
            listOf(
                ScanHistoryEntry(
                    scanId = "failed_1",
                    imageUrl = null,
                    verdict = ProductVerdict.SAFE,
                    scannedAt = null,
                    productName = "Broken item",
                    calories = null,
                    status = ScanStatus.FAILED,
                )
            )
        )

        val viewModel = ScanHistoryViewModel(getRecentScansUseCase)
        advanceUntilIdle()

        val item = viewModel.state.value.displayedHistoryItems.first()
        assertEquals(R.string.scan_status_failed, item.verdictLabelResId)
        assertEquals(VerdictType.RED, item.verdictType)
    }

    @Test
    fun `completed scan with null verdict is mapped to Unknown label`() = runTest {
        coEvery {
            getRecentScansUseCase(page = 0, size = 8, date = null, verdict = null)
        } returns Result.success(
            listOf(
                ScanHistoryEntry(
                    scanId = "unknown_1",
                    imageUrl = null,
                    verdict = null,
                    scannedAt = null,
                    productName = "No verdict item",
                    calories = null,
                    status = ScanStatus.COMPLETED,
                )
            )
        )

        val viewModel = ScanHistoryViewModel(getRecentScansUseCase)
        advanceUntilIdle()

        val item = viewModel.state.value.displayedHistoryItems.first()
        assertEquals(R.string.verdict_unknown, item.verdictLabelResId)
        assertEquals(VerdictType.CYAN, item.verdictType)
    }

    @Test
    fun `safe filter with selected date requests scans endpoint with both date and SAFE verdict`() = runTest {
        coEvery { getRecentScansUseCase(any(), any(), any(), any()) } returns Result.success(emptyList())

        val viewModel = ScanHistoryViewModel(getRecentScansUseCase)
        advanceUntilIdle()

        viewModel.onEvent(ScanHistoryEvent.FilterSelected(HistoryFilter.SAFE))
        advanceUntilIdle()

        val selectedDate = LocalDate.of(2026, 8, 7)
        val millis = selectedDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        viewModel.onEvent(ScanHistoryEvent.DateSelected(millis))
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            getRecentScansUseCase(page = 0, size = 8, date = "2026-08-07", verdict = "SAFE")
        }
    }
}
