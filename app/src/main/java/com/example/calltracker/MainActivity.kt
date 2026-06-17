package com.example.calltracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.calltracker.service.TrackerService
import com.example.calltracker.ui.screen.DashboardScreen
import com.example.calltracker.ui.screen.PermissionsScreen
import com.example.calltracker.ui.theme.CallTrackerTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calltracker.ui.screen.LoginScreen
import com.example.calltracker.ui.screen.SignUpScreen
import com.example.calltracker.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val userSession by authViewModel.userSession.collectAsState(initial = null)

                    // Decide start destination based on session presence
                    val startDestination = if (userSession != null) "permissions" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToSignUp = { navController.navigate("signup") },
                                onLoginSuccess = {
                                    navController.navigate("permissions") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                                onSignUpSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("permissions") {
                            PermissionsScreen(
                                onAllPermissionsGranted = { resultCode, resultData ->
                                    startTrackerService(resultCode, resultData)
                                    navController.navigate("dashboard") {
                                        popUpTo("permissions") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen()
                        }
                    }
                }
            }
        }
    }

    private fun startTrackerService(resultCode: Int? = null, resultData: Intent? = null) {
        val serviceIntent = Intent(this, TrackerService::class.java)
        if (resultCode != null && resultData != null) {
            serviceIntent.putExtra("EXTRA_RESULT_CODE", resultCode)
            serviceIntent.putExtra("EXTRA_RESULT_DATA", resultData)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}