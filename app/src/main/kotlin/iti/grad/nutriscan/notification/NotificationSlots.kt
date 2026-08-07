package iti.grad.nutriscan.notification

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Wall-clock scheduling arithmetic for the reminder slots. Kept separate from the scheduler
 * so it can be tested without WorkManager. */
object NotificationSlots {

    /** Set on every slot's `inputData` so a worker can tell how late WorkManager ran it. */
    const val KEY_SLOT_MINUTE_OF_DAY = "slot_minute_of_day"

    private const val MAX_LATE_MINUTES = 90

    /**
     * Minutes from [now] to the next occurrence of [slot], rolling to tomorrow when the slot has
     * already passed. A slot falling exactly on [now] counts as tomorrow — returning 0 here is
     * what made a fresh install fire every reminder the moment it was enqueued.
     */
    fun minutesUntilNext(slot: LocalTime, now: LocalDateTime): Long {
        val todaysSlot = now.toLocalDate().atTime(slot)
        val next = if (todaysSlot.isAfter(now)) todaysSlot else todaysSlot.plusDays(1)
        return Duration.between(now, next).toMinutes()
    }

    /**
     * True when WorkManager ran a slot more than [MAX_LATE_MINUTES] past its time — Doze and
     * batching can defer work for hours, and a "time for a break" nudge is noise once the moment
     * has passed. A [slotMinuteOfDay] below zero means no slot was supplied, which never skips.
     */
    fun isTooLate(slotMinuteOfDay: Int, now: LocalTime): Boolean {
        if (slotMinuteOfDay < 0) return false
        return (now.toSecondOfDay() / 60) - slotMinuteOfDay > MAX_LATE_MINUTES
    }
}
