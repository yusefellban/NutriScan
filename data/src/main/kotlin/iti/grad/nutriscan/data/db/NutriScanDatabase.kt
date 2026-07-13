package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.entity.DummyEntity

@Database(
    entities = [DummyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NutriScanDatabase : RoomDatabase() {
    // abstract fun scanHistoryDao(): ScanHistoryDao
}
