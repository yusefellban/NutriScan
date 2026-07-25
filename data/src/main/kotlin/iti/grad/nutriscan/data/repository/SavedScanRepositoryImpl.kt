package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
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
                    flaggedIngredientsJson = scanResult.foodSafetyResponse?.flaggedIngredients?.let {
                        // Normally we'd map this back to a DTO before serializing, 
                        // but since it's just local cache we can serialize the domain model 
                        // if we make it @Serializable. Or just map to DTO manually here.
                        // For simplicity, we'll serialize a simple representation or make a Local model.
                        // Let's use a quick wrapper class or map it.
                        // To avoid adding @Serializable to domain models, we map to a simple list of maps.
                        val mapped = it.map { ing ->
                            mapOf(
                                "ingredient" to ing.ingredient,
                                "reason" to ing.reason,
                                "type" to ing.type,
                                "name" to ing.name.joinToString(",")
                            )
                        }
                        json.encodeToString(mapped)
                    },
                    nutritionFactsJson = scanResult.nutritionFacts?.let {
                        val map = mapOf(
                            "calories" to it.calories.toString(),
                            "proteinGrams" to it.proteinGrams.toString(),
                            "carbsGrams" to it.carbsGrams.toString(),
                            "fatG" to it.fatG.toString(),
                            "fiberGrams" to it.fiberGrams.toString(),
                            "sugarG" to it.sugarG.toString(),
                            "sodiumMg" to it.sodiumMg.toString()
                        )
                        json.encodeToString(map)
                    }
                )
                savedScanDao.insertScan(entity)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
