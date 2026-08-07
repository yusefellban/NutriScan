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
        assertTrue(useCase(prefs, loggedFoodToday = false, hasStreak = true, now = LocalTime.of(20, 0)))
    }

    @Test
    fun `does not warn once food has been logged`() {
        assertFalse(useCase(prefs, loggedFoodToday = true, hasStreak = true, now = LocalTime.of(20, 0)))
    }

    @Test
    fun `does not warn before the evening window`() {
        assertFalse(useCase(prefs, loggedFoodToday = false, hasStreak = true, now = LocalTime.of(10, 0)))
    }

    @Test
    fun `does not notify when there is no streak to preserve`() {
        assertFalse(
            useCase(prefs, loggedFoodToday = false, hasStreak = false, now = LocalTime.of(21, 30))
        )
    }

    @Test
    fun `notifies when a live streak is at risk`() {
        assertTrue(
            useCase(prefs, loggedFoodToday = false, hasStreak = true, now = LocalTime.of(21, 30))
        )
    }
}
