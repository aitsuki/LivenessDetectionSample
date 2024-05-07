package com.aitsuki.liveness.sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No LocalNavController provided")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                CompositionLocalProvider(LocalNavController provides navController) {
                    NavHost(
                        navController = navController,
                        startDestination = "start",
                    ) {
                        composable("start") {
                            StartScreen()
                        }
                        composable("liveness") {
                            LivenessScreen()
                        }
                        composable("camera") {
                            CardCameraScreen()
                        }
                    }
                }
            }
        }
    }
}