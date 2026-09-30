package com.example

import com.example.util.*
import org.junit.Assert.*
import org.junit.Test

class FairnessSettingsTest {

    @Test
    fun test1_defaultSettingsMatchExpectedConstants() {
        val defaults = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS
        assertEquals(70.0, defaults.minimumOverallFairnessScore, 0.001)
        assertEquals(2, defaults.maximumTeamStrengthDifference)
        assertEquals(5, defaults.maximumTeammatePenalty)
        assertEquals(5, defaults.maximumOpponentPenalty)
        assertEquals(5, defaults.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.BALANCED, defaults.fairnessMode)
    }

    @Test
    fun test2_presetRelaxedValues() {
        val relaxed = FairnessConfig.PRESET_RELAXED
        assertEquals(50.0, relaxed.minimumOverallFairnessScore, 0.001)
        assertEquals(4, relaxed.maximumTeamStrengthDifference)
        assertEquals(10, relaxed.maximumTeammatePenalty)
        assertEquals(10, relaxed.maximumOpponentPenalty)
        assertEquals(2, relaxed.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.RELAXED, relaxed.fairnessMode)
    }

    @Test
    fun test3_presetStrictValues() {
        val strict = FairnessConfig.PRESET_STRICT
        assertEquals(85.0, strict.minimumOverallFairnessScore, 0.001)
        assertEquals(1, strict.maximumTeamStrengthDifference)
        assertEquals(2, strict.maximumTeammatePenalty)
        assertEquals(2, strict.maximumOpponentPenalty)
        assertEquals(10, strict.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.STRICT, strict.fairnessMode)
    }

    @Test
    fun test4_getPresetHelperReturnsCorrectSettings() {
        assertEquals(FairnessConfig.PRESET_RELAXED, FairnessConfig.getPreset(FairnessMode.RELAXED))
        assertEquals(FairnessConfig.PRESET_BALANCED, FairnessConfig.getPreset(FairnessMode.BALANCED))
        assertEquals(FairnessConfig.PRESET_STRICT, FairnessConfig.getPreset(FairnessMode.STRICT))
        
        val customPreset = FairnessConfig.getPreset(FairnessMode.CUSTOM)
        assertEquals(FairnessMode.CUSTOM, customPreset.fairnessMode)
    }

    @Test
    fun test5_detectModeIdentifiesStandardPresets() {
        assertEquals(
            FairnessMode.BALANCED,
            FairnessConfig.detectMode(70.0, 2, 5, 5, 5)
        )
        assertEquals(
            FairnessMode.RELAXED,
            FairnessConfig.detectMode(50.0, 4, 10, 10, 2)
        )
        assertEquals(
            FairnessMode.STRICT,
            FairnessConfig.detectMode(85.0, 1, 2, 2, 10)
        )
    }

    @Test
    fun test6_detectModeIdentifiesCustomWhenThresholdsAreModified() {
        // Minimum score changed from 70 to 75
        assertEquals(
            FairnessMode.CUSTOM,
            FairnessConfig.detectMode(75.0, 2, 5, 5, 5)
        )

        // Strength diff changed from 2 to 3
        assertEquals(
            FairnessMode.CUSTOM,
            FairnessConfig.detectMode(70.0, 3, 5, 5, 5)
        )

        // Teammate penalty changed from 5 to 1
        assertEquals(
            FairnessMode.CUSTOM,
            FairnessConfig.detectMode(70.0, 2, 1, 5, 5)
        )

        // Opponent penalty changed from 5 to 8
        assertEquals(
            FairnessMode.CUSTOM,
            FairnessConfig.detectMode(70.0, 2, 5, 8, 5)
        )

        // Additional attempts changed from 5 to 7
        assertEquals(
            FairnessMode.CUSTOM,
            FairnessConfig.detectMode(70.0, 2, 5, 5, 7)
        )
    }

    @Test
    fun test7_qualityGateEvaluationWithRelaxedSettings() {
        val eval = FairnessEvaluation(
            overallScore = 65.0, // Below Balanced (70), above Relaxed (50)
            strengthDifference = 3, // Above Balanced (2), within Relaxed (4)
            teammatePenalty = 7, // Above Balanced (5), within Relaxed (10)
            opponentPenalty = 6  // Above Balanced (5), within Relaxed (10)
        )

        val balancedResult = FairnessQualityResult.evaluate(eval, FairnessConfig.PRESET_BALANCED)
        assertFalse("Should fail under BALANCED preset", balancedResult.passed)

        val relaxedResult = FairnessQualityResult.evaluate(eval, FairnessConfig.PRESET_RELAXED)
        assertTrue("Should pass under RELAXED preset", relaxedResult.passed)
    }

    @Test
    fun test8_qualityGateEvaluationWithStrictSettings() {
        val eval = FairnessEvaluation(
            overallScore = 80.0, // Passes Balanced (>=70), but fails Strict (>=85)
            strengthDifference = 2, // Passes Balanced (<=2), but fails Strict (<=1)
            teammatePenalty = 3, // Passes Balanced (<=5), but fails Strict (<=2)
            opponentPenalty = 3  // Passes Balanced (<=5), but fails Strict (<=2)
        )

        val balancedResult = FairnessQualityResult.evaluate(eval, FairnessConfig.PRESET_BALANCED)
        assertTrue("Should pass under BALANCED preset", balancedResult.passed)

        val strictResult = FairnessQualityResult.evaluate(eval, FairnessConfig.PRESET_STRICT)
        assertFalse("Should fail under STRICT preset", strictResult.passed)
        assertEquals(4, strictResult.failedChecks.size)
    }

    @Test
    fun test9_fairnessSettingsSummaryFormat() {
        val settings = FairnessSettings(
            minimumOverallFairnessScore = 75.0,
            maximumTeamStrengthDifference = 3,
            maximumTeammatePenalty = 6,
            maximumOpponentPenalty = 4,
            maxAdditionalGenerationAttempts = 8,
            fairnessMode = FairnessMode.CUSTOM
        )
        val summary = settings.toSummary()
        assertTrue(summary.contains("Min Score: 75"))
        assertTrue(summary.contains("Max Diff: 3"))
        assertTrue(summary.contains("Max TM Pen: 6"))
        assertTrue(summary.contains("Max Opp Pen: 4"))
        assertTrue(summary.contains("Retries: 8"))
    }
}
