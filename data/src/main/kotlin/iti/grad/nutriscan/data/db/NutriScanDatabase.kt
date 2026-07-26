package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity
import iti.grad.nutriscan.data.db.entity.SavedScanEntity

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
        SavedScanEntity::class
    ],
    // Bumped 5 -> 6 for embedding family members list inside UserEntity. Relies on fallbackToDestructiveMigration()
    // in DatabaseModule — this clears all local tables on upgrade.
    version = 6,
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
}
