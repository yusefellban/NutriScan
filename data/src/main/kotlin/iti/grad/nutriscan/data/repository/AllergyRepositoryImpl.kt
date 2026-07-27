package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.AllergyDao
import iti.grad.nutriscan.data.db.entity.AllergyEntity
import iti.grad.nutriscan.data.remote.datasource.IAllergyRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.domain.allergy.model.Allergy
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class AllergyRepositoryImpl @Inject constructor(
    private val remoteDataSource: IAllergyRemoteDataSource,
    private val allergyDao: AllergyDao,
    private val json: Json
) : IAllergyRepository {

    /** Polls the backend catalog every [SYNC_INTERVAL_MS] for as long as this flow is collected,
     * so an allergy added/edited server-side shows up without an app restart. Catalog data rarely
     * changes, so the interval is long — this is cheap insurance, not a live feed. */
    override fun getAllergiesOffline(): Flow<List<Allergy>> = channelFlow {
        syncAllergies()
        launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                syncAllergies()
            }
        }
        allergyDao.getAllergiesFlow()
            .map { entities -> entities.map { entity -> Allergy(id = entity.id, name = entity.name, description = entity.description) } }
            .collect { send(it) }
    }

    override suspend fun syncAllergies(): Result<Unit> {
        return try {
            val response = remoteDataSource.getAllergies()
            if (response.isSuccessful) {
                val entities = response.body().orEmpty().map { dto ->
                    AllergyEntity(id = dto.id, name = dto.name, description = dto.description)
                }
                allergyDao.insertAllergies(entities)
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Sync allergies failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while syncing allergies")
            Result.failure(e)
        }
    }

    override suspend fun getAllergies(): Result<List<Allergy>> {
        return try {
            val response = remoteDataSource.getAllergies()
            if (response.isSuccessful) {
                val allergies = response.body().orEmpty().map { dto ->
                    Allergy(id = dto.id, name = dto.name, description = dto.description)
                }
                Result.success(allergies)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Get allergies failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while fetching allergies")
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "An unexpected error occurred."
        return try {
            val apiError = json.decodeFromString<ApiErrorDto>(errorBody)
            apiError.message
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    private companion object {
        const val SYNC_INTERVAL_MS = 300_000L
    }
}
