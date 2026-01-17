package com.delightreza.kharcha.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import com.delightreza.kharcha.utils.Constants
import kotlinx.coroutines.launch
import java.time.Instant
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
    
    // State
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(true) }
    var showConfirmation by remember { mutableStateOf(false) }
    
    // Transaction Mode: "debit", "credit", "distribute", "settlement", "transfer"
    var type by remember { mutableStateOf("debit") }
    
    var amount by remember { mutableStateOf("") }
    
    // Primary Subject (Who paid credit/bill)
    var whoOrBill by remember { mutableStateOf("") }
    
    // For Settlement/Transfer (Two Subjects)
    var fromSubject by remember { mutableStateOf("") }
    var toSubject by remember { mutableStateOf("") }
    
    var note by remember { mutableStateOf("") }
    var exemptions by remember { mutableStateOf(setOf<String>()) }
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    
    // Dynamic Lists
    var allPeople by remember { mutableStateOf(Constants.DEFAULT_MEMBERS) }
    var billTypes by remember { mutableStateOf(Constants.DEFAULT_BILL_TYPES) }
    
    // Keeps original ID if editing
    var originalId by remember { mutableStateOf("") }
    var originalDate by remember { mutableStateOf("") }

    // Load Data
    LaunchedEffect(Unit) {
        val data = repository.getCachedData() ?: repository.fetchData()
        
        if (data != null) {
            if (data.people.isNotEmpty()) allPeople = data.people.keys.toList().sorted()
            if (data.billTypes.isNotEmpty()) billTypes = data.billTypes.keys.toList().sorted()
            
            // If editing, load fields
            if (transactionIdToEdit != null) {
                val tx = data.transactions.find { it.id == transactionIdToEdit }
                if (tx != null) {
                    originalId = tx.id
                    originalDate = tx.date
                    type = tx.type
                    amount = if(tx.amount % 1.0 == 0.0) tx.amount.toInt().toString() else tx.amount.toString()
                    whoOrBill = tx.whoOrBill
                    note = tx.note
                    exemptions = tx.exemptions?.toSet() ?: emptySet()
                }
            }
        }
        isLoadingData = false
    }

    // ... Time/Date Picker Logic ...
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedDateTime?.let { cal ->
                val newCal = cal.clone() as Calendar
                newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCal.set(Calendar.MINUTE, minute)
                selectedDateTime = newCal
            }
        },
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        Calendar.getInstance().get(Calendar.MINUTE),
        true
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val newCal = Calendar.getInstance()
            newCal.set(year, month, day)
            selectedDateTime = newCal
            timePickerDialog.show()
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    val dateDisplay = remember(selectedDateTime, originalDate) {
        if (selectedDateTime != null) {
            val cal = selectedDateTime!!
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            "$y-${if (m < 10) "0$m" else "$m"}-${if (d < 10) "0$d" else "$d"} ${if (h < 10) "0$h" else "$h"}:${if (min < 10) "0$min" else "$min"}"
        } else if (transactionIdToEdit != null && originalDate.isNotEmpty()) {
            "Original Date (Keep)"
        } else {
            "Today (Now)"
        }
    }

    val themeColor = when(type) {
        "credit" -> Color(0xFF059669)
        "distribute" -> Color(0xFF7C3AED)
        "settlement" -> Color(0xFF059669) // Emerald for Offline Payment
        "transfer" -> Color(0xFF2563EB)   // Blue for Transfer
        else -> Color(0xFFDC2626)
    }

    val handleSave = {
        isSubmitting = true
        scope.launch {
            val finalDate = if (selectedDateTime != null) {
                selectedDateTime!!.toInstant().toString()
            } else if (transactionIdToEdit != null) {
                originalDate
            } else {
                Instant.now().toString()
            }

            var success = false
            
            try {
                when (type) {
                    "distribute" -> {
                        success = repository.addDistribution(token, amount.toDouble(), note, finalDate)
                    }
                    "settlement" -> {
                        success = repository.addSettlement(token, fromSubject, toSubject, amount.toDouble(), note, finalDate)
                    }
                    "transfer" -> {
                        success = repository.addTransfer(token, fromSubject, toSubject, amount.toDouble(), note, finalDate)
                    }
                    else -> {
                        // Debit / Credit
                        val tx = Transaction(
                            id = if (transactionIdToEdit != null) originalId else "tx_${System.currentTimeMillis()}_app",
                            type = type,
                            whoOrBill = whoOrBill,
                            note = note,
                            amount = amount.toDouble(),
                            date = finalDate,
                            exemptions = if (type == "debit" && exemptions.isNotEmpty()) exemptions.toList() else null
                        )
                        
                        success = if (transactionIdToEdit != null) {
                            repository.editTransaction(token, tx)
                        } else {
                            repository.addTransaction(token, tx)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (success) {
                navController.popBackStack()
            } else {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(transactionIdToEdit != null) "Edit Transaction" else "New Transaction") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } }
            )
        }
    ) { p ->
        if (isLoadingData || isSubmitting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = themeColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if(isSubmitting) "Saving to GitHub..." else "Loading...", color = Color.Gray)
                }
            }
        } else {
            // --- UPDATED CONFIRMATION DIALOG ---
            if (showConfirmation) {
                AlertDialog(
                    onDismissRequest = { showConfirmation = false },
                    title = { Text("Confirm ${if(transactionIdToEdit!=null) "Update" else "Save"}") },
                    text = {
                        Column {
                            // Amount Display
                            Text(
                                text = "$amount SOM",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Details Display Logic
                            val detailText = when (type) {
                                "distribute" -> "Distribute equally to all"
                                "settlement" -> "$fromSubject ➔ $toSubject (Cash)"
                                "transfer" -> "$fromSubject ➔ $toSubject (Fund)"
                                else -> "${type.replaceFirstChar { it.uppercase() }}: $whoOrBill"
                            }
                            
                            Text(text = detailText, style = MaterialTheme.typography.titleMedium)
                            
                            // Note Display
                            if (note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"$note\"", 
                                    style = MaterialTheme.typography.bodyMedium, 
                                    fontStyle = FontStyle.Italic, 
                                    color = Color.Gray
                                )
                            }
                            
                            // Exemptions Display
                            if (type == "debit" && exemptions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Exempt: ${exemptions.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showConfirmation = false; handleSave() },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) { Text("Confirm") }
                    },
                    dismissButton = { OutlinedButton(onClick = { showConfirmation = false }) { Text("Cancel") } }
                )
            }

            Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
                
                // TYPE SELECTION ROW
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    item {
                        FilterChip(
                            selected = type == "debit",
                            onClick = { type = "debit"; whoOrBill = ""; exemptions = emptySet() },
                            label = { Text("Debit") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2), selectedLabelColor = Color(0xFFDC2626))
                        )
                    }
                    item {
                        FilterChip(
                            selected = type == "credit",
                            onClick = { type = "credit"; whoOrBill = "" },
                            label = { Text("Credit") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))
                        )
                    }
                    if (transactionIdToEdit == null) {
                        item {
                            FilterChip(
                                selected = type == "distribute",
                                onClick = { type = "distribute"; whoOrBill = "All" },
                                label = { Text("Distribute") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEDE9FE), selectedLabelColor = Color(0xFF7C3AED))
                            )
                        }
                        item {
                            FilterChip(
                                selected = type == "settlement",
                                onClick = { type = "settlement"; fromSubject = ""; toSubject = "" },
                                label = { Text("Settlement (Cash)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))
                            )
                        }
                        item {
                            FilterChip(
                                selected = type == "transfer",
                                onClick = { type = "transfer"; fromSubject = ""; toSubject = "" },
                                label = { Text("Transfer (Fund)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDBEAFE), selectedLabelColor = Color(0xFF2563EB))
                            )
                        }
                    }
                }

                // --- 1. SINGLE SUBJECT MODE (Debit / Credit) ---
                if (type == "debit" || type == "credit") {
                    Text("Select Subject", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val options = if(type == "credit") allPeople else billTypes
                    
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(options.size) { i ->
                            val item = options[i]
                            val isSelected = whoOrBill == item
                            InputChip(
                                selected = isSelected,
                                onClick = { whoOrBill = item },
                                label = { Text(item) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- 2. DUAL SUBJECT MODE (Settlement / Transfer) ---
                if (type == "settlement" || type == "transfer") {
                    val fromLabel = if(type == "settlement") "Paid By (Gave Cash)" else "Sender (Giving Fund)"
                    val toLabel = if(type == "settlement") "Received By (Got Cash)" else "Recipient (Getting Fund)"

                    Text("From Account", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allPeople.size) { i ->
                            val person = allPeople[i]
                            InputChip(
                                selected = fromSubject == person,
                                onClick = { fromSubject = person },
                                label = { Text(person) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)
                            )
                        }
                    }
                    Text(fromLabel, style = MaterialTheme.typography.bodySmall, color = themeColor)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("To Account", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allPeople.size) { i ->
                            val person = allPeople[i]
                            InputChip(
                                selected = toSubject == person,
                                onClick = { toSubject = person },
                                label = { Text(person) },
                                colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)
                            )
                        }
                    }
                    Text(toLabel, style = MaterialTheme.typography.bodySmall, color = themeColor)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // AMOUNT INPUT
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // NOTE INPUT
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // DATE INPUT
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date & Time") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, "") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                }

                // EXEMPTIONS (Only for Debit)
                if (type == "debit") {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Exemptions (Who doesn't pay?)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    allPeople.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { person ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).clickable {
                                        exemptions = if (exemptions.contains(person)) exemptions - person else exemptions + person
                                    }
                                ) {
                                    Checkbox(
                                        checked = exemptions.contains(person), 
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = themeColor)
                                    )
                                    Text(person, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Validation Logic
                val isValid = when(type) {
                    "debit", "credit" -> amount.isNotEmpty() && whoOrBill.isNotEmpty()
                    "distribute" -> amount.isNotEmpty()
                    "settlement", "transfer" -> amount.isNotEmpty() && fromSubject.isNotEmpty() && toSubject.isNotEmpty() && fromSubject != toSubject
                    else -> false
                }

                Button(
                    onClick = { showConfirmation = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text(if(transactionIdToEdit != null) "Update Transaction" else "Save to GitHub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
