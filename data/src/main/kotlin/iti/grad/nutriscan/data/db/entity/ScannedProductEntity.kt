package iti.grad.nutriscan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached OpenFoodFacts lookup result, keyed by barcode — lets a previously-scanned barcode
 * resolve offline. See [iti.grad.nutriscan.data.repository.ScanRepositoryImpl]. */
@Entity(tableName = "scanned_products")
data class ScannedProductEntity(
    @PrimaryKey val barcode: String,
    val productName: String?,
    val brand: String?,
    val imageUrl: String?,
    val healthTag: String?,
)
