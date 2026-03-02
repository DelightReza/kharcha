package com.delightreza.kharcha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import com.delightreza.kharcha.data.AppDataStore
import com.delightreza.kharcha.data.AppConfig
import com.delightreza.kharcha.data.KharchaData
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils
import com.delightreza.kharcha.utils.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    repository: Repository,
    dataStore: AppDataStore,
    currentUser: String,
    navController: NavController,
    onLogout: () -> Unit,
    onSwitchRepo: () -> Unit
) {
    var data by remember { mutableStateOf<KharchaData?>(null) }
    var config by remember { mutableStateOf<AppConfig?>(null) }
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
        config = repository.getAppConfig()
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

    val currentUserName = config?.members?.find { it.id == currentUser }?.name ?: currentUser

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Card
        item {
            val netBalance = balances[currentUser] ?: 0.0
            val given = data?.transactions?.filter { it.type == "credit" && (it.payerId == currentUser || it.whoOrBill == currentUser) }?.sumOf { it.amount } ?: 0.0
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
                                try { awaitRelease() } finally { pressJob.cancel() }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(text = currentUserName.take(1), color = MaterialTheme.colorScheme.onPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = currentUserName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Given", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Text(text = FormatUtils.formatAmount(given), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        VerticalDivider()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Text(text = FormatUtils.formatAmount(spent), style = MaterialTheme.typography.titleLarge, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        VerticalDivider()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Net", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Text(text = "${if(netBalance > 0) "+" else ""}${FormatUtils.formatAmount(netBalance)}", style = MaterialTheme.typography.titleLarge, color = if (netBalance >= 0) Color(0xFF86EFAC) else Color(0xFFFDA4AF), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        if (!savedToken.value.isNullOrBlank()) {
            item {
                OutlinedButton(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Config Settings (People & Bills)")
                }
            }
        }

        if (showAdminAccess) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
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

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { scope.launch { dataStore.clearUser(); onLogout() } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Switch User", textAlign = TextAlign.Center, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onSwitchRepo() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Switch Repo", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (data != null) {
            val myTransactions = data!!.transactions.filter { tx ->
                if (tx.type == "credit") {
                    tx.payerId == currentUser || tx.whoOrBill == currentUser
                } else {
                    if (tx.splitAmong != null) tx.splitAmong.contains(currentUser)
                    else {
                        val exemptions = tx.exemptions ?: emptyList()
                        !exemptions.contains(currentUser)
                    }
                }
            }
            if (myTransactions.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No recent activity.", color = Color.Gray, textAlign = TextAlign.Center) } }
            } else {
                items(myTransactions.take(30)) { tx ->
                    val myShare = if (tx.type == "debit") {
                        if (!tx.splitAmong.isNullOrEmpty()) tx.amount / tx.splitAmong.size
                        else {
                            val ex = tx.exemptions?.size ?: 0
                            val total = config?.members?.size ?: 1
                            tx.amount / (total - ex).coerceAtLeast(1)
                        }
                    } else {
                        tx.amount
                    }
                    ProfileTransactionRow(tx, myShare, config) {
                        val targetId = tx.parentId ?: tx.id
                        navController.navigate("detail/${targetId}")
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun ProfileTransactionRow(tx: Transaction, myShare: Double, config: AppConfig?, onClick: () -> Unit) {
    val localDate = DateUtils.formatToLocalDateOnly(tx.date)
    val isPositiveEffect = if (tx.type == "debit") false else myShare > 0
    val color = if (isPositiveEffect) Color(0xFF059669) else Color(0xFFDC2626)
    val icon = if (isPositiveEffect) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    
    // FIX: Logic to show Note if Bill Type is "Other"
    val title = when {
        tx.type == "credit" -> if (myShare < 0) "Transfer / Settle" else "Deposit / Received"
        tx.type == "debit" -> {
            val bid = tx.billTypeId ?: tx.whoOrBill
            val billName = config?.billTypes?.find { it.id == bid }?.name ?: bid
            if (billName.equals("Other", ignoreCase = true) && tx.note.isNotEmpty()) {
                tx.note
            } else {
                billName
            }
        }
        else -> tx.whoOrBill
    }

    val subtitle = if (tx.note.isNotEmpty() && tx.note != title) "${tx.note} • $localDate" else localDate

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                val amountDisplay = abs(myShare)
                val sign = if (isPositiveEffect) "+" else "-"
                Text(text = "$sign${FormatUtils.formatAmount(amountDisplay)}", color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                if (tx.type == "debit") Text(text = "My Share", fontSize = 10.sp, color = Color.LightGray, textAlign = TextAlign.End)
            }
        }
    }
}
