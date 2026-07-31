package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import iti.grad.nutriscan.data.remote.api.ScanApiService
import iti.grad.nutriscan.data.remote.dto.PageDto
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Covers the per-account scoping of [ScanRepositoryImpl]'s in-memory recent-scans cache. The cache
 * is a plain field on a singleton, so logout's `database.clearAllTables()` never cleared it and the
 * next account signing in was served the previous account's scans with no request made at all.
 */
class ScanRepositoryImplTest {

    private lateinit var openFoodFactsApiService: OpenFoodFactsApiService
    private lateinit var scanApiService: ScanApiService
    private lateinit var authRepository: IAuthRepository
    private lateinit var repository: ScanRepositoryImpl

    private fun page(vararg scanIds: String) = PageDto(
        content = scanIds.map { ScanHistoryItemDto(scanId = it, status = "COMPLETED") },
        totalElements = scanIds.size.toLong(),
        totalPages = 1,
        number = 0,
        size = scanIds.size,
        first = true,
        last = true,
        empty = scanIds.isEmpty(),
    )

    @BeforeEach
    fun setup() {
        openFoodFactsApiService = mockk()
        scanApiService = mockk()
        authRepository = mockk()
        repository = ScanRepositoryImpl(
            openFoodFactsApiService,
            scanApiService,
            authRepository,
            UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `a second account never receives the first account's cached scans`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-a"
        coEvery { scanApiService.getRecentScans(0, 1) } returns page("a-scan")
        assertEquals("a-scan", repository.getRecentScans(0, 1).getOrThrow().single().scanId)

        coEvery { authRepository.getCurrentUserId() } returns "user-b"
        coEvery { scanApiService.getRecentScans(0, 1) } returns page("b-scan")

        val result = repository.getRecentScans(0, 1).getOrThrow()

        assertEquals("b-scan", result.single().scanId)
        // The whole bug was the cache short-circuiting this second call away entirely.
        coVerify(exactly = 2) { scanApiService.getRecentScans(0, 1) }
    }

    @Test
    fun `the same account still reads from cache without a second request`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-a"
        coEvery { scanApiService.getRecentScans(0, 1) } returns page("a-scan")

        repository.getRecentScans(0, 1)
        val second = repository.getRecentScans(0, 1).getOrThrow()

        assertEquals("a-scan", second.single().scanId)
        coVerify(exactly = 1) { scanApiService.getRecentScans(0, 1) }
    }

    @Test
    fun `signing out drops the cache instead of serving it to the next caller`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-a"
        coEvery { scanApiService.getRecentScans(0, 1) } returns page("a-scan")
        repository.getRecentScans(0, 1)

        coEvery { authRepository.getCurrentUserId() } returns null
        coEvery { scanApiService.getRecentScans(0, 1) } returns page("fresh")

        assertEquals("fresh", repository.getRecentScans(0, 1).getOrThrow().single().scanId)
    }
}
