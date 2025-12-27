package com.delightreza.kharcha.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
    // Pass DataStore to Repository
    val repository = remember { Repository(dataStore) }
    val scope = rememberCoroutineScope()
    
    val userState = dataStore.userFlow.collectAsState(initial = null)
    val tokenState = dataStore.tokenFlow.collectAsState(initial = null)

    if (userState.value == null && tokenState.value == null) return 

    NavHost(
        navController = navController, 
        startDestination = if (userState.value.isNullOrEmpty()) "onboarding" else "main"
    ) {
        composable("onboarding") {
            UserSelectionScreen(
                onUserSelected = { user ->
                    scope.launch {
                        dataStore.saveUser(user)
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
                    text = { Text("You need a GitHub Token to add transactions. Go to Profile to add one.") },
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
