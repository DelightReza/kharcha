package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, repository: Repository, token: String) {
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    
    var type by remember { mutableStateOf("debit") }
    var amount by remember { mutableStateOf("") }
    var whoOrBill by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val people = listOf("Raza", "Salman", "Mujeeb", "Gulam", "Rana", "Naved", "Musawwar", "Nizamuddin")
    val bills = listOf("Electricity", "Water", "Gas", "Garbage", "Internet", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Transaction") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } }
            )
        }
    ) { p ->
        if (isSubmitting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(p).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "debit",
                        onClick = { type = "debit"; whoOrBill = "" },
                        label = { Text("Debit (Bill)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == "credit",
                        onClick = { type = "credit"; whoOrBill = "" },
                        label = { Text("Credit (Deposit)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Subject:", style = MaterialTheme.typography.labelMedium)
                val options = if(type == "credit") people else bills
                
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options.size) { i ->
                        val item = options[i]
                        InputChip(
                            selected = whoOrBill == item,
                            onClick = { whoOrBill = item },
                            label = { Text(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (amount.isNotEmpty() && whoOrBill.isNotEmpty()) {
                            isSubmitting = true
                            scope.launch {
                                val currentData = repository.fetchData()
                                if (currentData != null) {
                                    val tx = Transaction(
                                        id = "tx_${System.currentTimeMillis()}",
                                        type = type,
                                        whoOrBill = whoOrBill,
                                        amount = amount.toDouble(),
                                        note = note,
                                        date = java.time.Instant.now().toString()
                                    )
                                    
                                    val success = repository.addTransaction(token, currentData, tx)
                                    if (success) {
                                        navController.popBackStack()
                                    } else {
                                        isSubmitting = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amount.isNotEmpty() && whoOrBill.isNotEmpty()
                ) {
                    Text("Save to GitHub")
                }
            }
        }
    }
}
