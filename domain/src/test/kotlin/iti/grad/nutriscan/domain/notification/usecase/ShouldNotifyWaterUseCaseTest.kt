package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ShouldNotifyWaterUseCaseTest {

    private val quietHours = IsWithinQuietHoursUseCase()
    private val useCase = ShouldNotifyWaterUseCase(quietHours)
    private val enabledPrefs = NotificationPrefs.default()
    private val disabledPrefs = enabledPrefs.copy(
        enabled = enabledPrefs.enabled + (NotificationType.WATER to false)
    )

    @Test
    fun `notifies when behind goal, enabled, and outside quiet hours`() {
        assertTrue(useCase(enabledPrefs, consumed = 3, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when goal already met`() {
        assertFalse(useCase(enabledPrefs, consumed = 8, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when goal exceeded`() {
        assertFalse(useCase(enabledPrefs, consumed = 9, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when type disabled`() {
        assertFalse(useCase(disabledPrefs, consumed = 3, goal = 8, now = LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify during quiet hours`() {
        assertFalse(useCase(enabledPrefs, consumed = 3, goal = 8, now = LocalTime.of(23, 0)))
    }

    @Test
    fun `does not notify when no goal is set`() {
        assertFalse(useCase(enabledPrefs, consumed = 0, goal = 0, now = LocalTime.of(14, 0)))
    }
}
