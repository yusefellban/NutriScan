package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.local.datasource.INotificationPreferencesDataSource
import iti.grad.nutriscan.domain.common.runCatchingCancellable
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dataSource: INotificationPreferencesDataSource,
) : INotificationRepository {

    override fun observePrefs(): Flow<NotificationPrefs> = dataSource.getPrefs()

    override suspend fun setEnabled(type: NotificationType, enabled: Boolean): Result<Unit> =
        runCatchingCancellable { dataSource.setEnabled(type, enabled) }

    override suspend fun setQuietHours(start: LocalTime, end: LocalTime): Result<Unit> =
        runCatchingCancellable { dataSource.setQuietHours(start, end) }

    override suspend fun setQuietHoursEnabled(enabled: Boolean): Result<Unit> =
        runCatchingCancellable { dataSource.setQuietHoursEnabled(enabled) }
}
