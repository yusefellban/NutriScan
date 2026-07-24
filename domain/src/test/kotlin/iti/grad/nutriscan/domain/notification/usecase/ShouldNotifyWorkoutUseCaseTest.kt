package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ShouldNotifyWorkoutUseCaseTest {

    private val quietHours = IsWithinQuietHoursUseCase()
    private val useCase = ShouldNotifyWorkoutUseCase(quietHours)
    private val enabledPrefs = NotificationPrefs.default()
    private val disabledPrefs = enabledPrefs.copy(
        enabled = enabledPrefs.enabled + (NotificationType.WORKOUT to false)
    )

    @Test
    fun `notifies when not done, enabled, outside quiet hours`() {
        assertTrue(useCase(enabledPrefs, workoutDone = false, now = LocalTime.of(18, 0)))
    }

    @Test
    fun `does not notify when already done`() {
        assertFalse(useCase(enabledPrefs, workoutDone = true, now = LocalTime.of(18, 0)))
    }

    @Test
    fun `does not notify when disabled`() {
        assertFalse(useCase(disabledPrefs, workoutDone = false, now = LocalTime.of(18, 0)))
    }
}
