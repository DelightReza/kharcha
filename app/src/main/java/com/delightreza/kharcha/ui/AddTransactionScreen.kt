package com.delightreza.kharcha.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.BillTypeConfig
import com.delightreza.kharcha.data.MemberConfig
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController, 
    repository: Repository, 
    token: String,
    transactionIdToEdit: String? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(true) }
    var showConfirmation by remember { mutableStateOf(false) }
    
    var type by remember { mutableStateOf("debit") }
    var amount by remember { mutableStateOf("") }
    
    var selectedId by remember { mutableStateOf("") } 
    var fromId by remember { mutableStateOf("") }
    var toId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    var excludedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    
    var activeMembers by remember { mutableStateOf(listOf<MemberConfig>()) }
    var activeBillTypes by remember { mutableStateOf(listOf<BillTypeConfig>()) }
    var currency by remember { mutableStateOf("SOM") }
    
    var originalId by remember { mutableStateOf("") }
    var originalDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val config = repository.getAppConfig()
        if (config != null) {
            activeMembers = config.members.filter { it.active }
            activeBillTypes = config.billTypes.filter { it.active }
            currency = config.currency
        }

        if (transactionIdToEdit != null) {
            val data = repository.getCachedData()
            val tx = data?.transactions?.find { it.id == transactionIdToEdit }
            if (tx != null) {
                originalId = tx.id
                originalDate = tx.date
                type = tx.type
                amount = if(tx.amount % 1.0 == 0.0) tx.amount.toInt().toString() else tx.amount.toString()
                selectedId = if (type == "credit") tx.payerId ?: tx.whoOrBill else tx.billTypeId ?: tx.whoOrBill
                note = tx.note
                
                if (type == "debit" && tx.splitAmong != null && config != null) {
                    val split = tx.splitAmong
                    excludedIds = config.members.map { it.id }.filter { !split.contains(it) }.toSet()
                }
            }
        }
        isLoadingData = false
    }

    val timePickerDialog = TimePickerDialog(context, { _, h, m -> 
        selectedDateTime?.let { val n = it.clone() as Calendar; n.set(Calendar.HOUR_OF_DAY, h); n.set(Calendar.MINUTE, m); selectedDateTime = n } 
    }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true)
    
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> 
        val n = Calendar.getInstance(); n.set(y, m, d); selectedDateTime = n; timePickerDialog.show() 
    }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

    val dateDisplay = remember(selectedDateTime, originalDate) {
        if (selectedDateTime != null) "Selected: ${selectedDateTime!!.time}"
        else if (transactionIdToEdit != null) "Original Date"
        else "Today (Now)"
    }

    val themeColor = when(type) {
        "credit" -> Color(0xFF059669)
        "distribute" -> Color(0xFF7C3AED)
        "settlement" -> Color(0xFF059669) 
        "transfer" -> Color(0xFF2563EB)
        else -> Color(0xFFDC2626)
    }

    val handleSave = {
        isSubmitting = true
        scope.launch {
            // NEW DATE LOGIC: Use local string, no UTC conversion
            val finalDate = if (selectedDateTime != null) {
                DateUtils.getStringFromLocal(selectedDateTime!!)
            } else if (transactionIdToEdit != null) {
                originalDate
            } else {
                DateUtils.getCurrentTime()
            }

            var success = false
            try {
                when (type) {
                    "distribute" -> success = repository.addDistribution(token, amount.toDouble(), note, finalDate)
                    "settlement" -> success = repository.addSettlement(token, fromId, toId, amount.toDouble(), note, finalDate)
                    "transfer" -> success = repository.addTransfer(token, fromId, toId, amount.toDouble(), note, finalDate)
                    else -> {
                        var splitAmong: List<String>? = null
                        if (type == "debit") splitAmong = activeMembers.map { it.id }.filter { !excludedIds.contains(it) }
                        
                        val tx = Transaction(
                            id = if (transactionIdToEdit != null) originalId else "tx_${System.currentTimeMillis()}_app",
                            type = type, payerId = if (type == "credit") selectedId else null,
                            billTypeId = if (type == "debit") selectedId else null, splitAmong = splitAmong,
                            whoOrBill = selectedId, note = note, amount = amount.toDouble(), date = finalDate
                        )
                        success = if (transactionIdToEdit != null) repository.editTransaction(token, tx) else repository.addTransaction(token, tx)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            if (success) navController.popBackStack() else isSubmitting = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if(transactionIdToEdit != null) "Edit Transaction" else "New Transaction") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } }) }
    ) { p ->
        if (isLoadingData || isSubmitting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = themeColor) }
        } else {
            if (showConfirmation) {
                AlertDialog(
                    onDismissRequest = { showConfirmation = false },
                    title = { Text("Confirm") },
                    text = {
                        Column {
                            val cleanAmount = if(amount.contains(".0") && amount.endsWith("0")) amount.substringBefore(".") else amount
                            Text(text = "$cleanAmount $currency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = themeColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = type.uppercase(), style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    confirmButton = { Button(onClick = { showConfirmation = false; handleSave() }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Confirm") } },
                    dismissButton = { OutlinedButton(onClick = { showConfirmation = false }) { Text("Cancel") } }
                )
            }

            Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    item { FilterChip(selected = type == "debit", onClick = { type = "debit"; selectedId = ""; excludedIds = emptySet() }, label = { Text("Debit") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2), selectedLabelColor = Color(0xFFDC2626))) }
                    item { FilterChip(selected = type == "credit", onClick = { type = "credit"; selectedId = "" }, label = { Text("Credit") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))) }
                    if (transactionIdToEdit == null) {
                        item { FilterChip(selected = type == "distribute", onClick = { type = "distribute"; selectedId = "All" }, label = { Text("Distribute") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEDE9FE), selectedLabelColor = Color(0xFF7C3AED))) }
                        item { FilterChip(selected = type == "settlement", onClick = { type = "settlement"; fromId = ""; toId = "" }, label = { Text("Settlement") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))) }
                        item { FilterChip(selected = type == "transfer", onClick = { type = "transfer"; fromId = ""; toId = "" }, label = { Text("Transfer") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDBEAFE), selectedLabelColor = Color(0xFF2563EB))) }
                    }
                }

                if (type == "debit" || type == "credit") {
                    Text("Select Subject", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (type == "credit") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(activeMembers.size) { i -> val m = activeMembers[i]; InputChip(selected = selectedId == m.id, onClick = { selectedId = m.id }, label = { Text(m.name) }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)) } }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(activeBillTypes.size) { i -> val b = activeBillTypes[i]; InputChip(selected = selectedId == b.id, onClick = { selectedId = b.id }, label = { Text("${b.icon} ${b.name}") }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)) } }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (type == "settlement" || type == "transfer") {
                     Text("From / Paid By", style = MaterialTheme.typography.labelMedium, color = Color.Gray); Spacer(modifier = Modifier.height(8.dp))
                     LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(activeMembers.size) { i -> InputChip(selected = fromId == activeMembers[i].id, onClick = { fromId = activeMembers[i].id }, label = { Text(activeMembers[i].name) }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)) } }; Spacer(modifier = Modifier.height(16.dp))
                     Text("To / Received By", style = MaterialTheme.typography.labelMedium, color = Color.Gray); Spacer(modifier = Modifier.height(8.dp))
                     LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(activeMembers.size) { i -> InputChip(selected = toId == activeMembers[i].id, onClick = { toId = activeMembers[i].id }, label = { Text(activeMembers[i].name) }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)) } }; Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($currency)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor))
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = dateDisplay, onValueChange = {}, readOnly = true, label = { Text("Date") }, trailingIcon = { Icon(Icons.Default.CalendarToday, "") }, modifier = Modifier.fillMaxWidth(), enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline))
                    Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                }

                if (type == "debit") {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Exclude (Who didn't pay?)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    activeMembers.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { person ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { excludedIds = if (excludedIds.contains(person.id)) excludedIds - person.id else excludedIds + person.id }) {
                                    Checkbox(checked = excludedIds.contains(person.id), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color.Red))
                                    Text(person.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                val isValid = when(type) {
                    "debit", "credit" -> amount.isNotEmpty() && selectedId.isNotEmpty()
                    "distribute" -> amount.isNotEmpty()
                    "settlement", "transfer" -> amount.isNotEmpty() && fromId.isNotEmpty() && toId.isNotEmpty() && fromId != toId
                    else -> false
                }
                Button(onClick = { showConfirmation = true }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = isValid, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text(if(transactionIdToEdit != null) "Update" else "Save", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
