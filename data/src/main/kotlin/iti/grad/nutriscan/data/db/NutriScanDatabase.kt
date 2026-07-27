package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.dao.DailyTrackingDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.data.db.entity.SavedScanEntity
import iti.grad.nutriscan.data.db.entity.DailyTrackingEntity

import androidx.room.TypeConverters
import iti.grad.nutriscan.data.db.converter.IntListConverter
import iti.grad.nutriscan.data.db.converter.FamilyMemberListConverter
import iti.grad.nutriscan.data.db.converter.JsonTypeConverters
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.dao.DiseaseDao
import iti.grad.nutriscan.data.db.dao.AllergyDao
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import iti.grad.nutriscan.data.db.entity.AllergyEntity
import iti.grad.nutriscan.data.db.entity.ExerciseEntity
import iti.grad.nutriscan.data.db.entity.ExerciseCategoryEntity
import iti.grad.nutriscan.data.db.MIGRATION_4_5

@Database(
    entities = [
        FoodLogEntity::class,
        UserEntity::class,
        DiseaseEntity::class,
        AllergyEntity::class,
        ExerciseEntity::class,
        ExerciseCategoryEntity::class,
        SavedScanEntity::class,
        DailyTrackingEntity::class,
    ],
    // Bumped 6 -> 7 for FoodLogEntity's new pendingSync/deleted columns and the new
    // daily_tracking table. Bumped 7 -> 8 for DailyTrackingEntity's new exerciseKcal/
    // exerciseMinutes columns. Relies on fallbackToDestructiveMigration() in
    // DatabaseModule, same as the 5 -> 6 bump — this clears all local tables on upgrade.
    version = 8,
    exportSchema = false
)
@TypeConverters(IntListConverter::class, FamilyMemberListConverter::class, JsonTypeConverters::class)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun userDao(): UserDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun allergyDao(): AllergyDao
    abstract fun exercisesDao(): ExercisesDao
    abstract fun savedScanDao(): SavedScanDao
    abstract fun dailyTrackingDao(): DailyTrackingDao
}
