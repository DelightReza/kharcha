package com.delightreza.kharcha.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.Repository

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dataStore = remember { AppDataStore(context) }
    val repository = remember { Repository() }
    
    // Collect token state
    val tokenState = dataStore.tokenFlow.collectAsState(initial = null)
    
    NavHost(navController = navController, startDestination = "dashboard") {
        
        // Public Dashboard
        composable("dashboard") {
            DashboardScreen(
                navController = navController, 
                repository = repository
            )
        }

        // Settings (Add Token)
        composable("settings") {
            SettingsScreen(
                navController = navController,
                dataStore = dataStore,
                repository = repository
            )
        }

        // Admin Action: Add Transaction
        composable("add_transaction") {
            val token = tokenState.value
            
            // PROTECTION LOGIC:
            if (token.isNullOrBlank()) {
                // If no token, force redirect to settings
                LaunchedEffect(Unit) {
                    navController.navigate("settings")
                }
            } else {
                // If token exists locally, we assume it's valid (checked in Settings)
                AddTransactionScreen(
                    navController = navController,
                    repository = repository,
                    token = token
                )
            }
        }
    }
}
