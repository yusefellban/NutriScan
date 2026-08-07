package iti.grad.nutriscan.domain.notification.repository

interface ITestNotificationSender {
    fun sendNow()

    /** Posts one real-content notification per [iti.grad.nutriscan.domain.notification.model.NotificationType]
     * directly (bypassing WorkManager, whose periodic-work guard refuses to run a job "before
     * schedule" if forced immediately after enqueueing) — a manual way to see every type's real
     * copy and icon without waiting for its actual interval to elapse. */
    fun sendAllTypesNow()
}
