package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delightreza.kharcha.data.Repository
import kotlinx.coroutines.launch

@Composable
fun RepoSelectionScreen(
    repository: Repository,
    onConfigLoaded: () -> Unit
) {
    var urlInput by remember { mutableStateOf("https://delightreza.github.io/kitchen/config.json") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val handleSubmit = {
        if (urlInput.isBlank()) {
            errorMsg = "Please enter a URL"
        } else {
            isLoading = true
            errorMsg = null
            scope.launch {
                val config = repository.setActiveConfig(urlInput.trim())
                isLoading = false
                if (config != null) {
                    onConfigLoaded()
                } else {
                    errorMsg = "Failed to load config. Check URL."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Setup Repository", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Enter the URL to your config.json", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Config URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMsg != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { handleSubmit() })
        )

        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = handleSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Connect")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Helper chips
        Text("Presets:", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(
                onClick = { urlInput = "https://delightreza.github.io/kharcha/config.json" },
                label = { Text("Kharcha") }
            )
            SuggestionChip(
                onClick = { urlInput = "https://delightreza.github.io/kitchen/config.json" },
                label = { Text("Kitchen") }
            )
        }
    }
}
