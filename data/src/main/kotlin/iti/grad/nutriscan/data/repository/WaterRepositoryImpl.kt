package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.db.dao.WaterLogDao
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import iti.grad.nutriscan.data.di.IoDispatcher
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.water.model.WaterLog
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val dao: WaterLogDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : IWaterRepository {

    override fun observeToday(): Flow<WaterLog> = flow {
        emitAll(
            dao.observeByDate(today()).map { entity ->
                WaterLog(
                    glassCount = entity?.glassCount ?: 0,
                    goalGlasses = entity?.goalGlasses ?: DEFAULT_GOAL,
                )
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun logGlass(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = dao.observeByDate(today()).first()
            dao.upsert(
                WaterLogEntity(
                    date = today(),
                    glassCount = (current?.glassCount ?: 0) + 1,
                    goalGlasses = current?.goalGlasses ?: DEFAULT_GOAL,
                )
            )
        }
    }

    override suspend fun unlogGlass(): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = dao.observeByDate(today()).first()
            dao.upsert(
                WaterLogEntity(
                    date = today(),
                    glassCount = ((current?.glassCount ?: 0) - 1).coerceAtLeast(0),
                    goalGlasses = current?.goalGlasses ?: DEFAULT_GOAL,
                )
            )
        }
    }

    override suspend fun setGoal(glasses: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatchingCancellable {
            val current = dao.observeByDate(today()).first()
            dao.upsert(
                WaterLogEntity(
                    date = today(),
                    glassCount = current?.glassCount ?: 0,
                    goalGlasses = glasses,
                )
            )
        }
    }

    private fun today(): String = LocalDate.now().toString()

    private companion object {
        const val DEFAULT_GOAL = 8
    }
}
