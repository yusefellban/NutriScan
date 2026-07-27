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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyTrackingEntity)

    @Query("UPDATE daily_tracking SET syncedToBackend = 1 WHERE userId = :userId AND date = :date")
    suspend fun markSynced(userId: String, date: String)

    @Query("DELETE FROM daily_tracking WHERE userId = :userId AND date = :date")
    suspend fun deleteByUserAndDate(userId: String, date: String)
}
