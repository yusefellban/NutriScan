package iti.grad.nutriscan.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object OnboardingCarouselRoute

@Serializable
object LoginRoute

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
object CameraScanRoute

@Serializable
object ForgotPasswordRoute

@Serializable
data class ScanProcessingRoute(
    val barcode: String,
    val imageUri: String? = null,
)

@Serializable
data class ScanResultRoute(val imageUri: String)

@Serializable
data class NutriGptRoute(val scanResultId: String)

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
object ShoppingListRoute

@Serializable
object UserProfileRoute

@Serializable
object EditProfileRoute

@Serializable
object ManageFamilyRoute

@Serializable
data class EditConditionsRoute(val memberProfileId: String)

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
data class ProductDetailsPlaceholderRoute(val barcode: String)
