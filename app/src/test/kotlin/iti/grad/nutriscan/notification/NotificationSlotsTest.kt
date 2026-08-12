package iti.grad.nutriscan.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSlotsTest {

    @Test
    fun `counts forward to a slot later today`() {
        val now = LocalDateTime.of(2026, 8, 6, 9, 0)

        assertEquals(240, NotificationSlots.minutesUntilNext(LocalTime.of(13, 0), now))
    }

    @Test
    fun `rolls to tomorrow when the slot already passed today`() {
        val now = LocalDateTime.of(2026, 8, 6, 15, 0)

        assertEquals(1080, NotificationSlots.minutesUntilNext(LocalTime.of(9, 0), now))
    }

    @Test
    fun `never returns zero when the slot is exactly now`() {
        val now = LocalDateTime.of(2026, 8, 6, 9, 0)

        assertEquals(1440, NotificationSlots.minutesUntilNext(LocalTime.of(9, 0), now))
    }

    @Test
    fun `crosses midnight correctly`() {
        val now = LocalDateTime.of(2026, 8, 6, 23, 30)

        assertEquals(540, NotificationSlots.minutesUntilNext(LocalTime.of(8, 30), now))
    }

    @Test
    fun `is not too late when running on time`() {
        assertFalse(NotificationSlots.isTooLate(slotMinuteOfDay = 9 * 60, now = LocalTime.of(9, 5)))
    }

    @Test
    fun `is not too late when running early`() {
        assertFalse(NotificationSlots.isTooLate(slotMinuteOfDay = 9 * 60, now = LocalTime.of(8, 55)))
    }

    @Test
    fun `is too late beyond the grace window`() {
        assertTrue(NotificationSlots.isTooLate(slotMinuteOfDay = 9 * 60, now = LocalTime.of(11, 0)))
    }

    @Test
    fun `is never too late when no slot was supplied`() {
        assertFalse(NotificationSlots.isTooLate(slotMinuteOfDay = -1, now = LocalTime.of(23, 0)))
    }
}
