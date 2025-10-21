package com.secretari.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secretari.app.data.model.PromptType
import com.secretari.app.data.model.RecognizerLocale
import com.secretari.app.data.model.Settings
import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onBack: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }
    var showLocaleDialog by remember { mutableStateOf(false) }
    var showPromptTypeDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isEditingPrompt by remember { mutableStateOf(false) }
    var editedPromptText by remember { mutableStateOf("") }
    var showEditButton by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

            item {
                SettingsSection(title = "AI Settings") {
                    SettingsItem(
                        title = "Language",
                        value = currentSettings.selectedLocale.displayName,
                        onClick = { showLocaleDialog = true }
                    )
                    SettingsItem(
                        title = "Prompt Type",
                        value = currentSettings.promptType.name,
                        onClick = { showPromptTypeDialog = true }
                    )
                    
                    var audioThreshold by remember { mutableStateOf(currentSettings.audioSilentDB) }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Audio Silence Threshold: $audioThreshold dB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = audioThreshold.toFloat(),
                            onValueChange = { audioThreshold = it.toInt().toString() },
                            valueRange = -60f..-20f,
                            onValueChangeFinished = {
                                currentSettings = currentSettings.copy(audioSilentDB = audioThreshold)
                                onSettingsChange(currentSettings)
                            }
                        )
                    }
                }
            }
            
            item {
                SettingsSection(title = "Advanced") {
                    SettingsItem(
                        title = "Silent timer",
                        value = "30 min",
                        onClick = { }
                    )
                    SettingsItem(
                        title = "Max work time",
                        value = "8 hr",
                        onClick = { }
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (isEditingPrompt) {
                                OutlinedTextField(
                                    value = editedPromptText,
                                    onValueChange = { editedPromptText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    label = { Text("Instruction Text") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            isEditingPrompt = false
                                            editedPromptText = ""
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            // Save the custom prompt
                                            val updatedPrompts = currentSettings.prompt.toMutableMap()
                                            val localePrompts = updatedPrompts[currentSettings.promptType]?.toMutableMap() ?: mutableMapOf()
                                            localePrompts[currentSettings.selectedLocale] = editedPromptText
                                            updatedPrompts[currentSettings.promptType] = localePrompts
                                            
                                            currentSettings = currentSettings.copy(prompt = updatedPrompts)
                                            onSettingsChange(currentSettings)
                                            isEditingPrompt = false
                                            editedPromptText = ""
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                }
                            } else {
                                Text(
                                    text = currentSettings.prompt[currentSettings.promptType]?.get(currentSettings.selectedLocale) ?: "You are an intelligent secretary. Extract the important content from the following text and make a comprehensive summary. Divide it into appropriate sections. The output format should be plain text.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { showEditButton = !showEditButton }
                                )
                                if (showEditButton) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            editedPromptText = currentSettings.prompt[currentSettings.promptType]?.get(currentSettings.selectedLocale) ?: "You are an intelligent secretary. Extract the important content from the following text and make a comprehensive summary. Divide it into appropriate sections. The output format should be plain text."
                                            isEditingPrompt = true
                                            showEditButton = false
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            
            // Version footnote at the bottom
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
    
    if (showLocaleDialog) {
        AlertDialog(
            onDismissRequest = { showLocaleDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    RecognizerLocale.getAvailable().forEach { locale ->
                        TextButton(
                            onClick = {
                                currentSettings = currentSettings.copy(selectedLocale = locale)
                                onSettingsChange(currentSettings)
                                showLocaleDialog = false
                                showEditButton = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(locale.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showPromptTypeDialog) {
        AlertDialog(
            onDismissRequest = { showPromptTypeDialog = false },
            title = { Text("Select Prompt Type") },
            text = {
                Column {
                    PromptType.entries.forEach { type ->
                        TextButton(
                            onClick = {
                                currentSettings = currentSettings.copy(promptType = type)
                                onSettingsChange(currentSettings)
                                showPromptTypeDialog = false
                                showEditButton = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(type.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPromptTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all settings to default values?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentSettings = com.secretari.app.data.model.AppConstants.DEFAULT_SETTINGS
                        onSettingsChange(currentSettings)
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Divider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

