package com.secretari.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.secretari.app.ui.screens.*
import com.secretari.app.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Detail : Screen("detail")
    object Settings : Screen("settings")
    object Account : Screen("account")
    object Store : Screen("store")
    object Help : Screen("help")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    val records by viewModel.allRecords.collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState(initial = com.secretari.app.data.model.AppConstants.DEFAULT_SETTINGS)
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val isRecording by viewModel.isRecording.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamedText by viewModel.streamedText.collectAsState()
    val loginStatus by viewModel.loginStatus.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val audioFilePath by viewModel.audioFilePath.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val shouldNavigateBack by viewModel.shouldNavigateBack.collectAsState()
    val currentRecord by viewModel.currentRecord.collectAsState()
    
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                records = records,
                loginStatus = loginStatus,
                onRecordClick = { _ ->
                    navController.navigate(Screen.Detail.route)
                },
                onDeleteRecord = { record ->
                    viewModel.deleteRecord(record)
                },
                onStartRecording = {
                    viewModel.startRecording(settings.selectedLocale.code)
                    navController.navigate(Screen.Detail.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAccount = {
                    navController.navigate(Screen.Account.route)
                },
                onNavigateToStore = {
                    navController.navigate(Screen.Store.route)
                },
                onNavigateToHelp = {
                    navController.navigate(Screen.Help.route)
                }
            )
        }
        
        composable(Screen.Detail.route) {
            // Handle navigation back to main screen when AI processing is complete
            LaunchedEffect(shouldNavigateBack) {
                if (shouldNavigateBack) {
                    navController.popBackStack()
                    viewModel.resetNavigationFlag()
                }
            }
            
            DetailScreen(
                isRecording = isRecording,
                isStreaming = isStreaming,
                transcript = transcript,
                streamedText = streamedText,
                record = currentRecord, // Pass the current record from AI processing
                settings = settings,
                onStopRecording = {
                    viewModel.stopRecording()
                },
                onSendToAI = { text ->
                    // Create a record for manual AI processing
                    val record = com.secretari.app.data.model.AudioRecord(
                        transcript = text,
                        locale = settings.selectedLocale
                    )
                    viewModel.sendToAI(text, record)
                },
                onBack = {
                    navController.popBackStack()
                },
                onShare = {
                    // Implement share functionality
                },
                onTranslate = {
                    // Implement translate functionality
                },
                onRegenerate = {
                    // Handled in DetailScreen
                },
                onLocaleChange = { locale ->
                    viewModel.updateSettings(settings.copy(selectedLocale = locale))
                    viewModel.stopRecording()
                    viewModel.startRecording(locale.code)
                },
                isListening = isListening,
                audioLevel = audioLevel,
                audioFilePath = audioFilePath,
                errorMessage = errorMessage
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                settings = settings,
                onSettingsChange = { newSettings ->
                    viewModel.updateSettings(newSettings)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Account.route) {
            AccountScreen(
                user = currentUser,
                loginStatus = loginStatus,
                onLogin = { username, password ->
                    viewModel.login(username, password) { success ->
                        if (success) {
                            navController.popBackStack()
                        }
                    }
                },
                onRegister = { user ->
                    viewModel.register(user) { success ->
                        if (success) {
                            navController.popBackStack()
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Store.route) {
            // Placeholder for Store screen
            PlaceholderScreen(
                title = "Store",
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Help.route) {
            // Placeholder for Help screen
            PlaceholderScreen(
                title = "Help",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("$title - Coming Soon")
        }
    }
}

