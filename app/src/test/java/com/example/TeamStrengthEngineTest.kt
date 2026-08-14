package com.example

import com.example.util.Player
import com.example.util.TeamStrengthEngine
import com.example.util.GeneratedTeam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamStrengthEngineTest {

    @Test
    fun testCalculateTeamStrength() {
        val players = listOf(
            Player(id = "1", name = "Alice", skillRating = 8),
            Player(id = "2", name = "Bob", skillRating = 7),
            Player(id = "3", name = "Charlie", skillRating = 6)
        )
        val analysis = TeamStrengthEngine.calculateTeamStrength(players)
        assertEquals(21, analysis.totalStrength)
        assertEquals(7.0, analysis.averageStrength, 0.001)
        assertEquals(8, analysis.maxPlayerRating)
        assertEquals(6, analysis.minPlayerRating)
    }

    @Test
    fun testAllDefaultRatingsStrengthDifferenceIsZero() {
        val teamA = listOf(
            Player(id = "1", name = "P1", skillRating = 5),
            Player(id = "2", name = "P2", skillRating = 5)
        )
        val teamB = listOf(
            Player(id = "3", name = "P3", skillRating = 5),
            Player(id = "4", name = "P4", skillRating = 5)
        )
        val strA = TeamStrengthEngine.calculateTeamStrength(teamA)
        val strB = TeamStrengthEngine.calculateTeamStrength(teamB)
        
        val genA = GeneratedTeam(1, "Team A", teamA.map { it.name }, totalStrength = strA.totalStrength, averageStrength = strA.averageStrength)
        val genB = GeneratedTeam(2, "Team B", teamB.map { it.name }, totalStrength = strB.totalStrength, averageStrength = strB.averageStrength)

        val candidateAnalysis = TeamStrengthEngine.evaluateCandidateStrength(listOf(genA, genB))
        assertEquals(0, candidateAnalysis.strengthDifference)
        assertEquals(10.0, candidateAnalysis.averageTeamStrength, 0.001)
    }

    @Test
    fun testBalancedRatingsDistribution() {
        // Ratings: 8, 7, 6, 5, 4, 3
        // Ideal team split 1: {8, 5, 3} = 16, team split 2: {7, 6, 4} = 17 => Diff = 1
        val team1 = listOf(
            Player(id = "1", name = "P8", skillRating = 8),
            Player(id = "2", name = "P5", skillRating = 5),
            Player(id = "3", name = "P3", skillRating = 3)
        )
        val team2 = listOf(
            Player(id = "4", name = "P7", skillRating = 7),
            Player(id = "5", name = "P6", skillRating = 6),
            Player(id = "6", name = "P4", skillRating = 4)
        )

        val str1 = TeamStrengthEngine.calculateTeamStrength(team1)
        val str2 = TeamStrengthEngine.calculateTeamStrength(team2)

        val gen1 = GeneratedTeam(1, "Team A", team1.map { it.name }, totalStrength = str1.totalStrength, averageStrength = str1.averageStrength)
        val gen2 = GeneratedTeam(2, "Team B", team2.map { it.name }, totalStrength = str2.totalStrength, averageStrength = str2.averageStrength)

        val candidateAnalysis = TeamStrengthEngine.evaluateCandidateStrength(listOf(gen1, gen2))
        assertEquals(1, candidateAnalysis.strengthDifference)
        assertEquals(17, candidateAnalysis.strongestTeamStrength)
        assertEquals(16, candidateAnalysis.weakestTeamStrength)
    }
}
