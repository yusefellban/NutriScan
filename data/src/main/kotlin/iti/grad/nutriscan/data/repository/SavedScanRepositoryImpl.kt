package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.entity.FlaggedIngredientLocalModel
import iti.grad.nutriscan.data.db.entity.NutritionFactsLocalModel
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.model.ScanStatus
import iti.grad.nutriscan.domain.scan.model.FoodSafetyResponse
import iti.grad.nutriscan.domain.scan.model.ScanFlaggedIngredient
import iti.grad.nutriscan.domain.scan.model.NutritionFacts
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import iti.grad.nutriscan.data.di.IoDispatcher
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SavedScanRepositoryImpl @Inject constructor(
    private val savedScanDao: SavedScanDao,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ISavedScanRepository {

    override suspend fun saveScan(scanResult: ScanResult): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val entity = SavedScanEntity(
                    scanId = scanResult.scanId,
                    scannedAt = scanResult.scannedAt,
                    imageUrl = scanResult.imageUrl,
                    verdict = scanResult.foodSafetyResponse?.verdict?.name,
                    summary = scanResult.foodSafetyResponse?.summary,
                    productName = scanResult.productName,
                    flaggedIngredientsJson = scanResult.foodSafetyResponse?.flaggedIngredients?.let { ingredients ->
                        val mapped = ingredients.map { ing ->
                            FlaggedIngredientLocalModel(
                                ingredient = ing.ingredient,
                                reason = ing.reason,
                                type = ing.type,
                                name = ing.name
                            )
                        }
                        json.encodeToString(mapped)
                    },
                    nutritionFactsJson = scanResult.nutritionFacts?.let { nutrition ->
                        val mapped = NutritionFactsLocalModel(
                            calories = nutrition.calories,
                            proteinGrams = nutrition.proteinGrams,
                            carbsGrams = nutrition.carbsGrams,
                            fatG = nutrition.fatG,
                            fiberGrams = nutrition.fiberGrams,
                            sugarG = nutrition.sugarG,
                            sodiumMg = nutrition.sodiumMg
                        )
                        json.encodeToString(mapped)
                    }
                )
                savedScanDao.insertScan(entity)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getSavedScans(): Flow<List<ScanResult>> {
        return savedScanDao.getAllSavedScans().map { entities ->
            entities.map { entity ->
                val ingredientsList = entity.flaggedIngredientsJson?.let { jsonStr ->
                    try {
                        val list = json.decodeFromString<List<FlaggedIngredientLocalModel>>(jsonStr)
                        list.map { model ->
                            ScanFlaggedIngredient(
                                ingredient = model.ingredient,
                                reason = model.reason,
                                type = model.type,
                                name = model.name
                            )
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()

                val nutritionFacts = entity.nutritionFactsJson?.let { jsonStr ->
                    try {
                        val model = json.decodeFromString<NutritionFactsLocalModel>(jsonStr)
                        NutritionFacts(
                            calories = model.calories,
                            proteinGrams = model.proteinGrams,
                            carbsGrams = model.carbsGrams,
                            fatG = model.fatG,
                            fiberGrams = model.fiberGrams,
                            sugarG = model.sugarG,
                            sodiumMg = model.sodiumMg
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val verdict = try {
                    entity.verdict?.let { ProductVerdict.valueOf(it) }
                } catch (e: Exception) {
                    null
                }

                ScanResult(
                    scanId = entity.scanId,
                    status = ScanStatus.COMPLETED,
                    scannedAt = entity.scannedAt,
                    imageUrl = entity.imageUrl,
                    productName = entity.productName,
                    foodSafetyResponse = if (verdict != null) {
                        FoodSafetyResponse(
                            verdict = verdict,
                            flaggedIngredients = ingredientsList,
                            summary = entity.summary
                        )
                    } else null,
                    nutritionFacts = nutritionFacts
                )
            }
        }
    }

    override suspend fun deleteScan(scanId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                savedScanDao.deleteScanById(scanId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSavedScanById(scanId: String): Result<ScanResult?> {
        return withContext(ioDispatcher) {
            try {
                val entity = savedScanDao.getSavedScanById(scanId) ?: return@withContext Result.success(null)
                
                val ingredientsList = entity.flaggedIngredientsJson?.let { jsonStr ->
                    try {
                        val list = json.decodeFromString<List<FlaggedIngredientLocalModel>>(jsonStr)
                        list.map { model ->
                            ScanFlaggedIngredient(
                                ingredient = model.ingredient,
                                reason = model.reason,
                                type = model.type,
                                name = model.name
                            )
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()

                val nutritionFacts = entity.nutritionFactsJson?.let { jsonStr ->
                    try {
                        val model = json.decodeFromString<NutritionFactsLocalModel>(jsonStr)
                        NutritionFacts(
                            calories = model.calories,
                            proteinGrams = model.proteinGrams,
                            carbsGrams = model.carbsGrams,
                            fatG = model.fatG,
                            fiberGrams = model.fiberGrams,
                            sugarG = model.sugarG,
                            sodiumMg = model.sodiumMg
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val verdict = try {
                    entity.verdict?.let { ProductVerdict.valueOf(it) }
                } catch (e: Exception) {
                    null
                }

                val scanResult = ScanResult(
                    scanId = entity.scanId,
                    status = ScanStatus.COMPLETED,
                    scannedAt = entity.scannedAt,
                    imageUrl = entity.imageUrl,
                    productName = entity.productName,
                    foodSafetyResponse = if (verdict != null) {
                        FoodSafetyResponse(
                            verdict = verdict,
                            flaggedIngredients = ingredientsList,
                            summary = entity.summary
                        )
                    } else null,
                    nutritionFacts = nutritionFacts
                )
                Result.success(scanResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
