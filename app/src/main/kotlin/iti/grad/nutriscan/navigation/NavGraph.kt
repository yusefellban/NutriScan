package iti.grad.nutriscan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import iti.grad.nutriscan.presentation.home.HomeScreen
import iti.grad.nutriscan.presentation.onboarding.splash.SplashScreen

/**
 * Root navigation graph for the NutriScan application.
 *
 * Architecture notes:
 * - [SplashRoute] is always the start destination.
 * - After the splash animation, the nav controller pops [SplashRoute] inclusive=true
 *   so the back stack only contains [HomeRoute] — pressing Back on Home exits the app.
 * - Type-safe routes only (AGENTS.md §1.6 — string routes are BANNED).
 *
 * @param navController The [NavHostController] that manages the back stack.
 *   Defaults to [rememberNavController] so callers don't need to create one.
 * @param modifier Modifier applied to the [NavHost].
 */
@Composable
fun NutriScanNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
    ) {

        composable<SplashRoute> {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        // Pop splash off the back stack so the user cannot navigate back to it
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> {
            HomeScreen()
        }
    }
}
