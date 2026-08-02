package iti.grad.nutriscan.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.data.repository.OnboardingRepositoryImpl
import iti.grad.nutriscan.domain.settings.repository.IThemeRepository
import iti.grad.nutriscan.data.repository.ThemeRepositoryImpl
import iti.grad.nutriscan.domain.settings.repository.ILanguageRepository
import iti.grad.nutriscan.data.repository.LanguageRepositoryImpl
import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.data.repository.AuthRepositoryImpl
import iti.grad.nutriscan.domain.disease.repository.IDiseaseRepository
import iti.grad.nutriscan.data.repository.DiseaseRepositoryImpl
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import iti.grad.nutriscan.data.repository.UserRepositoryImpl
import iti.grad.nutriscan.domain.allergy.repository.IAllergyRepository
import iti.grad.nutriscan.data.repository.AllergyRepositoryImpl
import iti.grad.nutriscan.domain.steps.repository.IStepsRepository
import iti.grad.nutriscan.data.repository.StepsRepositoryImpl
import javax.inject.Singleton
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import iti.grad.nutriscan.data.repository.ScanRepositoryImpl
import iti.grad.nutriscan.domain.foodlog.repository.IFoodLogRepository
import iti.grad.nutriscan.data.repository.FoodLogRepositoryImpl
import iti.grad.nutriscan.domain.news.repository.INewsRepository
import iti.grad.nutriscan.data.repository.NewsRepositoryImpl
import iti.grad.nutriscan.domain.water.repository.IWaterRepository
import iti.grad.nutriscan.data.repository.WaterRepositoryImpl
import iti.grad.nutriscan.domain.workout.repository.IWorkoutRepository
import iti.grad.nutriscan.data.repository.WorkoutRepositoryImpl
import iti.grad.nutriscan.domain.streak.repository.IStreakRepository
import iti.grad.nutriscan.data.repository.StreakRepositoryImpl
import iti.grad.nutriscan.domain.notification.repository.INotificationRepository
import iti.grad.nutriscan.data.repository.NotificationRepositoryImpl
import iti.grad.nutriscan.domain.notification.repository.IQuoteRepository
import iti.grad.nutriscan.repository.QuoteRepositoryImpl
import iti.grad.nutriscan.domain.notification.repository.ITestNotificationSender
import iti.grad.nutriscan.notification.TestNotificationSenderImpl
import iti.grad.nutriscan.domain.family.repository.IFamilyMemberRepository
import iti.grad.nutriscan.data.repository.FamilyMemberRepositoryImpl
import iti.grad.nutriscan.domain.exercises.repository.IExercisesRepository
import iti.grad.nutriscan.data.repository.ExercisesRepositoryImpl
import iti.grad.nutriscan.domain.nutrigpt.repository.INutriGptRepository
import iti.grad.nutriscan.data.repository.NutriGptRepositoryImpl
import iti.grad.nutriscan.data.repository.SavedScanRepositoryImpl
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import iti.grad.nutriscan.domain.dailytracking.repository.IDailyTrackingRepository
import iti.grad.nutriscan.data.repository.DailyTrackingRepositoryImpl
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRepository
import iti.grad.nutriscan.data.repository.NotificationHistoryRepositoryImpl
import iti.grad.nutriscan.domain.notification.repository.INotificationHistoryRecorder
import iti.grad.nutriscan.data.repository.NotificationHistoryRecorderImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): IOnboardingRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: ThemeRepositoryImpl
    ): IThemeRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        impl: LanguageRepositoryImpl
    ): ILanguageRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        impl: ScanRepositoryImpl
    ): IScanRepository
     @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindDiseaseRepository(
        impl: DiseaseRepositoryImpl
    ): IDiseaseRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAllergyRepository(
        impl: AllergyRepositoryImpl
    ): IAllergyRepository

    @Binds
    @Singleton
    abstract fun bindStepsRepository(
        impl: StepsRepositoryImpl
    ): IStepsRepository

    @Binds
    @Singleton
    abstract fun bindFoodLogRepository(
        impl: FoodLogRepositoryImpl
    ): IFoodLogRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): INewsRepository

    @Binds
    @Singleton
    abstract fun bindWaterRepository(
        impl: WaterRepositoryImpl
    ): IWaterRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): IWorkoutRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(
        impl: StreakRepositoryImpl
    ): IStreakRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): INotificationRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        impl: QuoteRepositoryImpl
    ): IQuoteRepository

    @Binds
    @Singleton
    abstract fun bindTestNotificationSender(
        impl: TestNotificationSenderImpl
    ): ITestNotificationSender

    @Binds
    @Singleton
    abstract fun bindFamilyMemberRepository(
        impl: FamilyMemberRepositoryImpl
    ): IFamilyMemberRepository

    @Binds
    @Singleton
    abstract fun bindExercisesRepository(
        impl: ExercisesRepositoryImpl
    ): IExercisesRepository

    @Binds
    @Singleton
    abstract fun bindNutriGptRepository(
        impl: NutriGptRepositoryImpl
    ): INutriGptRepository

    @Binds
    @Singleton
    abstract fun bindSavedScanRepository(
        impl: SavedScanRepositoryImpl
    ): ISavedScanRepository

    @Binds
    @Singleton
    abstract fun bindDailyTrackingRepository(
        impl: DailyTrackingRepositoryImpl
    ): IDailyTrackingRepository

    @Binds
    @Singleton
    abstract fun bindStepHistoryRepository(
        impl: iti.grad.nutriscan.data.repository.StepHistoryRepositoryImpl
    ): iti.grad.nutriscan.domain.steps.history.repository.IStepHistoryRepository

    @Binds
    @Singleton
    abstract fun bindNotificationHistoryRepository(
        impl: NotificationHistoryRepositoryImpl
    ): INotificationHistoryRepository

    @Binds
    @Singleton
    abstract fun bindNotificationHistoryRecorder(
        impl: NotificationHistoryRecorderImpl
    ): INotificationHistoryRecorder
}
