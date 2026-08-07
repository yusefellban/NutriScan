package iti.grad.nutriscan.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.db.MIGRATION_4_5
import iti.grad.nutriscan.data.db.NutriScanDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
import iti.grad.nutriscan.data.db.dao.ExercisesDao
import iti.grad.nutriscan.data.db.dao.NotificationHistoryDao
import iti.grad.nutriscan.data.db.dao.SavedScanDao
import iti.grad.nutriscan.data.db.migration.MIGRATION_3_4
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNutriScanDatabase(@ApplicationContext context: Context): NutriScanDatabase {
        return Room.databaseBuilder(
            context,
            NutriScanDatabase::class.java,
            "nutriscan_db"
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
    fun provideWorkoutLogDao(db: NutriScanDatabase) = db.workoutLogDao()

    @Provides
    @Singleton
    fun provideStreakDao(db: NutriScanDatabase) = db.streakDao()

    @Provides
    @Singleton
    fun provideExercisesDao(db: NutriScanDatabase): ExercisesDao = db.exercisesDao()

    @Provides
    @Singleton
    fun provideSavedScanDao(db: NutriScanDatabase): SavedScanDao = db.savedScanDao()

    @Provides
    @Singleton
    fun provideDailyTrackingDao(db: NutriScanDatabase): iti.grad.nutriscan.data.db.dao.DailyTrackingDao =
        db.dailyTrackingDao()

    @Provides
    @Singleton
    fun provideNotificationHistoryDao(db: NutriScanDatabase): NotificationHistoryDao =
        db.notificationHistoryDao()
}
