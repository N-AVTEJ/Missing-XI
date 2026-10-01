package com.example.util

enum class MatchFairnessProfile(val displayName: String, val description: String) {
    GLOBAL_DEFAULT(
        "Global Default",
        "Uses current global fairness settings from Settings screen"
    ),
    RELAXED(
        "Relaxed",
        "Faster team generation with flexible fairness thresholds"
    ),
    BALANCED(
        "Balanced",
        "Recommended standard balancing team strength and player variety"
    ),
    STRICT(
        "Strict",
        "Maximum fairness enforcement with strict parity limits"
    ),
    CUSTOM(
        "Custom",
        "Customized thresholds specific to this match"
    )
}

data class MatchFairnessConfig(
    val profile: MatchFairnessProfile = MatchFairnessProfile.GLOBAL_DEFAULT,
    val customSettings: FairnessSettings = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS.copy(fairnessMode = FairnessMode.CUSTOM)
) {
    /**
     * Resolves the immutable FairnessSettings snapshot for this match profile.
     * Uses the provided globalSettings when profile is GLOBAL_DEFAULT.
     * Never mutates globalSettings.
     */
    fun resolveSettings(globalSettings: FairnessSettings): FairnessSettings {
        return when (profile) {
            MatchFairnessProfile.GLOBAL_DEFAULT -> globalSettings
            MatchFairnessProfile.RELAXED -> FairnessConfig.PRESET_RELAXED
            MatchFairnessProfile.BALANCED -> FairnessConfig.PRESET_BALANCED
            MatchFairnessProfile.STRICT -> FairnessConfig.PRESET_STRICT
            MatchFairnessProfile.CUSTOM -> customSettings.copy(fairnessMode = FairnessMode.CUSTOM)
        }
    }
}
