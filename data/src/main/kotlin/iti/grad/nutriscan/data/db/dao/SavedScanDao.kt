package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: SavedScanEntity)

    @Query("SELECT * FROM saved_scans ORDER BY scannedAt DESC")
    fun getAllSavedScans(): Flow<List<SavedScanEntity>>

    @Query("SELECT * FROM saved_scans WHERE scanId = :scanId LIMIT 1")
    suspend fun getSavedScanById(scanId: String): SavedScanEntity?

    @Query("DELETE FROM saved_scans WHERE scanId = :scanId")
    suspend fun deleteScanById(scanId: String)
}
