package iti.grad.nutriscan.steps

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Wakes the process periodically so [ObserveTodayStepsUseCase] (re-)registers the step-counter
 * sensor listener and persists the latest reading — the sensor listener itself only survives
 * for the current process lifetime (see StepsRepositoryImpl), so this is what keeps today's
 * step count accurate across app closures without needing a foreground service.
 * It also mirrors the settled reading into `daily_tracking` via [UpdateStepsCntUseCase], which is
 * what the Calories screen and the notification workers read.
 */
@HiltWorker
class StepsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkStepsPermission: CheckStepsPermissionUseCase,
    private val observeTodaySteps: ObserveTodayStepsUseCase,
    private val updateStepsCnt: UpdateStepsCntUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!checkStepsPermission()) return Result.success()

        val steps = observeTodaySteps()
        steps.first() // triggers sensor listener (re-)registration for this process
        delay(SENSOR_SETTLE_DELAY_MS) // give the hardware a moment to report a fresh cumulative value
        // Mirror into daily_tracking so the Calories screen, the notification workers, and the
        // backend sync all read the same number even if no screen was ever opened today.
        updateStepsCnt(steps.first())
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "steps_sync_worker"
        private const val SENSOR_SETTLE_DELAY_MS = 2_000L
    }
}
