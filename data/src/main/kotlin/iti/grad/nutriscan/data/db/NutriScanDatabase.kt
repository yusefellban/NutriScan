package iti.grad.nutriscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.entity.FoodLogEntity

import androidx.room.TypeConverters
import iti.grad.nutriscan.data.db.converter.IntListConverter
import iti.grad.nutriscan.data.db.dao.UserDao
import iti.grad.nutriscan.data.db.dao.DiseaseDao
import iti.grad.nutriscan.data.db.dao.AllergyDao
import iti.grad.nutriscan.data.db.dao.FamilyMemberDao
import iti.grad.nutriscan.data.db.entity.UserEntity
import iti.grad.nutriscan.data.db.entity.DiseaseEntity
import iti.grad.nutriscan.data.db.entity.AllergyEntity
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity

@Database(
    entities = [
        FoodLogEntity::class,
        UserEntity::class,
        DiseaseEntity::class,
        AllergyEntity::class,
        FamilyMemberEntity::class
    ],
    // Bumped 3 -> 4 for FamilyMemberEntity. Relies on fallbackToDestructiveMigration()
    // in DatabaseModule (same as prior entity additions) — this clears all local
    // tables on upgrade, not just the new one. Flagged per family-members plan §3.7.
    version = 4,
    exportSchema = false
)
@TypeConverters(IntListConverter::class)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun userDao(): UserDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun allergyDao(): AllergyDao
    abstract fun familyMemberDao(): FamilyMemberDao
}
