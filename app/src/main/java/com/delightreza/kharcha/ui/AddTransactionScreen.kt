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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.Repository
import com.delightreza.kharcha.data.Transaction
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(navController: NavController, repository: Repository, token: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Form State
    var type by remember { mutableStateOf("debit") }
    var amount by remember { mutableStateOf("") }
    var whoOrBill by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    // Date & Time Logic
    // We use a Calendar object to store the selection. Null means "Now".
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    
    val allPeople = listOf("Raza", "Salman", "Mujeeb", "Gulam", "Rana", "Naved", "Musawwar", "Nizamuddin")
    val bills = listOf("Electricity", "Water", "Gas", "Garbage", "Internet", "Other")
    
    var exemptions by remember { mutableStateOf(setOf<String>()) }

    // 2. Time Picker (Shown after Date Picker)
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            // Update the time part of the selected calendar
            selectedDateTime?.let { cal ->
                val newCal = cal.clone() as Calendar
                newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCal.set(Calendar.MINUTE, minute)
                selectedDateTime = newCal
            }
        },
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        Calendar.getInstance().get(Calendar.MINUTE),
        true // 24 hour view
    )

    // 1. Date Picker (Shown on click)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            // Set the date part
            val newCal = Calendar.getInstance()
            newCal.set(year, month, day)
            selectedDateTime = newCal
            
            // Immediately show Time Picker
            timePickerDialog.show()
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    // Helper to format display text
    val dateDisplay = remember(selectedDateTime) {
        if (selectedDateTime == null) {
            "Today (Now)"
        } else {
            val cal = selectedDateTime!!
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            
            // Format: YYYY-MM-DD HH:mm
            val mStr = if (m < 10) "0$m" else "$m"
            val dStr = if (d < 10) "0$d" else "$d"
            val hStr = if (h < 10) "0$h" else "$h"
            val minStr = if (min < 10) "0$min" else "$min"
            
            "$y-$mStr-$dStr $hStr:$minStr"
        }
    }

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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saving to GitHub...")
                }
            }
        } else {
            Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
                // Type Switcher
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "debit",
                        onClick = { type = "debit"; whoOrBill = ""; exemptions = emptySet() },
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

                // Subject Selection
                Text("Select Subject:", style = MaterialTheme.typography.labelMedium)
                val options = if(type == "credit") allPeople else bills
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker Input
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
                    // Transparent clickable surface on top
                    Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                }

                // Exemptions (Debit Only)
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
                                    Checkbox(checked = exemptions.contains(person), onCheckedChange = null)
                                    Text(person, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (amount.isNotEmpty() && whoOrBill.isNotEmpty()) {
                            isSubmitting = true
                            scope.launch {
                                // Final Date Logic
                                val finalDate = if (selectedDateTime == null) {
                                    java.time.Instant.now().toString()
                                } else {
                                    // Convert Calendar to Instant (UTC)
                                    selectedDateTime!!.toInstant().toString()
                                }

                                val tx = Transaction(
                                    id = "tx_${System.currentTimeMillis()}",
                                    type = type,
                                    whoOrBill = whoOrBill,
                                    amount = amount.toDouble(),
                                    note = note,
                                    date = finalDate,
                                    exemptions = if(type == "debit" && exemptions.isNotEmpty()) exemptions.toList() else null
                                )
                                
                                val success = repository.addTransaction(token, tx)
                                if (success) {
                                    navController.popBackStack()
                                } else {
                                    isSubmitting = false
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
