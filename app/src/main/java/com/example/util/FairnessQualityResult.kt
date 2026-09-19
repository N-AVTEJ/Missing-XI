package com.example.util

import kotlin.math.roundToInt

enum class FairnessQualityLabel {
    HIGH_QUALITY,
    ACCEPTABLE,
    BEST_AVAILABLE,
    FAILED
}

data class FairnessQualityResult(
    val passed: Boolean = true,
    val overallScorePassed: Boolean = true,
    val strengthPassed: Boolean = true,
    val teammatePenaltyPassed: Boolean = true,
    val opponentPenaltyPassed: Boolean = true,
    val failedChecks: List<String> = emptyList(),
    val qualityLabel: FairnessQualityLabel = FairnessQualityLabel.HIGH_QUALITY
) {
    companion object {
        fun evaluate(
            evaluation: FairnessEvaluation,
            minScore: Double = FairnessConfig.MINIMUM_OVERALL_FAIRNESS_SCORE,
            maxStrengthDiff: Int = FairnessConfig.MAXIMUM_TEAM_STRENGTH_DIFFERENCE,
            maxTeammatePenalty: Int = FairnessConfig.MAXIMUM_TEAMMATE_PENALTY,
            maxOpponentPenalty: Int = FairnessConfig.MAXIMUM_OPPONENT_PENALTY
        ): FairnessQualityResult {
            val overallScorePassed = evaluation.overallScore >= minScore
            val strengthPassed = evaluation.strengthDifference <= maxStrengthDiff
            val teammatePenaltyPassed = evaluation.teammatePenalty <= maxTeammatePenalty
            val opponentPenaltyPassed = evaluation.opponentPenalty <= maxOpponentPenalty

            val passed = overallScorePassed && strengthPassed && teammatePenaltyPassed && opponentPenaltyPassed

            val failedChecks = mutableListOf<String>()
            if (!overallScorePassed) {
                failedChecks.add("Fairness score below target (${evaluation.overallScore.roundToInt()} < ${minScore.toInt()})")
            }
            if (!strengthPassed) {
                failedChecks.add("Team strength difference above threshold (Diff: ${evaluation.strengthDifference} > $maxStrengthDiff)")
            }
            if (!teammatePenaltyPassed) {
                failedChecks.add("Teammate repetition above threshold (Penalty: ${evaluation.teammatePenalty} > $maxTeammatePenalty)")
            }
            if (!opponentPenaltyPassed) {
                failedChecks.add("Opponent repetition above threshold (Penalty: ${evaluation.opponentPenalty} > $maxOpponentPenalty)")
            }

            val qualityLabel = if (passed) {
                if (evaluation.overallScore >= 85.0) FairnessQualityLabel.HIGH_QUALITY else FairnessQualityLabel.ACCEPTABLE
            } else {
                FairnessQualityLabel.FAILED
            }

            return FairnessQualityResult(
                passed = passed,
                overallScorePassed = overallScorePassed,
                strengthPassed = strengthPassed,
                teammatePenaltyPassed = teammatePenaltyPassed,
                opponentPenaltyPassed = opponentPenaltyPassed,
                failedChecks = failedChecks,
                qualityLabel = qualityLabel
            )
        }
    }
}
