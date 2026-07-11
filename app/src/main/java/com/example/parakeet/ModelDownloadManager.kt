package com.example.parakeet

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface ModelDownloadState {
    data object Checking : ModelDownloadState
    data class Downloading(val fileName: String, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState {
        val progress: Float get() = if (totalBytes == 0L) 0f else bytesDownloaded.toFloat() / totalBytes
    }
    data object Ready : ModelDownloadState
    data class Error(val message: String) : ModelDownloadState
}

object ModelDownloadManager {
    private const val MODEL_DIR = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8"
    private const val BASE_URL = "https://huggingface.co/csukuangfj/$MODEL_DIR/resolve/main"
    private const val BUFFER_SIZE = 256 * 1024
    private const val EXTRA_FREE_SPACE = 100L * 1024 * 1024

    private data class ModelFile(val name: String, val size: Long, val sha256: String)

    private val files = listOf(
        ModelFile("encoder.int8.onnx", 652184281, "acfc2b4456377e15d04f0243af540b7fe7c992f8d898d751cf134c3a55fd2247"),
        ModelFile("decoder.int8.onnx", 11845275, "179e50c43d1a9de79c8a24149a2f9bac6eb5981823f2a2ed88d655b24248db4e"),
        ModelFile("joiner.int8.onnx", 6355277, "3164c13fc2821009440d20fcb5fdc78bff28b4db2f8d0f0b329101719c0948b3"),
        ModelFile("tokens.txt", 93939, "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d")
    )
    private val totalBytes = files.sumOf { it.size }
    private val mutex = Mutex()
    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Checking)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    fun modelDirectory(context: Context): File = File(context.filesDir, "models/$MODEL_DIR")

    suspend fun ensureModel(context: Context): File? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val directory = modelDirectory(context).apply { mkdirs() }
            try {
                _state.value = ModelDownloadState.Checking
                if (files.all { File(directory, it.name).length() == it.size }) {
                    _state.value = ModelDownloadState.Ready
                    return@withContext directory
                }

                val bytesStillNeeded = files.sumOf { spec ->
                    val complete = File(directory, spec.name)
                    val partial = File(directory, "${spec.name}.part")
                    if (complete.length() == spec.size) 0L else (spec.size - partial.length().coerceAtMost(spec.size))
                }
                if (directory.usableSpace < bytesStillNeeded + EXTRA_FREE_SPACE) {
                    throw IllegalStateException("Not enough storage. Free at least ${(bytesStillNeeded + EXTRA_FREE_SPACE) / (1024 * 1024)} MB and try again.")
                }

                var completedBytes = files.filter { File(directory, it.name).length() == it.size }.sumOf { it.size }
                for (spec in files) {
                    val target = File(directory, spec.name)
                    if (target.length() == spec.size) {
                        continue
                    }
                    downloadFile(spec, directory, completedBytes)
                    completedBytes += spec.size
                }
                _state.value = ModelDownloadState.Ready
                directory
            } catch (e: Exception) {
                _state.value = ModelDownloadState.Error(e.message ?: "Model download failed")
                null
            }
        }
    }

    private fun downloadFile(spec: ModelFile, directory: File, completedBytes: Long) {
        val target = File(directory, spec.name)
        val partial = File(directory, "${spec.name}.part")
        if (partial.length() > spec.size) partial.delete()
        var existing = partial.length()

        val connection = (URL("$BASE_URL/${spec.name}?download=true").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }

        try {
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK && existing > 0) {
                partial.delete()
                existing = 0
            } else if (connection.responseCode !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
                throw IllegalStateException("Download server returned HTTP ${connection.responseCode}")
            }

            RandomAccessFile(partial, "rw").use { output ->
                output.seek(existing)
                connection.inputStream.buffered(BUFFER_SIZE).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = existing
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        _state.value = ModelDownloadState.Downloading(spec.name, completedBytes + downloaded, totalBytes)
                    }
                }
            }

            if (partial.length() != spec.size) throw IllegalStateException("${spec.name} download was incomplete")
            if (sha256(partial) != spec.sha256) {
                partial.delete()
                throw IllegalStateException("${spec.name} failed its integrity check. Please retry.")
            }
            if (!partial.renameTo(target)) throw IllegalStateException("Could not finish saving ${spec.name}")
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
