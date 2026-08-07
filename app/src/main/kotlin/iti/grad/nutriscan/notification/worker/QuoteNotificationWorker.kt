package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import android.annotation.SuppressLint
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.auth.usecase.CheckIfUserIsLoggedInUseCase
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRecorder
import iti.grad.nutriscan.domain.notification.usecase.GetRandomQuoteUseCase
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NotificationSlots
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@HiltWorker
class QuoteNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
    private val historyRecorder: INotificationHistoryRecorder,
    private val checkIfUserIsLoggedIn: CheckIfUserIsLoggedInUseCase,
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Belt and braces: the scheduler cancels on logout, but work enqueued by an older
        // build — or a session ended by a failed token refresh — can still fire.
        if (!checkIfUserIsLoggedIn()) return Result.success()
        // WorkManager is best-effort — Doze can defer a slot for hours, and a reminder for a
        // moment that has passed is just noise.
        if (NotificationSlots.isTooLate(
                inputData.getInt(NotificationSlots.KEY_SLOT_MINUTE_OF_DAY, -1),
                LocalTime.now(),
            )
        ) {
            return Result.success()
        }
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.QUOTE)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()
        // WorkManager has no weekday scheduling, so the work is enqueued daily and filtered here.
        if (LocalDate.now().dayOfWeek !in QUOTE_DAYS) return Result.success()

        val quote = getRandomQuote()
        val title = applicationContext.getString(R.string.notification_push_quote_title)
        val body = quote.text
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.QUOTE,
            title = title,
            body = body,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.QUOTE).hashCode(), notification)
        historyRecorder.record(NotificationType.QUOTE, title, body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "quote_notification_worker"
        private val QUOTE_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    }
}
