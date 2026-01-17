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
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.utils.Constants

@Composable
fun UserSelectionScreen(
    repository: Repository, 
    onUserSelected: (String) -> Unit
) {
    var peopleList by remember { mutableStateOf(Constants.DEFAULT_MEMBERS) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Try cache first
        val cached = repository.getCachedData()
        if (cached != null && cached.people.isNotEmpty()) {
            peopleList = cached.people.keys.toList().sorted()
            isLoading = false
        } else {
            // Try fetch
            val fetched = repository.fetchData()
            if (fetched != null && fetched.people.isNotEmpty()) {
                peopleList = fetched.people.keys.toList().sorted()
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Kharcha", fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                    UserCard(person) { onUserSelected(person) }
                }
            }
        }
    }
}

@Composable
fun UserCard(name: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontWeight = FontWeight.Medium)
        }
    }
}
