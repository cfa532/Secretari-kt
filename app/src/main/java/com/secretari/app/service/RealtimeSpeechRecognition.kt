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

class RealtimeSpeechRecognition(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private var currentTranscript = ""
    
    // Recognition optimization data
    private val recognitionHistory = mutableListOf<RecognitionResult>()
    private var averageConfidence = 0.8f
    private var adaptiveThreshold = 0.7f
    private var noiseLevel = 0f
    private var consecutiveLowConfidence = 0
    
    sealed class RecognitionResult {
        data class PartialText(val text: String) : RecognitionResult()
        data class FinalText(val text: String) : RecognitionResult()
        data class Error(val message: String) : RecognitionResult()
        object Ready : RecognitionResult()
        object Listening : RecognitionResult()
    }
    
    // Data class for tracking recognition results with confidence
    private data class RecognitionWithConfidence(
        val text: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    )
    
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
            // Try default system service first (most compatible)
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
                        } else {
                            Log.d("RealtimeSpeech", "Not restarting after no match - recording stopped")
                        }
                    }, 800) // Increased delay for better emulator compatibility
                }
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            Log.d("RealtimeSpeech", "Speech timeout - no speech detected within timeout period")
                            // Restart listening for continuous recognition
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isRecording) {
                                    Log.d("RealtimeSpeech", "Restarting speech recognition after timeout")
                                    startListening(locale)
                                } else {
                                    Log.d("RealtimeSpeech", "Not restarting after timeout - recording stopped")
                                }
                            }, 800) // Increased delay for better emulator compatibility
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
                    // Only send error and stop recording for serious errors
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, 
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            // These are recoverable - don't stop recording, restart logic already handled above
                        }
                        SpeechRecognizer.ERROR_AUDIO,
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        SpeechRecognizer.ERROR_SERVER -> {
                            // These are serious errors - stop recording
                            channel.trySend(RecognitionResult.Error(errorMessage))
                            isRecording = false
                        }
                        else -> {
                            // Unknown errors - try to restart
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (isRecording) {
                                    Log.d("RealtimeSpeech", "Restarting after unknown error: $error")
                                    startListening(locale)
                                }
                            }, 1000)
                        }
                    }
                }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    Log.d("RealtimeSpeech", "Raw results: ${matches.joinToString(", ")}")
                    Log.d("RealtimeSpeech", "Confidence scores: ${confidenceScores?.joinToString(", ")}")
                    
                    // Use optimization algorithm to select best result
                    val bestResult = selectBestResult(matches, confidenceScores)
                    
                    if (bestResult != null) {
                        currentTranscript = bestResult.text
                        Log.d("RealtimeSpeech", "Optimized result: '${bestResult.text}' (confidence: ${bestResult.confidence})")
                        channel.trySend(RecognitionResult.FinalText(bestResult.text))
                    } else {
                        Log.d("RealtimeSpeech", "No result passed optimization filters")
                    }
                } else {
                    Log.d("RealtimeSpeech", "No final results received")
                }
                
                // Always restart listening for continuous speech recognition
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isRecording) {
                        Log.d("RealtimeSpeech", "Restarting speech recognition after onResults")
                        startListening(locale)
                    } else {
                        Log.d("RealtimeSpeech", "Not restarting - recording stopped")
                    }
                }, 500) // Increased delay for better emulator compatibility
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidenceScores = partialResults?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    // For partial results, we're more lenient with filtering
                    val bestResult = selectBestResult(matches, confidenceScores)
                    val text = bestResult?.text ?: matches[0] // Fallback to first result
                    
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
            // Google's official fix for short word recognition (Issue #448768895)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // Allow more results for continuous speech
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Configure for better speech detection - back to working configuration
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
    
    // ===== RECOGNITION OPTIMIZATION ALGORITHMS =====
    
    /**
     * Analyzes multiple recognition results and selects the best one based on confidence scores
     * and contextual validation
     */
    private fun selectBestResult(results: List<String>, confidenceScores: FloatArray?): RecognitionWithConfidence? {
        if (results.isEmpty()) return null
        
        val confidences = confidenceScores ?: FloatArray(results.size) { 0.5f }
        
        // Combine results with confidence scores
        val resultWithConfidence = results.zip(confidences.toList()).map { (text, confidence) ->
            RecognitionWithConfidence(text, confidence)
        }
        
        // Apply filtering and selection algorithms
        val filteredResults = filterResults(resultWithConfidence)
        val bestResult = selectOptimalResult(filteredResults)
        
        // Update recognition history for adaptive learning
        bestResult?.let { updateRecognitionHistory(it) }
        
        return bestResult
    }
    
    /**
     * Filters out low-quality results using multiple criteria
     */
    private fun filterResults(results: List<RecognitionWithConfidence>): List<RecognitionWithConfidence> {
        return results.filter { result ->
            // Confidence threshold (adaptive based on history)
            val confidenceThreshold = calculateAdaptiveThreshold()
            if (result.confidence < confidenceThreshold) {
                consecutiveLowConfidence++
                return@filter false
            }
            
            // Length validation (too short or too long results are suspicious)
            if (result.text.length < 2 || result.text.length > 200) {
                return@filter false
            }
            
            // Noise level adjustment
            if (noiseLevel > 0.7f && result.confidence < 0.8f) {
                return@filter false
            }
            
            consecutiveLowConfidence = 0
            true
        }
    }
    
    /**
     * Selects the optimal result using context-aware algorithms
     */
    private fun selectOptimalResult(results: List<RecognitionWithConfidence>): RecognitionWithConfidence? {
        if (results.isEmpty()) return null
        
        // If only one result, return it if it meets minimum criteria
        if (results.size == 1) {
            return if (results[0].confidence >= 0.6f) results[0] else null
        }
        
        // Score each result based on multiple factors
        val scoredResults = results.map { result ->
            val confidenceScore = result.confidence
            val contextScore = calculateContextScore(result.text)
            val lengthScore = calculateLengthScore(result.text)
            val noiseAdjustment = if (noiseLevel > 0.5f) 0.1f else 0f
            
            val totalScore = confidenceScore * 0.5f + contextScore * 0.3f + lengthScore * 0.2f - noiseAdjustment
            result to totalScore
        }
        
        // Return the result with highest score
        return scoredResults.maxByOrNull { it.second }?.first
    }
    
    /**
     * Calculates context score based on previous recognition history
     */
    private fun calculateContextScore(text: String): Float {
        if (recognitionHistory.isEmpty()) return 0.5f
        
        // Simple context scoring - can be enhanced with NLP
        val recentResults = recognitionHistory.takeLast(5).filterIsInstance<RecognitionResult.FinalText>()
        
        // Check for word repetition patterns (might indicate recognition issues)
        val wordRepetition = recentResults.any { it.text.contains(text) }
        if (wordRepetition) return 0.3f
        
        // Check for reasonable word transitions (basic implementation)
        val lastWords = recentResults.lastOrNull()?.text?.split(" ")?.takeLast(2) ?: emptyList()
        val currentWords = text.split(" ")
        
        // Basic grammar/context validation
        return when {
            currentWords.size == 1 && lastWords.isNotEmpty() -> 0.7f // Single word continuation
            currentWords.size > 1 -> 0.8f // Multi-word phrases
            else -> 0.5f
        }
    }
    
    /**
     * Calculates length-based score for result validation
     */
    private fun calculateLengthScore(text: String): Float {
        return when (text.length) {
            in 3..50 -> 1.0f // Optimal length
            in 1..2 -> 0.7f // Short words (numbers, articles)
            in 51..100 -> 0.8f // Longer phrases
            else -> 0.3f // Too short or too long
        }
    }
    
    /**
     * Calculates adaptive confidence threshold based on recognition history
     */
    private fun calculateAdaptiveThreshold(): Float {
        // Start with base threshold
        var threshold = 0.7f
        
        // Adjust based on recent success rate
        val recentResults = recognitionHistory.takeLast(10)
        if (recentResults.isNotEmpty()) {
            val successRate = recentResults.count { it is RecognitionResult.FinalText } / recentResults.size.toFloat()
            
            when {
                successRate > 0.8f -> threshold -= 0.1f // Lower threshold if doing well
                successRate < 0.5f -> threshold += 0.1f // Raise threshold if struggling
            }
        }
        
        // Adjust based on noise level
        if (noiseLevel > 0.6f) threshold += 0.1f
        if (noiseLevel < 0.3f) threshold -= 0.05f
        
        // Adjust based on consecutive low confidence
        if (consecutiveLowConfidence > 3) threshold -= 0.1f
        
        return threshold.coerceIn(0.4f, 0.9f)
    }
    
    /**
     * Updates recognition history for adaptive learning
     */
    private fun updateRecognitionHistory(result: RecognitionWithConfidence) {
        recognitionHistory.add(RecognitionResult.FinalText(result.text))
        
        // Keep only recent history (last 50 results)
        if (recognitionHistory.size > 50) {
            recognitionHistory.removeAt(0)
        }
        
        // Update average confidence
        val recentConfidences = recognitionHistory.takeLast(20)
            .filterIsInstance<RecognitionResult.FinalText>()
            .map { 0.8f } // Simplified - in real implementation, we'd track actual confidences
        
        if (recentConfidences.isNotEmpty()) {
            averageConfidence = recentConfidences.average().toFloat()
        }
    }
    
    /**
     * Updates noise level based on audio monitoring
     */
    private fun updateNoiseLevel(level: Float) {
        noiseLevel = (noiseLevel * 0.9f + level * 0.1f).coerceIn(0f, 1f)
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
