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

    @Query("SELECT * FROM saved_scans WHERE userId = :userId AND deleted = 0 ORDER BY scannedAt DESC")
    fun getAllSavedScans(userId: String): Flow<List<SavedScanEntity>>

    @Query("SELECT * FROM saved_scans WHERE scanId = :scanId AND userId = :userId LIMIT 1")
    suspend fun getSavedScanById(scanId: String, userId: String): SavedScanEntity?

    @Query("UPDATE saved_scans SET deleted = 1, pendingSync = 1 WHERE scanId = :scanId AND userId = :userId")
    suspend fun markDeletedForUser(scanId: String, userId: String)

    @Query("SELECT * FROM saved_scans WHERE pendingSync = 1 AND userId = :userId")
    suspend fun getPendingSyncEntries(userId: String): List<SavedScanEntity>

    @Query("UPDATE saved_scans SET pendingSync = 0 WHERE scanId = :scanId")
    suspend fun clearPendingSync(scanId: String)

    @Query("DELETE FROM saved_scans WHERE scanId = :scanId")
    suspend fun hardDelete(scanId: String)

    /** Removes local rows the backend no longer lists as favorited (e.g. unfavorited on another
     * device) — skips rows still [pendingSync] so an offline save/delete isn't wiped out. */
    @Query(
        "DELETE FROM saved_scans WHERE userId = :userId AND pendingSync = 0 AND scanId NOT IN (:remoteScanIds)"
    )
    suspend fun deleteStaleSynced(userId: String, remoteScanIds: List<String>)
}
