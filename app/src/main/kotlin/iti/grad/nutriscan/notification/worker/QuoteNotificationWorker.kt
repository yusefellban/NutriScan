package iti.grad.nutriscan.notification.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.usecase.GetRandomQuoteUseCase
import iti.grad.nutriscan.domain.notification.usecase.IsWithinQuietHoursUseCase
import iti.grad.nutriscan.domain.notification.usecase.ObserveNotificationPrefsUseCase
import iti.grad.nutriscan.notification.NotificationChannels
import iti.grad.nutriscan.notification.NutriScanNotificationBuilder
import iti.grad.presentation.R
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class QuoteNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observePrefs: ObserveNotificationPrefsUseCase,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val isWithinQuietHours: IsWithinQuietHoursUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = observePrefs().first()
        if (!prefs.isEnabled(NotificationType.QUOTE)) return Result.success()
        if (isWithinQuietHours(prefs, LocalTime.now())) return Result.success()

        val quote = getRandomQuote()
        val notification = NutriScanNotificationBuilder.build(
            context = applicationContext,
            type = NotificationType.QUOTE,
            title = applicationContext.getString(R.string.notification_push_quote_title),
            body = quote.text,
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationChannels.channelId(NotificationType.QUOTE).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "quote_notification_worker"
    }
}
