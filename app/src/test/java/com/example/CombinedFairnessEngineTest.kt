package com.example

import com.example.util.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedFairnessEngineTest {

    @Test
    fun test1_candidateSelectionBasedOnOverallScore() {
        val evalA = FairnessEvaluation(
            overallScore = 91.0,
            teammateScore = 90.0,
            opponentScore = 90.0,
            strengthScore = 92.0,
            jokerScore = 100.0,
            teammatePenalty = 4,
            rankingPosition = 2
        )

        val evalB = FairnessEvaluation(
            overallScore = 96.0,
            teammateScore = 98.0,
            opponentScore = 95.0,
            strengthScore = 95.0,
            jokerScore = 100.0,
            teammatePenalty = 1,
            rankingPosition = 1
        )

        val cmp = FairnessRankingEngine.compareCandidates(evalA, evalB)
        // compareCandidates returns negative if eval1 is better than eval2, or positive if eval2 is better than eval1.
        assertTrue("Eval B (96) should be ranked higher than Eval A (91)", cmp > 0)
    }

    @Test
    fun test2_tieBreakerUsesTeammatePenaltyWhenOverallScoresEqual() {
        val evalA = FairnessEvaluation(
            overallScore = 90.0,
            teammatePenalty = 5,
            opponentPenalty = 2,
            strengthDifference = 1,
            newTeammatePairs = 10
        )

        val evalB = FairnessEvaluation(
            overallScore = 90.0,
            teammatePenalty = 2,
            opponentPenalty = 2,
            strengthDifference = 1,
            newTeammatePairs = 10
        )

        val cmp = FairnessRankingEngine.compareCandidates(evalA, evalB)
        assertTrue("Eval B (penalty 2) should beat Eval A (penalty 5) on tie breaker", cmp > 0)
    }

    @Test
    fun test3_equalSkillRatingsResultInZeroStrengthDifferenceAndPerfectStrengthScore() {
        // Evaluate candidate with equal strength teams
        val candidateTeams = listOf(
            GeneratedTeam(teamNumber = 1, name = "Team A", players = listOf("P1", "P2"), playerIds = listOf("p1", "p2"), totalStrength = 10, averageStrength = 5.0),
            GeneratedTeam(teamNumber = 2, name = "Team B", players = listOf("P3", "P4"), playerIds = listOf("p3", "p4"), totalStrength = 10, averageStrength = 5.0)
        )

        val strengthAnalysis = TeamStrengthEngine.evaluateCandidateStrength(candidateTeams)
        assertEquals("Strength difference should be 0", 0, strengthAnalysis.strengthDifference)

        val strengthScore = FairnessRankingEngine.calculateTeamStrengthScore(strengthAnalysis)
        assertEquals("Strength score should be 100.0 for 0 strength difference", 100.0, strengthScore, 0.001)
    }

    @Test
    fun test4_zeroPreviousHistoryResultsInPerfectTeammateAndOpponentVarietyScores() {
        val emptyPairAnalysis = CandidatePairAnalysis(
            totalPairs = 10,
            newPairs = 10,
            repeatedPairs = 0,
            penaltyResult = CandidatePenaltyResult(totalPenalty = 0, highestPairPenalty = 0)
        )

        val emptyOpponentAnalysis = OpponentPairAnalysis(
            totalOpponentPairs = 10,
            newOpponentPairs = 10,
            repeatedOpponentPairs = 0,
            historicalRepeatOccurrences = 0
        )

        val teammateScore = FairnessRankingEngine.calculateTeammateVarietyScore(emptyPairAnalysis)
        val opponentScore = FairnessRankingEngine.calculateOpponentVarietyScore(emptyOpponentAnalysis)

        assertEquals("Teammate variety score should be 100 for 0 history", 100.0, teammateScore, 0.001)
        assertEquals("Opponent variety score should be 100 for 0 history", 100.0, opponentScore, 0.001)
    }

    @Test
    fun test5_noJokerRequiredDoesNotReduceOverallScore() {
        val jokerScore = FairnessRankingEngine.calculateJokerFairnessScore(
            joker = null,
            activePlayers = listOf("P1", "P2", "P3", "P4"),
            cycleJokers = emptyList()
        )

        assertEquals("Joker score should be 100 when no Joker is required", 100.0, jokerScore, 0.001)

        val overallScore = FairnessRankingEngine.calculateOverallFairnessScore(
            teammateScore = 100.0,
            opponentScore = 100.0,
            strengthScore = 100.0,
            jokerScore = jokerScore
        )

        assertEquals("Overall score should remain 100% when all components are 100%", 100.0, overallScore, 0.001)
    }

    @Test
    fun test6_fairnessEvaluationDataStructureAndWeights() {
        val overall = FairnessRankingEngine.calculateOverallFairnessScore(
            teammateScore = 100.0,
            opponentScore = 80.0,
            strengthScore = 90.0,
            jokerScore = 100.0
        )

        // 100*0.40 + 80*0.20 + 90*0.30 + 100*0.10 = 40 + 16 + 27 + 10 = 93.0
        assertEquals("Weighted fairness score calculation verified", 93.0, overall, 0.001)

        val rating = FairnessRankingEngine.getFairnessRating(overall)
        assertEquals("Rating for 93.0 should be 'Very Good'", "Very Good", rating)
    }
}
