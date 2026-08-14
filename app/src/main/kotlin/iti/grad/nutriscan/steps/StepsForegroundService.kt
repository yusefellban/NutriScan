package iti.grad.nutriscan.steps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import iti.grad.nutriscan.MainActivity
import iti.grad.nutriscan.domain.dailytracking.usecase.UpdateStepsCntUseCase
import iti.grad.nutriscan.domain.steps.usecase.CheckStepsPermissionUseCase
import iti.grad.nutriscan.domain.steps.usecase.ObserveTodayStepsUseCase
import iti.grad.presentation.R as PresentationR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps the step-counter sensor listener (see StepsRepositoryImpl) registered for as long as
 * Android lets this process live, instead of only while the Calories screen is composed. A
 * foreground service is what makes the count update live while the app is backgrounded or the
 * screen is off — WorkManager's [StepsSyncWorker] has a 15-minute floor and only exists as a
 * catch-up safety net for when the OS still kills this service under memory pressure.
 *
 * Started from [MainActivity.onResume] once ACTIVITY_RECOGNITION is granted (harmless/no-op to
 * call start on an already-running service), and self-stops if it somehow starts without the
 * permission.
 */
@AndroidEntryPoint
class StepsForegroundService : Service() {

    @Inject
    lateinit var checkStepsPermission: CheckStepsPermissionUseCase

    @Inject
    lateinit var observeTodaySteps: ObserveTodayStepsUseCase

    @Inject
    lateinit var updateStepsCnt: UpdateStepsCntUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Must be called within seconds of the service starting, before any permission check —
        // startForegroundService() requires it regardless of what we decide to do afterwards.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )

        scope.launch {
            if (!checkStepsPermission()) {
                stopSelf()
                return@launch
            }
            observeTodaySteps()
                .onEach { steps -> updateNotification(steps) }
                .launchIn(this)
            observeTodaySteps()
                .debounce(30.seconds)
                .onEach { steps -> updateStepsCnt(steps) }
                .launchIn(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification(steps: Int = 0): Notification {
        createChannelIfNeeded()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(PresentationR.string.steps_tracking_notification_title))
            .setContentText(
                getString(PresentationR.string.steps_tracking_notification_body, steps)
            )
            .setSmallIcon(PresentationR.drawable.steps)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(steps: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun createChannelIfNeeded() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Step Tracking", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        private const val CHANNEL_ID = "channel_steps_tracking"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, StepsForegroundService::class.java))
        }
    }
}
