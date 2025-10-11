package com.secretari.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secretari.app.data.database.AppDatabase
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.Settings
import com.secretari.app.data.model.User
import com.secretari.app.data.network.WebSocketClient
import com.secretari.app.data.repository.AudioRecordRepository
import com.secretari.app.service.SpeechRecognitionService
import com.secretari.app.util.SettingsManager
import com.secretari.app.util.UserManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = AudioRecordRepository(database.audioRecordDao())
    private val settingsManager = SettingsManager.getInstance(application)
    private val userManager = UserManager.getInstance(application)
    private val speechRecognitionService = SpeechRecognitionService(application)
    private val webSocketClient = WebSocketClient()
    
    val allRecords: Flow<List<AudioRecord>> = repository.allRecords
    val settings: Flow<Settings> = settingsManager.settingsFlow
    val currentUser: Flow<User?> = userManager.currentUserFlow
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _streamedText = MutableStateFlow("")
    val streamedText: StateFlow<String> = _streamedText.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _loginStatus = MutableStateFlow(UserManager.LoginStatus.SIGNED_OUT)
    val loginStatus: StateFlow<UserManager.LoginStatus> = _loginStatus.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            val token = userManager.userToken
            if (token != null) {
                _loginStatus.value = UserManager.LoginStatus.SIGNED_IN
            } else {
                currentUser.firstOrNull()?.let { user ->
                    _loginStatus.value = if (user.username.length > 20) {
                        UserManager.LoginStatus.UNREGISTERED
                    } else {
                        UserManager.LoginStatus.SIGNED_OUT
                    }
                }
            }
        }
    }
    
    fun startRecording(locale: String) {
        if (_isRecording.value) return
        
        viewModelScope.launch {
            _isRecording.value = true
            _transcript.value = ""
            
            speechRecognitionService.startRecognition(locale).collect { result ->
                when (result) {
                    is SpeechRecognitionService.RecognitionResult.Success -> {
                        if (_transcript.value.isEmpty()) {
                            _transcript.value = result.text
                        } else {
                            _transcript.value += " ${result.text}"
                        }
                    }
                    is SpeechRecognitionService.RecognitionResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is SpeechRecognitionService.RecognitionResult.Ready -> {
                        // Recording is ready
                    }
                }
            }
        }
    }
    
    fun stopRecording() {
        _isRecording.value = false
        speechRecognitionService.stopRecognition()
    }
    
    fun sendToAI(rawText: String, prompt: String = "") {
        viewModelScope.launch {
            val currentSettings = settings.firstOrNull() ?: return@launch
            val token = userManager.userToken ?: return@launch
            
            _isStreaming.value = true
            _streamedText.value = ""
            
            val defaultPrompt = currentSettings.prompt[currentSettings.promptType]
                ?.get(currentSettings.selectedLocale) ?: ""
            
            webSocketClient.connect(token) { message ->
                when (message) {
                    is WebSocketClient.WebSocketMessage.StreamData -> {
                        _streamedText.value += message.data
                    }
                    is WebSocketClient.WebSocketMessage.Result -> {
                        // Handle final result
                        val record = AudioRecord(
                            transcript = rawText,
                            locale = currentSettings.selectedLocale
                        )
                        record.resultFromAI(AudioRecord.TaskType.SUMMARIZE, message.answer, currentSettings)
                        
                        viewModelScope.launch {
                            repository.insert(record)
                        }
                        
                        if (message.eof) {
                            _isStreaming.value = false
                            webSocketClient.disconnect()
                        }
                    }
                    is WebSocketClient.WebSocketMessage.Error -> {
                        _errorMessage.value = message.message
                        _isStreaming.value = false
                        webSocketClient.disconnect()
                    }
                }
            }.launchIn(viewModelScope)
            
            // Send the message
            webSocketClient.sendMessage(
                rawText = rawText,
                prompt = prompt,
                defaultPrompt = defaultPrompt,
                promptType = currentSettings.promptType.value,
                hasPro = false, // This would need to be checked from entitlement manager
                llmParams = currentSettings.llmParams
            )
        }
    }
    
    fun insertRecord(record: AudioRecord) {
        viewModelScope.launch {
            repository.insert(record)
        }
    }
    
    fun updateRecord(record: AudioRecord) {
        viewModelScope.launch {
            repository.update(record)
        }
    }
    
    fun deleteRecord(record: AudioRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }
    
    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsManager.updateSettings(settings)
        }
    }
    
    fun login(username: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = userManager.login(username, password)
            if (success) {
                _loginStatus.value = UserManager.LoginStatus.SIGNED_IN
            }
            onResult(success)
        }
    }
    
    fun register(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = userManager.register(user)
            onResult(success)
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        speechRecognitionService.stopRecognition()
        webSocketClient.disconnect()
    }
}

