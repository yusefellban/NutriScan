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
import iti.grad.nutriscan.presentation.main.calories.stephistory.view.StepHistoryScreen
import iti.grad.nutriscan.presentation.calories_history.view.CaloriesHistoryScreen
import iti.grad.nutriscan.presentation.main.calories.view.CaloriesScreen
import iti.grad.nutriscan.presentation.main.container.view.MainScreen
import iti.grad.nutriscan.presentation.common.model.BottomNavTab
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
import iti.grad.nutriscan.presentation.settings.help.view.HelpScreen
import iti.grad.nutriscan.presentation.settings.terms.view.TermsAndConditionsScreen
import iti.grad.nutriscan.presentation.settings.notifications.view.NotificationSettingsScreen
import iti.grad.nutriscan.presentation.product_details.view.ProductDetailsScreen
import iti.grad.nutriscan.presentation.news.view.NewsScreen
import iti.grad.nutriscan.presentation.news.home.view.NewsHomeScreen
import iti.grad.nutriscan.presentation.news.state.NewsUiArticle
import iti.grad.nutriscan.presentation.news.detail.view.NewsDetailScreen
import iti.grad.nutriscan.presentation.nutrigpt.chat.view.NutriGptScreen
import iti.grad.nutriscan.presentation.nutrigpt.voice.view.NutriGptVoiceScreen
import iti.grad.nutriscan.presentation.scan.camera.view.CameraScanScreen
import iti.grad.nutriscan.presentation.scan_history.view.ScanHistoryScreen
import iti.grad.nutriscan.presentation.notification_history.view.NotificationHistoryScreen
import iti.grad.nutriscan.presentation.account_deletion.view.AccountPendingDeletionScreen
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
                    navController.navigate(MainRoute()) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(ProfileSetupPagerRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToAccountPendingDeletion = { scheduledDate ->
                    navController.navigate(AccountPendingDeletionRoute(scheduledDate)) {
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
                    navController.navigate(MainRoute()) {
                        popUpTo(LoginRoute(isFromRegistration = route.isFromRegistration)) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(ProfileSetupPagerRoute) {
                        popUpTo(LoginRoute(isFromRegistration = route.isFromRegistration)) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(ForgotPasswordRoute)
                },
                onNavigateToAccountPendingDeletion = { scheduledDate ->
                    navController.navigate(AccountPendingDeletionRoute(scheduledDate)) {
                        popUpTo(LoginRoute(isFromRegistration = route.isFromRegistration)) { inclusive = true }
                    }
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
                    navController.navigate(MainRoute()) {
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
                navController.navigate(MainRoute()) {
                    popUpTo(LoginRoute::class) { inclusive = true }
                }
            }
        }

        // 7. Main Screen (Container for Home, Scan, Calories, Saved, Profile)
        composable<MainRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MainRoute>()
            MainScreen(
                initialTab = route.initialTab,
                onNavigateToScanProcessing = { barcode ->
                    navController.navigate(ScanProcessingRoute(barcode = barcode))
                },
                onNavigateToProductDetail = { scanId ->
                    navController.navigate(ProductDetailsRoute(scanId = scanId))
                },
                onNavigateToNews = { navController.navigate(NewsHomeRoute) },
                onNavigateToChatWithAi = { navController.navigate(ChatWithAiRoute) },
                onNavigateToHistory = { navController.navigate(ScanHistoryRoute) },
                onNavigateToNotifications = { navController.navigate(NotificationHistoryRoute) },
                onNavigateToNotificationSettings = { navController.navigate(NotificationSettingsRoute) },
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToFamilyMemberDetail = { _ -> },
                onNavigateToSettings = { navController.navigate(AppSettingsRoute) },
                onNavigateToExercises = { navController.navigate(ExercisesRoute) },
                onNavigateToStepHistory = { navController.navigate(StepHistoryRoute) },
                onNavigateToCaloriesHistory = { navController.navigate(CaloriesHistoryRoute) },
                onNavigateToAccountPendingDeletion = { scheduledDate ->
                    navController.navigate(AccountPendingDeletionRoute(scheduledDeletionAt = scheduledDate)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable<AccountPendingDeletionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountPendingDeletionRoute>()
            AccountPendingDeletionScreen(
                scheduledDeletionAt = route.scheduledDeletionAt,
                onNavigateToHome = { navController.navigate(MainRoute()) { popUpTo(0) { inclusive = true } } },
                onNavigateToLogin = { navController.navigate(LoginRoute()) { popUpTo(0) { inclusive = true } } }
            )
        }

        // 9. Scan Processing (Placeholder)
        composable<ScanProcessingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ScanProcessingRoute>()
            PlaceholderScreen(
                title = "Scan Processing\nCode: ${route.barcode}",
                buttonText = "View Results"
            ) {
                // Navigate to Product Details directly if processing completes, currently this is just placeholder.
                navController.navigateUp()
            }
        }

        // 11. NutriGPT Chat
        composable<NutriGptRoute> {
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
                navController.navigate(MainRoute()) {
                    popUpTo<MainRoute> { inclusive = false }
                }
            }
        }

        // 15. Scan History
        composable<ScanHistoryRoute> {
            ScanHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToProductDetails = { scanId ->
                    navController.navigate(ProductDetailsRoute(scanId = scanId))
                }
            )
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
                buttonText = "Go Back"
            ) {
                navController.navigateUp()
            }
        }

        // 22. Notification Settings
        composable<NotificationSettingsRoute> {
            NotificationSettingsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }

        composable<NotificationHistoryRoute> {
            NotificationHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToSettings = { navController.navigate(NotificationSettingsRoute) }
            )
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



        // 25. Terms and Conditions
        composable<TermsAndConditionsRoute> {
            TermsAndConditionsScreen(
                onNavigateBack = { navController.navigateUp() },
            )
        }

        // 26. Help
        composable<HelpRoute> {
            HelpScreen(
                onNavigateBack = { navController.navigateUp() },
            )
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
                    navController.navigate(MainRoute(initialTab = BottomNavTab.CALORIES)) {
                        popUpTo<MainRoute> { inclusive = true }
                    }
                }
            )
        }
        // 29. Product Details
        composable<ProductDetailsRoute> {
            ProductDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
         // 30a. News Home (Landing)
        composable<NewsHomeRoute> {
            NewsHomeScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDiscover = { navController.navigate(NewsRoute) },
                onNavigateToDetail = { article ->
                    navController.navigate(
                        NewsDetailRoute(
                            title = article.title,
                            description = article.description,
                            url = article.url,
                            imageUrl = article.imageUrl,
                            sourceName = article.sourceName,
                            publishedAtLabel = article.publishedAtLabel,
                            author = article.author,
                            category = article.category
                        )
                    )
                }
            )
        }

         // 30b. News Discover (Search)
        composable<NewsRoute> {
            NewsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDetail = { article ->
                    navController.navigate(
                        NewsDetailRoute(
                            title = article.title,
                            description = article.description,
                            url = article.url,
                            imageUrl = article.imageUrl,
                            sourceName = article.sourceName,
                            publishedAtLabel = article.publishedAtLabel,
                            author = article.author,
                            category = article.category
                        )
                    )
                }
            )
        }

        // 30c. News Detail Screen
        composable<NewsDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<NewsDetailRoute>()
            val article = NewsUiArticle(
                title = route.title,
                description = route.description,
                url = route.url,
                imageUrl = route.imageUrl,
                sourceName = route.sourceName,
                publishedAtLabel = route.publishedAtLabel,
                author = route.author,
                category = route.category
            )
            NewsDetailScreen(
                article = article,
                onNavigateBack = { navController.navigateUp() }
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

        // 33. Step History
        composable<StepHistoryRoute> {
            StepHistoryScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 34. Calories History
        composable<CaloriesHistoryRoute> {
            CaloriesHistoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddMeals = {
                    navController.navigate(MainRoute(initialTab = BottomNavTab.SCAN)) {
                        popUpTo(MainRoute()) { inclusive = true }
                    }
                }
            )
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
