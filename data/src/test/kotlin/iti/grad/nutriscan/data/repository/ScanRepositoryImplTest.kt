package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.ScannedProductDao
import iti.grad.nutriscan.data.db.entity.ScannedProductEntity
import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import iti.grad.nutriscan.data.remote.api.ScanApiService
import iti.grad.nutriscan.data.remote.dto.OpenFoodFactsProductDto
import iti.grad.nutriscan.data.remote.dto.OpenFoodFactsResponseDto
import iti.grad.nutriscan.data.remote.dto.PageDto
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    private lateinit var scannedProductDao: ScannedProductDao
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
        scannedProductDao = mockk()
        repository = ScanRepositoryImpl(
            openFoodFactsApiService,
            scanApiService,
            authRepository,
            scannedProductDao,
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

    @Test
    fun `a successful barcode lookup caches the result for offline reuse`() = runTest {
        coEvery { openFoodFactsApiService.getProduct("111") } returns OpenFoodFactsResponseDto(
            status = 1,
            product = OpenFoodFactsProductDto(productName = "Milk", brands = "Almarai"),
        )
        coEvery { scannedProductDao.insert(any()) } returns Unit

        val result = repository.getProductByBarcode("111").getOrThrow()

        assertEquals("Milk", result.productName)
        coVerify(exactly = 1) {
            scannedProductDao.insert(
                ScannedProductEntity(
                    barcode = "111",
                    productName = "Milk",
                    brand = "Almarai",
                    imageUrl = null,
                    healthTag = null,
                )
            )
        }
    }

    @Test
    fun `a barcode lookup with no connectivity falls back to the cached product`() = runTest {
        coEvery { openFoodFactsApiService.getProduct("111") } throws java.io.IOException("no network")
        coEvery { scannedProductDao.getByBarcode("111") } returns ScannedProductEntity(
            barcode = "111",
            productName = "Milk",
            brand = "Almarai",
            imageUrl = null,
            healthTag = "A",
        )

        val result = repository.getProductByBarcode("111").getOrThrow()

        assertEquals("Milk", result.productName)
        assertEquals("A", result.healthTag)
    }

    @Test
    fun `a barcode lookup with no connectivity and no cache fails`() = runTest {
        coEvery { openFoodFactsApiService.getProduct("999") } throws java.io.IOException("no network")
        coEvery { scannedProductDao.getByBarcode("999") } returns null

        val result = repository.getProductByBarcode("999")

        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }
}
