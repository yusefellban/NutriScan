package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Query("SELECT * FROM food_log WHERE userId = :userId AND loggedDate = :date AND deleted = 0 ORDER BY addedAtEpochMillis DESC")
    fun observeByUserAndDate(userId: String, date: String): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodLogEntity)

    @Query("SELECT * FROM food_log WHERE id = :id AND userId = :userId")
    suspend fun getByIdForUser(id: String, userId: String): FoodLogEntity?

    @Query("UPDATE food_log SET deleted = 1, pendingSync = 1 WHERE id = :id AND userId = :userId")
    suspend fun markDeletedForUser(id: String, userId: String)

    @Query("SELECT * FROM food_log WHERE pendingSync = 1 AND userId = :userId")
    suspend fun getPendingSyncEntries(userId: String): List<FoodLogEntity>

    @Query("UPDATE food_log SET pendingSync = 0 WHERE id = :id")
    suspend fun clearPendingSync(id: String)

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun hardDelete(id: String)
}
