package com.secretari.app.service

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay
import java.util.Locale

class RealtimeSpeechRecognition(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private var currentTranscript = ""
    
    sealed class RecognitionResult {
        data class PartialText(val text: String) : RecognitionResult()
        data class FinalText(val text: String) : RecognitionResult()
        data class Error(val message: String) : RecognitionResult()
        object Ready : RecognitionResult()
        object Listening : RecognitionResult()
    }
    
    fun startRealtimeRecognition(locale: String): Flow<RecognitionResult> = callbackFlow {
        Log.d("RealtimeSpeech", "=== CALLBACK FLOW STARTED ===")
        Log.d("RealtimeSpeech", "startRealtimeRecognition called with locale: $locale")

        if (isRecording) {
            Log.e("RealtimeSpeech", "Already recording")
            trySend(RecognitionResult.Error("Already recording"))
            close()
            return@callbackFlow
        }

        // Check permissions first
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("RealtimeSpeech", "RECORD_AUDIO permission not granted")
            trySend(RecognitionResult.Error("RECORD_AUDIO permission not granted"))
            close()
            return@callbackFlow
        }
        Log.d("RealtimeSpeech", "RECORD_AUDIO permission granted")

        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("RealtimeSpeech", "Speech recognition not available on this device")
            trySend(RecognitionResult.Error("Speech recognition not available on this device"))
            close()
            return@callbackFlow
        }
        Log.d("RealtimeSpeech", "Speech recognition is available on this device")

        Log.d("RealtimeSpeech", "Starting system speech recognition")

        // Try to use system speech recognition
        try {
            Log.d("RealtimeSpeech", "About to call startSystemSpeechRecognition")
            startSystemSpeechRecognition(locale, this@callbackFlow)
            Log.d("RealtimeSpeech", "startSystemSpeechRecognition completed")
        } catch (e: Exception) {
            Log.e("RealtimeSpeech", "System speech recognition failed", e)
            trySend(RecognitionResult.Error("Speech recognition service failed: ${e.message}"))
            close()
            return@callbackFlow
        }
        
        awaitClose {
            Log.d("RealtimeSpeech", "Flow closed, stopping recognition")
            stopRecognition()
        }
    }
    
    private suspend fun startSystemSpeechRecognition(locale: String, channel: kotlinx.coroutines.channels.SendChannel<RecognitionResult>) {
        Log.d("RealtimeSpeech", "Creating SpeechRecognizer")
        
        // List of well-known speech recognition services to try
        val speechServices = listOf(
            // Try Google Assistant's service first since it works
            ComponentName("com.google.android.googlequicksearchbox", "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"),
            
            // Try default system service second
            null, // This will use the default SpeechRecognizer
            
            // Other Google Speech Services
            ComponentName("com.google.android.gms", "com.google.android.gms.speech.serviceapi.GoogleRecognitionService"),
            
            // iFlytek (Chinese speech recognition)
            ComponentName("com.iflytek.speechsuite", "com.iflytek.speechsuite.SpeechService"),
            ComponentName("com.iflytek.speechsuite", "com.iflytek.speechsuite.RecognitionService"),
            
            // Samsung (Samsung devices)
            ComponentName("com.samsung.android.bixby.agent", "com.samsung.android.bixby.agent.speech.SpeechService"),
            ComponentName("com.samsung.android.svoice", "com.samsung.android.svoice.service.SpeechService"),
            
            // Microsoft (if available)
            ComponentName("com.microsoft.cortana", "com.microsoft.cortana.speech.SpeechService"),
            
            // Baidu (Chinese alternative)
            ComponentName("com.baidu.speech", "com.baidu.speech.service.SpeechService")
        )
        
        var lastError: Exception? = null
        var serviceName = "unknown"
        
        for (service in speechServices) {
            try {
                if (service != null) {
                    Log.d("RealtimeSpeech", "Trying speech service: ${service.packageName}/${service.className}")
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, service)
                    serviceName = "${service.packageName}/${service.className}"
                } else {
                    Log.d("RealtimeSpeech", "Trying default SpeechRecognizer")
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    serviceName = "default"
                }
                
                if (speechRecognizer != null) {
                    Log.d("RealtimeSpeech", "SpeechRecognizer created successfully with service: $serviceName")
                    break
                } else {
                    Log.w("RealtimeSpeech", "SpeechRecognizer creation returned null for service: $serviceName")
                }
            } catch (e: Exception) {
                Log.w("RealtimeSpeech", "Failed to create SpeechRecognizer with service: $serviceName", e)
                lastError = e
                speechRecognizer = null
            }
        }
        
        if (speechRecognizer == null) {
            val errorMessage = "No speech recognition service available. Last error: ${lastError?.message ?: "Unknown error"}"
            Log.e("RealtimeSpeech", errorMessage)
            channel.trySend(RecognitionResult.Error(errorMessage))
            return
        }
        
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("RealtimeSpeech", "Ready for speech")
                channel.trySend(RecognitionResult.Ready)
            }
            
            override fun onBeginningOfSpeech() {
                Log.d("RealtimeSpeech", "Beginning of speech")
                channel.trySend(RecognitionResult.Listening)
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level feedback could be added here
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer received
            }
            
            override fun onEndOfSpeech() {
                Log.d("RealtimeSpeech", "End of speech")
            }
            
                override fun onError(error: Int) {
                    val errorMessage = getErrorText(error)
                    Log.e("RealtimeSpeech", "Error: $errorMessage (code: $error)")
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            Log.d("RealtimeSpeech", "No speech detected - user may need to speak louder or closer to mic")
                            // Restart listening for continuous recognition on non-fatal errors
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isRecording) {
                                    Log.d("RealtimeSpeech", "Restarting speech recognition after no match")
                                    startListening(locale)
                                }
                            }, 100)
                        }
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            Log.d("RealtimeSpeech", "Speech timeout - no speech detected within timeout period")
                            // Restart listening for continuous recognition
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isRecording) {
                                    Log.d("RealtimeSpeech", "Restarting speech recognition after timeout")
                                    startListening(locale)
                                }
                            }, 100)
                        }
                        SpeechRecognizer.ERROR_AUDIO -> Log.d("RealtimeSpeech", "Audio error - microphone may not be working")
                        SpeechRecognizer.ERROR_CLIENT -> Log.d("RealtimeSpeech", "Client error - speech recognition service issue")
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Log.d("RealtimeSpeech", "Permission error - microphone permission may be missing")
                        SpeechRecognizer.ERROR_NETWORK -> Log.d("RealtimeSpeech", "Network error - speech recognition requires internet")
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Log.d("RealtimeSpeech", "Network timeout - speech recognition service unreachable")
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Log.d("RealtimeSpeech", "Recognizer busy - another app may be using speech recognition")
                        SpeechRecognizer.ERROR_SERVER -> Log.d("RealtimeSpeech", "Server error - speech recognition service error")
                        else -> Log.d("RealtimeSpeech", "Unknown error: $error")
                    }
                    // Only send error and stop recording for serious errors (not ERROR_NO_MATCH or ERROR_SPEECH_TIMEOUT)
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        channel.trySend(RecognitionResult.Error(errorMessage))
                        isRecording = false
                    }
                }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    currentTranscript = text
                    Log.d("RealtimeSpeech", "Final result: $text")
                    channel.trySend(RecognitionResult.FinalText(text))
                    
                    // Restart listening for continuous speech recognition
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (isRecording) {
                            Log.d("RealtimeSpeech", "Restarting speech recognition after result")
                            startListening(locale)
                        }
                    }, 100) // Small delay to ensure proper restart
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d("RealtimeSpeech", "Partial result: $text")
                    channel.trySend(RecognitionResult.PartialText(text))
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle events if needed
            }
        }
        
        speechRecognizer?.setRecognitionListener(recognitionListener)
        Log.d("RealtimeSpeech", "Recognition listener set, starting to listen")
        
        // Add a delay to ensure listener is set before starting
        kotlinx.coroutines.delay(100)
        startListening(locale)
    }
    
    
    private fun startListening(locale: String) {
        val intent = createRecognitionIntent(locale)
        isRecording = true
        Log.d("RealtimeSpeech", "Starting to listen with intent")
        speechRecognizer?.startListening(intent)
        Log.d("RealtimeSpeech", "startListening called")
    }
    
    private fun startListeningWithRecognizer(recognizer: SpeechRecognizer, locale: String) {
        val intent = createRecognitionIntent(locale)
        recognizer.startListening(intent)
    }
    
    
    private fun createRecognitionIntent(locale: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // Allow more results for continuous speech
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Configure for better speech detection
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500) // 0.5 second minimum
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000) // 8 seconds silence timeout
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000) // 5 seconds partial silence
            // Enable continuous recognition
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            putExtra("android.speech.extra.DICTATION_MODE", true)
            putExtra("android.speech.extra.CONTINUOUS_SPEECH", true)
        }
    }
    
    fun stopRecognition() {
        isRecording = false
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    fun getCurrentTranscript(): String = currentTranscript
    
    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error: $errorCode"
        }
    }
}
