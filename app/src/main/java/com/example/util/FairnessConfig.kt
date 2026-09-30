package com.example.util

object FairnessConfig {
    // Standard default constants preserved for backward compatibility
    const val MINIMUM_OVERALL_FAIRNESS_SCORE: Double = 70.0
    const val MAXIMUM_TEAM_STRENGTH_DIFFERENCE: Int = 2
    const val MAXIMUM_TEAMMATE_PENALTY: Int = 5
    const val MAXIMUM_OPPONENT_PENALTY: Int = 5
    const val MAX_ADDITIONAL_GENERATION_ATTEMPTS: Int = 5

    // Centralized Default Settings
    val DEFAULT_FAIRNESS_SETTINGS = FairnessSettings(
        minimumOverallFairnessScore = 70.0,
        maximumTeamStrengthDifference = 2,
        maximumTeammatePenalty = 5,
        maximumOpponentPenalty = 5,
        maxAdditionalGenerationAttempts = 5,
        fairnessMode = FairnessMode.BALANCED
    )

    // Presets
    val PRESET_RELAXED = FairnessSettings(
        minimumOverallFairnessScore = 50.0,
        maximumTeamStrengthDifference = 4,
        maximumTeammatePenalty = 10,
        maximumOpponentPenalty = 10,
        maxAdditionalGenerationAttempts = 2,
        fairnessMode = FairnessMode.RELAXED
    )

    val PRESET_BALANCED = DEFAULT_FAIRNESS_SETTINGS.copy(
        fairnessMode = FairnessMode.BALANCED
    )

    val PRESET_STRICT = FairnessSettings(
        minimumOverallFairnessScore = 85.0,
        maximumTeamStrengthDifference = 1,
        maximumTeammatePenalty = 2,
        maximumOpponentPenalty = 2,
        maxAdditionalGenerationAttempts = 10,
        fairnessMode = FairnessMode.STRICT
    )

    fun getPreset(mode: FairnessMode): FairnessSettings {
        return when (mode) {
            FairnessMode.RELAXED -> PRESET_RELAXED
            FairnessMode.BALANCED -> PRESET_BALANCED
            FairnessMode.STRICT -> PRESET_STRICT
            FairnessMode.CUSTOM -> DEFAULT_FAIRNESS_SETTINGS.copy(fairnessMode = FairnessMode.CUSTOM)
        }
    }

    /**
     * Determines whether the given threshold configuration matches any established preset,
     * or is a CUSTOM configuration.
     */
    fun detectMode(
        minScore: Double,
        maxStrengthDiff: Int,
        maxTeammatePenalty: Int,
        maxOpponentPenalty: Int,
        maxAttempts: Int
    ): FairnessMode {
        fun matches(preset: FairnessSettings): Boolean {
            return kotlin.math.abs(preset.minimumOverallFairnessScore - minScore) < 0.001 &&
                    preset.maximumTeamStrengthDifference == maxStrengthDiff &&
                    preset.maximumTeammatePenalty == maxTeammatePenalty &&
                    preset.maximumOpponentPenalty == maxOpponentPenalty &&
                    preset.maxAdditionalGenerationAttempts == maxAttempts
        }

        return when {
            matches(PRESET_BALANCED) -> FairnessMode.BALANCED
            matches(PRESET_RELAXED) -> FairnessMode.RELAXED
            matches(PRESET_STRICT) -> FairnessMode.STRICT
            else -> FairnessMode.CUSTOM
        }
    }
}
