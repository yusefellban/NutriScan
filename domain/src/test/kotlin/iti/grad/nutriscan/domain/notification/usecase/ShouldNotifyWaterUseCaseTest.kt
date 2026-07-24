package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import iti.grad.nutriscan.domain.notification.model.NotificationType
import iti.grad.nutriscan.domain.water.model.WaterLog
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
        val water = WaterLog(glassCount = 3, goalGlasses = 8)
        assertTrue(useCase(enabledPrefs, water, LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when goal already met`() {
        val water = WaterLog(glassCount = 8, goalGlasses = 8)
        assertFalse(useCase(enabledPrefs, water, LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify when type disabled`() {
        val water = WaterLog(glassCount = 3, goalGlasses = 8)
        assertFalse(useCase(disabledPrefs, water, LocalTime.of(14, 0)))
    }

    @Test
    fun `does not notify during quiet hours`() {
        val water = WaterLog(glassCount = 3, goalGlasses = 8)
        assertFalse(useCase(enabledPrefs, water, LocalTime.of(23, 0)))
    }
}
