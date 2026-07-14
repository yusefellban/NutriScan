package iti.grad.nutriscan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import iti.grad.nutriscan.presentation.auth.login.view.LoginScreen
import iti.grad.nutriscan.presentation.auth.register.view.RegisterScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = LoginRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<LoginRoute> {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                }
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigateUp()
                }
            )
        }
        composable<HomeRoute> {
            // Placeholder for Home screen
        }
    }
}
