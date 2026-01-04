package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController, 
    repository: Repository, 
    transactionId: String?,
    hasToken: Boolean = false,
    token: String? = null
) {
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(transactionId) {
        val data = repository.fetchData() // Fetch fresh data
        transaction = data?.transactions?.find { it.id == transactionId }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        showDeleteDialog = false
                        scope.launch {
                            if (token != null && transaction != null) {
                                val success = repository.deleteTransaction(token, transaction!!.id)
                                if (success) navController.popBackStack()
                            }
                            isDeleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } },
                actions = {
                    if (hasToken && transaction != null && !isDeleting) {
                        IconButton(onClick = { 
                            navController.navigate("add_transaction?txId=${transaction!!.id}") 
                        }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { p ->
        if (isDeleting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Red)
                    Text("Deleting...", modifier = Modifier.padding(top = 16.dp))
                }
            }
        } else if (transaction == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val tx = transaction!!
            Column(modifier = Modifier.padding(p).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if(tx.type=="credit") Color(0xFFD1FAE5) else Color(0xFFFFE4E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if(tx.type=="credit") Icons.Default.Check else Icons.Default.Receipt,
                                contentDescription = null,
                                tint = if(tx.type=="credit") Color(0xFF059669) else Color(0xFFE11D48),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if(tx.type=="credit") "Money Received" else "Bill Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Text("${tx.amount}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        Text("SOM", fontSize = 12.sp, color = Color.Gray)
                    }
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))
                    
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        DetailRow("Subject", tx.whoOrBill)
                        DetailRow("Date", DateUtils.formatToLocal(tx.date))
                        DetailRow("ID", tx.id.takeLast(8))
                        
                        if (tx.note.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(tx.note, fontSize = 16.sp)
                        }

                        if (!tx.exemptions.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text("Exemptions", color = Color(0xFFC2410C), fontWeight = FontWeight.Bold)
                                    Text(tx.exemptions.joinToString(", "), color = Color(0xFF9A3412))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
