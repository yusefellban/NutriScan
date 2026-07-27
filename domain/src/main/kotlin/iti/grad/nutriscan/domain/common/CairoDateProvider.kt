package iti.grad.nutriscan.domain.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for "what day is it" across the app. Every day-boundary
 * concern (food log, steps reset, water reset, the nightly sync job) is
 * Cairo-anchored regardless of device timezone — see
 * docs/plans/2026-07-27-daily-tracking-sync.md for why.
 */
object CairoDateProvider {
    val ZONE: ZoneId = ZoneId.of("Africa/Cairo")

    fun today(): LocalDate = todayAt(Instant.now())

    fun todayAt(instant: Instant): LocalDate = LocalDate.ofInstant(instant, ZONE)
}
