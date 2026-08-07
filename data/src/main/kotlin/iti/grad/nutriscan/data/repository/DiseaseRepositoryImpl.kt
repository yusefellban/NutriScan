package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.DiseaseDao
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.remote.datasource.IDiseaseRemoteDataSource
import iti.grad.nutriscan.data.remote.dto.ApiErrorDto
import iti.grad.nutriscan.domain.disease.model.Disease
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class DiseaseRepositoryImpl @Inject constructor(
    private val remoteDataSource: IDiseaseRemoteDataSource,
    private val diseaseDao: DiseaseDao,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IDiseaseRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Polls the backend catalog every [SYNC_INTERVAL_MS] for as long as at least one collector is
     * subscribed, so a disease added/edited server-side shows up without an app restart. Catalog
     * data rarely changes, so the interval is long — this is cheap insurance, not a live feed.
     * [shareIn] multicasts this single poll loop to every collector — [DiseaseRepositoryImpl] is a
     * singleton, so without it, two screens observing diseases at once would each spin up their
     * own independent poller. */
    private val diseasesFlow: Flow<List<Disease>> = channelFlow {
        syncDiseases()
        launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                syncDiseases()
            }
        }
        diseaseDao.getDiseasesFlow()
            .map { entities -> entities.map { entity -> Disease(id = entity.id, name = entity.name, description = entity.description) } }
            .collect { send(it) }
    }.shareIn(repositoryScope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), replay = 1)

    override fun getDiseasesOffline(): Flow<List<Disease>> = diseasesFlow

    override suspend fun syncDiseases(): Result<Unit> {
        return try {
            val response = remoteDataSource.getDiseases()
            if (response.isSuccessful) {
                val entities = response.body().orEmpty().map { dto ->
                    DiseaseEntity(id = dto.id, name = dto.name, description = dto.description)
                }
                diseaseDao.insertDiseases(entities)
                Result.success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Sync diseases failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while syncing diseases")
            Result.failure(e)
        }
    }

    override suspend fun getDiseases(): Result<List<Disease>> {
        return try {
            val response = remoteDataSource.getDiseases()
            if (response.isSuccessful) {
                val diseases = response.body().orEmpty().map { dto ->
                    Disease(id = dto.id, name = dto.name, description = dto.description)
                }
                Result.success(diseases)
            } else {
                val rawError = response.errorBody()?.string()
                Timber.e("Get diseases failed with code: ${response.code()}, errorBody: $rawError")
                Result.failure(Exception(parseErrorMessage(rawError)))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while fetching diseases")
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
        const val SHARE_STOP_TIMEOUT_MS = 5_000L
    }
}
