package com.secretari.app.service

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.IOException

class UniversalAudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var startTime = 0L
    private var recordingChannel: SendChannel<RecordingResult>? = null

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

        recordingChannel = this

        try {
            audioFile = createAudioFile()

            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
            }

            mediaRecorder?.prepare()
            mediaRecorder?.start()

            isRecording = true
            startTime = System.currentTimeMillis()

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

                kotlinx.coroutines.delay(100)
            }

        } catch (e: IOException) {
            Log.e("UniversalRecorder", "Recording failed", e)
            trySend(RecordingResult.Error("Failed to start recording: ${e.message}"))
            recordingChannel = null
            close()
        }

        awaitClose {
            recordingChannel = null
            releaseRecorder()
        }
    }

    fun stopRecording(): String? {
        if (!isRecording) return null

        isRecording = false
        val filePath = audioFile?.absolutePath
        val duration = System.currentTimeMillis() - startTime

        try {
            releaseRecorder()
            Log.d("UniversalRecorder", "Recording stopped: $filePath, duration: ${duration}ms")

            recordingChannel?.trySend(RecordingResult.Stopped(filePath ?: "", duration))
            recordingChannel?.close()
            recordingChannel = null

            return filePath

        } catch (e: Exception) {
            Log.e("UniversalRecorder", "Error stopping recording", e)
            recordingChannel?.close()
            recordingChannel = null
            return null
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w("UniversalRecorder", "Error stopping media recorder", e)
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w("UniversalRecorder", "Error releasing media recorder", e)
        }
        mediaRecorder = null
    }

    private fun createAudioFile(): File {
        // Prefer external storage; fall back to internal storage (important for emulator)
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val storageDir = File(baseDir, "Secretari")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val timeStamp = System.currentTimeMillis()
        return File(storageDir, "recording_$timeStamp.m4a")
    }

    fun isRecording(): Boolean = isRecording

    fun getRecordingDuration(): Long {
        return if (isRecording) System.currentTimeMillis() - startTime else 0L
    }
}
