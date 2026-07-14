package iti.grad.nutriscan.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.db.NutriScanDatabase
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
        ).build()
    }

    // @Provides
    // fun provideScanHistoryDao(db: NutriScanDatabase): ScanHistoryDao = db.scanHistoryDao()
}
