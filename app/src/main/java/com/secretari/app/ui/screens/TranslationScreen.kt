package com.secretari.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.RecognizerLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    record: AudioRecord?,
    isStreaming: Boolean,
    streamedText: String,
    onBack: () -> Unit,
    onTranslate: (RecognizerLocale) -> Unit,
    onShare: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, "Share")
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
            if (isStreaming) {
                // Show streaming view
                StreamingTranslationView(streamedText = streamedText)
            } else {
                // Show language selection
                LanguageSelectionView(
                    record = record,
                    onTranslate = onTranslate
                )
            }
        }
    }
}

@Composable
fun StreamingTranslationView(streamedText: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            PulsingDot()
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI is translating...", style = MaterialTheme.typography.bodyMedium)
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
                    text = "Translation:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    item {
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

@Composable
fun LanguageSelectionView(
    record: AudioRecord?,
    onTranslate: (RecognizerLocale) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Select one of the following languages to translate the Summary. If summary exists, it will be overwritten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Language buttons
        val languages = listOf(
            RecognizerLocale.ENGLISH to "English",
            RecognizerLocale.CHINESE to "中文",
            RecognizerLocale.INDONESIAN to "Indonesia",
            RecognizerLocale.JAPANESE to "日本語🇯🇵",
            RecognizerLocale.VIETNAMESE to "Việt Nam🇻🇳",
            RecognizerLocale.FILIPINO to "Filipino🇵🇭",
            RecognizerLocale.THAI to "แบบไทย🇹🇭",
            RecognizerLocale.SPANISH to "Español🇪🇸",
            RecognizerLocale.KOREAN to "한국인🇰🇷"
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages.size) { index ->
                val (locale, displayName) = languages[index]
                Button(
                    onClick = { onTranslate(locale) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(displayName)
                }
            }
        }
    }
}

