package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import iti.grad.nutriscan.data.remote.api.ScanApiService
import iti.grad.nutriscan.data.remote.dto.PageDto
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.data.remote.dto.ScanResultResponseDto
import iti.grad.nutriscan.data.remote.dto.UpdateScanDto
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.model.FoodSafetyResponse
import iti.grad.nutriscan.domain.scan.model.NutritionFacts
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeSavedScanDao : SavedScanDao {
    val entries = MutableStateFlow<List<SavedScanEntity>>(emptyList())

    override suspend fun insertScan(scan: SavedScanEntity) {
        entries.value = entries.value.filterNot { it.scanId == scan.scanId } + scan
    }

    override fun getAllSavedScans(userId: String): Flow<List<SavedScanEntity>> =
        MutableStateFlow(entries.value.filter { it.userId == userId && !it.deleted })

    override suspend fun getSavedScanById(scanId: String, userId: String): SavedScanEntity? =
        entries.value.find { it.scanId == scanId && it.userId == userId }

    override suspend fun markDeletedForUser(scanId: String, userId: String) {
        entries.value = entries.value.map {
            if (it.scanId == scanId && it.userId == userId) it.copy(deleted = true, pendingSync = true) else it
        }
    }

    override suspend fun getPendingSyncEntries(userId: String): List<SavedScanEntity> =
        entries.value.filter { it.pendingSync && it.userId == userId }

    override suspend fun clearPendingSync(scanId: String) {
        entries.value = entries.value.map { if (it.scanId == scanId) it.copy(pendingSync = false) else it }
    }

    override suspend fun hardDelete(scanId: String) {
        entries.value = entries.value.filterNot { it.scanId == scanId }
    }

    override suspend fun deleteStaleSynced(userId: String, remoteScanIds: List<String>) {
        entries.value = entries.value.filterNot {
            it.userId == userId && !it.pendingSync && it.scanId !in remoteScanIds
        }
    }
}

class SavedScanRepositoryImplTest {

    private lateinit var dao: FakeSavedScanDao
    private lateinit var scanApiService: ScanApiService
    private lateinit var authRepository: IAuthRepository
    private lateinit var repository: SavedScanRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun scanResult(id: String = "scan-1") = ScanResult(
        scanId = id,
        status = ScanStatus.COMPLETED,
        scannedAt = "2026-07-27T10:00:00Z",
        imageUrl = null,
        foodSafetyResponse = FoodSafetyResponse(ProductVerdict.SAFE, emptyList(), null),
        nutritionFacts = NutritionFacts(100L, 1f, 2f, 3f, 4f, 5f, 6f),
        productName = "Apple",
    )

    @BeforeEach
    fun setup() {
        dao = FakeSavedScanDao()
        scanApiService = mockk()
        authRepository = mockk()
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { scanApiService.getFavoriteScans(any(), any()) } returns PageDto(
            content = emptyList(), totalElements = 0, totalPages = 0, number = 0, size = 200, first = true, last = true, empty = true
        )
        repository = SavedScanRepositoryImpl(dao, scanApiService, authRepository, Json { ignoreUnknownKeys = true }, testDispatcher)
    }

    @Test
    fun `saveScan writes locally then clears pendingSync once the backend confirms`() = runTest(testDispatcher) {
        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = true)) } returns ScanResultResponseDto(scanId = "scan-1", status = "COMPLETED")

        val result = repository.saveScan(scanResult())

        assertTrue(result.isSuccess)
        assertEquals(0, dao.getPendingSyncEntries("user-1").size)
        assertTrue(dao.entries.value.any { it.scanId == "scan-1" })
    }

    @Test
    fun `saveScan keeps pendingSync when the backend push fails`() = runTest(testDispatcher) {
        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = true)) } throws RuntimeException("offline")

        repository.saveScan(scanResult())

        assertEquals(1, dao.getPendingSyncEntries("user-1").size)
    }

    @Test
    fun `deleteScan soft-deletes then hard-deletes once the backend confirms`() = runTest(testDispatcher) {
        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = true)) } returns ScanResultResponseDto(scanId = "scan-1", status = "COMPLETED")
        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = false)) } returns ScanResultResponseDto(scanId = "scan-1", status = "COMPLETED")
        repository.saveScan(scanResult())

        val result = repository.deleteScan("scan-1")

        assertTrue(result.isSuccess)
        assertTrue(dao.entries.value.none { it.scanId == "scan-1" })
    }

    @Test
    fun `getSavedScans seeds Room from the backend favorites list on first collection`() = runTest(testDispatcher) {
        coEvery { scanApiService.getFavoriteScans(0, 100) } returns PageDto(
            content = listOf(
                ScanHistoryItemDto(
                    scanId = "remote-1", imageUrl = null, verdict = ProductVerdict.SAFE,
                    scannedAt = "2026-07-27T10:00:00Z", productName = "Banana", calories = 90L, status = "COMPLETED"
                )
            ),
            totalElements = 1, totalPages = 1, number = 0, size = 100, first = true, last = true, empty = false,
        )

        val scans = repository.getSavedScans().first()

        assertEquals(1, scans.size)
        assertEquals("remote-1", scans.first().scanId)
    }

    @Test
    fun `getSavedScans drops local rows the backend no longer lists as favorited`() = runTest(testDispatcher) {
        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = true)) } returns ScanResultResponseDto(scanId = "scan-1", status = "COMPLETED")
        repository.saveScan(scanResult())

        val scans = repository.getSavedScans().first()

        assertTrue(scans.isEmpty())
    }

    @Test
    fun `retryPendingSync clears a pending save once the backend confirms`() = runTest(testDispatcher) {
        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = true)) } throws RuntimeException("offline")
        repository.saveScan(scanResult())
        assertEquals(1, dao.getPendingSyncEntries("user-1").size)

        coEvery { scanApiService.updateScan("scan-1", UpdateScanDto(favorite = true)) } returns ScanResultResponseDto(scanId = "scan-1", status = "COMPLETED")
        val result = repository.retryPendingSync()

        assertTrue(result.isSuccess)
        assertEquals(0, dao.getPendingSyncEntries("user-1").size)
    }
}
