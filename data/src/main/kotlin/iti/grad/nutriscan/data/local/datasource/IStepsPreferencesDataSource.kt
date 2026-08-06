package iti.grad.nutriscan.data.local.datasource

interface IStepsPreferencesDataSource {
    /** ISO-8601 local date the current baseline was captured for, or null if never captured. */
    suspend fun getBaselineDate(): String?

    /** Raw cumulative sensor reading (steps since last device reboot) at the start of [getBaselineDate]. */
    suspend fun getBaselineSteps(): Float

    suspend fun saveBaseline(date: String, steps: Float)

    /** Last known step total for [date] (ISO-8601). Returns 0 when the persisted total belongs
     * to a different day — a stale total is worse than no total, since it is shown to the user
     * before the first sensor event of the day arrives. */
    suspend fun getDailySteps(date: String): Int

    suspend fun saveDailySteps(date: String, steps: Int)
}
