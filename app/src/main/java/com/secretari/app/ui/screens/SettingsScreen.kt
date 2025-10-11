package com.secretari.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secretari.app.data.model.PromptType
import com.secretari.app.data.model.RecognizerLocale
import com.secretari.app.data.model.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onBack: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }
    var showLocaleDialog by remember { mutableStateOf(false) }
    var showPromptTypeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSection(title = "Recognition") {
                    SettingsItem(
                        title = "Language",
                        value = currentSettings.selectedLocale.displayName,
                        onClick = { showLocaleDialog = true }
                    )
                }
            }
            
            item {
                SettingsSection(title = "AI Settings") {
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
                SettingsSection(title = "About") {
                    SettingsItem(
                        title = "Version",
                        value = "1.0.0",
                        onClick = { }
                    )
                }
            }
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
                    PromptType.values().forEach { type ->
                        TextButton(
                            onClick = {
                                currentSettings = currentSettings.copy(promptType = type)
                                onSettingsChange(currentSettings)
                                showPromptTypeDialog = false
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

