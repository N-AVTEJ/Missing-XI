package com.example.util

enum class FairnessMode {
    RELAXED,
    BALANCED,
    STRICT,
    CUSTOM
}

data class FairnessSettings(
    val minimumOverallFairnessScore: Double = 70.0,
    val maximumTeamStrengthDifference: Int = 2,
    val maximumTeammatePenalty: Int = 5,
    val maximumOpponentPenalty: Int = 5,
    val maxAdditionalGenerationAttempts: Int = 5,
    val fairnessMode: FairnessMode = FairnessMode.BALANCED
) {
    fun toSummary(): String {
        return "Min Score: ${minimumOverallFairnessScore.toInt()} | Max Diff: $maximumTeamStrengthDifference | Max TM Pen: $maximumTeammatePenalty | Max Opp Pen: $maximumOpponentPenalty | Retries: $maxAdditionalGenerationAttempts"
    }
}
