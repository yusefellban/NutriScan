package iti.grad.nutriscan.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.data.local.datasource.IOnboardingPreferencesDataSource
import iti.grad.nutriscan.data.local.datasource.OnboardingPreferencesDataSourceImpl
import iti.grad.nutriscan.data.local.datasource.IThemePreferencesDataSource
import iti.grad.nutriscan.data.local.datasource.ThemePreferencesDataSourceImpl
import iti.grad.nutriscan.data.local.datasource.ILanguagePreferencesDataSource
import iti.grad.nutriscan.data.local.datasource.LanguagePreferencesDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingPreferencesDataSource(
        impl: OnboardingPreferencesDataSourceImpl
    ): IOnboardingPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindThemePreferencesDataSource(
        impl: ThemePreferencesDataSourceImpl
    ): IThemePreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindLanguagePreferencesDataSource(
        impl: LanguagePreferencesDataSourceImpl
    ): ILanguagePreferencesDataSource

    companion object {
        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("nutriscan_preferences") }
            )
        }
    }
}
