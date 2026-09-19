package com.example

import com.example.util.*
import org.junit.Assert.*
import org.junit.Test

class FairnessQualityGateTest {

    @Test
    fun test1_candidatePassesAllCriteria() {
        val eval = FairnessEvaluation(
            overallScore = 88.0,
            strengthDifference = 1,
            teammatePenalty = 2,
            opponentPenalty = 3
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertTrue("Candidate should pass quality gate", result.passed)
        assertTrue("Overall score check should pass", result.overallScorePassed)
        assertTrue("Strength check should pass", result.strengthPassed)
        assertTrue("Teammate penalty check should pass", result.teammatePenaltyPassed)
        assertTrue("Opponent penalty check should pass", result.opponentPenaltyPassed)
        assertTrue("Failed checks list should be empty", result.failedChecks.isEmpty())
        assertEquals(FairnessQualityLabel.HIGH_QUALITY, result.qualityLabel)
    }

    @Test
    fun test2_lowFairnessScoreFailsQualityGate() {
        val eval = FairnessEvaluation(
            overallScore = 62.0,
            strengthDifference = 1,
            teammatePenalty = 2,
            opponentPenalty = 3
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertFalse("Candidate should fail quality gate due to low overall score", result.passed)
        assertFalse("Overall score check should fail", result.overallScorePassed)
        assertTrue("Strength check should pass", result.strengthPassed)
        assertEquals(FairnessQualityLabel.FAILED, result.qualityLabel)
        assertEquals(1, result.failedChecks.size)
        assertTrue(result.failedChecks[0].contains("Fairness score below target"))
    }

    @Test
    fun test3_poorTeamBalanceFailsQualityGate() {
        val eval = FairnessEvaluation(
            overallScore = 80.0,
            strengthDifference = 4, // > threshold 2
            teammatePenalty = 2,
            opponentPenalty = 3
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertFalse("Candidate should fail quality gate due to high strength difference", result.passed)
        assertFalse("Strength check should fail", result.strengthPassed)
        assertEquals(1, result.failedChecks.size)
        assertTrue(result.failedChecks[0].contains("Team strength difference above threshold"))
    }

    @Test
    fun test4_highTeammatePenaltyFailsQualityGate() {
        val eval = FairnessEvaluation(
            overallScore = 78.0,
            strengthDifference = 1,
            teammatePenalty = 8, // > threshold 5
            opponentPenalty = 2
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertFalse("Candidate should fail quality gate due to high teammate penalty", result.passed)
        assertFalse("Teammate penalty check should fail", result.teammatePenaltyPassed)
        assertEquals(1, result.failedChecks.size)
        assertTrue(result.failedChecks[0].contains("Teammate repetition above threshold"))
    }

    @Test
    fun test5_highOpponentPenaltyFailsQualityGate() {
        val eval = FairnessEvaluation(
            overallScore = 75.0,
            strengthDifference = 1,
            teammatePenalty = 3,
            opponentPenalty = 7 // > threshold 5
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertFalse("Candidate should fail quality gate due to high opponent penalty", result.passed)
        assertFalse("Opponent penalty check should fail", result.opponentPenaltyPassed)
        assertEquals(1, result.failedChecks.size)
        assertTrue(result.failedChecks[0].contains("Opponent repetition above threshold"))
    }

    @Test
    fun test6_multipleFailedConditionsAreCaptured() {
        val eval = FairnessEvaluation(
            overallScore = 55.0, // Fail
            strengthDifference = 5, // Fail
            teammatePenalty = 9, // Fail
            opponentPenalty = 8  // Fail
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertFalse(result.passed)
        assertFalse(result.overallScorePassed)
        assertFalse(result.strengthPassed)
        assertFalse(result.teammatePenaltyPassed)
        assertFalse(result.opponentPenaltyPassed)
        assertEquals(4, result.failedChecks.size)
    }

    @Test
    fun test7_acceptableLabelForScoresBetween70And84() {
        val eval = FairnessEvaluation(
            overallScore = 75.0,
            strengthDifference = 2,
            teammatePenalty = 4,
            opponentPenalty = 4
        )
        val result = FairnessQualityResult.evaluate(eval)

        assertTrue(result.passed)
        assertEquals(FairnessQualityLabel.ACCEPTABLE, result.qualityLabel)
    }

    @Test
    fun test8_customThresholdEvaluation() {
        val eval = FairnessEvaluation(
            overallScore = 75.0,
            strengthDifference = 3,
            teammatePenalty = 2,
            opponentPenalty = 2
        )
        // With default maxStrengthDiff = 2, this fails. With custom maxStrengthDiff = 5, this passes.
        val defaultResult = FairnessQualityResult.evaluate(eval)
        val customResult = FairnessQualityResult.evaluate(
            eval,
            minScore = 70.0,
            maxStrengthDiff = 5,
            maxTeammatePenalty = 5,
            maxOpponentPenalty = 5
        )

        assertFalse(defaultResult.passed)
        assertTrue(customResult.passed)
    }

    @Test
    fun test9_fairnessConfigConstantsAreCentralized() {
        assertEquals(70.0, FairnessConfig.MINIMUM_OVERALL_FAIRNESS_SCORE, 0.001)
        assertEquals(2, FairnessConfig.MAXIMUM_TEAM_STRENGTH_DIFFERENCE)
        assertEquals(5, FairnessConfig.MAXIMUM_TEAMMATE_PENALTY)
        assertEquals(5, FairnessConfig.MAXIMUM_OPPONENT_PENALTY)
        assertEquals(5, FairnessConfig.MAX_ADDITIONAL_GENERATION_ATTEMPTS)
    }

    @Test
    fun test10_bestAvailableStatusFallbackCopy() {
        val eval = FairnessEvaluation(
            overallScore = 65.0,
            strengthDifference = 3,
            teammatePenalty = 6,
            opponentPenalty = 6
        )
        val failedResult = FairnessQualityResult.evaluate(eval)
        val bestAvailableResult = failedResult.copy(qualityLabel = FairnessQualityLabel.BEST_AVAILABLE)

        assertFalse(bestAvailableResult.passed)
        assertEquals(FairnessQualityLabel.BEST_AVAILABLE, bestAvailableResult.qualityLabel)
        assertEquals(4, bestAvailableResult.failedChecks.size)
    }
}
