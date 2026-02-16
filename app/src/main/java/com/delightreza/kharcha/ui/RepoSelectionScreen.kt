package com.delightreza.kharcha.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
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
import java.net.URI

@Composable
fun RepoSelectionScreen(
    repository: Repository,
    onConfigLoaded: () -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val savedRepos by repository.getSavedRepos().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    fun attemptConnection(url: String) {
        if (url.isBlank()) {
            errorMsg = "Please enter a URL"
            return
        }
        isLoading = true
        errorMsg = null
        scope.launch {
            val config = repository.setActiveConfig(url.trim())
            isLoading = false
            if (config != null) {
                onConfigLoaded()
            } else {
                errorMsg = "Failed to load config. Check URL."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Setup Repository", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Connect to a config.json URL", color = Color.Gray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Config URL") },
            placeholder = { Text("https://.../config.json") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMsg != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { attemptConnection(urlInput) })
        )

        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { attemptConnection(urlInput) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Connect")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (savedRepos.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saved Repositories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(savedRepos.toList()) { url ->
                    SavedRepoItem(
                        url = url,
                        onClick = { attemptConnection(url) },
                        onDelete = { 
                            scope.launch { repository.removeSavedRepo(url) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedRepoItem(url: String, onClick: () -> Unit, onDelete: () -> Unit) {
    // Attempt to extract repo name from URL for display title
    val name = try {
        val uri = URI(url)
        val parts = uri.path.split("/")
        if (parts.size >= 3) parts[2] else "Repository" // e.g. /delightreza/kharcha/config.json -> kharcha
    } catch (e: Exception) {
        "Repository"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                Text(url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Remove", tint = Color.Gray)
            }
        }
    }
}
