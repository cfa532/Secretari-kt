package com.secretari.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.PromptType
import com.secretari.app.data.model.RecognizerLocale
import com.secretari.app.data.model.Settings
import com.secretari.app.ui.viewmodel.MainViewModel
import com.secretari.app.util.UserManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    records: List<AudioRecord>,
    loginStatus: UserManager.LoginStatus,
    viewModel: MainViewModel = viewModel(),
    onRecordClick: (AudioRecord) -> Unit,
    onDeleteRecord: (AudioRecord) -> Unit,
    onStartRecording: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val settings by viewModel.settings.collectAsState(initial = com.secretari.app.data.model.AppConstants.DEFAULT_SETTINGS)
    
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
                            modifier = Modifier
                                .size(80.dp)
                                .alpha(0.5f)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
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
                                elevation = 6.dp,
                                shape = CircleShape,
                                clip = false
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
                            settings = settings,
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
                                elevation = 6.dp,
                                shape = CircleShape,
                                clip = false
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
    settings: Settings,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val deleteButtonWidth = with(density) { 60.dp.toPx() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Delete button background
        if (offsetX < 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(60.dp)
                    .height(IntrinsicSize.Min),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Main card
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = with(density) { offsetX.toDp() })
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetX + delta
                        offsetX = newOffset.coerceAtLeast(-deleteButtonWidth).coerceAtMost(0f)
                    },
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    onDragStopped = {
                        // Snap to either fully open or fully closed
                        if (offsetX < -deleteButtonWidth / 2) {
                            offsetX = -deleteButtonWidth
                        } else {
                            offsetX = 0f
                        }
                    }
                )
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
                    
                    // Display content based on prompt type
                    val displayText = when (settings.promptType) {
                        PromptType.CHECKLIST -> {
                            if (record.memo.isNotEmpty()) {
                                // Concatenate checklist items into one paragraph
                                record.memo.joinToString(" • ") { item ->
                                    val title = item.title[record.locale] ?: "Unknown item"
                                    "${if (item.isChecked) "✓" else "○"} $title"
                                }
                            } else {
                                // Fallback to transcript if no checklist items
                                record.transcript
                            }
                        }
                        else -> {
                            // For SUMMARY and SUBSCRIPTION, show summary if available, otherwise transcript
                            val summaryText = record.summary[record.locale]
                            if (!summaryText.isNullOrEmpty()) {
                                summaryText
                            } else {
                                record.transcript
                            }
                        }
                    }
                    
                    Text(
                        text = displayText.take(150) + if (displayText.length > 150) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        lineHeight = 18.sp
                    )
                }
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

