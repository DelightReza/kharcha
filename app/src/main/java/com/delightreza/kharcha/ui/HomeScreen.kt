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
import com.delightreza.kharcha.data.AppConfig
import com.delightreza.kharcha.data.KharchaData
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils
import com.delightreza.kharcha.utils.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier, 
    repository: Repository,
    navController: NavController
) {
    var data by remember { mutableStateOf<KharchaData?>(null) }
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var balances by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }
    
    var displayedCount by remember { mutableIntStateOf(20) }
    
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    suspend fun updateData(newData: KharchaData) {
        data = newData
        balances = repository.calculateBalances(newData)
    }

    fun loadData(forceNetwork: Boolean) {
        scope.launch {
            if (forceNetwork) isRefreshing = true
            if (forceNetwork) displayedCount = 20

            config = repository.getAppConfig()

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
            } else if (data != null && config != null) {
                val currency = config!!.currency
                
                // Helper functions to resolve names from IDs
                val resolveName = { id: String -> config!!.members.find { it.id == id }?.name ?: id }
                val resolveBillName = { id: String -> config!!.billTypes.find { it.id == id }?.name ?: id }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    
                    // 0. Header
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = config?.siteTitle ?: "Fund", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = config?.siteSubtitle ?: "Expense Tracker", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 1. Stats
                    item {
                        val totalCredits = data!!.transactions.filter { it.type == "credit" }.sumOf { it.amount }
                        val totalDebits = data!!.transactions.filter { it.type == "debit" }.sumOf { it.amount }
                        val currentBalance = totalCredits - totalDebits
                        
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6)), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Current Balance", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${FormatUtils.formatAmount(currentBalance)} $currency", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Collected", totalCredits, Color(0xFF10B981), Modifier.weight(1f).fillMaxHeight(), currency)
                            StatCard("Spent", totalDebits, Color(0xFFEF4444), Modifier.weight(1f).fillMaxHeight(), currency)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 2. Member Status
                    item {
                        Text("Member Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val activeBalances = balances.entries.filter { (id, balance) ->
                        val isActive = config!!.members.find { it.id == id }?.active == true
                        isActive || balance != 0.0
                    }.sortedByDescending { it.value }

                    items(activeBalances.chunked(2)) { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { (id, net) ->
                                val name = resolveName(id)
                                val given = data!!.transactions.filter { it.type == "credit" && (it.payerId == id || it.whoOrBill == id) }.sumOf { it.amount }
                                CompactMemberCard(name, net, given, Modifier.weight(1f).fillMaxHeight(), currency)
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 3. Activity
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val visibleTransactions = data!!.transactions.take(displayedCount)
                    
                    items(visibleTransactions) { tx ->
                        // FIX: Logic to show Note if Bill Type is "Other"
                        val displayName = if (tx.type == "credit") {
                            resolveName(tx.payerId ?: tx.whoOrBill)
                        } else {
                            val bid = tx.billTypeId ?: tx.whoOrBill
                            val billName = resolveBillName(bid)
                            
                            if (billName.equals("Other", ignoreCase = true) && tx.note.isNotEmpty()) {
                                tx.note // Show note if "Other"
                            } else {
                                billName
                            }
                        }

                        TransactionRow(tx, displayName, currency) {
                            navController.navigate("detail/${tx.id}")
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    }
                    
                    item {
                        if (displayedCount < data!!.transactions.size) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                OutlinedButton(onClick = { displayedCount += 20 }) { Text("Load Older Transactions") }
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
fun TransactionRow(tx: Transaction, displayTitle: String, currency: String, onClick: () -> Unit) {
    val localDate = DateUtils.formatToLocalDateOnly(tx.date)
    // If we used note as title, don't repeat it in subtitle
    val displaySubtitle = if (tx.note.isNotEmpty() && tx.note != displayTitle) "$localDate • ${tx.note}" else localDate

    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(displayTitle, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(displaySubtitle, fontSize = 12.sp) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if(tx.type=="credit") "+" else "-"}${FormatUtils.formatAmount(tx.amount)} $currency",
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
fun StatCard(title: String, amount: Double, color: Color, modifier: Modifier, currency: String) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${FormatUtils.formatAmount(amount)} $currency", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CompactMemberCard(name: String, net: Double, given: Double, modifier: Modifier, currency: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Given: ${FormatUtils.formatAmount(given)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if(net>0) "+" else ""}${FormatUtils.formatAmount(net)}", 
                        color = if (net >= 0) Color(0xFF059669) else Color(0xFFE11D48), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        textAlign = TextAlign.End
                    )
                    Text(currency, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End)
                }
            }
        }
    }
}
