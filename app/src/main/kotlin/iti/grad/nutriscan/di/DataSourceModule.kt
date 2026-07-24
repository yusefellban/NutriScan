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
import iti.grad.nutriscan.data.local.datasource.IStepsPreferencesDataSource
import iti.grad.nutriscan.data.local.datasource.StepsPreferencesDataSourceImpl
import iti.grad.nutriscan.data.local.datasource.INotificationPreferencesDataSource
import iti.grad.nutriscan.data.local.datasource.NotificationPreferencesDataSourceImpl
import iti.grad.nutriscan.data.remote.datasource.AuthRemoteDataSourceImpl
import iti.grad.nutriscan.data.remote.datasource.IAuthRemoteDataSource
import iti.grad.nutriscan.data.remote.datasource.DiseaseRemoteDataSourceImpl
import iti.grad.nutriscan.data.remote.datasource.IDiseaseRemoteDataSource
import iti.grad.nutriscan.data.remote.datasource.UserRemoteDataSourceImpl
import iti.grad.nutriscan.data.remote.datasource.IUserRemoteDataSource
import iti.grad.nutriscan.data.remote.datasource.AllergyRemoteDataSourceImpl
import iti.grad.nutriscan.data.remote.datasource.IAllergyRemoteDataSource
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

    @Binds
    @Singleton
    abstract fun bindStepsPreferencesDataSource(
        impl: StepsPreferencesDataSourceImpl
    ): IStepsPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationPreferencesDataSource(
        impl: NotificationPreferencesDataSourceImpl
    ): INotificationPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        impl: AuthRemoteDataSourceImpl
    ): IAuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDiseaseRemoteDataSource(
        impl: DiseaseRemoteDataSourceImpl
    ): IDiseaseRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(
        impl: UserRemoteDataSourceImpl
    ): IUserRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAllergyRemoteDataSource(
        impl: AllergyRemoteDataSourceImpl
    ): IAllergyRemoteDataSource


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
