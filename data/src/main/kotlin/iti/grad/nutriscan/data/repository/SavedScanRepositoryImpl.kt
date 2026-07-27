package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.entity.FlaggedIngredientLocalModel
import iti.grad.nutriscan.data.db.entity.NutritionFactsLocalModel
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.api.ScanApiService
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.data.remote.dto.UpdateScanDto
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.scan.model.FoodSafetyResponse
import iti.grad.nutriscan.domain.scan.model.NutritionFacts
import iti.grad.nutriscan.domain.scan.model.ScanFlaggedIngredient
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SavedScanRepositoryImpl @Inject constructor(
    private val savedScanDao: SavedScanDao,
    private val scanApiService: ScanApiService,
    private val authRepository: IAuthRepository,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ISavedScanRepository {

    override suspend fun saveScan(scanResult: ScanResult): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            savedScanDao.insertScan(scanResult.toEntity(userId, pendingSync = true))
            val pushed = runCatching { scanApiService.updateScan(scanResult.scanId, UpdateScanDto(favorite = true)) }
            pushed.exceptionOrNull()?.let {
                android.util.Log.e("SavedScanSync", "PATCH favorite=true failed for ${scanResult.scanId}", it)
            }
            if (pushed.isSuccess) savedScanDao.clearPendingSync(scanResult.scanId)
        }
    }

    /** Polls the backend every [RECONCILE_INTERVAL_MS] for as long as this flow is collected (i.e.
     * while a screen showing saved scans is open), so a favorite added/removed on another device
     * shows up here without needing an app restart — this backend is plain REST, no
     * push/WebSocket to listen on instead. Each reconcile writes into Room, and Room's own flow
     * (collected concurrently below) emits the update immediately. */
    override fun getSavedScans(): Flow<List<ScanResult>> = channelFlow {
        val userId = resolveUserId()
        reconcileFromBackend(userId)
        launch {
            while (isActive) {
                delay(RECONCILE_INTERVAL_MS)
                reconcileFromBackend(userId)
            }
        }
        savedScanDao.getAllSavedScans(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .collect { send(it) }
    }.flowOn(ioDispatcher)

    override suspend fun deleteScan(scanId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            savedScanDao.markDeletedForUser(scanId, userId)
            val pushed = runCatching { scanApiService.updateScan(scanId, UpdateScanDto(favorite = false)) }
            if (pushed.isSuccess) savedScanDao.hardDelete(scanId)
        }
    }

    override suspend fun getSavedScanById(scanId: String): Result<ScanResult?> = withContext(ioDispatcher) {
        runCatchingCancellable {
            savedScanDao.getSavedScanById(scanId, resolveUserId())?.toDomain()
        }
    }

    override suspend fun retryPendingSync(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            for (entity in savedScanDao.getPendingSyncEntries(userId)) {
                val result = runCatching {
                    scanApiService.updateScan(entity.scanId, UpdateScanDto(favorite = !entity.deleted))
                }
                if (result.isSuccess) {
                    if (entity.deleted) savedScanDao.hardDelete(entity.scanId) else savedScanDao.clearPendingSync(entity.scanId)
                }
            }
        }
    }

    /** Seeds Room from the backend's favorites list (needed after reinstall, when Room is empty)
     * and drops local rows the backend no longer lists as favorited — mirrors the un-favorite
     * happening from another device. Best-effort: failures (offline) just fall back to whatever
     * is already in Room. */
    private suspend fun reconcileFromBackend(userId: String) {
        runCatching {
            val response = scanApiService.getFavoriteScans(page = 0, size = FAVORITES_PAGE_SIZE)
            val remoteScanIds = response.content.map { it.scanId }
            response.content.forEach { savedScanDao.insertScan(it.toEntity(userId)) }
            savedScanDao.deleteStaleSynced(userId, remoteScanIds)
        }.onFailure {
            android.util.Log.e("SavedScanSync", "GET /scans/favorites reconcile failed for $userId", it)
        }
    }

    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId() ?: LOCAL_USER_ID

    private fun ScanResult.toEntity(userId: String, pendingSync: Boolean = false) = SavedScanEntity(
        scanId = scanId,
        userId = userId,
        scannedAt = scannedAt,
        imageUrl = imageUrl,
        verdict = foodSafetyResponse?.verdict?.name,
        summary = foodSafetyResponse?.summary,
        productName = productName,
        flaggedIngredientsJson = foodSafetyResponse?.flaggedIngredients?.let { ingredients ->
            json.encodeToString(ingredients.map {
                FlaggedIngredientLocalModel(ingredient = it.ingredient, reason = it.reason, type = it.type, name = it.name)
            })
        },
        nutritionFactsJson = nutritionFacts?.let { json.encodeToString(it.toLocalModel()) },
        pendingSync = pendingSync,
    )

    /** Reconciled rows only carry the summary shape the backend's favorites list returns (no
     * flagged-ingredients/full nutrition breakdown) — the Saved screen only reads
     * productName/imageUrl/verdict/calories, so a minimal nutrition model with just calories set
     * is enough; product-detail navigation re-fetches the full [ScanResult] by id separately. */
    private fun ScanHistoryItemDto.toEntity(userId: String) = SavedScanEntity(
        scanId = scanId,
        userId = userId,
        scannedAt = scannedAt,
        imageUrl = imageUrl,
        verdict = verdict?.name,
        summary = null,
        productName = productName,
        flaggedIngredientsJson = null,
        nutritionFactsJson = json.encodeToString(
            NutritionFactsLocalModel(
                calories = calories ?: 0L,
                proteinGrams = 0f,
                carbsGrams = 0f,
                fatG = 0f,
                fiberGrams = 0f,
                sugarG = 0f,
                sodiumMg = 0f,
            )
        ),
        pendingSync = false,
    )

    private fun NutritionFacts.toLocalModel() = NutritionFactsLocalModel(
        calories = calories,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatG = fatG,
        fiberGrams = fiberGrams,
        sugarG = sugarG,
        sodiumMg = sodiumMg,
    )

    private fun SavedScanEntity.toDomain(): ScanResult {
        val ingredients = flaggedIngredientsJson?.let { jsonStr ->
            runCatching { json.decodeFromString<List<FlaggedIngredientLocalModel>>(jsonStr) }.getOrNull()
        }.orEmpty().map { ScanFlaggedIngredient(it.ingredient, it.reason, it.type, it.name) }

        val nutritionFacts = nutritionFactsJson?.let { jsonStr ->
            runCatching { json.decodeFromString<NutritionFactsLocalModel>(jsonStr) }.getOrNull()
        }?.let {
            NutritionFacts(it.calories, it.proteinGrams, it.carbsGrams, it.fatG, it.fiberGrams, it.sugarG, it.sodiumMg)
        }

        val parsedVerdict = verdict?.let { runCatching { ProductVerdict.valueOf(it) }.getOrNull() }

        return ScanResult(
            scanId = scanId,
            status = ScanStatus.COMPLETED,
            scannedAt = scannedAt,
            imageUrl = imageUrl,
            productName = productName,
            foodSafetyResponse = parsedVerdict?.let { FoodSafetyResponse(it, ingredients, summary) },
            nutritionFacts = nutritionFacts,
            favorite = true,
        )
    }

    private companion object {
        const val LOCAL_USER_ID = "local_device_user"
        const val FAVORITES_PAGE_SIZE = 100
        const val RECONCILE_INTERVAL_MS = 15_000L
    }
}
