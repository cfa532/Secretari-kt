package com.secretari.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.secretari.app.data.model.AudioRecord
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
    onRegenerate: () -> Unit,
    onLocaleChange: (RecognizerLocale) -> Unit,
    isListening: Boolean = false,
    audioLevel: Float = -60f,
    audioFilePath: String? = null,
    errorMessage: String? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
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
        floatingActionButton = {
            if (isRecording) {
                FloatingActionButton(
                    onClick = onStopRecording,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Stop, "Stop")
                }
            }
        }
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
                        audioLevel = audioLevel,
                        audioFilePath = audioFilePath,
                        errorMessage = errorMessage
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
}

@Composable
fun RecordingView(
    transcript: String,
    settings: Settings,
    onLocaleChange: (RecognizerLocale) -> Unit,
    isListening: Boolean = false,
    audioLevel: Float = -60f,
    audioFilePath: String? = null,
    errorMessage: String? = null
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
        
        // Debug information
        Text(
            text = "Debug: Listening=$isListening, AudioLevel=${audioLevel.toInt()}dB, FilePath=${audioFilePath?.takeLast(20) ?: "null"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn {
            items(transcript.split("\n")) { line ->
                if (line.isNotEmpty()) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
        
        // Show audio level indicator
        if (audioLevel > -60f) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Audio Level: ${audioLevel.toInt()}dB", style = MaterialTheme.typography.bodySmall)
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
            Text("Streaming from AI", style = MaterialTheme.typography.bodyMedium)
        }
        
        LazyColumn {
            item {
                Text(
                    text = streamedText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun SummaryView(record: AudioRecord, settings: Settings) {
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
        
        item {
            val summaryText = record.summary[record.locale] ?: "No summary. Try to regenerate summary"
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

