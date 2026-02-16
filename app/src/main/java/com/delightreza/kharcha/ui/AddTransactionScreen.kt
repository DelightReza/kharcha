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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(true) }
    var showConfirmation by remember { mutableStateOf(false) }
    
    var type by remember { mutableStateOf("debit") }
    var amount by remember { mutableStateOf("") }
    var whoOrBill by remember { mutableStateOf("") }
    var fromSubject by remember { mutableStateOf("") }
    var toSubject by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var exemptions by remember { mutableStateOf(setOf<String>()) }
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    
    var allPeople by remember { mutableStateOf(Constants.DEFAULT_MEMBERS) }
    var billTypes by remember { mutableStateOf(Constants.DEFAULT_BILL_TYPES) }
    var currency by remember { mutableStateOf("SOM") }
    
    var originalId by remember { mutableStateOf("") }
    var originalDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val config = repository.getAppConfig()
        if (config != null) {
            allPeople = config.people.sorted()
            billTypes = config.billTypes.map { it.name }.sorted()
            currency = config.currency
        } else {
            // Fallback to fetch data if config missing (unlikely if nav flow is correct)
            val data = repository.fetchData()
            if (data != null) {
                allPeople = data.people.keys.sorted()
                billTypes = data.billTypes.keys.sorted()
            }
        }

        if (transactionIdToEdit != null) {
            val data = repository.getCachedData()
            val tx = data?.transactions?.find { it.id == transactionIdToEdit }
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
        isLoadingData = false
    }

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
        "settlement" -> Color(0xFF059669) 
        "transfer" -> Color(0xFF2563EB)
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
                    "distribute" -> success = repository.addDistribution(token, amount.toDouble(), note, finalDate)
                    "settlement" -> success = repository.addSettlement(token, fromSubject, toSubject, amount.toDouble(), note, finalDate)
                    "transfer" -> success = repository.addTransfer(token, fromSubject, toSubject, amount.toDouble(), note, finalDate)
                    else -> {
                        val tx = Transaction(
                            id = if (transactionIdToEdit != null) originalId else "tx_${System.currentTimeMillis()}_app",
                            type = type,
                            whoOrBill = whoOrBill,
                            note = note,
                            amount = amount.toDouble(),
                            date = finalDate,
                            exemptions = if (type == "debit" && exemptions.isNotEmpty()) exemptions.toList() else null
                        )
                        success = if (transactionIdToEdit != null) repository.editTransaction(token, tx) else repository.addTransaction(token, tx)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            if (success) navController.popBackStack() else isSubmitting = false
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
                CircularProgressIndicator(color = themeColor)
            }
        } else {
            if (showConfirmation) {
                AlertDialog(
                    onDismissRequest = { showConfirmation = false },
                    title = { Text("Confirm") },
                    text = {
                        Column {
                            Text(text = "$amount $currency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = themeColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            val detailText = when (type) {
                                "distribute" -> "Distribute equally"
                                "settlement" -> "$fromSubject ➔ $toSubject (Cash)"
                                "transfer" -> "$fromSubject ➔ $toSubject (Fund)"
                                else -> "${type.uppercase()}: $whoOrBill"
                            }
                            Text(text = detailText, style = MaterialTheme.typography.titleMedium)
                            if (note.isNotEmpty()) Text(text = "\"$note\"", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Color.Gray)
                        }
                    },
                    confirmButton = { Button(onClick = { showConfirmation = false; handleSave() }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Confirm") } },
                    dismissButton = { OutlinedButton(onClick = { showConfirmation = false }) { Text("Cancel") } }
                )
            }

            Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    item { FilterChip(selected = type == "debit", onClick = { type = "debit"; whoOrBill = ""; exemptions = emptySet() }, label = { Text("Debit") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2), selectedLabelColor = Color(0xFFDC2626))) }
                    item { FilterChip(selected = type == "credit", onClick = { type = "credit"; whoOrBill = "" }, label = { Text("Credit") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))) }
                    if (transactionIdToEdit == null) {
                        item { FilterChip(selected = type == "distribute", onClick = { type = "distribute"; whoOrBill = "All" }, label = { Text("Distribute") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEDE9FE), selectedLabelColor = Color(0xFF7C3AED))) }
                        item { FilterChip(selected = type == "settlement", onClick = { type = "settlement"; fromSubject = ""; toSubject = "" }, label = { Text("Settlement") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))) }
                        item { FilterChip(selected = type == "transfer", onClick = { type = "transfer"; fromSubject = ""; toSubject = "" }, label = { Text("Transfer") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFDBEAFE), selectedLabelColor = Color(0xFF2563EB))) }
                    }
                }

                if (type == "debit" || type == "credit") {
                    Text("Select Subject", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val options = if(type == "credit") allPeople else billTypes
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(options.size) { i ->
                            val item = options[i]
                            InputChip(selected = whoOrBill == item, onClick = { whoOrBill = item }, label = { Text(item) }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (type == "settlement" || type == "transfer") {
                    Text("From / Paid By", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allPeople.size) { i -> InputChip(selected = fromSubject == allPeople[i], onClick = { fromSubject = allPeople[i] }, label = { Text(allPeople[i]) }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("To / Received By", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allPeople.size) { i -> InputChip(selected = toSubject == allPeople[i], onClick = { toSubject = allPeople[i] }, label = { Text(allPeople[i]) }, colors = InputChipDefaults.inputChipColors(selectedContainerColor = themeColor, selectedLabelColor = Color.White)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                    Text("Exemptions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    allPeople.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { person ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { exemptions = if (exemptions.contains(person)) exemptions - person else exemptions + person }) {
                                    Checkbox(checked = exemptions.contains(person), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = themeColor))
                                    Text(person, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                val isValid = when(type) {
                    "debit", "credit" -> amount.isNotEmpty() && whoOrBill.isNotEmpty()
                    "distribute" -> amount.isNotEmpty()
                    "settlement", "transfer" -> amount.isNotEmpty() && fromSubject.isNotEmpty() && toSubject.isNotEmpty() && fromSubject != toSubject
                    else -> false
                }
                Button(onClick = { showConfirmation = true }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = isValid, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text(if(transactionIdToEdit != null) "Update" else "Save", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
