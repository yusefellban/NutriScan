package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.repository.mapper.today
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.domain.foodlog.model.FoodLogEntry
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FoodLogRepositoryImpl @Inject constructor(
    private val dao: FoodLogDao,
    private val authRepository: IAuthRepository,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IFoodLogRepository {

    override fun observeTodayFoodLog(): Flow<List<FoodLogEntry>> = flow {
        emitAll(
            dao.observeByUserAndDate(resolveUserId(), today().toString())
                .map { entities -> entities.map { it.toDomain() } }
        )
    }.flowOn(ioDispatcher)

    override suspend fun addFoodEntry(entry: FoodLogEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            // The backend's scanId is FoodLogEntry.productId (the scanned product's id) — id is a
            // locally-generated UUID, never sent to the backend. See SavedViewModel.addToFoodLog,
            // which sets id = UUID.randomUUID() and productId = the scanned product's own id.
            val scanId = entry.productId ?: entry.id
            dao.insert(entry.toEntity(userId))

            val pushResult = dailyTrackingRepository.pushMeal(entry.loggedDate, scanId, mealCnt = 1)
            if (pushResult.isFailure) {
                dao.insert(entry.toEntity(userId).copy(pendingSync = true))
            }
        }
    }

    override suspend fun addFoodEntryLocalOnly(entry: FoodLogEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.insert(entry.toEntity(resolveUserId()))
        }
    }

    override suspend fun removeFoodEntry(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val existing = dao.getByIdForUser(entryId, userId)
            val scanId = existing?.productId ?: entryId
            dao.markDeletedForUser(entryId, userId)

            val deleteResult = dailyTrackingRepository.deleteMeal(today(), scanId)
            if (deleteResult.isSuccess) {
                dao.hardDelete(entryId)
            }
        }
    }

    // ponytail: falls back to a shared local-device id when logged out (e.g. testing against
    // the still-mock Saved catalog) so the food log stays usable before real auth is wired
    // through end to end. Swap for a hard "not authenticated" failure once that's in place.
    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId() ?: LOCAL_USER_ID

    private companion object {
        const val LOCAL_USER_ID = "local_device_user"
    }
}
