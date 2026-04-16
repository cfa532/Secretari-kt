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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretari.app.R
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.PromptType
import com.secretari.app.data.model.RecognizerLocale
import com.secretari.app.data.model.Settings
import com.secretari.app.data.model.User
import com.secretari.app.ui.viewmodel.MainViewModel
import com.secretari.app.util.UserManager
import kotlinx.serialization.InternalSerializationApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
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
    val currentUser by viewModel.currentUser.collectAsState(initial = null)

    val accountMenuLabel = stringResource(when (loginStatus) {
        UserManager.LoginStatus.SIGNED_IN -> R.string.account
        UserManager.LoginStatus.SIGNED_OUT -> R.string.login
        UserManager.LoginStatus.UNREGISTERED -> R.string.register
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.records),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings)) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(accountMenuLabel) },
                            onClick = {
                                showMenu = false
                                onNavigateToAccount()
                            },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.purchase)) },
                            onClick = {
                                showMenu = false
                                onNavigateToStore()
                            },
                            leadingIcon = { Icon(Icons.Default.Star, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.help)) },
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
                        Text(
                            stringResource(R.string.no_records),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.push_start_button),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.select_language_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
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
                            stringResource(R.string.start),
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

                currentUser?.let { user ->
                    if (user.username.length > 20 && user.dollarBalance <= 0.1) {
                        BalanceWarningCard(
                            user = user,
                            onRegister = onNavigateToAccount,
                            onRecharge = onNavigateToStore
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
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
                            stringResource(R.string.start),
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

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun RecordListItem(
    record: AudioRecord,
    settings: Settings,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val unknownItemText = stringResource(R.string.unknown_item)
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val deleteButtonWidth = with(density) { 60.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
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
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

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

                    val displayText = when (settings.promptType) {
                        PromptType.CHECKLIST -> {
                            if (record.memo.isNotEmpty()) {
                                record.memo.joinToString(" • ") { item ->
                                    val title = item.title[record.locale] ?: unknownItemText
                                    "${if (item.isChecked) "✓" else "○"} $title"
                                }
                            } else {
                                record.transcript
                            }
                        }
                        else -> {
                            val summaryText = record.summary[record.locale]
                            if (!summaryText.isNullOrEmpty()) summaryText else record.transcript
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

@OptIn(InternalSerializationApi::class)
@Preview(showBackground = true)
@Composable
fun MainScreenWithRecordsPreview() {
    MaterialTheme {
        val sampleRecords = listOf(
            AudioRecord(
                recordDate = System.currentTimeMillis() - 86400000,
                transcript = "This is a sample transcript of a recorded speech. It contains multiple sentences to demonstrate how the list item looks with longer text content.",
                summary = mapOf(RecognizerLocale.ENGLISH to "Sample summary of the recorded speech content.")
            ),
            AudioRecord(
                recordDate = System.currentTimeMillis() - 172800000,
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

@OptIn(InternalSerializationApi::class)
@Composable
fun BalanceWarningCard(
    user: User,
    onRegister: () -> Unit,
    onRecharge: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.low_balance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.balance_amount, user.dollarBalance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.low_balance_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRegister,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.register))
                }
                Button(
                    onClick = onRecharge,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.recharge))
                }
            }
        }
    }
}
