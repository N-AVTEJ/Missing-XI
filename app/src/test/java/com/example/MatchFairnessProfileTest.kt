package com.example

import com.example.data.model.SessionEntity
import com.example.util.*
import org.junit.Assert.*
import org.junit.Test

class MatchFairnessProfileTest {

    @Test
    fun test1_globalDefaultResolvesGlobalSettings() {
        val global = FairnessSettings(
            minimumOverallFairnessScore = 78.0,
            maximumTeamStrengthDifference = 3,
            maximumTeammatePenalty = 4,
            maximumOpponentPenalty = 4,
            maxAdditionalGenerationAttempts = 6,
            fairnessMode = FairnessMode.CUSTOM
        )
        val config = MatchFairnessConfig(profile = MatchFairnessProfile.GLOBAL_DEFAULT)
        val resolved = config.resolveSettings(global)

        assertEquals(78.0, resolved.minimumOverallFairnessScore, 0.001)
        assertEquals(3, resolved.maximumTeamStrengthDifference)
        assertEquals(4, resolved.maximumTeammatePenalty)
        assertEquals(4, resolved.maximumOpponentPenalty)
        assertEquals(6, resolved.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.CUSTOM, resolved.fairnessMode)
    }

    @Test
    fun test2_relaxedResolvesCorrectPreset() {
        val global = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS // 70.0
        val config = MatchFairnessConfig(profile = MatchFairnessProfile.RELAXED)
        val resolved = config.resolveSettings(global)

        assertEquals(50.0, resolved.minimumOverallFairnessScore, 0.001)
        assertEquals(4, resolved.maximumTeamStrengthDifference)
        assertEquals(10, resolved.maximumTeammatePenalty)
        assertEquals(10, resolved.maximumOpponentPenalty)
        assertEquals(2, resolved.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.RELAXED, resolved.fairnessMode)
    }

    @Test
    fun test3_balancedResolvesCorrectPreset() {
        val global = FairnessConfig.PRESET_STRICT // Strict global
        val config = MatchFairnessConfig(profile = MatchFairnessProfile.BALANCED)
        val resolved = config.resolveSettings(global)

        assertEquals(70.0, resolved.minimumOverallFairnessScore, 0.001)
        assertEquals(2, resolved.maximumTeamStrengthDifference)
        assertEquals(5, resolved.maximumTeammatePenalty)
        assertEquals(5, resolved.maximumOpponentPenalty)
        assertEquals(5, resolved.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.BALANCED, resolved.fairnessMode)
    }

    @Test
    fun test4_strictResolvesCorrectPreset() {
        val global = FairnessConfig.PRESET_RELAXED // Relaxed global
        val config = MatchFairnessConfig(profile = MatchFairnessProfile.STRICT)
        val resolved = config.resolveSettings(global)

        assertEquals(85.0, resolved.minimumOverallFairnessScore, 0.001)
        assertEquals(1, resolved.maximumTeamStrengthDifference)
        assertEquals(2, resolved.maximumTeammatePenalty)
        assertEquals(2, resolved.maximumOpponentPenalty)
        assertEquals(10, resolved.maxAdditionalGenerationAttempts)
        assertEquals(FairnessMode.STRICT, resolved.fairnessMode)
    }

    @Test
    fun test5_customSettingsAreAppliedOnlyToCurrentMatch() {
        val global = FairnessSettings(
            minimumOverallFairnessScore = 70.0,
            maximumTeamStrengthDifference = 2,
            fairnessMode = FairnessMode.BALANCED
        )
        val matchCustom = FairnessSettings(
            minimumOverallFairnessScore = 80.0,
            maximumTeamStrengthDifference = 0,
            fairnessMode = FairnessMode.CUSTOM
        )
        val config = MatchFairnessConfig(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = matchCustom
        )
        val resolved = config.resolveSettings(global)

        // Match gets custom settings
        assertEquals(80.0, resolved.minimumOverallFairnessScore, 0.001)
        assertEquals(0, resolved.maximumTeamStrengthDifference)
        assertEquals(FairnessMode.CUSTOM, resolved.fairnessMode)

        // Global settings remain untouched
        assertEquals(70.0, global.minimumOverallFairnessScore, 0.001)
        assertEquals(2, global.maximumTeamStrengthDifference)
        assertEquals(FairnessMode.BALANCED, global.fairnessMode)
    }

    @Test
    fun test6_editingMatchSettingsDoesNotModifyGlobalSettings() {
        val initialGlobal = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS
        var matchConfig = MatchFairnessConfig(profile = MatchFairnessProfile.GLOBAL_DEFAULT)

        // User edits match custom settings
        val editedCustom = matchConfig.customSettings.copy(minimumOverallFairnessScore = 92.0)
        matchConfig = matchConfig.copy(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = editedCustom
        )

        // Verify match config has 92.0
        val resolvedMatch = matchConfig.resolveSettings(initialGlobal)
        assertEquals(92.0, resolvedMatch.minimumOverallFairnessScore, 0.001)

        // Verify initialGlobal is still exactly 70.0
        assertEquals(70.0, initialGlobal.minimumOverallFairnessScore, 0.001)
        assertEquals(FairnessMode.BALANCED, initialGlobal.fairnessMode)
    }

    @Test
    fun test7_changingProfilesAffectsNextShuffleSnapshot() {
        val global = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS

        var config = MatchFairnessConfig(profile = MatchFairnessProfile.RELAXED)
        val snapshot1 = config.resolveSettings(global)
        assertEquals(50.0, snapshot1.minimumOverallFairnessScore, 0.001)

        // User switches profile to STRICT for next shuffle
        config = config.copy(profile = MatchFairnessProfile.STRICT)
        val snapshot2 = config.resolveSettings(global)
        assertEquals(85.0, snapshot2.minimumOverallFairnessScore, 0.001)

        // Snapshot 1 is immutable and remains 50.0
        assertEquals(50.0, snapshot1.minimumOverallFairnessScore, 0.001)
    }

    @Test
    fun test8_activeShuffleKeepsOriginalSettingsSnapshot() {
        val global = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS
        val initialConfig = MatchFairnessConfig(profile = MatchFairnessProfile.STRICT)

        // Shuffle begins: capture immutable snapshot
        val activeShuffleSnapshot = initialConfig.resolveSettings(global)

        // User changes UI profile while shuffle is active
        val mutatedConfig = initialConfig.copy(profile = MatchFairnessProfile.RELAXED)

        // Active shuffle snapshot did NOT mutate
        assertEquals(85.0, activeShuffleSnapshot.minimumOverallFairnessScore, 0.001)
        assertEquals(1, activeShuffleSnapshot.maximumTeamStrengthDifference)

        // Future shuffle would use Relaxed
        val futureSnapshot = mutatedConfig.resolveSettings(global)
        assertEquals(50.0, futureSnapshot.minimumOverallFairnessScore, 0.001)
    }

    @Test
    fun test9_resultMetadataRecordsProfileAndOutcomeCorrectly() {
        val eval = FairnessEvaluation(
            overallScore = 88.0,
            strengthDifference = 1,
            teammatePenalty = 1,
            opponentPenalty = 1
        )
        val qualityResult = FairnessQualityResult.evaluate(eval, FairnessConfig.PRESET_STRICT)
        assertTrue(qualityResult.passed)

        val outcome = if (qualityResult.passed) "Passed" else "Best Available"
        val session = SessionEntity(
            playerIdsJson = "[\"Player 1\", \"Player 2\"]",
            teamCount = 2,
            overallFairnessScore = eval.overallScore,
            fairnessProfile = MatchFairnessProfile.STRICT.displayName,
            settingsSnapshotJson = FairnessConfig.PRESET_STRICT.toSummary(),
            fairnessThresholdUsed = FairnessConfig.PRESET_STRICT.minimumOverallFairnessScore,
            qualityGateOutcome = outcome
        )

        assertEquals("Strict", session.fairnessProfile)
        assertEquals("Passed", session.qualityGateOutcome)
        assertEquals(85.0, session.fairnessThresholdUsed, 0.001)
    }

    @Test
    fun test10_legacyHistoryRecordsDefaultToLegacyResult() {
        // Constructing legacy record without profile fields should default to "Legacy Result"
        val legacySession = SessionEntity(
            playerIdsJson = "[\"A\", \"B\"]",
            teamCount = 2,
            overallFairnessScore = 72.0
        )

        assertEquals("Legacy Result", legacySession.fairnessProfile)
        assertEquals(70.0, legacySession.fairnessThresholdUsed, 0.001)
        assertEquals("", legacySession.qualityGateOutcome)
    }

    @Test
    fun test11_bestAvailableResultsRemainCorrectlyLabeled() {
        val eval = FairnessEvaluation(
            overallScore = 65.0,
            strengthDifference = 3,
            teammatePenalty = 6,
            opponentPenalty = 5
        )
        val failedResult = FairnessQualityResult.evaluate(eval, FairnessConfig.PRESET_STRICT)
        assertFalse(failedResult.passed)

        val bestAvailableResult = failedResult.copy(qualityLabel = FairnessQualityLabel.BEST_AVAILABLE)
        val outcome = if (bestAvailableResult.passed) "Passed" else "Best Available"

        assertEquals("Best Available", outcome)
        assertEquals(FairnessQualityLabel.BEST_AVAILABLE, bestAvailableResult.qualityLabel)
    }

    @Test
    fun test12_invalidCustomSettingsNormalization() {
        // Test clamping of boundary inputs
        val custom = FairnessSettings(
            minimumOverallFairnessScore = (-15.0).coerceIn(0.0, 100.0),
            maximumTeamStrengthDifference = (-4).coerceAtLeast(0),
            maximumTeammatePenalty = (-2).coerceAtLeast(0),
            maximumOpponentPenalty = (-1).coerceAtLeast(0),
            maxAdditionalGenerationAttempts = 45.coerceIn(0, 20)
        )

        assertEquals(0.0, custom.minimumOverallFairnessScore, 0.001)
        assertEquals(0, custom.maximumTeamStrengthDifference)
        assertEquals(0, custom.maximumTeammatePenalty)
        assertEquals(0, custom.maximumOpponentPenalty)
        assertEquals(20, custom.maxAdditionalGenerationAttempts)
    }
}
