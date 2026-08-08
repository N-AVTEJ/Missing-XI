package com.example

import com.example.util.OpponentPairTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpponentPairTrackerTest {

    @Test
    fun testTwoTeamsOpponents() {
        val teams = listOf(
            listOf("A", "B", "C"),
            listOf("D", "E", "F")
        )
        val opponentPairs = OpponentPairTracker.getOpponentPairs(teams)
        assertEquals(9, opponentPairs.size)
        // Expected opponent pairs
        assertTrue(opponentPairs.contains("A|D"))
        assertTrue(opponentPairs.contains("A|E"))
        assertTrue(opponentPairs.contains("A|F"))
        assertTrue(opponentPairs.contains("B|D"))
        assertTrue(opponentPairs.contains("B|E"))
        assertTrue(opponentPairs.contains("B|F"))
        assertTrue(opponentPairs.contains("C|D"))
        assertTrue(opponentPairs.contains("C|E"))
        assertTrue(opponentPairs.contains("C|F"))
    }

    @Test
    fun testUpdateOpponentHistory() {
        val initialCounts = mapOf("A|D" to 1)
        val teams = listOf(
            listOf("A", "B"),
            listOf("D", "E")
        )
        val updatedCounts = OpponentPairTracker.updateOpponentHistory(initialCounts, teams)
        assertEquals(2, updatedCounts["A|D"])
        assertEquals(1, updatedCounts["A|E"])
        assertEquals(1, updatedCounts["B|D"])
        assertEquals(1, updatedCounts["B|E"])
        assertEquals(4, updatedCounts.size)
    }

    @Test
    fun testAnalyzeOpponentPairs() {
        val history = mapOf("A|D" to 2, "B|E" to 1)
        val candidateTeams = listOf(
            listOf("A", "B"),
            listOf("D", "E")
        )
        val analysis = OpponentPairTracker.analyzeOpponentPairs(candidateTeams, history)
        
        assertEquals(4, analysis.totalOpponentPairs)
        assertEquals(2, analysis.newOpponentPairs)
        assertEquals(2, analysis.repeatedOpponentPairs)
        assertEquals(3, analysis.historicalRepeatOccurrences) // 2 for A|D, 1 for B|E
        assertEquals(2, analysis.maxHistoricalOpponentCount)
    }

    @Test
    fun testEmptyHistoryAnalysis() {
        val candidateTeams = listOf(
            listOf("A", "B"),
            listOf("D", "E")
        )
        val analysis = OpponentPairTracker.analyzeOpponentPairs(candidateTeams, emptyMap())
        
        assertEquals(4, analysis.totalOpponentPairs)
        assertEquals(4, analysis.newOpponentPairs)
        assertEquals(0, analysis.repeatedOpponentPairs)
        assertEquals(0, analysis.historicalRepeatOccurrences)
        assertEquals(0, analysis.maxHistoricalOpponentCount)
    }

    @Test
    fun testOpponentStatistics() {
        val history = mapOf(
            "A|D" to 2,
            "B|E" to 1,
            "C|F" to 0
        )
        val stats = OpponentPairTracker.getOpponentStatistics(history)
        
        assertEquals(2, stats.uniquePairsCount)
        assertEquals(3, stats.totalPairOccurrences)
        assertEquals(1, stats.repeatedPairOccurrences)
        assertEquals(2, stats.maxRepeatedPairCount)
    }
}
