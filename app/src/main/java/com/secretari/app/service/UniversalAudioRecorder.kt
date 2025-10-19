package com.secretari.app.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.IOException

class UniversalAudioRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null
    
    sealed class RecordingResult {
        object Started : RecordingResult()
        data class AudioLevel(val level: Float) : RecordingResult()
        data class Stopped(val filePath: String, val durationMs: Long) : RecordingResult()
        data class Error(val message: String) : RecordingResult()
    }
    
    fun startRecording(): Flow<RecordingResult> = callbackFlow {
        if (isRecording) {
            trySend(RecordingResult.Error("Already recording"))
            close()
            return@callbackFlow
        }
        
        try {
            // Create audio file
            audioFile = createAudioFile()
            
            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                
                // Set audio quality settings for better compatibility
                setAudioSamplingRate(8000)
                setAudioEncodingBitRate(12200)
            }
            
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            
            isRecording = true
            val _startTime = System.currentTimeMillis()
            
            Log.d("UniversalRecorder", "Recording started: ${audioFile?.absolutePath}")
            trySend(RecordingResult.Started)
            
            // Monitor audio levels
            while (isRecording && mediaRecorder != null) {
                try {
                    val maxAmplitude = mediaRecorder?.maxAmplitude ?: 0
                    val level = if (maxAmplitude > 0) {
                        (20 * Math.log10(maxAmplitude.toDouble() / 32767.0)).toFloat()
                    } else {
                        -60f
                    }
                    trySend(RecordingResult.AudioLevel(level))
                } catch (e: Exception) {
                    // Ignore amplitude errors
                }
                
                kotlinx.coroutines.delay(100) // Check every 100ms
            }
            
        } catch (e: IOException) {
            Log.e("UniversalRecorder", "Recording failed", e)
            trySend(RecordingResult.Error("Failed to start recording: ${e.message}"))
            close()
        }
        
        awaitClose {
            stopRecording()
        }
    }
    
    fun stopRecording(): String? {
        if (!isRecording) return null
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            val filePath = audioFile?.absolutePath
            Log.d("UniversalRecorder", "Recording stopped: $filePath")
            
            return filePath
            
        } catch (e: Exception) {
            Log.e("UniversalRecorder", "Error stopping recording", e)
            return null
        }
    }
    
    private fun createAudioFile(): File {
        val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Secretari")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        val timeStamp = System.currentTimeMillis()
        return File(storageDir, "recording_$timeStamp.3gp")
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getRecordingDuration(): Long {
        // This is a simplified duration calculation
        // In a real implementation, you'd track the start time
        return if (isRecording) {
            System.currentTimeMillis() - System.currentTimeMillis() // Placeholder
        } else {
            0
        }
    }
}
