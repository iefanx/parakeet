package com.example.parakeet

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder {
    private val TAG = "AudioRecorder"
    private val SAMPLE_RATE = 16000
    private val MAX_RECORDING_SECONDS = 300
    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private val audioChunks = mutableListOf<ShortArray>()
    private val lock = Any()

    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope): Boolean {
        if (!isRecording.compareAndSet(false, true)) return false
        
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size")
            isRecording.set(false)
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "AudioRecord permission security exception", e)
            isRecording.set(false)
            releaseRecorder()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord creation failed", e)
            isRecording.set(false)
            releaseRecorder()
            return false
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            isRecording.set(false)
            releaseRecorder()
            return false
        }

        synchronized(lock) {
            audioChunks.clear()
        }

        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            isRecording.set(false)
            releaseRecorder()
            return false
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            var consecutiveErrors = 0
            val maxSamples = SAMPLE_RATE * MAX_RECORDING_SECONDS
            var capturedSamples = 0
            while (isRecording.get() && capturedSamples < maxSamples) {
                val buffer = ShortArray(minBufferSize)
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    consecutiveErrors = 0
                    val chunk = if (readSize == buffer.size) buffer else buffer.copyOf(readSize)
                    synchronized(lock) {
                        audioChunks.add(chunk)
                    }
                    capturedSamples += readSize
                } else {
                    consecutiveErrors++
                    if (consecutiveErrors > 5) {
                        // Sleep to avoid high CPU spin if reading is failing persistently
                        delay(50)
                    }
                }
            }
        }
        
        return true
    }

    fun stopRecording(): FloatArray {
        if (!isRecording.getAndSet(false)) return FloatArray(0)
        
        recordingJob?.cancel()
        recordingJob = null
        releaseRecorder()

        // Concatenate and convert to FloatArray in a single pass
        synchronized(lock) {
            val totalSamples = audioChunks.sumOf { it.size }
            val floatSamples = FloatArray(totalSamples)
            var offset = 0
            for (chunk in audioChunks) {
                for (i in chunk.indices) {
                    floatSamples[offset + i] = chunk[i] / 32768.0f
                }
                offset += chunk.size
            }
            audioChunks.clear()
            return floatSamples
        }
    }

    private fun releaseRecorder() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.d(TAG, "AudioRecord stop ignored", e)
        }

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }
}
