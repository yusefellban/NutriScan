package iti.grad.nutriscan.navigation

import kotlinx.serialization.Serializable
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
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
data class MainRoute(val initialTab: String = "HOME")

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
object SavedRoute

@Serializable
object UserProfileRoute

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

val ProductUiModelNavType = object : NavType<ProductUiModel>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): ProductUiModel? {
        return bundle.getString(key)?.let { Json.decodeFromString(it) }
    }
    override fun parseValue(value: String): ProductUiModel {
        return Json.decodeFromString(Uri.decode(value))
    }
    override fun put(bundle: Bundle, key: String, value: ProductUiModel) {
        bundle.putString(key, Json.encodeToString(value))
    }
    override fun serializeAsValue(value: ProductUiModel): String {
        return Uri.encode(Json.encodeToString(value))
    }
}

@Serializable
data class ProductDetailsRoute(val product: ProductUiModel)
@Serializable
data class EmailVerificationRoute(val email: String)

@Serializable
object CaloriesRoute

@Serializable
object ExercisesRoute

@Serializable
data class ExerciseWorkoutRoute(val exerciseId: String)

@Serializable
object NewsRoute

@Serializable
object ChatWithAiRoute
