package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.entity.DummyEntity

import androidx.room.TypeConverters
import iti.grad.nutriscan.data.db.converter.IntListConverter
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.entity.UserEntity

@Database(
    entities = [DummyEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(IntListConverter::class)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    // abstract fun scanHistoryDao(): ScanHistoryDao
}
