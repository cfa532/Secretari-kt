package com.secretari.app.service

import android.Manifest
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
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            if (speechRecognizer == null) {
                Log.e("RealtimeSpeech", "SpeechRecognizer creation failed!")
                channel.trySend(RecognitionResult.Error("Failed to create SpeechRecognizer"))
                return
            }
            Log.d("RealtimeSpeech", "SpeechRecognizer created successfully")
        } catch (e: Exception) {
            Log.e("RealtimeSpeech", "Exception creating SpeechRecognizer", e)
            channel.trySend(RecognitionResult.Error("Exception creating SpeechRecognizer: ${e.message}"))
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
                Log.e("RealtimeSpeech", "Error: $errorMessage")
                channel.trySend(RecognitionResult.Error(errorMessage))
                isRecording = false
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    currentTranscript = text
                    Log.d("RealtimeSpeech", "Final result: $text")
                    channel.trySend(RecognitionResult.FinalText(text))
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
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Add continuous recognition parameters
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
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
