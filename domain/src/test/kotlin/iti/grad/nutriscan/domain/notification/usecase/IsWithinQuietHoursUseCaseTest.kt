package iti.grad.nutriscan.domain.notification.usecase

import iti.grad.nutriscan.domain.notification.model.NotificationPrefs
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class IsWithinQuietHoursUseCaseTest {

    private val useCase = IsWithinQuietHoursUseCase()
    private val prefs = NotificationPrefs.default() // 22:00 - 07:00

    @Test
    fun `returns true for a time inside the overnight quiet window`() {
        assertTrue(useCase(prefs, LocalTime.of(23, 30)))
        assertTrue(useCase(prefs, LocalTime.of(6, 0)))
    }

    @Test
    fun `returns false for a time outside the quiet window`() {
        assertFalse(useCase(prefs, LocalTime.of(12, 0)))
    }

    @Test
    fun `boundary start time counts as quiet`() {
        assertTrue(useCase(prefs, LocalTime.of(22, 0)))
    }

    @Test
    fun `boundary end time does not count as quiet`() {
        assertFalse(useCase(prefs, LocalTime.of(7, 0)))
    }

    @Test
    fun `returns false whenever quiet hours are disabled, even inside the window`() {
        val disabled = prefs.copy(quietHoursEnabled = false)
        assertFalse(useCase(disabled, LocalTime.of(23, 30)))
    }
}
