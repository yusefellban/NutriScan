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
import iti.grad.nutriscan.presentation.auth.login.view.LoginScreen
import iti.grad.nutriscan.presentation.auth.register.view.RegisterScreen
import iti.grad.nutriscan.presentation.home.view.HomeScreen
import iti.grad.nutriscan.presentation.auth.forgot_password.view.ForgotPasswordScreen
import iti.grad.nutriscan.presentation.onboarding.carousel.view.OnboardingCarouselScreen
import iti.grad.nutriscan.presentation.profile_setup.view.ProfileSetupPagerScreen
import iti.grad.nutriscan.presentation.onboarding.splash.SplashScreen
import iti.grad.nutriscan.presentation.common.theme.AppTheme
import iti.grad.nutriscan.presentation.settings.profile.view.UserProfileScreen
import iti.grad.nutriscan.presentation.settings.profile.edit.view.EditProfileScreen
import iti.grad.nutriscan.presentation.settings.app.view.AppSettingsScreen
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
                    navController.navigate(LoginRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<OnboardingRoute> {
            OnboardingCarouselScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
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
                navController.navigate(LoginRoute) {
                    popUpTo(OnboardingCarouselRoute) { inclusive = true }
                }
            }
        }

        // 3. Login Screen
        composable<LoginRoute> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
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
                onNavigateToHome = {
                    navController.navigate(ProfileSetupPagerRoute) {
                        popUpTo(RegisterRoute) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigateUp()
                }
            )
        }

        // 5. Profile Setup Pager
        composable<ProfileSetupPagerRoute> {
            ProfileSetupPagerScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
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
                navController.navigate(HomeRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                }
            }
        }

        // 7. Home Screen
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToScan = {
                    navController.navigate(CameraScanRoute)
                },
                onNavigateToHistory = {
                    navController.navigate(ScanHistoryRoute)
                },
                onNavigateToShopping = {
                    navController.navigate(ShoppingListRoute)
                },
                onNavigateToProfile = {
                    navController.navigate(UserProfileRoute)
                },
                onNavigateToNotifications = {
                    navController.navigate(NotificationSettingsRoute)
                },
                onNavigateToScanResult = { scanId ->
                    navController.navigate(ScanResultRoute(scanId))
                },
            )
        }

        // 8. Camera Scan (Placeholder)
        composable<CameraScanRoute> {
            PlaceholderScreen(
                title = "Camera Scan",
                buttonText = "Capture & Process"
            ) {
                navController.navigate(ScanProcessingRoute("content://media/external/images/media/dummy"))
            }
        }

        // 9. Scan Processing (Placeholder)
        composable<ScanProcessingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScanProcessingRoute>()
            PlaceholderScreen(
                title = "Scan Processing\nImage URI: ${route.imageUri}",
                buttonText = "View Results"
            ) {
                navController.navigate(ScanResultRoute(route.imageUri)) {
                    popUpTo(ScanProcessingRoute(route.imageUri)) { inclusive = true }
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

        // 11. NutriGPT Chat (Placeholder)
        composable<NutriGptRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NutriGptRoute>()
            PlaceholderScreen(
                title = "NutriGPT Chat\nScan ID: ${route.scanResultId}",
                buttonText = "Back to Home"
            ) {
                navController.navigate(HomeRoute) {
                    popUpTo(HomeRoute) { inclusive = false }
                }
            }
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
                navController.navigate(HomeRoute) {
                    popUpTo(HomeRoute) { inclusive = false }
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

        // 18. Shopping List (Placeholder)
        composable<ShoppingListRoute> {
            PlaceholderScreen(
                title = "Shopping List",
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }

        // 19. User Profile
        composable<UserProfileRoute> {
            UserProfileScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                    }
                },
                onNavigateToScan = { navController.navigate(CameraScanRoute) },
                onNavigateToScanHistory = { navController.navigate(ScanHistoryRoute) },
                onNavigateToShopping = { navController.navigate(ShoppingListRoute) },
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToFamilyMemberDetail = { memberId ->
                    navController.navigate(EditConditionsRoute(memberId))
                },
                onNavigateToNotifications = { navController.navigate(NotificationSettingsRoute) },
                onNavigateToSettings = { navController.navigate(AppSettingsRoute) },
            )
        }

        // 19b. Edit Profile
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
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // 24. Edit Profile (Placeholder)
        composable<EditProfileRoute> {
            PlaceholderScreen(
                title = stringResource(R.string.placeholder_edit_profile_title),
                buttonText = stringResource(R.string.action_go_back),
            ) {
                navController.navigateUp()
            }
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
