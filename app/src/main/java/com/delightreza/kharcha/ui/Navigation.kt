package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.Repository
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dataStore = remember { AppDataStore(context) }
    // Pass DataStore to Repository for caching
    val repository = remember { Repository(dataStore) }
    val scope = rememberCoroutineScope()
    
    // TRICK: Use a specific string "__LOADING__" as the initial value.
    // This lets us distinguish between "Loading..." and "User is not set (null)"
    val userState = dataStore.userFlow.collectAsState(initial = "__LOADING__")
    val tokenState = dataStore.tokenFlow.collectAsState(initial = null)

    // 1. SHOW LOADING SCREEN (Fixes White Screen)
    if (userState.value == "__LOADING__") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 2. DETERMINE START DESTINATION
    // If value is null/empty (after loading), go to Onboarding. Otherwise, Main.
    val startDest = if (userState.value.isNullOrEmpty()) "onboarding" else "main"

    NavHost(
        navController = navController, 
        startDestination = startDest
    ) {
        composable("onboarding") {
            UserSelectionScreen(
                onUserSelected = { user ->
                    scope.launch {
                        dataStore.saveUser(user)
                        // Navigate to main and clear backstack so they can't go back to onboarding
                        navController.navigate("main") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                rootNavController = navController, 
                repository = repository, 
                dataStore = dataStore, 
                currentUser = userState.value ?: "",
                hasToken = !tokenState.value.isNullOrBlank()
            )
        }

        composable("add_transaction") {
            val token = tokenState.value
            if (token.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { navController.popBackStack() },
                    title = { Text("Admin Access Required") },
                    text = { Text("You need a GitHub Token to add transactions. Go to Profile tab to add one.") },
                    confirmButton = {
                        TextButton(onClick = { navController.popBackStack() }) { Text("OK") }
                    }
                )
            } else {
                AddTransactionScreen(
                    navController = navController,
                    repository = repository,
                    token = token
                )
            }
        }

        composable(
            "detail/{txId}",
            arguments = listOf(navArgument("txId") { type = NavType.StringType })
        ) { backStackEntry ->
            val txId = backStackEntry.arguments?.getString("txId")
            TransactionDetailScreen(
                navController = navController,
                repository = repository,
                transactionId = txId
            )
        }
    }
}
