package iti.grad.nutriscan.domain.notification.repository

/** Device-local reminder scheduling. Implemented in `:app` over WorkManager, which domain
 * cannot see — same arrangement as [ITestNotificationSender]. */
interface INotificationScheduler {
    /** Idempotent: safe to call on every login and every cold start. */
    fun scheduleAll()

    fun cancelAll()
}
