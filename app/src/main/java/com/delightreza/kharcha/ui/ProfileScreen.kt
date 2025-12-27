package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.KharchaData
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils // Added Import
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    repository: Repository,
    dataStore: AppDataStore,
    currentUser: String,
    onLogout: () -> Unit
) {
    var data by remember { mutableStateOf<KharchaData?>(null) }
    var balances by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var tokenInput by remember { mutableStateOf("") }
    var tokenStatus by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    
    val savedToken = dataStore.tokenFlow.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val result = repository.fetchData()
        if (result != null) {
            data = result
            balances = repository.calculateBalances(result)
        }
    }

    LaunchedEffect(savedToken.value) {
        if (!savedToken.value.isNullOrEmpty()) {
            tokenInput = savedToken.value!!
        }
    }

    LazyColumn(modifier = modifier.padding(16.dp)) {
        // 1. Profile Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser.take(1),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(currentUser, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    
                    val balance = balances[currentUser] ?: 0.0
                    Text(
                        text = "Net Balance: ${balance.toInt()}",
                        color = if (balance >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. Admin Access (Token)
        item {
            Text("Admin Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GitHub Personal Access Token", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("ghp_...") },
                        trailingIcon = {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else if (savedToken.value == tokenInput && tokenInput.isNotEmpty()) {
                                Icon(Icons.Default.Check, "Saved", tint = Color(0xFF059669))
                            }
                        }
                    )
                    
                    if (tokenStatus.isNotEmpty()) {
                        Text(
                            text = tokenStatus,
                            color = if (tokenStatus.startsWith("Success")) Color(0xFF059669) else Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            isVerifying = true
                            tokenStatus = "Verifying..."
                            scope.launch {
                                val isValid = repository.verifyToken(tokenInput)
                                isVerifying = false
                                if (isValid) {
                                    dataStore.saveToken(tokenInput)
                                    tokenStatus = "Success! Token Verified."
                                } else {
                                    tokenStatus = "Invalid Token."
                                    dataStore.saveToken("") // Clear
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        enabled = !isVerifying
                    ) {
                        Text("Verify & Save Token")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Switch User
        item {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        dataStore.clearUser() // Clear user
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch User")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Personal History
        item {
            Text("Your Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (data != null) {
            val myTransactions = data!!.transactions.filter { tx ->
                tx.whoOrBill == currentUser || (tx.type == "credit" && tx.whoOrBill == currentUser)
            }
            
            if (myTransactions.isEmpty()) {
                item { Text("No recent transactions found for you.", color = Color.Gray, fontSize = 14.sp) }
            } else {
                items(myTransactions.take(20)) { tx ->
                    TransactionRow(tx)
                    Divider(color = Color.LightGray.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    // FIXED: Use Local Date
    val localDate = DateUtils.formatToLocalDateOnly(tx.date)

    ListItem(
        headlineContent = { Text(tx.whoOrBill, fontWeight = FontWeight.Medium) },
        supportingContent = { 
            Column {
                Text(localDate, fontSize = 12.sp)
                if(tx.note.isNotEmpty()) Text(tx.note, fontSize = 11.sp, color = Color.Gray)
            }
        },
        trailingContent = {
            Text(
                text = "${if(tx.type=="credit") "+" else "-"}${tx.amount.toInt()}",
                color = if(tx.type=="credit") Color(0xFF059669) else Color(0xFFDC2626),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
