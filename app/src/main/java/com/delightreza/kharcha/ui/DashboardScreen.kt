package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.KharchaData
import com.delightreza.kharcha.data.Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, repository: Repository) {
    var data by remember { mutableStateOf<KharchaData?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        data = repository.fetchData()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kharcha") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_transaction") }) {
                Icon(Icons.Default.Add, "Add Transaction")
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p).fillMaxSize()) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (data != null) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        val total = data!!.transactions.filter{it.type == "credit"}.sumOf{it.amount} - 
                                    data!!.transactions.filter{it.type == "debit"}.sumOf{it.amount}
                        
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Current Balance")
                                Text("${total.toInt()} SOM", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(data!!.transactions.take(10)) { tx ->
                        ListItem(
                            headlineContent = { Text(tx.whoOrBill) },
                            supportingContent = { Text(tx.date.split("T")[0]) },
                            trailingContent = {
                                Text(
                                    text = "${if(tx.type=="credit") "+" else "-"}${tx.amount.toInt()}",
                                    color = if(tx.type=="credit") Color(0xFF059669) else Color(0xFFDC2626),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        )
                        Divider()
                    }
                }
            } else {
                Text("Failed to load data", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
