package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delightreza.kharcha.data.MemberConfig
import com.delightreza.kharcha.data.Repository

@Composable
fun UserSelectionScreen(
    repository: Repository, 
    onUserSelected: (String) -> Unit,
    onChangeRepo: () -> Unit
) {
    var peopleList by remember { mutableStateOf(listOf<MemberConfig>()) }
    var isLoading by remember { mutableStateOf(true) }
    var repoTitle by remember { mutableStateOf("Fund") }

    LaunchedEffect(Unit) {
        var config = repository.getAppConfig()
        
        // Retry fetch if config is null or members are empty (first launch edge case)
        if (config == null) {
            val fetchedData = repository.fetchData()
            if (fetchedData != null) {
                config = repository.getAppConfig()
            }
        }

        if (config != null) {
            // FIX: Handle potential null list safely with .orEmpty()
            peopleList = config!!.members.orEmpty().filter { it.active }.sortedBy { it.name }
            repoTitle = config!!.siteTitle
        }
        
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to $repoTitle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Who are you?", fontSize = 16.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(peopleList) { person ->
                    UserCard(person.name) { onUserSelected(person.id) } 
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = onChangeRepo) {
            Text("Switch Repository", color = Color.LightGray)
        }
    }
}

@Composable
fun UserCard(name: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontWeight = FontWeight.Medium)
        }
    }
}
