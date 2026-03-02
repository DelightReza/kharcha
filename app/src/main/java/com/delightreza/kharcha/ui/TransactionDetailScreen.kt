package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
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
import com.delightreza.kharcha.data.AppConfig
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils
import com.delightreza.kharcha.utils.FormatUtils
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
    var groupTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(transactionId) {
        val data = repository.fetchData()
        config = repository.getAppConfig()
        
        val single = data?.transactions?.find { it.id == transactionId }
        
        if (single != null) {
            transaction = single
        } else {
            val group = data?.transactions?.filter { it.parentId == transactionId }
            if (!group.isNullOrEmpty()) {
                groupTransactions = group
            }
        }
        isLoading = false
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { 
                Text(if (groupTransactions.isNotEmpty()) "This is a Group Transaction. Deleting it will remove ALL entries." else "Delete this transaction?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        showDeleteDialog = false
                        scope.launch {
                            if (token != null) {
                                val targetId = transaction?.id ?: groupTransactions.firstOrNull()?.id
                                if (targetId != null) {
                                    val success = repository.deleteTransaction(token, targetId)
                                    if (success) navController.popBackStack()
                                }
                            }
                            isDeleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupTransactions.isNotEmpty()) "Group Details" else "Details") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } },
                actions = {
                    if (hasToken && !isDeleting && (transaction != null || groupTransactions.isNotEmpty())) {
                        if (transaction != null && transaction!!.parentId == null) {
                            IconButton(onClick = { 
                                navController.navigate("add_transaction?txId=${transaction!!.id}") 
                            }) { Icon(Icons.Default.Edit, "Edit") }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
                    }
                }
            )
        }
    ) { p ->
        if (isDeleting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.Red) }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (groupTransactions.isNotEmpty()) {
            GroupDetailView(groupTransactions, p, config)
        } else if (transaction != null) {
            SingleTransactionView(transaction!!, p, navController, config)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Transaction not found") }
        }
    }
}

@Composable
fun GroupDetailView(transactions: List<Transaction>, paddingValues: PaddingValues, config: AppConfig?) {
    val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount }
    val totalDebit = transactions.filter { it.type == "debit" }.sumOf { it.amount }
    val firstTx = transactions.first()
    val groupTitle = if(firstTx.note.contains("Settlement")) "Settlement Group" else if (firstTx.note.contains("Transfer")) "Transfer Group" else "Transaction Group"
    val currency = config?.currency ?: "SOM"

    Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Layers, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(groupTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                // DATE UTILS USED HERE
                Text(DateUtils.formatToLocal(firstTx.date), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Credit", style = MaterialTheme.typography.labelMedium)
                        Text("${FormatUtils.formatAmount(totalCredit)} $currency", style = MaterialTheme.typography.titleLarge, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Debit", style = MaterialTheme.typography.labelMedium)
                        Text("${FormatUtils.formatAmount(totalDebit)} $currency", style = MaterialTheme.typography.titleLarge, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Included Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions) { tx ->
                val name = if (tx.type == "credit") config?.members?.find { it.id == (tx.payerId ?: tx.whoOrBill) }?.name ?: tx.whoOrBill else tx.whoOrBill
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            if(tx.note.isNotEmpty()) Text(tx.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text("${if(tx.amount>=0) "+" else ""}${FormatUtils.formatAmount(tx.amount)} $currency", color = if(tx.amount>=0) Color(0xFF059669) else Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SingleTransactionView(tx: Transaction, paddingValues: PaddingValues, navController: NavController, config: AppConfig?) {
    Column(modifier = Modifier.padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(if(tx.type=="credit") Color(0xFFD1FAE5) else Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if(tx.type=="credit") Icons.Default.Check else Icons.Default.Receipt, null, tint = if(tx.type=="credit") Color(0xFF059669) else Color(0xFFE11D48), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(if(tx.type=="credit") "Money Received" else "Bill Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("${FormatUtils.formatAmount(tx.amount)} ${config?.currency ?: "SOM"}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
            Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                val displayName = if(tx.type == "credit") {
                    config?.members?.find { it.id == (tx.payerId ?: tx.whoOrBill) }?.name ?: tx.whoOrBill
                } else {
                    config?.billTypes?.find { it.id == (tx.billTypeId ?: tx.whoOrBill) }?.name ?: tx.whoOrBill
                }
                
                DetailRow("Subject", displayName)
                
                // DATE UTILS USED HERE
                DetailRow("Date", DateUtils.formatToLocal(tx.date))
                
                if (tx.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(tx.note, fontSize = 16.sp)
                }

                if (tx.type == "debit" && !tx.splitAmong.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text("Split Among", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                            val names = tx.splitAmong.map { id -> config?.members?.find { it.id == id }?.name ?: id }
                            Text(names.joinToString(", "), color = Color(0xFF1E40AF))
                        }
                    }
                }
                
                if (tx.parentId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { navController.navigate("detail/${tx.parentId}") }, modifier = Modifier.fillMaxWidth()) {
                        Text("View Group Transaction")
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
