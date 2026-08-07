package iti.grad.nutriscan.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.BuildConfig
import iti.grad.nutriscan.domain.notification.usecase.SendTestNotificationUseCase

/** adb shell am broadcast -a iti.grad.nutriscan.DEBUG_SEND_ALL_NOTIFICATIONS -p iti.grad.nutriscan
 * Debug-only manual trigger to preview every notification type's real content/icon at once,
 * bypassing WorkManager's periodic-work "run before schedule" guard — see
 * ITestNotificationSender.sendAllTypesNow(). No-ops on release builds. */
class DebugNotificationReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun sendTestNotificationUseCase(): SendTestNotificationUseCase
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        if (intent.action != ACTION_SEND_ALL) return
        EntryPointAccessors.fromApplication(context.applicationContext, Entry::class.java)
            .sendTestNotificationUseCase()
            .sendAllTypes()
    }

    companion object {
        const val ACTION_SEND_ALL = "iti.grad.nutriscan.DEBUG_SEND_ALL_NOTIFICATIONS"
    }
}
