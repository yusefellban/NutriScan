package iti.grad.nutriscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import iti.grad.nutriscan.navigation.NutriScanNavGraph
import iti.grad.nutriscan.presentation.common.theme.AppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Edge-to-edge must be enabled BEFORE installSplashScreen
        enableEdgeToEdge()

        // 2. Install the SplashScreen API — reads Theme.NutriScan.Splash from Manifest
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // 3. Set Compose content — Starting Window is dismissed instantly when Compose draws
        setContent {
            AppTheme {
                NutriScanNavGraph()
            }
        }
    }
}