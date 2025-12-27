package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.delightreza.kharcha.utils.DateUtils // Added Import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(navController: NavController, repository: Repository, transactionId: String?) {
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    
    LaunchedEffect(transactionId) {
        val data = repository.fetchData() // Or getCachedData() for speed
        transaction = data?.transactions?.find { it.id == transactionId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } }
            )
        }
    ) { p ->
        if (transaction == null) {
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
                        
                        // FIXED: Show Local Time here
                        DetailRow("Date", DateUtils.formatToLocal(tx.date))
                        
                        DetailRow("ID", tx.id)
                        
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
