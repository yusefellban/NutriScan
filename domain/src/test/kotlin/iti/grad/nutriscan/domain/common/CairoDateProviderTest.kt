package iti.grad.nutriscan.domain.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class CairoDateProviderTest {

    @Test
    fun `an instant just before Cairo midnight is still yesterday in Cairo`() {
        // 2026-07-27T21:00:00Z = 2026-07-28T00:00:00+03:00 (Cairo, UTC+3 DST, reintroduced 2023)
        // Use a time that's just before Cairo midnight when Cairo is UTC+3
        val instant = Instant.parse("2026-07-27T20:59:00Z")
        assertEquals(LocalDate.parse("2026-07-27"), CairoDateProvider.todayAt(instant))
    }

    @Test
    fun `an instant just after Cairo midnight is already the next day in Cairo`() {
        // 2026-07-27T21:01:00Z = 2026-07-28T00:01:00+03:00 (Cairo, UTC+3 DST)
        val instant = Instant.parse("2026-07-27T21:01:00Z")
        assertEquals(LocalDate.parse("2026-07-28"), CairoDateProvider.todayAt(instant))
    }

    @Test
    fun `today delegates to todayAt with the current instant`() {
        val today = CairoDateProvider.today()
        val expected = CairoDateProvider.todayAt(Instant.now())
        // Same call within the same second — allow either today's or (extremely rarely,
        // right at a day boundary) the next day's date rather than asserting exact equality.
        assert(today == expected || today == expected.plusDays(1))
    }
}
