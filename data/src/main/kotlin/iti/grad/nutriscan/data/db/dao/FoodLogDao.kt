package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Query("SELECT * FROM food_log WHERE userId = :userId AND loggedDate = :date ORDER BY addedAtEpochMillis DESC")
    fun observeByUserAndDate(userId: String, date: String): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodLogEntity)

    @Query("DELETE FROM food_log WHERE id = :id AND userId = :userId")
    suspend fun deleteByIdForUser(id: String, userId: String)
}
