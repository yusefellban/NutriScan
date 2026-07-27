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
import iti.grad.nutriscan.data.db.dao.WaterLogDao
import iti.grad.nutriscan.data.db.dao.WorkoutLogDao
import iti.grad.nutriscan.data.db.dao.StreakDao
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import iti.grad.nutriscan.data.db.entity.AllergyEntity
import iti.grad.nutriscan.data.db.entity.WaterLogEntity
import iti.grad.nutriscan.data.db.entity.WorkoutLogEntity
import iti.grad.nutriscan.data.db.entity.StreakEntity
import iti.grad.nutriscan.data.db.entity.ExerciseEntity
import iti.grad.nutriscan.data.db.entity.ExerciseCategoryEntity
import iti.grad.nutriscan.data.db.MIGRATION_4_5

@Database(
    entities = [
        FoodLogEntity::class,
        UserEntity::class,
        DiseaseEntity::class,
        AllergyEntity::class,
        WaterLogEntity::class,
        WorkoutLogEntity::class,
        StreakEntity::class,
        ExerciseEntity::class,
        ExerciseCategoryEntity::class,
        SavedScanEntity::class,
        DailyTrackingEntity::class,
    ],
    // v3 -> v4: MIGRATION_3_4 (water_log/workout_log/streak). v4 -> v5: MIGRATION_4_5
    // (users.bmi/tdee). Everything after (family_members, exercises, saved_scan,
    // daily_tracking, FoodLogEntity's pendingSync/deleted columns, SavedScanEntity's
    // userId/pendingSync/deleted columns) relies on fallbackToDestructiveMigration()
    // in DatabaseModule — this clears all local tables on upgrade.
    version = 10,
    exportSchema = false
)
@TypeConverters(IntListConverter::class, FamilyMemberListConverter::class, JsonTypeConverters::class)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun userDao(): UserDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun allergyDao(): AllergyDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun streakDao(): StreakDao
    abstract fun exercisesDao(): ExercisesDao
    abstract fun savedScanDao(): SavedScanDao
    abstract fun dailyTrackingDao(): DailyTrackingDao
}
