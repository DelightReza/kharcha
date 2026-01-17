package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.KharchaData
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.Constants
import com.delightreza.kharcha.utils.DateUtils
import kotlinx.coroutines.delay
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
    
    // Admin Access State
    var showAdminAccess by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var tokenStatus by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isTokenVisible by remember { mutableStateOf(false) }
    
    val savedToken = dataStore.tokenFlow.collectAsState(initial = "")
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Card (With Secret Long Press)
        item {
            val netBalance = balances[currentUser] ?: 0.0
            val given = data?.people?.get(currentUser) ?: 0.0
            val spent = given - netBalance
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                val pressJob = scope.launch {
                                    delay(3000)
                                    showAdminAccess = !showAdminAccess
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                try {
                                    awaitRelease()
                                } finally {
                                    pressJob.cancel()
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(text = currentUser.take(1), color = MaterialTheme.colorScheme.onPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = currentUser, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Stats Row - Equal Heights
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Given", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Text(text = "%.2f".format(given), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        VerticalDivider()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Text(text = "%.2f".format(spent), style = MaterialTheme.typography.titleLarge, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        VerticalDivider()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Net", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Text(text = "${if(netBalance > 0) "+" else ""}${"%.2f".format(netBalance)}", style = MaterialTheme.typography.titleLarge, color = if (netBalance >= 0) Color(0xFF86EFAC) else Color(0xFFFDA4AF), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // 2. Admin Access Card
        if (showAdminAccess) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Admin Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            label = { Text("GitHub Token") },
                            placeholder = { Text("ghp_...") },
                            trailingIcon = { IconButton(onClick = { isTokenVisible = !isTokenVisible }) { Icon(if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle visibility") } },
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (tokenStatus.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = tokenStatus, color = if (tokenStatus.startsWith("Success")) Color(0xFF059669) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                                        tokenStatus = "Error: Invalid Token."
                                        dataStore.saveToken("") 
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isVerifying,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying...", textAlign = TextAlign.Center)
                            } else {
                                Text("Verify & Save Token", textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // 3. Switch User
        item {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        dataStore.clearUser()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch User", textAlign = TextAlign.Center)
            }
        }

        // 4. Personal History
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (data != null) {
            // UPDATED: Calculate using dynamic list size
            val currentMemberCount = data!!.people.size
            
            val myTransactions = data!!.transactions.filter { tx ->
                if (tx.type == "credit") {
                    tx.whoOrBill == currentUser
                } else {
                    val exemptions = tx.exemptions ?: emptyList()
                    !exemptions.contains(currentUser)
                }
            }
            
            if (myTransactions.isEmpty()) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No recent activity.", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(myTransactions.take(30)) { tx ->
                    val exemptionsCount = tx.exemptions?.size ?: 0
                    val payingPeopleCount = currentMemberCount - exemptionsCount
                    // Avoid division by zero
                    val myShare = if (tx.type == "debit" && payingPeopleCount > 0) tx.amount / payingPeopleCount else tx.amount

                    ProfileTransactionRow(tx, myShare)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)))
}

@Composable
fun ProfileTransactionRow(tx: Transaction, myShare: Double) {
    val localDate = DateUtils.formatToLocalDateOnly(tx.date)
    val isCredit = tx.type == "credit"
    val color = if(isCredit) Color(0xFF059669) else Color(0xFFDC2626)
    val icon = if(isCredit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if(isCredit) "Deposit" else tx.whoOrBill, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = if(tx.note.isNotEmpty()) tx.note else localDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${if(isCredit) "+" else "-"}${"%.2f".format(myShare)}", color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                if (!isCredit) {
                    Text("My Share", fontSize = 10.sp, color = Color.LightGray, textAlign = TextAlign.End)
                }
            }
        }
    }
}
