package iti.grad.nutriscan.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.db.NutriScanDatabase
import iti.grad.nutriscan.data.db.dao.FoodLogDao
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
        ).fallbackToDestructiveMigration().build()
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
    fun provideFamilyMemberDao(db: NutriScanDatabase) = db.familyMemberDao()
}
