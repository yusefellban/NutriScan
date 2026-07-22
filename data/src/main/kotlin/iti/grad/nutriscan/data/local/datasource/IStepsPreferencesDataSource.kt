package iti.grad.nutriscan.data.local.datasource

interface IStepsPreferencesDataSource {
    /** ISO-8601 local date the current baseline was captured for, or null if never captured. */
    suspend fun getBaselineDate(): String?

    /** Raw cumulative sensor reading (steps since last device reboot) at the start of [getBaselineDate]. */
    suspend fun getBaselineSteps(): Float

    suspend fun saveBaseline(date: String, steps: Float)

    /** Last known today-steps total, persisted so the UI has a value to show before the first sensor event. */
    suspend fun getDailySteps(): Int

    suspend fun saveDailySteps(steps: Int)
}
