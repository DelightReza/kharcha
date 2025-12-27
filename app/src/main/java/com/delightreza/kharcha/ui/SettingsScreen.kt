package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.Repository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, dataStore: AppDataStore, repository: Repository) {
    var tokenInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val currentToken = dataStore.tokenFlow.collectAsState(initial = "")

    LaunchedEffect(currentToken.value) {
        if (!currentToken.value.isNullOrEmpty()) {
            tokenInput = currentToken.value!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text("Admin Access Token", style = MaterialTheme.typography.titleMedium)
            Text("Enter your GitHub PAT to enable admin access.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("GitHub Token (ghp_...)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            
            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    color = if (statusMessage.startsWith("Error")) Color.Red else Color(0xFF059669),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (tokenInput.isBlank()) {
                        statusMessage = "Error: Token cannot be empty"
                        return@Button
                    }
                    
                    isVerifying = true
                    statusMessage = "Verifying token..."
                    
                    scope.launch {
                        // VERIFY TOKEN BEFORE SAVING
                        val isValid = repository.verifyToken(tokenInput)
                        isVerifying = false
                        
                        if (isValid) {
                            dataStore.saveToken(tokenInput)
                            statusMessage = "Success! Token verified and saved."
                            // Optional: Automatically go back
                            // navController.popBackStack() 
                        } else {
                            statusMessage = "Error: Invalid Token or No Access to Repository."
                            dataStore.clearToken() // Clear invalid token if any was saved
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isVerifying
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying...")
                } else {
                    Text("Verify & Save")
                }
            }
        }
    }
}
