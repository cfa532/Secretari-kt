package com.secretari.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.AppConstants
import com.secretari.app.data.model.RecognizerLocale
import com.secretari.app.data.model.Settings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    isRecording: Boolean,
    isStreaming: Boolean,
    transcript: String,
    streamedText: String,
    record: AudioRecord?,
    settings: Settings,
    onStopRecording: () -> Unit,
    onSendToAI: (String) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onTranslate: () -> Unit,
    onRegenerate: () -> Unit = {},
    onEditTranscript: (String) -> Unit = {},
    onLocaleChange: (RecognizerLocale) -> Unit,
    isListening: Boolean = false,
    audioLevel: Float = -60f,
    audioFilePath: String? = null,
    errorMessage: String? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showEditTranscriptDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    LaunchedEffect(transcript) {
        if (isRecording && transcript.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    LaunchedEffect(streamedText) {
        if (isStreaming && streamedText.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isRecording
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMenu = true },
                        enabled = !isRecording
                    ) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Translate") },
                            onClick = {
                                showMenu = false
                                onTranslate()
                            },
                            leadingIcon = { Icon(Icons.Default.Translate, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Original Transcript") },
                            onClick = {
                                showMenu = false
                                showEditTranscriptDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Regenerate") },
                            onClick = {
                                showMenu = false
                                showRegenerateDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isRecording -> {
                    RecordingView(
                        transcript = transcript, 
                        settings = settings, 
                        onLocaleChange = onLocaleChange,
                        isListening = isListening,
                        isStreaming = isStreaming,
                        audioLevel = audioLevel,
                        audioFilePath = audioFilePath,
                        errorMessage = errorMessage,
                        onStopRecording = onStopRecording
                    )
                }
                isStreaming -> {
                    StreamingView(streamedText = streamedText)
                }
                else -> {
                    record?.let {
                        SummaryView(record = it, settings = settings)
                    }
                }
            }
            
            // Stop button at the bottom like Records screen
            if (isRecording) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.error,
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
                            "Stop",
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
    
    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text("Alert") },
            text = { Text("Regenerate summary from the transcript. Existing content will be overwritten.") },
            confirmButton = {
                TextButton(onClick = {
                    showRegenerateDialog = false
                    record?.let { onSendToAI(it.transcript) }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text("No")
                }
            }
        )
    }
    
    if (showEditTranscriptDialog) {
        EditTranscriptDialog(
            record = record,
            onDismiss = { showEditTranscriptDialog = false },
            onSave = { editedTranscript ->
                showEditTranscriptDialog = false
                onEditTranscript(editedTranscript)
            }
        )
    }
}

@Composable
fun RecordingView(
    transcript: String,
    settings: Settings,
    onLocaleChange: (RecognizerLocale) -> Unit,
    isListening: Boolean = false,
    isStreaming: Boolean = false,
    audioLevel: Float = -60f,
    audioFilePath: String? = null,
    errorMessage: String? = null,
    onStopRecording: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        errorMessage != null -> "Error: $errorMessage"
                        isStreaming -> "Receiving..."
                        isListening -> "Listening..."
                        audioFilePath != null -> "Recording audio..."
                        else -> "Starting..."
                    }, 
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(settings.selectedLocale.displayName)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    RecognizerLocale.getAvailable().forEach { locale ->
                        DropdownMenuItem(
                            text = { Text(locale.displayName) },
                            onClick = {
                                expanded = false
                                onLocaleChange(locale)
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        
        // Display recognized text in a scrollable container
        if (transcript.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Recognized Text:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(transcript.split("\n").filter { it.isNotEmpty() }) { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingView(streamedText: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            PulsingDot()
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI is generating summary...", style = MaterialTheme.typography.bodyMedium)
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, false),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Generated Summary:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    item {
                        SelectionContainer {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = streamedText.ifEmpty { "Waiting for AI response..." },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                // Show typing cursor when streaming
                                if (streamedText.isNotEmpty()) {
                                    Text(
                                        text = "|",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryView(record: AudioRecord, settings: Settings = AppConstants.DEFAULT_SETTINGS) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(
                text = dateFormat.format(Date(record.recordDate)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Show transcript first
        item {
            Text(
                text = "Transcript:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SelectionContainer {
                OutlinedTextField(
                    value = record.transcript,
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    minLines = 3,
                    maxLines = Int.MAX_VALUE
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Show summary
        item {
            Text(
                text = "Summary:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val summaryText = record.summary[record.locale] ?: "No summary available. Try to regenerate summary."
            SelectionContainer {
                OutlinedTextField(
                    value = summaryText,
                    onValueChange = { },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    minLines = 10,
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

@Composable
fun EditTranscriptDialog(
    record: AudioRecord?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val originalTranscript = record?.transcript ?: ""
    var editedTranscript by remember { mutableStateOf(originalTranscript) }
    val hasChanges = editedTranscript != originalTranscript
    
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            OutlinedTextField(
                value = editedTranscript,
                onValueChange = { editedTranscript = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                maxLines = 15,
                label = { Text("Original Transcript") },
                placeholder = { Text("Enter the corrected transcript...") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(editedTranscript) },
                enabled = hasChanges && editedTranscript.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize()
        ) {}
    }
}


