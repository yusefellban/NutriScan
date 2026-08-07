package iti.grad.nutriscan.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import iti.grad.nutriscan.data.local.datasource.IStepsPreferencesDataSource
import iti.grad.nutriscan.domain.steps.repository.IStepsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import iti.grad.nutriscan.domain.common.CairoDateProvider
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Sensor.TYPE_STEP_COUNTER] reports a cumulative total since the last device reboot, so
 * "today's steps" = latest reading minus a baseline captured at the start of the current local
 * day. This class is a Hilt singleton and starts tracking at most once per process — [isTracking]
 * guards against a second [SensorEventListener] ever being registered, regardless of how many
 * screens/ViewModels collect [observeTodaySteps] or how many times navigation re-creates them;
 * a duplicate registration would otherwise double- or triple-count every real step. The listener
 * is intentionally never unregistered — it stays live for the process lifetime so
 * the count keeps updating even while the Calories screen isn't visible, and `TYPE_STEP_COUNTER`
 * is a low-power sensor that's cheap to keep registered. It is registered with
 * `maxReportLatencyUs = 0` so the hardware reports each change immediately rather than batching.
 */
@Singleton
class StepsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: IStepsPreferencesDataSource
) : IStepsRepository {

    private val sensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isTracking = AtomicBoolean(false)
    private val dailySteps = MutableStateFlow<Int?>(null)

    override suspend fun hasStepsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun observeTodaySteps(): Flow<Int> {
        ensureTracking()
        return dailySteps.filterNotNull()
    }

    private fun ensureTracking() {
        if (!isTracking.compareAndSet(false, true)) return

        scope.launch {
            val started = runCatchingCancellable { startTracking() }
                .onFailure { Timber.e(it, "Step tracking failed to start") }
                .getOrDefault(false)
            // Releasing the latch on failure is what lets a later collection retry. Without
            // it a single failed start pinned the flow at 0 until the process was killed.
            if (!started) isTracking.set(false)
        }
    }

    /** Returns true once the sensor listener is registered. False means the caller should
     * release [isTracking] so a later [observeTodaySteps] collection can try again. */
    private suspend fun startTracking(): Boolean {
        val today = CairoDateProvider.today().toString()
        var baselineDate = preferences.getBaselineDate()
        var baselineSteps = preferences.getBaselineSteps()

        // A baseline captured on an earlier day makes every persisted total stale, so today
        // starts at 0 until the next sensor event re-baselines. Doing this here and not only
        // in onSensorChanged means a day with no sensor event still reads 0 rather than
        // yesterday's total.
        dailySteps.value = if (baselineDate == today) preferences.getDailySteps(today) else 0

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            dailySteps.value = 0
            return false
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val eventDay = CairoDateProvider.today().toString()
                val cumulative = event.values[0]

                // New day, first run, or the device rebooted since the baseline was
                // captured (the sensor resets to 0 on reboot) — re-baseline to now.
                if (baselineDate != eventDay || cumulative < baselineSteps) {
                    baselineDate = eventDay
                    baselineSteps = cumulative
                    scope.launch { preferences.saveBaseline(date = eventDay, steps = cumulative) }
                }

                val newDailySteps = (cumulative - baselineSteps).toInt().coerceAtLeast(0)
                if (newDailySteps != dailySteps.value) {
                    dailySteps.value = newDailySteps
                    scope.launch { preferences.saveDailySteps(date = eventDay, steps = newDailySteps) }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // maxReportLatencyUs = 0 disables hardware batching. With batching on, the first
        // event after registration could be minutes late, which is what made the count
        // appear frozen or zero right after opening the app.
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            0,
            mainHandler
        )
        return true
    }
}
