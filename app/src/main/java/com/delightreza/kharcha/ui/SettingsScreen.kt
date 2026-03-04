package com.delightreza.kharcha.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.delightreza.kharcha.data.AppConfig
import com.delightreza.kharcha.data.BillTypeConfig
import com.delightreza.kharcha.data.MemberConfig
import com.delightreza.kharcha.data.Repository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    repository: Repository,
    token: String
) {
    val scope = rememberCoroutineScope()
    
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Edit States
    var newPersonName by remember { mutableStateOf("") }
    var newBillName by remember { mutableStateOf("") }
    var newBillIcon by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        config = repository.getAppConfig()
        isLoading = false
    }

    fun saveConfig() {
        if (config == null) return
        isSaving = true
        scope.launch {
            val success = repository.updateRemoteConfig(token, config!!)
            isSaving = false
            if (success) {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "") } },
                actions = {
                    TextButton(onClick = { saveConfig() }, enabled = !isSaving) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp)) else Text("Save")
                    }
                }
            )
        }
    ) { p ->
        if (isLoading || config == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(p).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // MEMBERS SECTION
                item {
                    Text("People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newPersonName,
                            onValueChange = { newPersonName = it },
                            label = { Text("Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newPersonName.isNotBlank() && config!!.members.none { it.name == newPersonName }) {
                                    val id = newPersonName.trim().lowercase().replace("\\s+".toRegex(), "_")
                                    val newMember = MemberConfig(id = id, name = newPersonName.trim(), active = true)
                                    config = config!!.copy(members = config!!.members + newMember)
                                    newPersonName = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors()
                        ) { Icon(Icons.Default.Add, "") }
                    }
                }

                items(config!!.members) { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("👤 ${member.name}")
                            Text(if(member.active) "Active" else "Inactive", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        Row {
                            // Toggle Active
                            TextButton(onClick = {
                                val updatedMembers = config!!.members.map { 
                                    if(it.id == member.id) it.copy(active = !it.active) else it 
                                }
                                config = config!!.copy(members = updatedMembers)
                            }) { Text(if(member.active) "Disable" else "Enable") }
                            
                            IconButton(
                                onClick = {
                                    config = config!!.copy(members = config!!.members.filter { it.id != member.id })
                                }
                            ) { Icon(Icons.Default.Delete, "", tint = Color.Red.copy(alpha = 0.6f)) }
                        }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }

                // BILL TYPES SECTION
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Bill Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newBillIcon,
                            onValueChange = { newBillIcon = it },
                            placeholder = { Text("🏠") },
                            modifier = Modifier.width(60.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newBillName,
                            onValueChange = { newBillName = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newBillName.isNotBlank() && config!!.billTypes.none { it.name == newBillName }) {
                                    val icon = if (newBillIcon.isBlank()) "🧾" else newBillIcon.trim()
                                    val id = newBillName.trim().lowercase().replace("\\s+".toRegex(), "_")
                                    val newBill = BillTypeConfig(id = id, name = newBillName.trim(), icon = icon)
                                    config = config!!.copy(billTypes = config!!.billTypes + newBill)
                                    newBillName = ""
                                    newBillIcon = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors()
                        ) { Icon(Icons.Default.Add, "") }
                    }
                }

                items(config!!.billTypes) { bill ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${bill.icon} ${bill.name}")
                        IconButton(
                            onClick = {
                                config = config!!.copy(billTypes = config!!.billTypes.filter { it.id != bill.id })
                            }
                        ) { Icon(Icons.Default.Delete, "", tint = Color.Red.copy(alpha = 0.6f)) }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Note: Saving will commit changes to config.json on GitHub.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
