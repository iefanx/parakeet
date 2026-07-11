package com.example.parakeet

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TranslationLanguageOption(val name: String, val tag: String)

object PostProcessingStore {
    private const val PREFS = "post_processing"
    private const val TRANSLATE_ENABLED = "translate_enabled"
    private const val TARGET_LANGUAGE = "target_language"
    private const val GRAMMAR_ENABLED = "grammar_enabled"

    val languages = listOf(
        TranslationLanguageOption("Spanish", TranslateLanguage.SPANISH),
        TranslationLanguageOption("French", TranslateLanguage.FRENCH),
        TranslationLanguageOption("German", TranslateLanguage.GERMAN),
        TranslationLanguageOption("Hindi", TranslateLanguage.HINDI),
        TranslationLanguageOption("Portuguese", TranslateLanguage.PORTUGUESE),
        TranslationLanguageOption("Italian", TranslateLanguage.ITALIAN),
        TranslationLanguageOption("Arabic", TranslateLanguage.ARABIC),
        TranslationLanguageOption("Japanese", TranslateLanguage.JAPANESE),
        TranslationLanguageOption("Korean", TranslateLanguage.KOREAN),
        TranslationLanguageOption("Chinese", TranslateLanguage.CHINESE)
    )

    fun translationEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(TRANSLATE_ENABLED, false)

    fun targetLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TARGET_LANGUAGE, TranslateLanguage.SPANISH) ?: TranslateLanguage.SPANISH

    fun saveTranslation(context: Context, enabled: Boolean, target: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(TRANSLATE_ENABLED, enabled)
            .putString(TARGET_LANGUAGE, target)
            .apply()
    }

    fun grammarEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(GRAMMAR_ENABLED, false)

    fun saveGrammar(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(GRAMMAR_ENABLED, enabled)
            .apply()
    }

    fun grammarLevel(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("grammar_level_v2", 0)

    fun saveGrammarLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("grammar_level_v2", level)
            .putBoolean(GRAMMAR_ENABLED, level > 0)
            .apply()
    }
}

object OnDeviceTranslationManager {
    suspend fun download(targetLanguage: String) {
        createTranslator(targetLanguage).use { translator ->
            translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build()).await()
        }
    }

    suspend fun translateIfEnabled(context: Context, text: String): String {
        if (!PostProcessingStore.translationEnabled(context) || text.isBlank()) return text
        val target = PostProcessingStore.targetLanguage(context)
        return createTranslator(target).use { translator ->
            translator.downloadModelIfNeeded().await()
            translator.translate(text).await()
        }
    }

    fun createTranslator(targetLanguage: String) = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLanguage)
            .build()
    )
}

object OnDeviceGrammarManager {
    private const val TAG = "OnDeviceGrammarManager"

    suspend fun correctIfEnabled(context: Context, text: String): String {
        val level = PostProcessingStore.grammarLevel(context)
        if (level == 0 || text.isBlank()) return text

        val trimmed = text.trim()
        val words = trimmed.split(Regex("\\s+"))
        if (words.size < 4) {
            Log.d(TAG, "Bypassing grammar correction for short query/command: '$trimmed'")
            return text
        }

        // We choose the intermediate language based on selected intensity level:
        // 1 = French (Low), 2 = Spanish (Medium), 3 = German (Intense)
        val intermediateLang = when (level) {
            1 -> TranslateLanguage.FRENCH
            2 -> TranslateLanguage.SPANISH
            3 -> TranslateLanguage.GERMAN
            else -> TranslateLanguage.SPANISH
        }

        return try {
            val forwardTranslator = OnDeviceTranslationManager.createTranslator(intermediateLang)
            forwardTranslator.downloadModelIfNeeded().await()
            val intermediateText = forwardTranslator.translate(text).await()
            forwardTranslator.close()

            val backwardTranslator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(intermediateLang)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()
            )
            backwardTranslator.downloadModelIfNeeded().await()
            val correctedText = backwardTranslator.translate(intermediateText).await()
            backwardTranslator.close()

            correctedText
        } catch (e: Exception) {
            Log.e(TAG, "On-device grammar correction via back-translation failed; using original text", e)
            text
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}
