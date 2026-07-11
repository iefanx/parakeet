package com.example.parakeet

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryManagerTest {
    @Test
    fun usageStatsCountEntriesWordsCharactersAndLongestEntry() {
        val entries = listOf(
            HistoryEntry("1", 0L, "one two three"),
            HistoryEntry("2", 0L, "four\n five")
        )

        val stats = HistoryManager.getUsageStats(entries)

        assertEquals(2, stats.entries)
        assertEquals(5, stats.words)
        assertEquals(23, stats.characters)
        assertEquals(3, stats.longestEntryWords)
        assertEquals(0, stats.todayWords)
    }

    @Test
    fun usageStatsTreatBlankTextAsZeroWords() {
        val stats = HistoryManager.getUsageStats(
            listOf(HistoryEntry("1", System.currentTimeMillis(), "  \n  "))
        )

        assertEquals(0, stats.words)
        assertEquals(0, stats.todayWords)
        assertEquals(0, stats.longestEntryWords)
    }
}
