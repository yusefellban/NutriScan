package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.WorkoutLogDao
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IWorkoutRepository {

    override fun observeTodayDone(): Flow<Boolean> = flow {
        emitAll(dao.observeByDate(today()).map { it?.done ?: false })
    }.flowOn(ioDispatcher)

    override suspend fun markDone(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            dao.upsert(WorkoutLogEntity(date = today(), done = true))
        }
    }

    private fun today(): String = LocalDate.now().toString()
}
