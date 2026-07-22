package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.data.repository.mapper.today
import iti.grad.nutriscan.data.repository.mapper.toDomain
import iti.grad.nutriscan.data.repository.mapper.toEntity
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.common.runCatchingCancellable
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
            dao.insert(entry.toEntity(resolveUserId()))
        }
    }

    override suspend fun removeFoodEntry(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.deleteByIdForUser(entryId, resolveUserId())
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
