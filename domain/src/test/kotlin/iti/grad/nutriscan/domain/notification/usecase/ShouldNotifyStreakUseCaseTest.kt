package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class ShouldNotifyStreakUseCaseTest {

    private val quietHours = IsWithinQuietHoursUseCase()
    private val useCase = ShouldNotifyStreakUseCase(quietHours)
    private val prefs = NotificationPrefs.default()

    @Test
    fun `warns in the evening when no food logged yet today`() {
        assertTrue(useCase(prefs, loggedFoodToday = false, now = LocalTime.of(20, 0)))
    }

    @Test
    fun `does not warn once food has been logged`() {
        assertFalse(useCase(prefs, loggedFoodToday = true, now = LocalTime.of(20, 0)))
    }

    @Test
    fun `does not warn before the evening window`() {
        assertFalse(useCase(prefs, loggedFoodToday = false, now = LocalTime.of(10, 0)))
    }
}
