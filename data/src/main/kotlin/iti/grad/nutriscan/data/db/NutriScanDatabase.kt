package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity

@Database(
    entities = [FoodLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
}
