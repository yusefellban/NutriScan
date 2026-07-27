package iti.grad.nutriscan.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import iti.grad.nutriscan.data.local.datasource.INotificationPreferencesDataSource
import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    private val dataSource: INotificationPreferencesDataSource = mockk()
    private val repository = NotificationRepositoryImpl(dataSource)

    @Test
    fun `observePrefs delegates to data source`() = runTest {
        coEvery { dataSource.getPrefs() } returns flowOf(NotificationPrefs.default())

        val result = repository.observePrefs().first()

        assertTrue(result.isEnabled(NotificationType.WATER))
    }

    @Test
    fun `setEnabled delegates to data source and succeeds`() = runTest {
        coEvery { dataSource.setEnabled(any(), any()) } returns Unit

        val result = repository.setEnabled(NotificationType.WATER, false)

        assertTrue(result.isSuccess)
        coVerify { dataSource.setEnabled(NotificationType.WATER, false) }
    }

    @Test
    fun `setQuietHours delegates to data source and succeeds`() = runTest {
        coEvery { dataSource.setQuietHours(any(), any()) } returns Unit

        val result = repository.setQuietHours(LocalTime.of(21, 0), LocalTime.of(6, 0))

        assertTrue(result.isSuccess)
        coVerify { dataSource.setQuietHours(LocalTime.of(21, 0), LocalTime.of(6, 0)) }
    }
}
