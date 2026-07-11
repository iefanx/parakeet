package com.example.parakeet

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val text: String
)

data class UsageStats(
    val entries: Int,
    val words: Int,
    val characters: Int,
    val todayWords: Int,
    val longestEntryWords: Int
)

object HistoryManager {
    private const val FILE_NAME = "transcription_history.json"
    private const val TAG = "HistoryManager"

    @Synchronized
    fun saveEntry(context: Context, text: String): HistoryEntry {
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            text = text.trim()
        )
        val history = getHistory(context).toMutableList()
        history.add(0, entry) // Insert at the top (newest first)
        saveHistory(context, history)
        return entry
    }

    @Synchronized
    fun getHistory(context: Context): List<HistoryEntry> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        val list = mutableListOf<HistoryEntry>()
        try {
            val content = file.readText()
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryEntry(
                        id = obj.optString("id", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        text = obj.optString("text", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read transcription history", e)
            // Self-healing: delete corrupted history file so it starts fresh next time
            try {
                file.delete()
            } catch (ex: Exception) {
                // Ignore
            }
        }
        return list
    }

    @Synchronized
    fun clearHistory(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    @Synchronized
    fun deleteEntry(context: Context, id: String) {
        val history = getHistory(context).filterNot { it.id == id }
        saveHistory(context, history)
    }

    fun getUsageStats(context: Context): UsageStats {
        return getUsageStats(getHistory(context))
    }

    fun getUsageStats(history: List<HistoryEntry>): UsageStats {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        var words = 0
        var characters = 0
        var todayWords = 0
        var longest = 0

        for (entry in history) {
            val entryWords = countWords(entry.text)
            words += entryWords
            characters += entry.text.length
            longest = maxOf(longest, entryWords)
            if (entry.timestamp >= todayStart) {
                todayWords += entryWords
            }
        }

        return UsageStats(
            entries = history.size,
            words = words,
            characters = characters,
            todayWords = todayWords,
            longestEntryWords = longest
        )
    }

    private fun countWords(text: String): Int {
        return text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    private fun saveHistory(context: Context, history: List<HistoryEntry>) {
        val file = File(context.filesDir, FILE_NAME)
        try {
            val array = JSONArray()
            for (entry in history) {
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("timestamp", entry.timestamp)
                    put("text", entry.text)
                }
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save transcription history", e)
        }
    }
}
