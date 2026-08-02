package iti.grad.nutriscan.domain.notification.repository

import iti.grad.nutriscan.domain.notification.model.NotificationType

/**
 * Lightweight recorder that each notification worker calls after posting
 * a notification so it is persisted into the local history.
 *
 * Intentionally separated from [INotificationHistoryRepository] to keep
 * the write-side concern (used by workers) minimal and distinct from the
 * read-side concern (used by the UI).
 */
interface INotificationHistoryRecorder {

    /** Record a dispatched notification for history. */
    suspend fun record(type: NotificationType, title: String, body: String)
}
