package iti.grad.nutriscan.domain.notification.model

import java.time.LocalTime

data class NotificationPrefs(
    val enabled: Map<NotificationType, Boolean>,
    val quietHoursEnabled: Boolean,
    val quietHoursStart: LocalTime,
    val quietHoursEnd: LocalTime,
) {
    fun isEnabled(type: NotificationType): Boolean = enabled[type] ?: true

    companion object {
        val DEFAULT_QUIET_START: LocalTime = LocalTime.of(22, 0)
        val DEFAULT_QUIET_END: LocalTime = LocalTime.of(7, 0)

        fun default(): NotificationPrefs = NotificationPrefs(
            enabled = NotificationType.entries.associateWith { true },
            quietHoursEnabled = true,
            quietHoursStart = DEFAULT_QUIET_START,
            quietHoursEnd = DEFAULT_QUIET_END,
        )
    }
}
