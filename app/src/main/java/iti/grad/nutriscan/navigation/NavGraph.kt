package iti.grad.nutriscan.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import iti.grad.nutriscan.presentation.navigation.*

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = {
                    navController.navigate(OnboardingCarouselRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }) {
                    Text("Splash Screen (Click to Go to Onboarding)")
                }
            }
        }

        composable<OnboardingCarouselRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = {
                    navController.navigate(HealthProfileSetupRoute) {
                        popUpTo(OnboardingCarouselRoute) { inclusive = true }
                    }
                }) {
                    Text("Onboarding Carousel (Click to Get Started)")
                }
            }
        }

        composable<HealthProfileSetupRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = {
                    navController.navigate(FamilyProfileSetupRoute) {
                        popUpTo(HealthProfileSetupRoute) { inclusive = true }
                    }
                }) {
                    Text("Health Profile Setup (Click to Save Profile)")
                }
            }
        }

        composable<FamilyProfileSetupRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = {
                    navController.navigate(HomeRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }) {
                    Text("Family Profile Setup (Click to Complete Setup)")
                }
            }
        }

        composable<HomeRoute> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Home Screen (Setup Completed)")
            }
        }
    }
}
