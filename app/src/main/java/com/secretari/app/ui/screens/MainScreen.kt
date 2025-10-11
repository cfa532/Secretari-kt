package com.secretari.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.util.UserManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    records: List<AudioRecord>,
    loginStatus: UserManager.LoginStatus,
    onRecordClick: (AudioRecord) -> Unit,
    onDeleteRecord: (AudioRecord) -> Unit,
    onStartRecording: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Records") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(when (loginStatus) {
                                    UserManager.LoginStatus.SIGNED_IN -> "Account"
                                    UserManager.LoginStatus.SIGNED_OUT -> "Login"
                                    UserManager.LoginStatus.UNREGISTERED -> "Register"
                                })
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToAccount()
                            },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Purchase") },
                            onClick = {
                                showMenu = false
                                onNavigateToStore()
                            },
                            leadingIcon = { Icon(Icons.Default.Star, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Help") },
                            onClick = {
                                showMenu = false
                                onNavigateToHelp()
                            },
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartRecording,
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text("Start", color = MaterialTheme.colorScheme.onError)
            }
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No records",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Push the START button to record your own speech. A summary will be generated automatically after STOP button is pushed.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "First make sure to select the right language for recognition in settings ⚙️ Otherwise the built-in speech recognizer cannot work properly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(records, key = { it.recordDate }) { record ->
                    RecordListItem(
                        record = record,
                        onClick = { onRecordClick(record) },
                        onDelete = { onDeleteRecord(record) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListItem(
    record: AudioRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(record.recordDate)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.transcript.take(100) + if (record.transcript.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

