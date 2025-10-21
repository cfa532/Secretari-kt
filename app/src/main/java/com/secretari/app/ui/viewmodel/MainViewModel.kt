package com.secretari.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secretari.app.data.database.AppDatabase
import com.secretari.app.data.model.AppConstants
import com.secretari.app.data.model.AudioRecord
import com.secretari.app.data.model.Settings
import com.secretari.app.data.model.User
import com.secretari.app.data.network.ServerStatusResponse
import com.secretari.app.data.network.WebSocketClient
import com.secretari.app.data.repository.AudioRecordRepository
import com.secretari.app.service.RealtimeSpeechRecognition
import com.secretari.app.service.SpeechRecognitionService
import com.secretari.app.service.UniversalAudioRecorder
import com.secretari.app.util.SettingsManager
import com.secretari.app.util.UserManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

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
    @OptIn(InternalSerializationApi::class)
    val settings: Flow<Settings> = settingsManager.settingsFlow
    @OptIn(InternalSerializationApi::class)
    val currentUser: Flow<User?> = userManager.currentUserFlow
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _streamedText = MutableStateFlow("")
    val streamedText: StateFlow<String> = _streamedText.asStateFlow()
    
    private val _shouldNavigateBack = MutableStateFlow(false)
    val shouldNavigateBack: StateFlow<Boolean> = _shouldNavigateBack.asStateFlow()
    
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
    
    private val _currentRecord = MutableStateFlow<AudioRecord?>(null)
    val currentRecord: StateFlow<AudioRecord?> = _currentRecord.asStateFlow()
    
    init {
        initializeUserAccount()
    }
    
    @OptIn(InternalSerializationApi::class)
    private fun initializeUserAccount() {
        viewModelScope.launch {
            try {
                // Initialize anonymous account on first launch
                val success = userManager.initializeAnonymousAccount()
                if (success) {
                    checkLoginStatus()
                } else {
                    _errorMessage.value = "Failed to initialize user account"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Account initialization error: ${e.message}"
            }
        }
    }
    
    @OptIn(InternalSerializationApi::class)
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
    
    @OptIn(InternalSerializationApi::class)
    fun startRecording(locale: String) {
        if (_isRecording.value) return

        viewModelScope.launch {
            try {
                _isRecording.value = true
                _isListening.value = false
                _audioFilePath.value = null
                _audioLevel.value = -60f
                _errorMessage.value = null
                _currentRecord.value = null // Clear current record when starting new recording
                
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
                       kotlinx.coroutines.withTimeout(1800000) { // 30 minute timeout
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
                                    _transcript.value += result.text + " "
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
                    Log.w("MainViewModel", "Speech recognition timed out after 30 minutes, creating record")
                    Toast.makeText(getApplication<Application>(), "Recording timed out after 30 minutes, saving transcript", Toast.LENGTH_LONG).show()
                    
                    // Stop recording and create a record with current transcript
                    _isRecording.value = false
                    _isListening.value = false
                    realtimeSpeechRecognition.stopRecognition()
                    val filePath = universalAudioRecorder.stopRecording()
                    if (filePath != null) {
                        _audioFilePath.value = filePath
                    }
                    
                    // Create record with current transcript and send to AI for summary
                    val currentTranscript = _transcript.value
                    if (currentTranscript.isNotEmpty()) {
                        val currentSettings = settings.firstOrNull() ?: AppConstants.DEFAULT_SETTINGS
                        val record = AudioRecord(
                            transcript = currentTranscript,
                            locale = currentSettings.selectedLocale
                        )
                        sendToAI(currentTranscript, record)
                    } else {
                        Log.w("MainViewModel", "No transcript available after timeout")
                        _errorMessage.value = "No speech was recognized during the recording session"
                    }
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
    
    @OptIn(InternalSerializationApi::class)
    fun stopRecording() {
        Log.d("MainViewModel", "Stopping recording...")
        _isRecording.value = false
        _isListening.value = false
        realtimeSpeechRecognition.stopRecognition()
        val filePath = universalAudioRecorder.stopRecording()
        if (filePath != null) {
            _audioFilePath.value = filePath
            Log.d("MainViewModel", "Audio file saved: $filePath")
        }
        
        // Get current transcript without clearing it
        val currentTranscript = _transcript.value
        
        Log.d("MainViewModel", "Current transcript length: ${currentTranscript.length}")
        
        // Only create AudioRecord if there's a transcript (following iOS logic)
        if (currentTranscript.isNotEmpty()) {
            viewModelScope.launch {
                val currentSettings = settings.firstOrNull() ?: AppConstants.DEFAULT_SETTINGS
                val record = AudioRecord(
                    transcript = currentTranscript,
                    locale = currentSettings.selectedLocale
                )
                
                // Save the record immediately to database
                repository.insert(record)
                Log.d("MainViewModel", "AudioRecord saved immediately with transcript: '$currentTranscript'")
                
                // Generate summary in the same language as the transcript
                Log.d("MainViewModel", "Sending transcript to AI: '${currentTranscript.take(50)}...'")
                sendToAI(currentTranscript, record, "") // Empty prompt = use default prompt for transcript language
            }
        } else {
            Log.w("MainViewModel", "No transcript available when stopping recording")
            _errorMessage.value = "No speech was recognized during the recording session"
            // Don't create AudioRecord if there's no transcript
        }
    }
    
    @OptIn(InternalSerializationApi::class)
    fun sendToAI(rawText: String, record: AudioRecord, prompt: String = "") {
        viewModelScope.launch {
            val currentSettings = settings.firstOrNull() ?: return@launch
            var token = userManager.userToken
            
            // If no token, create a temporary user first
            if (token == null) {
                Log.d("MainViewModel", "No user token found, creating temporary user...")
                val tempUser = userManager.createTempUser()
                if (tempUser != null) {
                    token = userManager.userToken
                    Log.d("MainViewModel", "Temporary user created, token: ${token?.take(10)}...")
                } else {
                    Log.e("MainViewModel", "Failed to create temporary user")
                    _errorMessage.value = "Failed to authenticate with server"
                    return@launch
                }
            }
            
            _isStreaming.value = true
            _streamedText.value = ""
            _shouldNavigateBack.value = false
            
            Log.d("MainViewModel", "Starting AI processing for transcript: '$rawText'")
            
            val defaultPrompt = currentSettings.prompt[currentSettings.promptType]
                ?.get(currentSettings.selectedLocale) ?: ""
            
            webSocketClient.connect(token!!) { message ->
                when (message) {
                    is WebSocketClient.WebSocketMessage.StreamData -> {
                        // Accumulate streaming chunks word by word
                        _streamedText.value += message.data
                        Log.d("MainViewModel", "Received stream chunk: '${message.data}' | Total: '${_streamedText.value}'")
                    }
                    is WebSocketClient.WebSocketMessage.Result -> {
                        Log.d("MainViewModel", "Received result: eof=${message.eof}, answer=${message.answer}")
                        
                        // Only create and save AudioRecord when eof=true (final chunk)
                        if (message.eof) {
                            // Use only the answer from result message as the final summary
                            val finalSummary = message.answer
                            
                            // Update the existing record with AI result
                            record.resultFromAI(AudioRecord.TaskType.SUMMARIZE, finalSummary, currentSettings)
                            
                            viewModelScope.launch {
                                repository.update(record)
                                Log.d("MainViewModel", "AudioRecord updated with AI summary: $finalSummary")
                            }
                            
                            // Clear streaming and show final result
                            _isStreaming.value = false
                            _streamedText.value = ""
                            
                            // Set the final record to show the result
                            _currentRecord.value = record
                            
                            webSocketClient.disconnect()
                        } else {
                            // For non-final chunks, accumulate the answer (this shouldn't happen in current backend)
                            _streamedText.value += message.answer
                            Log.d("MainViewModel", "Received non-final result chunk: ${message.answer}")
                        }
                    }
                    is WebSocketClient.WebSocketMessage.Error -> {
                        Log.e("MainViewModel", "WebSocket error: ${message.message}")
                        _errorMessage.value = message.message
                        _isStreaming.value = false
                        _shouldNavigateBack.value = true
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
    
    fun selectRecord(record: AudioRecord) {
        _currentRecord.value = record
        // Clear recording states when viewing an existing record (only if we're not currently recording)
        if (!_isRecording.value) {
            _isStreaming.value = false
            _isListening.value = false
            _streamedText.value = ""
            _errorMessage.value = null
        }
    }
    
    @OptIn(InternalSerializationApi::class)
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
    
    @OptIn(InternalSerializationApi::class)
    fun register(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = userManager.register(user)
            onResult(success)
        }
    }
    
    fun redeemCoupon(coupon: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = userManager.redeemCoupon(coupon)
            onResult(success)
        }
    }
    
    fun getServerStatus(onResult: (ServerStatusResponse?) -> Unit) {
        viewModelScope.launch {
            val status = userManager.getServerStatus()
            onResult(status)
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun resetNavigationFlag() {
        _shouldNavigateBack.value = false
    }
    
    @OptIn(InternalSerializationApi::class)
    suspend fun checkRegistrationRequirement(): Boolean {
        return try {
            userManager.needsRegistration()
        } catch (e: Exception) {
            false
        }
    }
    
    @OptIn(InternalSerializationApi::class)
    fun shareRecord(record: AudioRecord?) {
        if (record == null) return
        
        viewModelScope.launch {
            val settings = settingsManager.getSettings()
            val dateFormat = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
            val dateString = dateFormat.format(java.util.Date(record.recordDate))
            
            var textToShare = "$dateString:\n"
            
            // Get the summary text for the current locale
            val summaryText = record.summary[record.locale] ?: "No summary available"
            textToShare += summaryText
            
            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textToShare)
            }
            
            // Start the share activity
            val context = getApplication<Application>()
            val chooserIntent = Intent.createChooser(shareIntent, "Share Summary")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }
    
    @OptIn(InternalSerializationApi::class)
    fun translateRecord(record: AudioRecord?, targetLocale: com.secretari.app.data.model.RecognizerLocale) {
        if (record == null) return
        
        viewModelScope.launch {
            val currentSettings = settingsManager.getSettings()
            val sourceText = record.summary[record.locale] ?: record.transcript
            
            if (sourceText.isEmpty()) {
                _errorMessage.value = "No content to translate"
                return@launch
            }
            
            // Create translation prompt based on target locale
            val translationPrompt = when (targetLocale) {
                com.secretari.app.data.model.RecognizerLocale.ENGLISH -> "Translate the following text into English. Export with plain text."
                com.secretari.app.data.model.RecognizerLocale.CHINESE -> "將下面的文字翻譯成繁體中文。以純文字導出。"
                com.secretari.app.data.model.RecognizerLocale.INDONESIAN -> "Terjemahkan teks berikut ke dalam bahasa Indonesia. Ekspor dengan teks biasa."
                com.secretari.app.data.model.RecognizerLocale.JAPANESE -> "次のテキストを日本語に翻訳し、プレーンテキストでエクスポートします。"
                com.secretari.app.data.model.RecognizerLocale.VIETNAMESE -> "Dịch đoạn văn sau sang tiếng Việt. Xuất với văn bản thuần túy."
                com.secretari.app.data.model.RecognizerLocale.FILIPINO -> "Isalin sa Filipino ang sumusunod na teksto. I-export gamit ang plain text."
                com.secretari.app.data.model.RecognizerLocale.THAI -> "แปลข้อความต่อไปนี้เป็นภาษาไทย ส่งออกด้วยข้อความธรรมดา"
                com.secretari.app.data.model.RecognizerLocale.SPANISH -> "Traduce el siguiente texto al español. Exporta con texto plano."
                com.secretari.app.data.model.RecognizerLocale.KOREAN -> "다음 텍스트를 한국어로 번역하세요. 일반 텍스트로 내보내세요."
            }
            
            // Create a new record for translation
            val translationRecord = AudioRecord(
                transcript = sourceText,
                locale = targetLocale
            )
            
            // Send to AI for translation
            sendToAI(sourceText, translationRecord, translationPrompt)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechRecognitionService.stopRecognition()
        universalAudioRecorder.stopRecording()
        realtimeSpeechRecognition.stopRecognition()
        webSocketClient.disconnect()
    }
}

