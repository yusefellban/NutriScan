package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiseaseDao {
    @Query("SELECT * FROM diseases")
    fun getDiseasesFlow(): Flow<List<DiseaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiseases(diseases: List<DiseaseEntity>)

    @Query("DELETE FROM diseases")
    suspend fun clearDiseases()
}
