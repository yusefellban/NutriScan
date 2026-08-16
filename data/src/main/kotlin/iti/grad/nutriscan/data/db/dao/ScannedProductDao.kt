package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import iti.grad.nutriscan.data.db.entity.ScannedProductEntity

@Dao
interface ScannedProductDao {
    @Query("SELECT * FROM scanned_products WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): ScannedProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ScannedProductEntity)
}
