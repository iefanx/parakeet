package com.example.parakeet

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class SpeechRecognizer(private val context: Context) {
    private val TAG = "SpeechRecognizer"
    private var recognizer: OfflineRecognizer? = null
    private val loadMutex = Mutex()
    private val decodeMutex = Mutex()
    var isModelLoaded = false
        private set

    suspend fun loadModel(): Boolean = loadMutex.withLock {
        if (isModelLoaded && recognizer != null) return true

        return withContext(Dispatchers.IO) {
            try {
                val modelDirectory = ModelDownloadManager.ensureModel(context) ?: return@withContext false
                Log.d(TAG, "Loading Parakeet TDT 0.6B v3 model from ${modelDirectory.absolutePath}...")

                val transducerConfig = OfflineTransducerModelConfig(
                    encoder = File(modelDirectory, "encoder.int8.onnx").absolutePath,
                    decoder = File(modelDirectory, "decoder.int8.onnx").absolutePath,
                    joiner = File(modelDirectory, "joiner.int8.onnx").absolutePath
                )

                val modelConfig = OfflineModelConfig(
                    transducer = transducerConfig,
                    tokens = File(modelDirectory, "tokens.txt").absolutePath,
                    numThreads = 4,
                    debug = BuildConfig.DEBUG,
                    modelType = "nemo_transducer"
                )

                val featConfig = FeatureConfig(
                    sampleRate = 16000,
                    featureDim = 80
                )

                val recognizerConfig = OfflineRecognizerConfig(
                    featConfig = featConfig,
                    modelConfig = modelConfig,
                    decodingMethod = "greedy_search"
                )

                // Downloaded absolute file paths must be loaded without an AssetManager.
                // sherpa-onnx intentionally terminates the process when both are supplied.
                recognizer = OfflineRecognizer(null, recognizerConfig)
                isModelLoaded = true
                Log.d(TAG, "Model loaded successfully!")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model", e)
                isModelLoaded = false
                recognizer = null
                false
            }
        }
    }

    suspend fun transcribe(samples: FloatArray): String = decodeMutex.withLock {
        val currentRecognizer = recognizer
        if (currentRecognizer == null || !isModelLoaded) {
            return "Model not loaded"
        }

        if (samples.isEmpty()) {
            return ""
        }

        return withContext(Dispatchers.Default) {
            var stream: com.k2fsa.sherpa.onnx.OfflineStream? = null
            try {
                Log.d(TAG, "Transcribing ${samples.size} samples...")
                stream = currentRecognizer.createStream()
                stream.acceptWaveform(samples, 16000)
                currentRecognizer.decode(stream)
                val result = currentRecognizer.getResult(stream)
                Log.d(TAG, "Transcription result: ${result.text}")
                result.text.trim()
            } catch (e: Exception) {
                Log.e(TAG, "Error during transcription", e)
                "Error transcribing: ${e.localizedMessage}"
            } finally {
                try {
                    stream?.release()
                } catch (ex: Exception) {
                    // Ignore release failures from native cleanup.
                }
            }
        }
    }
}
