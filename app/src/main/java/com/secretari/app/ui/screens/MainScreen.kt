package com.secretari.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.RecognizerLocale
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
                title = { 
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Records",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
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
    ) { padding ->
        if (records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Main content area - centered
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        // Large document icon
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // "No records" title
                        Text(
                            "No records",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Main instruction text
                        Text(
                            "Press the START button to begin recording your speech. Once you press the STOP button, a summary will be generated automatically by OpenAI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Settings instruction with blue text - all in one paragraph
                        Text(
                            "Make sure to select the correct recognizable language in the settings ⚙️ Otherwise, the built-in speech recognizer will not function properly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                // Start button at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStartRecording,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 4.dp,
                            hoveredElevation = 6.dp,
                            focusedElevation = 6.dp
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Start",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(records, key = { it.recordDate }) { record ->
                        RecordListItem(
                            record = record,
                            onClick = { onRecordClick(record) },
                            onDelete = { onDeleteRecord(record) }
                        )
                    }
                }
                
                // Start button at the bottom when records exist
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStartRecording,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 4.dp,
                            hoveredElevation = 6.dp,
                            focusedElevation = 6.dp
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Start",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen(
            records = emptyList(),
            loginStatus = UserManager.LoginStatus.SIGNED_OUT,
            onRecordClick = {},
            onDeleteRecord = {},
            onStartRecording = {},
            onNavigateToSettings = {},
            onNavigateToAccount = {},
            onNavigateToStore = {},
            onNavigateToHelp = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenWithRecordsPreview() {
    MaterialTheme {
        val sampleRecords = listOf(
            AudioRecord(
                recordDate = System.currentTimeMillis() - 86400000, // 1 day ago
                transcript = "This is a sample transcript of a recorded speech. It contains multiple sentences to demonstrate how the list item looks with longer text content.",
                summary = mapOf(RecognizerLocale.ENGLISH to "Sample summary of the recorded speech content.")
            ),
            AudioRecord(
                recordDate = System.currentTimeMillis() - 172800000, // 2 days ago
                transcript = "Another sample record with different content to show variety in the list.",
                summary = mapOf(RecognizerLocale.ENGLISH to "Another sample summary.")
            )
        )
        
        MainScreen(
            records = sampleRecords,
            loginStatus = UserManager.LoginStatus.SIGNED_IN,
            onRecordClick = {},
            onDeleteRecord = {},
            onStartRecording = {},
            onNavigateToSettings = {},
            onNavigateToAccount = {},
            onNavigateToStore = {},
            onNavigateToHelp = {}
        )
    }
}

