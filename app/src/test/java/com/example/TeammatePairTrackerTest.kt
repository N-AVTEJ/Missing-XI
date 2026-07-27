package com.example

import com.example.util.Player
import com.example.util.TeammatePairTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeammatePairTrackerTest {

    @Test
    fun testCanonicalPairKeySorting() {
        val key1 = TeammatePairTracker.createPairKey("player_7", "player_2")
        val key2 = TeammatePairTracker.createPairKey("player_2", "player_7")
        assertEquals("player_2|player_7", key1)
        assertEquals("player_2|player_7", key2)
    }

    @Test
    fun testTeamPairsCountFor4Players() {
        val teamPlayerIds = listOf("player_1", "player_2", "player_3", "player_4")
        val pairs = TeammatePairTracker.getTeamPairs(teamPlayerIds)

        // 4 * (4 - 1) / 2 = 6 pairs
        assertEquals(6, pairs.size)
        assertTrue(pairs.contains("player_1|player_2"))
        assertTrue(pairs.contains("player_1|player_3"))
        assertTrue(pairs.contains("player_1|player_4"))
        assertTrue(pairs.contains("player_2|player_3"))
        assertTrue(pairs.contains("player_2|player_4"))
        assertTrue(pairs.contains("player_3|player_4"))
    }

    @Test
    fun testPairCountsIncrementOnAcceptedShuffle() {
        var counts = emptyMap<String, Int>()

        // Shuffle 1: Team with A, B, C, D
        val acceptedTeam1 = listOf(listOf("player_A", "player_B", "player_C", "player_D"))
        counts = TeammatePairTracker.updateTeammatePairCounts(counts, acceptedTeam1)

        assertEquals(1, TeammatePairTracker.getPairCount(counts, "player_A", "player_B"))
        assertEquals(1, TeammatePairTracker.getPairCount(counts, "player_A", "player_C"))

        // Shuffle 2: Team with A, B, E, F
        val acceptedTeam2 = listOf(listOf("player_A", "player_B", "player_E", "player_F"))
        counts = TeammatePairTracker.updateTeammatePairCounts(counts, acceptedTeam2)

        // A-B should increment from 1 -> 2
        assertEquals(2, TeammatePairTracker.getPairCount(counts, "player_A", "player_B"))
        // A-C remains 1
        assertEquals(1, TeammatePairTracker.getPairCount(counts, "player_A", "player_C"))
        // A-E is 1
        assertEquals(1, TeammatePairTracker.getPairCount(counts, "player_A", "player_E"))
    }

    @Test
    fun testPairStatisticsCalculation() {
        var counts = emptyMap<String, Int>()
        
        // Accepted teams:
        // Shuffle 1: Team 1 [A, B, C]
        counts = TeammatePairTracker.updateTeammatePairCounts(counts, listOf(listOf("A", "B", "C")))
        // Shuffle 2: Team 1 [A, B, D]
        counts = TeammatePairTracker.updateTeammatePairCounts(counts, listOf(listOf("A", "B", "D")))

        // Pairs created:
        // Shuffle 1: A-B (1), A-C (1), B-C (1)
        // Shuffle 2: A-B (2), A-D (1), B-D (1)
        // Active pairs: A-B (2), A-C (1), B-C (1), A-D (1), B-D (1) => 5 unique pairs
        // Total pair occurrences = 2 + 1 + 1 + 1 + 1 = 6
        // Repeated pair occurrences = (2 - 1) + 0 + 0 + 0 + 0 = 1
        // Max repeated count = 2

        val stats = TeammatePairTracker.calculatePairStatistics(counts)

        assertEquals(5, stats.uniquePairsCount)
        assertEquals(6, stats.totalPairOccurrences)
        assertEquals(1, stats.repeatedPairOccurrences)
        assertEquals(2, stats.maxRepeatedPairCount)
    }

    @Test
    fun testSameNameDifferentIdsAreTrackedSeparately() {
        val player1 = Player("player_1", "Rahul")
        val player2 = Player("player_2", "Rahul")
        val player3 = Player("player_3", "Amit")

        val team = listOf(player1.id, player2.id, player3.id)
        val pairs = TeammatePairTracker.getTeamPairs(team)

        assertEquals(3, pairs.size)
        assertTrue(pairs.contains("player_1|player_2"))
        assertTrue(pairs.contains("player_1|player_3"))
        assertTrue(pairs.contains("player_2|player_3"))
    }

    @Test
    fun testCandidatePairAnalysisFreshSession() {
        val candidateTeam = listOf(listOf("player_A", "player_B", "player_C", "player_D"))
        val history = emptyMap<String, Int>()

        val analysis = TeammatePairTracker.analyzeCandidatePairs(candidateTeam, history)

        assertEquals(6, analysis.totalPairs)
        assertEquals(6, analysis.newPairs)
        assertEquals(0, analysis.repeatedPairs)
        assertEquals(100.0, analysis.newPairPercentage, 0.01)
        assertEquals(0.0, analysis.repeatedPairPercentage, 0.01)
        assertEquals(0, analysis.maxHistoricalPairCount)
    }

    @Test
    fun testCandidatePairAnalysisOneRepeatedPair() {
        // History: A-B was teammates once
        val history = mapOf("player_A|player_B" to 1)

        // Candidate: Team with A, B, E, F
        val candidateTeam = listOf(listOf("player_A", "player_B", "player_E", "player_F"))
        val analysis = TeammatePairTracker.analyzeCandidatePairs(candidateTeam, history)

        assertEquals(6, analysis.totalPairs)
        assertEquals(5, analysis.newPairs)
        assertEquals(1, analysis.repeatedPairs)
        assertEquals(83.333, analysis.newPairPercentage, 0.01)
        assertEquals(16.666, analysis.repeatedPairPercentage, 0.01)
        assertEquals(1, analysis.maxHistoricalPairCount)
        assertEquals(1, analysis.historicalRepeatOccurrences)
    }

    @Test
    fun testCandidatePairAnalysisHeavilyRepeatedPair() {
        val history = mapOf(
            "player_A|player_B" to 5,
            "player_C|player_D" to 2
        )

        val candidateTeams = listOf(
            listOf("player_A", "player_B"),
            listOf("player_C", "player_D")
        )

        val analysis = TeammatePairTracker.analyzeCandidatePairs(candidateTeams, history)

        assertEquals(2, analysis.totalPairs)
        assertEquals(0, analysis.newPairs)
        assertEquals(2, analysis.repeatedPairs)
        assertEquals(0.0, analysis.newPairPercentage, 0.01)
        assertEquals(100.0, analysis.repeatedPairPercentage, 0.01)
        assertEquals(5, analysis.maxHistoricalPairCount)
        assertEquals(7, analysis.historicalRepeatOccurrences)
    }

    @Test
    fun testCandidatePairAnalysisDoesNotMutateHistory() {
        val history = mapOf("player_A|player_B" to 2)
        val candidateTeam = listOf(listOf("player_A", "player_B", "player_C"))

        val analysis = TeammatePairTracker.analyzeCandidatePairs(candidateTeam, history)

        // Ensure returned analysis is correct
        assertEquals(3, analysis.totalPairs)
        assertEquals(1, analysis.repeatedPairs)

        // Ensure original history map is unchanged
        assertEquals(1, history.size)
        assertEquals(2, history["player_A|player_B"])
    }
}
