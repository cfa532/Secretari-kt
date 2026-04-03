package com.secretari.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretari.app.R
import com.secretari.app.data.model.AppConstants
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.PromptType
import com.secretari.app.data.model.RecognizerLocale
import com.secretari.app.data.model.Settings
import com.secretari.app.ui.viewmodel.MainViewModel
import kotlinx.serialization.InternalSerializationApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun DetailScreen(
    isRecording: Boolean,
    isStreaming: Boolean,
    transcript: String,
    streamedText: String,
    record: AudioRecord?,
    viewModel: MainViewModel = viewModel(),
    onStopRecording: () -> Unit,
    onSendToAI: (String) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onTranslate: () -> Unit,
    onRegenerate: () -> Unit = {},
    onEditTranscript: (String) -> Unit = {},
    onEditSummary: (String) -> Unit = {},
    onToggleChecklistItem: (Int) -> Unit = {},
    onEditChecklistItem: (Int, String) -> Unit = { _, _ -> },
    onAddChecklistItem: () -> Unit = {},
    onRemoveChecklistItem: (Int) -> Unit = {},
    onLocaleChange: (RecognizerLocale) -> Unit,
    onDisplayLocaleChange: (RecognizerLocale) -> Unit = {},
    isListening: Boolean = false,
    audioLevel: Float = -60f,
    audioFilePath: String? = null,
    errorMessage: String? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showEditTranscriptDialog by remember { mutableStateOf(false) }
    var showLocalePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val settings by viewModel.settings.collectAsState(initial = com.secretari.app.data.model.AppConstants.DEFAULT_SETTINGS)

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

    LaunchedEffect(record, settings.promptType) {
        record?.let { currentRecord ->
            val hasSummary = when (settings.promptType) {
                PromptType.CHECKLIST -> currentRecord.memo.isNotEmpty()
                else -> currentRecord.summary[currentRecord.locale]?.isNotEmpty() == true
            }
            if (currentRecord.transcript.isNotEmpty() && !hasSummary && !isRecording && !isStreaming) {
                onSendToAI(currentRecord.transcript)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.records)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isRecording
                    ) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
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
                            text = { Text(stringResource(R.string.share)) },
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.translate)) },
                            onClick = {
                                showMenu = false
                                onTranslate()
                            },
                            leadingIcon = { Icon(Icons.Default.Translate, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.original_transcript)) },
                            onClick = {
                                showMenu = false
                                showEditTranscriptDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.regenerate)) },
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
                        val availableLocales = if (settings.promptType == PromptType.CHECKLIST && it.memo.isNotEmpty()) {
                            it.memo.firstOrNull()?.title?.keys?.sorted()?.toList() ?: emptyList()
                        } else if (it.summary.isNotEmpty()) {
                            it.summary.keys.sorted().toList()
                        } else {
                            emptyList()
                        }
                        if (availableLocales.size >= 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box {
                                    TextButton(onClick = { showLocalePicker = true }) {
                                        Text(it.locale.name)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = showLocalePicker,
                                        onDismissRequest = { showLocalePicker = false }
                                    ) {
                                        availableLocales.forEach { locale ->
                                            DropdownMenuItem(
                                                text = { Text(locale.name) },
                                                onClick = {
                                                    showLocalePicker = false
                                                    onDisplayLocaleChange(locale)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        key(it.locale) {
                            if (settings.promptType == PromptType.CHECKLIST) {
                                ChecklistView(
                                    record = it,
                                    onToggleItem = { itemId -> onToggleChecklistItem(itemId) },
                                    onEditItem = { itemId, newText -> onEditChecklistItem(itemId, newText) },
                                    onAddItem = { onAddChecklistItem() },
                                    onRemoveItem = { itemId -> onRemoveChecklistItem(itemId) }
                                )
                            } else {
                                SummaryView(record = it, settings = settings, onEditSummary = onEditSummary)
                            }
                        }
                    }
                }
            }

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
                            stringResource(R.string.stop),
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

    if (!isRecording && !isStreaming && record == null && errorMessage != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.recording_failed)) },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearError()
                    onBack()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text(stringResource(R.string.alert)) },
            text = { Text(stringResource(R.string.regenerate_summary_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showRegenerateDialog = false
                    record?.let { onSendToAI(it.transcript) }
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) {
                    Text(stringResource(R.string.no))
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

@OptIn(InternalSerializationApi::class)
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
    val receivingStr = stringResource(R.string.receiving)
    val listeningStr = stringResource(R.string.listening)
    val recordingAudioStr = stringResource(R.string.recording_audio)
    val startingStr = stringResource(R.string.starting)
    val errorPrefixStr = stringResource(R.string.error_prefix)

    val statusText = when {
        errorMessage != null -> String.format(errorPrefixStr, errorMessage)
        isStreaming -> receivingStr
        isListening -> listeningStr
        audioFilePath != null -> recordingAudioStr
        else -> startingStr
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot()
                Spacer(modifier = Modifier.width(8.dp))
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
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
                        text = stringResource(R.string.recognized_text),
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
    val waitingForAiStr = stringResource(R.string.waiting_for_ai)

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            PulsingDot()
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_generating_summary), style = MaterialTheme.typography.bodyMedium)
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
                    text = stringResource(R.string.generated_summary),
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
                                    text = streamedText.ifEmpty { waitingForAiStr },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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

@OptIn(InternalSerializationApi::class)
@Composable
fun ChecklistView(
    record: AudioRecord,
    onToggleItem: (Int) -> Unit = {},
    onEditItem: (Int, String) -> Unit = { _, _ -> },
    onAddItem: () -> Unit = {},
    onRemoveItem: (Int) -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    var editingItemId by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(
                text = dateFormat.format(Date(record.recordDate)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(record.memo.size) { index ->
            val memoItem = record.memo[index]
            val title = memoItem.title[record.locale] ?: "Unknown item"
            val isEditing = editingItemId == memoItem.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (memoItem.isChecked)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = memoItem.isChecked,
                        onCheckedChange = { onToggleItem(memoItem.id) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        onEditItem(memoItem.id, editingText)
                                        editingItemId = null
                                        editingText = ""
                                    }) {
                                        Icon(Icons.Default.Check, stringResource(R.string.save))
                                    }
                                    IconButton(onClick = {
                                        editingItemId = null
                                        editingText = ""
                                    }) {
                                        Icon(Icons.Default.Close, stringResource(R.string.cancel))
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "• $title",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editingItemId = memoItem.id
                                    editingText = title
                                },
                            color = if (memoItem.isChecked)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!isEditing) {
                        IconButton(onClick = { onRemoveItem(memoItem.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(R.string.delete_failure),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_new_item),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.add_new_item),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAddItem() },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun SummaryView(record: AudioRecord, settings: Settings = AppConstants.DEFAULT_SETTINGS, onEditSummary: (String) -> Unit = {}) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val noSummaryStr = stringResource(R.string.no_summary)
    val summaryText = record.summary[record.locale] ?: noSummaryStr
    var editedSummary by remember(record.locale) { mutableStateOf(summaryText) }
    val hasChanges = editedSummary != summaryText

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
            OutlinedTextField(
                value = editedSummary,
                onValueChange = { editedSummary = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
                maxLines = Int.MAX_VALUE,
                label = { Text(stringResource(R.string.summary)) },
                placeholder = { Text(stringResource(R.string.enter_summary)) },
                trailingIcon = {
                    if (hasChanges) {
                        IconButton(onClick = { onEditSummary(editedSummary) }) {
                            Icon(Icons.Default.Save, stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
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
                label = { Text(stringResource(R.string.original_transcript)) },
                placeholder = { Text(stringResource(R.string.enter_transcript)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(editedTranscript) },
                enabled = hasChanges && editedTranscript.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
