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
    val repository = remember { Repository(dataStore) }
    val scope = rememberCoroutineScope()
    
    val userState = dataStore.userFlow.collectAsState(initial = "__LOADING__")
    val tokenState = dataStore.tokenFlow.collectAsState(initial = null)

    if (userState.value == "__LOADING__") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDest = if (userState.value.isNullOrEmpty()) "onboarding" else "main"

    NavHost(navController = navController, startDestination = startDest) {
        composable("onboarding") {
            UserSelectionScreen(
                repository = repository, // Pass repository
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

        composable(
            "add_transaction?txId={txId}",
            arguments = listOf(navArgument("txId") { nullable = true; type = NavType.StringType })
        ) { backStackEntry ->
            val token = tokenState.value
            val txId = backStackEntry.arguments?.getString("txId")

            if (token.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { navController.popBackStack() },
                    title = { Text("Admin Access Required") },
                    text = { Text("You need a GitHub Token to add/edit transactions. Go to Profile tab to add one.") },
                    confirmButton = {
                        TextButton(onClick = { navController.popBackStack() }) { Text("OK") }
                    }
                )
            } else {
                AddTransactionScreen(
                    navController = navController,
                    repository = repository,
                    token = token,
                    transactionIdToEdit = txId 
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
                transactionId = txId,
                hasToken = !tokenState.value.isNullOrBlank(), 
                token = tokenState.value
            )
        }
    }
}
