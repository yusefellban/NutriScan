package iti.grad.nutriscan.data.repository

import android.util.Log
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
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
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
    private val streakRepository: IStreakRepository,
    private val dailyTrackingRepository: IDailyTrackingRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IFoodLogRepository {

    override fun observeTodayFoodLog(): Flow<List<FoodLogEntry>> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            dao.observeByUserAndDate(userId, today().toString())
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
            val existing = dao.getByUserProductAndDate(userId, scanId, entry.loggedDate.toString())

            if (existing != null) {
                val newCnt = existing.mealCnt + 1
                Log.d(TAG, "addFoodEntry: existing row for scanId=$scanId date=${entry.loggedDate} — incrementing mealCnt ${existing.mealCnt} -> $newCnt, will PUT")
                dao.updateMealCnt(existing.id, newCnt)
                val pushResult = dailyTrackingRepository.updateMeal(entry.loggedDate, scanId, newCnt)
                if (pushResult.isSuccess) {
                    dao.clearPendingSync(existing.id)
                    Log.d(TAG, "addFoodEntry: PUT mealCnt=$newCnt OK for scanId=$scanId — pendingSync cleared")
                } else {
                    Log.e(TAG, "addFoodEntry: PUT mealCnt=$newCnt FAILED for scanId=$scanId — row stays pendingSync, worker will retry", pushResult.exceptionOrNull())
                }
            } else {
                Log.d(TAG, "addFoodEntry: no existing row for scanId=$scanId date=${entry.loggedDate} — inserting mealCnt=1, will POST")
                dao.insert(entry.toEntity(userId).copy(pendingSync = true))
                val pushResult = dailyTrackingRepository.pushMeal(entry.loggedDate, scanId, mealCnt = 1)
                if (pushResult.isSuccess) {
                    dao.markBackendCreated(entry.id)
                    dao.clearPendingSync(entry.id)
                    Log.d(TAG, "addFoodEntry: POST mealCnt=1 OK for scanId=$scanId — backendCreated=true, pendingSync cleared")
                } else {
                    Log.e(TAG, "addFoodEntry: POST mealCnt=1 FAILED for scanId=$scanId — row stays pendingSync/backendCreated=false, worker will retry as POST", pushResult.exceptionOrNull())
                }
            }
            Unit
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
            val newCnt = (existing?.mealCnt ?: 1) - 1

            if (newCnt > 0) {
                Log.d(TAG, "removeFoodEntry: scanId=$scanId still has servings left — decrementing mealCnt ${existing?.mealCnt} -> $newCnt, will PUT (no DELETE)")
                dao.updateMealCnt(entryId, newCnt)
                val updateResult = dailyTrackingRepository.updateMeal(today(), scanId, newCnt)
                if (updateResult.isSuccess) {
                    dao.clearPendingSync(entryId)
                    Log.d(TAG, "removeFoodEntry: PUT mealCnt=$newCnt OK for scanId=$scanId — pendingSync cleared")
                } else {
                    Log.e(TAG, "removeFoodEntry: PUT mealCnt=$newCnt FAILED for scanId=$scanId — row stays pendingSync, worker will retry", updateResult.exceptionOrNull())
                }
            } else {
                Log.d(TAG, "removeFoodEntry: scanId=$scanId hit 0 servings — soft-deleting locally, will DELETE")
                dao.markDeletedForUser(entryId, userId)
                val deleteResult = dailyTrackingRepository.deleteMeal(today(), scanId)
                if (deleteResult.isSuccess) {
                    dao.hardDelete(entryId)
                    Log.d(TAG, "removeFoodEntry: DELETE OK for scanId=$scanId — row hard-deleted")
                } else {
                    Log.e(TAG, "removeFoodEntry: DELETE FAILED for scanId=$scanId — tombstone kept, worker will retry", deleteResult.exceptionOrNull())
                }
            }
            Unit
        }
    }

    override suspend fun removeFoodEntryCompletely(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val userId = resolveUserId()
            val existing = dao.getByIdForUser(entryId, userId)
            val scanId = existing?.productId ?: entryId

            Log.d(TAG, "removeFoodEntryCompletely: scanId=$scanId mealCnt=${existing?.mealCnt} — removing all servings, will DELETE")
            dao.markDeletedForUser(entryId, userId)
            val deleteResult = dailyTrackingRepository.deleteMeal(today(), scanId)
            if (deleteResult.isSuccess) {
                dao.hardDelete(entryId)
                Log.d(TAG, "removeFoodEntryCompletely: DELETE OK for scanId=$scanId — row hard-deleted")
            } else {
                Log.e(TAG, "removeFoodEntryCompletely: DELETE FAILED for scanId=$scanId — tombstone kept, worker will retry", deleteResult.exceptionOrNull())
            }
            Unit
        }
    }

    /** Fails rather than falling back to a shared device-local id: that fallback made every
     * account on the device read and write the same rows, and it engaged for *every* signed-in
     * user because getCurrentUserId() was null until it learned to read the access token. Callers
     * wrap this in runCatchingCancellable, so a pre-sign-in call surfaces as Result.failure. */
    private suspend fun resolveUserId(): String = authRepository.getCurrentUserId()
        ?: error("No authenticated user - per-user data is unavailable until sign-in completes")

    private companion object {
        /** Grep logcat for this to trace the add/remove meal flow end to end. The matching
         * HTTP request/response bodies are logged separately by OkHttp's BODY-level
         * HttpLoggingInterceptor (debug builds only, see NetworkModule). */
        const val TAG = "FoodLogSync"
    }
}
