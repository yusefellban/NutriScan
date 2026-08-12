package iti.grad.nutriscan.navigation

import kotlinx.serialization.Serializable
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
@Serializable
object SplashRoute

@Serializable
object OnboardingCarouselRoute

@Serializable
data class LoginRoute(val isFromRegistration: Boolean = false)

@Serializable
object OnboardingRoute

@Serializable
object RegisterRoute

@Serializable
object ProfileSetupPagerRoute

@Serializable
object FamilyProfileSetupRoute

@Serializable
object HomeRoute

@Serializable
data class MainRoute(val initialTab: BottomNavTab = BottomNavTab.HOME)

@Serializable
object CameraScanRoute

@Serializable
object ForgotPasswordRoute

@Serializable
data class ScanProcessingRoute(
    val barcode: String,
    val imageUri: String? = null,
)

@Serializable
object NutriGptRoute

@Serializable
data class IngredientDetailRoute(val ingredientName: String)

@Serializable
object ReceiptCaptureRoute

@Serializable
data class ReceiptResultRoute(val receiptImageUri: String)

@Serializable
object ScanHistoryRoute

@Serializable
object ReportListRoute

@Serializable
data class ReportDetailRoute(val reportId: String)

@Serializable
object SavedRoute

@Serializable
object UserProfileRoute

@Serializable
object ManageFamilyRoute

@Serializable
object NotificationSettingsRoute

@Serializable
object AppSettingsRoute

@Serializable
object EditProfileRoute

@Serializable
object TermsAndConditionsRoute


@Serializable
object HelpRoute

@Serializable
data class ProductDetailsRoute(val scanId: String)
@Serializable
data class EmailVerificationRoute(val email: String)

@Serializable
object CaloriesRoute

@Serializable
object ExercisesRoute

@Serializable
data class ExerciseWorkoutRoute(val exerciseId: String)

@Serializable
object NewsHomeRoute

@Serializable
data class NewsDetailRoute(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAtLabel: String,
    val author: String?,
    val category: String,
)

@Serializable
object NewsRoute

@Serializable
object ChatWithAiRoute

@Serializable
object NutriGptVoiceRoute

@Serializable
object StepHistoryRoute
@Serializable
object NotificationHistoryRoute
@Serializable
object CaloriesHistoryRoute

@Serializable
data class AccountPendingDeletionRoute(val scheduledDeletionAt: String)