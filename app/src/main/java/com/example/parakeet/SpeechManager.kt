package com.example.parakeet

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object SpeechManager {
    private var speechRecognizer: SpeechRecognizer? = null
    private val audioRecorder = AudioRecorder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun getRecognizer(context: Context): SpeechRecognizer {
        synchronized(this) {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer(context.applicationContext)
                // Proactively trigger model loading
                scope.launch {
                    speechRecognizer?.loadModel()
                }
            }
            return speechRecognizer!!
        }
    }

    fun getRecorder(): AudioRecorder = audioRecorder
}
