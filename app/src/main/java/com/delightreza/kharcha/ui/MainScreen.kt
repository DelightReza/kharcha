package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.Repository

@Composable
fun MainScreen(
    rootNavController: NavController,
    repository: Repository,
    dataStore: AppDataStore,
    currentUser: String,
    hasToken: Boolean,
    onSwitchRepo: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && hasToken) {
                FloatingActionButton(onClick = { rootNavController.navigate("add_transaction") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        if (selectedTab == 0) {
            HomeScreen(
                modifier = Modifier.padding(padding),
                repository = repository,
                navController = rootNavController
            )
        } else {
            ProfileScreen(
                modifier = Modifier.padding(padding),
                repository = repository,
                dataStore = dataStore,
                currentUser = currentUser,
                navController = rootNavController,
                onLogout = {
                    rootNavController.navigate("onboarding") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                onSwitchRepo = onSwitchRepo
            )
        }
    }
}
