package iti.grad.nutriscan.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.db.NutriScanDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.db.dao.SavedScanDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `family_members` (
                    `id` TEXT NOT NULL, 
                    `ownerUserId` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `allergyIds` TEXT NOT NULL, 
                    `diseaseIds` TEXT NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """)
                CREATE TABLE IF NOT EXISTS `exercises` (
                    `id` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `bodyPart` TEXT NOT NULL, 
                    `equipment` TEXT NOT NULL, 
                    `target` TEXT NOT NULL, 
                    `secondaryMuscles` TEXT NOT NULL, 
                    `instructions` TEXT NOT NULL, 
                    `instructionSteps` TEXT NOT NULL, 
                    `imageUrl` TEXT, 
                    `gifUrl` TEXT, 
                    `repKcal` REAL, 
                    `minKcal` REAL, 
                    PRIMARY KEY(`id`)
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `exercise_categories` (
                    `name` TEXT NOT NULL, 
                    PRIMARY KEY(`name`)
                )
            """)
        }
    }

    @Provides
    @Singleton
    fun provideNutriScanDatabase(@ApplicationContext context: Context): NutriScanDatabase {
        return Room.databaseBuilder(
            context,
            NutriScanDatabase::class.java,
            "nutriscan_db"
        )
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFoodLogDao(db: NutriScanDatabase): FoodLogDao = db.foodLogDao()

    @Provides
    @Singleton
    fun provideUserDao(db: NutriScanDatabase) = db.userDao()

    @Provides
    @Singleton
    fun provideDiseaseDao(db: NutriScanDatabase) = db.diseaseDao()

    @Provides
    @Singleton
    fun provideAllergyDao(db: NutriScanDatabase) = db.allergyDao()

    @Provides
    @Singleton
    fun provideExercisesDao(db: NutriScanDatabase): ExercisesDao = db.exercisesDao()

    @Provides
    @Singleton
    fun provideSavedScanDao(db: NutriScanDatabase): SavedScanDao = db.savedScanDao()
}
