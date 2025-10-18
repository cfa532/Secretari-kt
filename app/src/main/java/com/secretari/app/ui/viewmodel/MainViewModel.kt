package com.secretari.app.ui.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secretari.app.data.database.AppDatabase
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.Settings
import com.secretari.app.data.model.User
import com.secretari.app.data.network.WebSocketClient
import com.secretari.app.data.repository.AudioRecordRepository
import com.secretari.app.service.SpeechRecognitionService
import com.secretari.app.service.UniversalAudioRecorder
import com.secretari.app.service.RealtimeSpeechRecognition
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
    private val universalAudioRecorder = UniversalAudioRecorder(application)
    private val realtimeSpeechRecognition = RealtimeSpeechRecognition(application)
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
    
    private val _audioFilePath = MutableStateFlow<String?>(null)
    val audioFilePath: StateFlow<String?> = _audioFilePath.asStateFlow()
    
    private val _audioLevel = MutableStateFlow(-60f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
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
            try {
                _isRecording.value = true
                _transcript.value = ""
                _isListening.value = false
                _audioFilePath.value = null
                _audioLevel.value = -60f
                _errorMessage.value = null
                
                Log.d("MainViewModel", "Starting recording with locale: $locale")
                Log.d("MainViewModel", "State: isRecording=${_isRecording.value}, isListening=${_isListening.value}, audioFilePath=${_audioFilePath.value}")
                
                         Log.d("MainViewModel", "About to call startRealtimeRecognition")
                         val recognitionFlow = try {
                             realtimeSpeechRecognition.startRealtimeRecognition(locale)
                         } catch (e: Exception) {
                             Log.e("MainViewModel", "Exception creating recognition flow: ${e.message}", e)
                             _errorMessage.value = "Failed to create recognition flow: ${e.message}"
                             startAudioRecordingFallback()
                             return@launch
                         }
                         Log.d("MainViewModel", "Got recognition flow, starting collection")
                         Log.d("MainViewModel", "Flow created successfully, about to collect")
                
                // Use withTimeout to ensure fallback if speech recognition hangs
                try {
                    kotlinx.coroutines.withTimeout(5000) { // 5 second timeout
                        Log.d("MainViewModel", "Starting flow collection with timeout...")
                        recognitionFlow.collect { result ->
                            Log.d("MainViewModel", "Received result: $result")
                            when (result) {
                                is RealtimeSpeechRecognition.RecognitionResult.Ready -> {
                                    Log.d("MainViewModel", "Speech recognition ready")
                                }
                                is RealtimeSpeechRecognition.RecognitionResult.Listening -> {
                                    Log.d("MainViewModel", "Speech recognition listening")
                                    _isListening.value = true
                                    Log.d("MainViewModel", "State after listening: isListening=${_isListening.value}")
                                }
                                is RealtimeSpeechRecognition.RecognitionResult.PartialText -> {
                                    Log.d("MainViewModel", "Partial text: ${result.text}")
                                    _transcript.value = result.text
                                }
                                is RealtimeSpeechRecognition.RecognitionResult.FinalText -> {
                                    Log.d("MainViewModel", "Final text: ${result.text}")
                                    _transcript.value = result.text
                                    _isListening.value = false
                                }
                                        is RealtimeSpeechRecognition.RecognitionResult.Error -> {
                                            Log.e("MainViewModel", "Speech recognition error: ${result.message}")
                                            _errorMessage.value = result.message
                                            _isListening.value = false
                                            Log.d("MainViewModel", "State after error: isListening=${_isListening.value}, errorMessage=${_errorMessage.value}")
                                            
                                            // Show toast message to user
                                            Toast.makeText(getApplication<Application>(), "Speech recognition failed: ${result.message}", Toast.LENGTH_LONG).show()
                                            
                                            // Always fall back to audio recording when speech recognition fails
                                            startAudioRecordingFallback()
                                        }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.w("MainViewModel", "Speech recognition timed out, starting fallback")
                    Toast.makeText(getApplication<Application>(), "Speech recognition timed out, falling back to audio recording", Toast.LENGTH_LONG).show()
                    startAudioRecordingFallback()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error collecting speech recognition flow: ${e.message}")
                    _errorMessage.value = "Flow collection error: ${e.message}"
                    startAudioRecordingFallback()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in startRecording: ${e.message}")
                _errorMessage.value = "Failed to start recording: ${e.message}"
                _isRecording.value = false
            }
        }
    }
    
    private fun startAudioRecordingFallback() {
        viewModelScope.launch {
            Log.d("MainViewModel", "Starting audio recording fallback")
            _audioFilePath.value = null
            _audioLevel.value = -60f
            
            universalAudioRecorder.startRecording().collect { result ->
                when (result) {
                    is UniversalAudioRecorder.RecordingResult.Started -> {
                        Log.d("MainViewModel", "Audio recording started successfully")
                        _isListening.value = true  // Set listening to true for audio recording
                        Log.d("MainViewModel", "Fallback state: isRecording=${_isRecording.value}, isListening=${_isListening.value}, audioFilePath=${_audioFilePath.value}")
                    }
                             is UniversalAudioRecorder.RecordingResult.AudioLevel -> {
                                 _audioLevel.value = result.level
                             }
                    is UniversalAudioRecorder.RecordingResult.Stopped -> {
                        Log.d("MainViewModel", "Audio recording stopped, file: ${result.filePath}")
                        _audioFilePath.value = result.filePath
                        _isRecording.value = false
                        Log.d("MainViewModel", "Final state: audioFilePath=${_audioFilePath.value}, isRecording=${_isRecording.value}")
                    }
                    is UniversalAudioRecorder.RecordingResult.Error -> {
                        Log.e("MainViewModel", "Audio recording error: ${result.message}")
                        _errorMessage.value = result.message
                        _isRecording.value = false
                    }
                }
            }
        }
    }
    
    fun stopRecording() {
        _isRecording.value = false
        _isListening.value = false
        realtimeSpeechRecognition.stopRecognition()
        val filePath = universalAudioRecorder.stopRecording()
        if (filePath != null) {
            _audioFilePath.value = filePath
        }
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
        universalAudioRecorder.stopRecording()
        realtimeSpeechRecognition.stopRecognition()
        webSocketClient.disconnect()
    }
}

