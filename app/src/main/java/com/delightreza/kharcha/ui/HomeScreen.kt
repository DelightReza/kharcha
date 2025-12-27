package com.delightreza.kharcha.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.KharchaData
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier, 
    repository: Repository,
    navController: NavController
) {
    var data by remember { mutableStateOf<KharchaData?>(null) }
    var balances by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }
    
    var displayedCount by remember { mutableIntStateOf(20) }
    
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    fun updateData(newData: KharchaData) {
        data = newData
        balances = repository.calculateBalances(newData)
    }

    fun loadData(forceNetwork: Boolean) {
        scope.launch {
            if (forceNetwork) isRefreshing = true
            if (forceNetwork) displayedCount = 20

            if (isInitialLoad && !forceNetwork) {
                val cached = repository.getCachedData()
                if (cached != null) updateData(cached)
            }

            val freshData = repository.fetchData()
            if (freshData != null) {
                updateData(freshData)
            }
            
            isRefreshing = false
            isInitialLoad = false
        }
    }

    LaunchedEffect(Unit) {
        loadData(forceNetwork = false)
    }

    Scaffold(modifier = modifier) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadData(forceNetwork = true) },
            state = pullRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            if (data == null && isInitialLoad) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (data != null) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    
                    // 1. Hero Stats
                    item {
                        val totalCredits = data!!.transactions.filter { it.type == "credit" }.sumOf { it.amount }
                        val totalDebits = data!!.transactions.filter { it.type == "debit" }.sumOf { it.amount }
                        val currentBalance = totalCredits - totalDebits
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Current Balance", 
                                    color = Color.White.copy(alpha = 0.9f), 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "${currentBalance.toInt()}", 
                                    color = Color.White, 
                                    fontSize = 32.sp, 
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        // Equal Height Row
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), 
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard("Total Collected", totalCredits, Color(0xFF10B981), Modifier.weight(1f).fillMaxHeight())
                            StatCard("Total Spent", totalDebits, Color(0xFFEF4444), Modifier.weight(1f).fillMaxHeight())
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 2. Member Status
                    item {
                        Text("Member Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(balances.toList().sortedByDescending { it.second }.chunked(2)) { rowItems ->
                        // Equal Height Row for Members
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), 
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (name, net) ->
                                val given = data!!.people[name] ?: 0.0
                                CompactMemberCard(name, net, given, Modifier.weight(1f).fillMaxHeight())
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 3. Expenses Summary
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                data!!.billTypes.forEach { (type, amount) ->
                                    if (amount > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(type, color = Color.Gray)
                                            Text("${amount.toInt()}", fontWeight = FontWeight.Bold)
                                        }
                                        LinearProgressIndicator(
                                            progress = { (amount / (data!!.billTypes.values.maxOrNull() ?: 1.0)).toFloat() },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = Color(0xFFEF4444),
                                            trackColor = Color(0xFFFEE2E2),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 4. Recent Activity
                    item {
                        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val visibleTransactions = data!!.transactions.take(displayedCount)
                    
                    items(visibleTransactions) { tx ->
                        TransactionRow(tx) {
                            navController.navigate("detail/${tx.id}")
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    }
                    
                    item {
                        if (displayedCount < data!!.transactions.size) {
                            val remaining = data!!.transactions.size - displayedCount
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { displayedCount += 20 },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                                ) {
                                    Text("Load Older Transactions ($remaining)", textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val displayTitle = if (tx.whoOrBill == "Other" && tx.note.isNotEmpty()) {
        tx.note
    } else {
        tx.whoOrBill
    }

    val localDate = DateUtils.formatToLocalDateOnly(tx.date)
    val displaySubtitle = if (tx.whoOrBill == "Other" && tx.note.isNotEmpty()) {
        "Other • $localDate"
    } else {
        localDate
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(displayTitle, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(displaySubtitle, fontSize = 12.sp) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if(tx.type=="credit") "+" else "-"}${tx.amount.toInt()}",
                    color = if(tx.type=="credit") Color(0xFF059669) else Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, "", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun StatCard(title: String, amount: Double, color: Color, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp), 
            verticalArrangement = Arrangement.Center, 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title, 
                color = Color.White.copy(alpha = 0.8f), 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "${amount.toInt()}", 
                color = Color.White, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactMemberCard(name: String, net: Double, given: Double, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Given: ${given.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if(net>0) "+" else ""}${net.toInt()}", 
                        color = if (net >= 0) Color(0xFF059669) else Color(0xFFE11D48), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        textAlign = TextAlign.End
                    )
                    Text("Net", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End)
                }
            }
        }
    }
}
