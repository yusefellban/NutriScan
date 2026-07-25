package iti.grad.nutriscan.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import iti.grad.nutriscan.domain.common.model.ProductVerdict
import kotlin.reflect.typeOf
import iti.grad.nutriscan.presentation.main.container.view.MainScreen
import iti.grad.nutriscan.presentation.auth.login.view.LoginScreen
import iti.grad.nutriscan.presentation.auth.email_verification.view.EmailVerificationScreen
import iti.grad.nutriscan.presentation.auth.register.view.RegisterScreen
import iti.grad.nutriscan.presentation.home.view.HomeScreen
import iti.grad.nutriscan.presentation.saved.view.SavedScreen
import iti.grad.nutriscan.presentation.auth.forgot_password.view.ForgotPasswordScreen
import iti.grad.nutriscan.presentation.common.model.ProductUiModel
import iti.grad.nutriscan.presentation.onboarding.carousel.view.OnboardingCarouselScreen
import iti.grad.nutriscan.presentation.profile_setup.view.ProfileSetupPagerScreen
import iti.grad.nutriscan.presentation.onboarding.splash.SplashScreen
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.edit.view.EditProfileScreen
import iti.grad.nutriscan.presentation.settings.app.view.AppSettingsScreen
import iti.grad.nutriscan.presentation.product_details.view.ProductDetailsScreen
import iti.grad.nutriscan.presentation.news.view.NewsScreen
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.NutriGptScreen
import iti.grad.nutriscan.presentation.nutrigpt.voice.view.NutriGptVoiceScreen
import iti.grad.nutriscan.presentation.scan.camera.view.CameraScanScreen
import iti.grad.nutriscan.presentation.exercises.view.ExercisesScreen
import iti.grad.nutriscan.presentation.exercises.workout.view.ExerciseWorkoutScreen
import iti.grad.presentation.R

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = SplashRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Splash Screen
        composable<SplashRoute> {
            SplashScreen(
//                onNavigateToHome = {
//                    navController.navigate(OnboardingCarouselRoute)
//                },
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(LoginRoute()) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(MainRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<OnboardingRoute> {
            OnboardingCarouselScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute()) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )
        }

        // 2. Onboarding Carousel (Placeholder)
        composable<OnboardingCarouselRoute> {
            PlaceholderScreen(
                title = "Onboarding Carousel",
                buttonText = "Get Started"
            ) {
                navController.navigate(LoginRoute()) {
                    popUpTo(OnboardingCarouselRoute) { inclusive = true }
                }
            }
        }

        // 3. Login Screen
        composable<LoginRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<LoginRoute>()
            LoginScreen(
                onNavigateToHome = {
                    if (route.isFromRegistration) {
                        navController.navigate(ProfileSetupPagerRoute) {
                            popUpTo(LoginRoute(isFromRegistration = true)) { inclusive = true }
                        }
                    } else {
                        navController.navigate(MainRoute) {
                            popUpTo(LoginRoute(isFromRegistration = false)) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(ForgotPasswordRoute)
                }
            )
        }

        // 4. Forgot Password Screen
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // 5. Register Screen
        composable<RegisterRoute> {
            RegisterScreen(
                onNavigateToEmailVerification = { email ->
                    navController.navigate(EmailVerificationRoute(email)) {
                        popUpTo(RegisterRoute) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigateUp()
                }
            )
        }

        // 5b. Email Verification Screen
        composable<EmailVerificationRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EmailVerificationRoute>()
            EmailVerificationScreen(
                onNavigateToSignIn = {
                    navController.navigate(LoginRoute(isFromRegistration = true)) {
                        popUpTo(EmailVerificationRoute(route.email)) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // 5. Profile Setup Pager
        composable<ProfileSetupPagerRoute> {
            ProfileSetupPagerScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToHome = {
                    navController.navigate(MainRoute) {
                        popUpTo(ProfileSetupPagerRoute) { inclusive = true }
                    }
                }
            )
        }

        // 6. Family Profile Setup (Placeholder)
        composable<FamilyProfileSetupRoute> {
            PlaceholderScreen(
                title = "Family Profile Setup",
                buttonText = "Complete Setup"
            ) {
                navController.navigate(MainRoute) {
                    popUpTo(LoginRoute::class) { inclusive = true }
                }
            }
        }

        // 7. Main Screen (Container for Home, Scan, Calories, Saved, Profile)
        composable<MainRoute> {
            MainScreen(
                onNavigateToScanResult = { scanId ->
                    navController.navigate(ScanResultRoute(scanId))
                },
                onNavigateToScanProcessing = { barcode ->
                    navController.navigate(ScanProcessingRoute(barcode = barcode))
                },
                onNavigateToProductDetail = { product ->
                    navController.navigate(ProductDetailsRoute(product = product))
                },
                onNavigateToNews = { navController.navigate(NewsRoute) },
                onNavigateToChatWithAi = { navController.navigate(ChatWithAiRoute) },
                onNavigateToHistory = { navController.navigate(ScanHistoryRoute) },
                onNavigateToNotifications = { navController.navigate(NotificationSettingsRoute) },
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToFamilyMemberDetail = { memberId ->
                    navController.navigate(EditConditionsRoute(memberId))
                },
                onNavigateToSettings = { navController.navigate(AppSettingsRoute) },
                onNavigateToExercises = { navController.navigate(ExercisesRoute) },
            )
        }

        // 9. Scan Processing (Placeholder)
        composable<ScanProcessingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScanProcessingRoute>()
            PlaceholderScreen(
                title = "Scan Processing\nCode: ${route.barcode}",
                buttonText = "View Results"
            ) {
                navController.navigate(ScanResultRoute(route.imageUri ?: "")) {
                    popUpTo(ScanProcessingRoute(barcode = route.barcode, imageUri = route.imageUri)) { inclusive = true }
                }
            }
        }

        // 10. Scan Result (Placeholder)
        composable<ScanResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScanResultRoute>()
            PlaceholderScreen(
                title = "Scan Result\nImage URI: ${route.imageUri}",
                buttonText = "Chat with NutriGPT"
            ) {
                navController.navigate(NutriGptRoute("dummy_scan_id"))
            }
        }

        // 11. NutriGPT Chat
        composable<NutriGptRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NutriGptRoute>()
            NutriGptScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToVoice = { navController.navigate(NutriGptVoiceRoute) }
            )
        }

        // 12. Ingredient Detail (Placeholder)
        composable<IngredientDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<IngredientDetailRoute>()
            PlaceholderScreen(
                title = "Ingredient Detail\nName: ${route.ingredientName}",
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }

        // 13. Receipt Capture (Placeholder)
        composable<ReceiptCaptureRoute> {
            PlaceholderScreen(
                title = "Receipt Capture",
                buttonText = "Process Receipt"
            ) {
                navController.navigate(ReceiptResultRoute("content://media/external/images/media/receipt_dummy"))
            }
        }

        // 14. Receipt Result (Placeholder)
        composable<ReceiptResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReceiptResultRoute>()
            PlaceholderScreen(
                title = "Receipt Result\nURI: ${route.receiptImageUri}",
                buttonText = "Back to Home"
            ) {
                navController.navigate(MainRoute) {
                    popUpTo(MainRoute) { inclusive = false }
                }
            }
        }

        // 15. Scan History (Placeholder)
        composable<ScanHistoryRoute> {
            PlaceholderScreen(
                title = "Scan History",
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }

        // 16. Report List (Placeholder)
        composable<ReportListRoute> {
            PlaceholderScreen(
                title = "Weekly Reports",
                buttonText = "View Detail"
            ) {
                navController.navigate(ReportDetailRoute("report_123"))
            }
        }

        // 17. Report Detail (Placeholder)
        composable<ReportDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReportDetailRoute>()
            PlaceholderScreen(
                title = "Report Detail\nReport ID: ${route.reportId}",
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }


        composable<EditProfileRoute> {
            EditProfileScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 20. Manage Family (Placeholder)
        composable<ManageFamilyRoute> {
            PlaceholderScreen(
                title = "Manage Family",
                buttonText = "Edit Conditions"
            ) {
                navController.navigate(EditConditionsRoute("family_member_456"))
            }
        }

        // 21. Edit Conditions (Placeholder)
        composable<EditConditionsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditConditionsRoute>()
            PlaceholderScreen(
                title = "Edit Conditions\nMember ID: ${route.memberProfileId}",
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }

        // 22. Notification Settings (Placeholder)
        composable<NotificationSettingsRoute> {
            PlaceholderScreen(
                title = "Notification Settings",
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }

        // 23. App Settings
        composable<AppSettingsRoute> {
            AppSettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToTermsAndConditions = { navController.navigate(TermsAndConditionsRoute) },
                onNavigateToHelp = { navController.navigate(HelpRoute) },
                onNavigateToLogin = {
                    navController.navigate(LoginRoute()) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }



        // 25. Terms and Conditions (Placeholder)
        composable<TermsAndConditionsRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.app_settings_terms_and_conditions),
                buttonText = stringResource(R.string.action_go_back),
            ) {
                navController.navigateUp()
            }
        }

        // 26. Help (Placeholder)
        composable<HelpRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.app_settings_help),
                buttonText = stringResource(R.string.action_go_back),
            ) {
                navController.navigateUp()
            }
        }


        // 28. Exercises
        composable<ExercisesRoute> {
            ExercisesScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToWorkout = { exerciseId ->
                    navController.navigate(ExerciseWorkoutRoute(exerciseId))
                }
            )
        }

        // 28b. Exercise Workout Screen
        composable<ExerciseWorkoutRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ExerciseWorkoutRoute>()
            ExerciseWorkoutScreen(
                exerciseId = route.exerciseId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToCalories = {
                    navController.navigate(CaloriesRoute) {
                        popUpTo<ExercisesRoute> { inclusive = true }
                    }
                }
            )
        }
        // 29. Product Details
        composable<ProductDetailsRoute>(
            typeMap = mapOf(typeOf<ProductUiModel>() to ProductUiModelNavType)
        ) {
            ProductDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
         // 30. News
        composable<NewsRoute> {
            NewsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }

        // 31. Chat with AI
        composable<ChatWithAiRoute> {
            NutriGptScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToVoice = { navController.navigate(NutriGptVoiceRoute) }
            )
        }

        // 32. Voice Chat with AI
        composable<NutriGptVoiceRoute> {
            NutriGptVoiceScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 29. Product Details Placeholder

    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    buttonText: String,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.Background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext) {
                Text(buttonText)
            }
        }
    }
}
