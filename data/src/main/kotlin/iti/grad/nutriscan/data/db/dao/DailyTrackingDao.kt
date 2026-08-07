package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTrackingDao {

    @Query("SELECT * FROM daily_tracking WHERE userId = :userId AND date = :date")
    fun observeByUserAndDate(userId: String, date: String): Flow<DailyTrackingEntity?>

    @Query("SELECT * FROM daily_tracking WHERE userId = :userId AND date = :date")
    suspend fun getByUserAndDate(userId: String, date: String): DailyTrackingEntity?

    /** Dates are stored as ISO `yyyy-MM-dd`, so a lexicographic BETWEEN is a real date range. */
    @Query("SELECT * FROM daily_tracking WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getRange(userId: String, startDate: String, endDate: String): List<DailyTrackingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyTrackingEntity)

    /** Every day still owing a backend push, oldest first — not just today. A day that ended with
     * unsynced changes (logged late, or pushed while offline) would otherwise never be retried,
     * since the worker only ever asked for the current date. */
    @Query("SELECT * FROM daily_tracking WHERE userId = :userId AND syncedToBackend = 0 ORDER BY date ASC")
    suspend fun getUnsyncedDays(userId: String): List<DailyTrackingEntity>

    @Query("UPDATE daily_tracking SET syncedToBackend = 1 WHERE userId = :userId AND date = :date")
    suspend fun markSynced(userId: String, date: String)

    @Query("DELETE FROM daily_tracking WHERE userId = :userId AND date = :date")
    suspend fun deleteByUserAndDate(userId: String, date: String)
}
