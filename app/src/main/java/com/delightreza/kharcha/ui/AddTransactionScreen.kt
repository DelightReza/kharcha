package com.delightreza.kharcha.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showConfirmation by remember { mutableStateOf(false) }
    
    // Form State
    var type by remember { mutableStateOf("debit") }
    var amount by remember { mutableStateOf("") }
    var whoOrBill by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    // Date & Time Logic
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    
    val allPeople = listOf("Raza", "Salman", "Mujeeb", "Gulam", "Rana", "Naved", "Musawwar", "Nizamuddin")
    val bills = listOf("Electricity", "Water", "Gas", "Garbage", "Internet", "Other")
    
    var exemptions by remember { mutableStateOf(setOf<String>()) }

    // Pickers
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

    val dateDisplay = remember(selectedDateTime) {
        if (selectedDateTime == null) "Today (Now)"
        else {
            val cal = selectedDateTime!!
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            "$y-${if (m < 10) "0$m" else "$m"}-${if (d < 10) "0$d" else "$d"} ${if (h < 10) "0$h" else "$h"}:${if (min < 10) "0$min" else "$min"}"
        }
    }

    // Colors
    val themeColor = if (type == "credit") Color(0xFF059669) else Color(0xFFDC2626) // Green vs Red

    // Save Logic
    val handleSave = {
        isSubmitting = true
        scope.launch {
            val finalDate = if (selectedDateTime == null) {
                java.time.Instant.now().toString()
            } else {
                selectedDateTime!!.toInstant().toString()
            }

            val tx = Transaction(
                id = "tx_${System.currentTimeMillis()}_app",
                type = type,
                whoOrBill = whoOrBill,
                note = note,
                amount = amount.toDouble(),
                date = finalDate,
                exemptions = if (type == "debit" && exemptions.isNotEmpty()) exemptions.toList() else null
            )
            
            val success = repository.addTransaction(token, tx)
            if (success) {
                navController.popBackStack()
            } else {
                isSubmitting = false
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Transaction") },
            text = {
                Column {
                    Text("Are you sure you want to save this?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Type: ${type.uppercase()}", fontWeight = FontWeight.Bold)
                    Text("Amount: $amount SOM", fontWeight = FontWeight.Bold)
                    Text("Subject: $whoOrBill")
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showConfirmation = false
                        handleSave() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmation = false }) { Text("Cancel") }
            }
        )
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
                    CircularProgressIndicator(color = themeColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saving to GitHub...", color = Color.Gray)
                }
            }
        } else {
            Column(modifier = Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
                
                // 1. Type Switcher
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "debit",
                        onClick = { type = "debit"; whoOrBill = ""; exemptions = emptySet() },
                        label = { Text("Debit (Bill)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFEE2E2), selectedLabelColor = Color(0xFFDC2626))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == "credit",
                        onClick = { type = "credit"; whoOrBill = "" },
                        label = { Text("Credit (Deposit)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF059669))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Subject Selection
                Text("Select Subject", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                val options = if(type == "credit") allPeople else bills
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options.size) { i ->
                        val item = options[i]
                        val isSelected = whoOrBill == item
                        InputChip(
                            selected = isSelected,
                            onClick = { whoOrBill = item },
                            label = { Text(item) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = themeColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        focusedLabelColor = themeColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Date Picker
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

                // 6. Exemptions (Debit Only)
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

                // 7. SUMMARY INFO & SAVE BUTTON
                if (amount.isNotEmpty() && whoOrBill.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (type == "credit") "Receiving Credit" else "Recording Expense",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "$amount SOM",
                                style = MaterialTheme.typography.headlineMedium,
                                color = themeColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Subject: $whoOrBill",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = { showConfirmation = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = amount.isNotEmpty() && whoOrBill.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("Save to GitHub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
