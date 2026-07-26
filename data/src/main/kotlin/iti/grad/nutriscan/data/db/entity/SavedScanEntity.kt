package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_scans")
data class SavedScanEntity(
    @PrimaryKey val scanId: String,
    val scannedAt: String?,
    val imageUrl: String?,
    val verdict: String?, // SAFE, CAUTION, UNSAFE
    val summary: String?,
    // The flagged ingredients and nutrition facts will be stored as JSON strings
    val flaggedIngredientsJson: String?,
    val nutritionFactsJson: String?
)
