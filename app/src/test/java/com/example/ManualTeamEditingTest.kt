package com.example

import com.example.data.model.SessionEntity
import com.example.util.*
import org.junit.Assert.*
import org.junit.Test

class ManualTeamEditingTest {

    private fun sampleEightPlayers(): List<Player> = listOf(
        Player("p1", "Rahul", 8),
        Player("p2", "Kiran", 6),
        Player("p3", "Varun", 5),
        Player("p4", "Rohit", 5),
        Player("p5", "Arjun", 8),
        Player("p6", "Naveen", 6),
        Player("p7", "Sai", 5),
        Player("p8", "Ajay", 5)
    )

    private fun sampleNinePlayers(): List<Player> =
        sampleEightPlayers() + Player("p9", "Vikram", 7)

    private fun buildTeam(teamNumber: Int, name: String, players: List<Player>): GeneratedTeam {
        val str = TeamStrengthEngine.calculateTeamStrength(players)
        return GeneratedTeam(
            teamNumber = teamNumber,
            name = name,
            players = players.map { it.name },
            playerIds = players.map { it.id },
            totalStrength = str.totalStrength,
            averageStrength = str.averageStrength,
            maxPlayerRating = str.maxPlayerRating,
            minPlayerRating = str.minPlayerRating
        )
    }

    private fun createManager(
        players: List<Player> = sampleEightPlayers(),
        teamAIds: List<String> = listOf("p1", "p2", "p3", "p4"),
        teamBIds: List<String> = listOf("p5", "p6", "p7", "p8"),
        joker: Player? = null,
        constraints: Map<String, PlayerMatchConstraint> = emptyMap(),
        fixedGroups: List<LockedPlayerGroup> = emptyList(),
        baselineTeammatePairs: Map<String, Int> = emptyMap(),
        baselineOpponentPairs: Map<String, Int> = emptyMap(),
        baselineSignatures: Set<String> = emptySet(),
        fairnessSettings: FairnessSettings = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS,
        allowTempImbalance: Boolean = false
    ): ManualTeamEditManager {
        val map = players.associateBy { it.id }
        val teamA = buildTeam(1, "Team A", teamAIds.mapNotNull { map[it] })
        val teamB = buildTeam(2, "Team B", teamBIds.mapNotNull { map[it] })
        val excluded = players.filter { constraints[it.id]?.isExcluded == true }
        val arrangement = TeamArrangement(
            teams = listOf(teamA, teamB),
            joker = joker?.name,
            jokerId = joker?.id,
            excludedPlayers = excluded
        )
        val matchConstraints = MatchConstraints(
            allPlayers = players,
            numTeams = 2,
            playerConstraints = constraints,
            fixedGroups = fixedGroups,
            fairnessSettings = fairnessSettings,
            allowTemporaryImbalance = allowTempImbalance
        )
        return ManualTeamEditManager(
            initialArrangement = arrangement,
            baseConstraints = matchConstraints,
            baselineTeammatePairCounts = baselineTeammatePairs,
            baselineOpponentPairCounts = baselineOpponentPairs,
            baselinePreviousSignatures = baselineSignatures,
            baselineCycleJokers = emptyList()
        )
    }

    @Test
    fun test01_movePlayer_succeedsWhenBalancedAndPreservesSkillRating() {
        // 9 players, 2 teams (5 vs 4 -> moving from Team A to Team B produces 4 vs 5, still balanced!)
        val players = sampleNinePlayers()
        val manager = createManager(
            players = players,
            teamAIds = listOf("p1", "p2", "p3", "p4", "p9"),
            teamBIds = listOf("p5", "p6", "p7", "p8")
        )

        val res = manager.movePlayer("p9", "team_2")
        assertTrue("Expected balanced 5->4 move to succeed", res.success)

        val state = manager.getState()
        val teamA = state.currentArrangement.teams[0]
        val teamB = state.currentArrangement.teams[1]

        assertFalse("p9 should be removed from Team A", "p9" in teamA.playerIds)
        assertTrue("p9 should be added to Team B", "p9" in teamB.playerIds)
        assertEquals(4, teamA.playerIds.size)
        assertEquals(5, teamB.playerIds.size)
        // Skill rating preserved (Vikram = 7; Team B was 8+6+5+5=24, now 24+7=31)
        assertEquals(24, teamA.totalStrength)
        assertEquals(31, teamB.totalStrength)
        assertTrue(state.hasUnsavedChanges)

        // Nonexistent team rejected
        val badDest = manager.movePlayer("p1", "team_99")
        assertFalse(badDest.success)
        assertTrue(badDest.message!!.contains("does not exist"))
    }

    @Test
    fun test02_movePlayer_respectsTemporaryImbalancePolicy() {
        // 8 players, 2 teams (4 vs 4). Default policy blocks 3 vs 5 imbalance.
        val manager = createManager()
        val blocked = manager.movePlayer("p1", "Team B")
        assertFalse("Default policy should block unbalanced 3 vs 5 move", blocked.success)
        assertTrue(blocked.message!!.contains("unbalanced", ignoreCase = true))
        assertFalse("Invalid move must not enter undo history", manager.getState().canUndo)

        // Enable temporary imbalance -> multi-step move works, then balancing move restores valid sizes
        manager.setAllowTemporaryImbalance(true)
        val step1 = manager.movePlayer("p1", "Team B")
        assertTrue("Move should succeed when temporary imbalance is enabled", step1.success)
        assertEquals(3, manager.getState().currentArrangement.teams[0].playerIds.size)
        assertEquals(5, manager.getState().currentArrangement.teams[1].playerIds.size)
        // Strict confirmation still blocks unbalanced teams!
        assertFalse(manager.validateForConfirmation().valid)
        assertNull(manager.buildConfirmedResult())

        // Complete second move to re-balance (4 vs 4)
        val step2 = manager.movePlayer("p5", "Team A")
        assertTrue(step2.success)
        assertEquals(4, manager.getState().currentArrangement.teams[0].playerIds.size)
        assertEquals(4, manager.getState().currentArrangement.teams[1].playerIds.size)
        assertTrue(manager.validateForConfirmation().valid)
    }

    @Test
    fun test03_swapPlayers_atomicSwapBetweenTeams() {
        val manager = createManager()
        val swap = manager.swapPlayers("p1", "p5") // Rahul <-> Arjun
        assertTrue(swap.success)

        val state = manager.getState()
        val teamA = state.currentArrangement.teams[0]
        val teamB = state.currentArrangement.teams[1]

        assertTrue("Arjun (p5) should now be on Team A", "p5" in teamA.playerIds)
        assertFalse("Rahul (p1) should no longer be on Team A", "p1" in teamA.playerIds)
        assertTrue("Rahul (p1) should now be on Team B", "p1" in teamB.playerIds)
        assertFalse("Arjun (p5) should no longer be on Team B", "p5" in teamB.playerIds)
        assertEquals(4, teamA.playerIds.size)
        assertEquals(4, teamB.playerIds.size)
        assertTrue(state.validationResult.valid)
    }

    @Test
    fun test04_preventDuplicatePlayerAssignment() {
        val players = sampleEightPlayers()
        val map = players.associateBy { it.id }
        // Manually construct an arrangement with p1 duplicated in both Team A and Team B
        val badTeamA = buildTeam(1, "Team A", listOf(map["p1"]!!, map["p2"]!!, map["p3"]!!, map["p4"]!!))
        val badTeamB = buildTeam(2, "Team B", listOf(map["p1"]!!, map["p6"]!!, map["p7"]!!, map["p8"]!!))
        val result = ManualArrangementValidator.validateManualArrangement(
            arrangement = TeamArrangement(listOf(badTeamA, badTeamB)),
            constraints = MatchConstraints(allPlayers = players, numTeams = 2)
        )
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("multiple teams", ignoreCase = true) })

        // Same-team move via manager is also rejected
        val manager = createManager()
        val sameTeamRes = manager.movePlayer("p1", "Team A")
        assertFalse(sameTeamRes.success)
        assertTrue(sameTeamRes.message!!.contains("already on", ignoreCase = true))
    }

    @Test
    fun test05_preventMovementOrSwapOfLockedPlayers() {
        val constraints = mapOf(
            "p1" to PlayerMatchConstraint("p1", isLocked = true, lockedTeamIndex = 0)
        )
        val manager = createManager(constraints = constraints, allowTempImbalance = true)

        val moveRes = manager.movePlayer("p1", "Team B")
        assertFalse("Locked player must not be movable", moveRes.success)
        assertTrue(moveRes.message!!.contains("locked to Team A", ignoreCase = true))

        val swapRes = manager.swapPlayers("p1", "p5")
        assertFalse("Locked player must not be swappable", swapRes.success)
        assertTrue(swapRes.message!!.contains("locked to Team A", ignoreCase = true))

        assertFalse("Rejected lock operations must not enter undo history", manager.getState().canUndo)
    }

    @Test
    fun test06_preserveFixedGroups() {
        val group = LockedPlayerGroup(id = "g1", name = "Striker Duo", playerIds = listOf("p1", "p2"))
        val manager = createManager(fixedGroups = listOf(group), allowTempImbalance = true)

        val moveRes = manager.movePlayer("p1", "Team B")
        assertFalse("Individual member of fixed group cannot be moved alone", moveRes.success)
        assertTrue(moveRes.message!!.contains("Striker Duo"))

        val swapRes = manager.swapPlayers("p2", "p6")
        assertFalse("Individual member of fixed group cannot be swapped alone", swapRes.success)
        assertTrue(swapRes.message!!.contains("Striker Duo"))

        // Validator also catches split group directly
        val players = sampleEightPlayers()
        val map = players.associateBy { it.id }
        val splitArrangement = TeamArrangement(
            teams = listOf(
                buildTeam(1, "Team A", listOf(map["p1"]!!, map["p5"]!!, map["p3"]!!, map["p4"]!!)),
                buildTeam(2, "Team B", listOf(map["p2"]!!, map["p6"]!!, map["p7"]!!, map["p8"]!!))
            )
        )
        val valRes = ManualArrangementValidator.validateManualArrangement(
            arrangement = splitArrangement,
            constraints = MatchConstraints(allPlayers = players, numTeams = 2, fixedGroups = listOf(group))
        )
        assertFalse(valRes.valid)
        assertTrue(valRes.errors.any { it.contains("Striker Duo") })
    }

    @Test
    fun test07_preventExcludedPlayersFromJoiningTeams() {
        val players = sampleNinePlayers()
        val constraints = mapOf(
            "p9" to PlayerMatchConstraint("p9", isExcluded = true)
        )
        val manager = createManager(
            players = players,
            teamAIds = listOf("p1", "p2", "p3", "p4"),
            teamBIds = listOf("p5", "p6", "p7", "p8"),
            constraints = constraints,
            allowTempImbalance = true
        )

        val moveExcluded = manager.movePlayer("p9", "Team A")
        assertFalse(moveExcluded.success)
        assertTrue(moveExcluded.message!!.contains("excluded", ignoreCase = true))

        val swapExcluded = manager.swapPlayers("p9", "p1")
        assertFalse(swapExcluded.success)
        assertTrue(swapExcluded.message!!.contains("excluded", ignoreCase = true))

        // Direct validator check when excluded player is injected into a team
        val map = players.associateBy { it.id }
        val badArr = TeamArrangement(
            teams = listOf(
                buildTeam(1, "Team A", listOf(map["p9"]!!, map["p2"]!!, map["p3"]!!, map["p4"]!!)),
                buildTeam(2, "Team B", listOf(map["p5"]!!, map["p6"]!!, map["p7"]!!, map["p8"]!!))
            )
        )
        val valRes = ManualArrangementValidator.validateManualArrangement(
            arrangement = badArr,
            constraints = MatchConstraints(allPlayers = players, numTeams = 2, playerConstraints = constraints)
        )
        assertFalse(valRes.valid)
        assertTrue(valRes.errors.any { it.contains("Excluded player", ignoreCase = true) })
    }

    @Test
    fun test08_undoRedoAndClearingRedoStackOnNewEdit() {
        val manager = createManager()
        val originalSig = manager.getState().currentArrangement.signature

        // Edit 1: swap p1 <-> p5
        assertTrue(manager.swapPlayers("p1", "p5").success)
        val afterEdit1Sig = manager.getState().currentArrangement.signature
        assertEquals(1, manager.getState().undoCount)
        assertEquals(0, manager.getState().redoCount)

        // Edit 2: swap p2 <-> p6
        assertTrue(manager.swapPlayers("p2", "p6").success)
        val afterEdit2Sig = manager.getState().currentArrangement.signature
        assertEquals(2, manager.getState().undoCount)
        assertFalse(manager.getState().canRedo)

        // Undo -> back to Edit 1
        assertTrue(manager.undo().success)
        assertEquals(afterEdit1Sig, manager.getState().currentArrangement.signature)
        assertEquals(1, manager.getState().undoCount)
        assertEquals(1, manager.getState().redoCount)
        assertTrue(manager.getState().canRedo)

        // Redo -> back to Edit 2
        assertTrue(manager.redo().success)
        assertEquals(afterEdit2Sig, manager.getState().currentArrangement.signature)
        assertEquals(2, manager.getState().undoCount)
        assertEquals(0, manager.getState().redoCount)

        // Undo twice -> back to Original
        assertTrue(manager.undo().success)
        assertTrue(manager.undo().success)
        assertEquals(originalSig, manager.getState().currentArrangement.signature)
        assertEquals(2, manager.getState().redoCount)

        // New edit after undo clears redo stack!
        assertTrue(manager.swapPlayers("p3", "p7").success)
        assertEquals(0, manager.getState().redoCount)
        assertFalse(manager.getState().canRedo)
        assertEquals(1, manager.getState().undoCount)
    }

    @Test
    fun test09_resetChangesRestoresOriginalArrangement() {
        val manager = createManager()
        val originalTeams = manager.getState().originalArrangement.teams
        val originalScore = manager.getState().originalFairness.fairnessScore

        assertTrue(manager.swapPlayers("p1", "p7").success) // Strong (8) <-> Weak (5)
        assertTrue(manager.getState().hasUnsavedChanges)

        val resetRes = manager.resetChanges()
        assertTrue(resetRes.success)

        val state = manager.getState()
        assertEquals(originalTeams, state.currentArrangement.teams)
        assertEquals(originalScore, state.currentFairness.fairnessScore)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun test10_fairnessRecalculatesAndEmitsWarningsReadOnly() {
        // Historical pair count: p1 & p6 played together 3 times previously!
        val historicalTeammates = mutableMapOf(
            TeammatePairTracker.createPairKey("p1", "p6") to 3
        )
        val historicalOpponents = mutableMapOf<String, Int>()
        val manager = createManager(
            baselineTeammatePairs = historicalTeammates,
            baselineOpponentPairs = historicalOpponents
        )

        val beforeState = manager.getState()
        assertEquals(0, beforeState.originalFairness.strengthAnalysis.strengthDifference)
        assertEquals(100, beforeState.originalFairness.fairnessScore)

        // Swap p1 (rating 8) with p7 (rating 5):
        // Team A becomes [p7(5), p2(6), p3(5), p4(5)] = 21
        // Team B becomes [p5(8), p6(6), p1(8), p8(5)] = 27 -> Diff = 6!
        // AND p1 + p6 are now teammates -> repeated teammate penalty!
        assertTrue(manager.swapPlayers("p1", "p7").success)

        val afterState = manager.getState()
        assertEquals(6, afterState.currentFairness.strengthAnalysis.strengthDifference)
        assertTrue("Teammate penalty should increase", afterState.currentFairness.evaluation.teammatePenalty > 0)
        assertTrue("Overall fairness score should decrease", afterState.currentFairness.fairnessScore < beforeState.originalFairness.fairnessScore)
        assertTrue("Warnings should report score drop", afterState.currentFairness.fairnessWarnings.any { it.contains("Fairness score decreased") })
        assertTrue("Warnings should report strength difference", afterState.currentFairness.fairnessWarnings.any { it.contains("Team Strength Difference: 6") })
        assertTrue("Warnings should report repeated teammate combination", afterState.currentFairness.fairnessWarnings.any { it.contains("repeated teammate") })

        // Verify read-only: baseline history maps were NOT mutated!
        assertEquals(1, historicalTeammates.size)
        assertEquals(3, historicalTeammates[TeammatePairTracker.createPairKey("p1", "p6")])
        assertTrue(historicalOpponents.isEmpty())

        // Non-blocking: user can still confirm valid arrangement even with lower fairness score
        val confirmed = manager.buildConfirmedResult()
        assertNotNull("Fairness warnings must not block valid confirmation", confirmed)
        assertTrue(confirmed!!.validationResult.valid)
        assertTrue(confirmed.validationResult.warnings.isNotEmpty())
    }

    @Test
    fun test11_hardValidationErrorsForJokerUnknownAndMissingPlayers() {
        val players = sampleNinePlayers() // 9 players
        val map = players.associateBy { it.id }

        // 1. Player assigned both to Team A and as Joker
        val overlapArr = TeamArrangement(
            teams = listOf(
                buildTeam(1, "Team A", listOf(map["p1"]!!, map["p2"]!!, map["p3"]!!, map["p4"]!!)),
                buildTeam(2, "Team B", listOf(map["p5"]!!, map["p6"]!!, map["p7"]!!, map["p8"]!!))
            ),
            joker = "Rahul",
            jokerId = "p1"
        )
        val overlapRes = ManualArrangementValidator.validateManualArrangement(
            arrangement = overlapArr,
            constraints = MatchConstraints(allPlayers = players, numTeams = 2)
        )
        assertFalse(overlapRes.valid)
        assertTrue(overlapRes.errors.any { it.contains("both to Team A and as Joker") })
        assertTrue(overlapRes.errors.any { it.contains("missing from arrangement") }) // p9 missing

        // 2. Unknown player ID
        val unknownTeam = GeneratedTeam(
            teamNumber = 1,
            name = "Team A",
            players = listOf("Ghost", "Kiran", "Varun", "Rohit"),
            playerIds = listOf("unknown_99", "p2", "p3", "p4")
        )
        val unknownRes = ManualArrangementValidator.validateManualArrangement(
            arrangement = TeamArrangement(
                teams = listOf(unknownTeam, buildTeam(2, "Team B", listOf(map["p5"]!!, map["p6"]!!, map["p7"]!!, map["p8"]!!)))
            ),
            constraints = MatchConstraints(allPlayers = sampleEightPlayers(), numTeams = 2)
        )
        assertFalse(unknownRes.valid)
        assertTrue(unknownRes.errors.any { it.contains("Unknown player ID") })
    }

    @Test
    fun test12_duplicateArrangementDetection() {
        val players = sampleEightPlayers()
        val map = players.associateBy { it.id }

        // Suppose an earlier accepted shuffle already used [Arjun, Kiran, Varun, Rohit] vs [Rahul, Naveen, Sai, Ajay]
        val previousTeamA = buildTeam(1, "Team A", listOf(map["p5"]!!, map["p2"]!!, map["p3"]!!, map["p4"]!!))
        val previousTeamB = buildTeam(2, "Team B", listOf(map["p1"]!!, map["p6"]!!, map["p7"]!!, map["p8"]!!))
        val previousSig = ArrangementSignatureUtil.generateSignature(listOf(previousTeamA, previousTeamB), null)

        val manager = createManager(baselineSignatures = setOf(previousSig))
        assertFalse(manager.getState().isDuplicateArrangement)

        // Swap Rahul (p1) <-> Arjun (p5) -> matches previousSig!
        assertTrue(manager.swapPlayers("p1", "p5").success)

        val state = manager.getState()
        assertTrue("Duplicate arrangement must be detected", state.isDuplicateArrangement)
        assertTrue(state.currentFairness.fairnessWarnings.contains("This arrangement has already been used."))

        // Confirmation still allowed, and unique set size does not falsely grow
        val confirmed = manager.buildConfirmedResult()
        assertNotNull(confirmed)
        assertTrue(confirmed!!.isDuplicateArrangement)
        val updatedSigs = setOf(previousSig) + confirmed.finalArrangement.signature
        assertEquals(1, updatedSigs.size)
    }

    @Test
    fun test13_confirmationAndSingleHistoryPersistenceIntegrity() {
        val baselineTeammates = mapOf(TeammatePairTracker.createPairKey("p1", "p2") to 1)
        val baselineOpponents = mapOf(TeammatePairTracker.createPairKey("p1", "p5") to 1)
        val manager = createManager(
            baselineTeammatePairs = baselineTeammates,
            baselineOpponentPairs = baselineOpponents
        )

        // Perform edit: swap p1 <-> p5
        assertTrue(manager.swapPlayers("p1", "p5").success)
        val confirmed = manager.buildConfirmedResult()
        assertNotNull(confirmed)
        assertTrue(confirmed!!.wasModified)

        // Simulate finalization from baseline (exactly once, using final teams)
        val finalTeamIds = confirmed.finalArrangement.teams.map { it.playerIds }
        val updatedTeammates = TeammatePairTracker.updateTeammatePairCounts(baselineTeammates, finalTeamIds)
        val updatedOpponents = OpponentPairTracker.updateOpponentHistory(baselineOpponents, finalTeamIds)

        // p1 is no longer with p2 in the final teams -> count stays 1 (not 2!)
        assertEquals(1, updatedTeammates[TeammatePairTracker.createPairKey("p1", "p2")])
        // p5 is now with p2 in the final teams -> count becomes 1
        assertEquals(1, updatedTeammates[TeammatePairTracker.createPairKey("p5", "p2")])
        // p1 and p5 are still opponents (swapped sides) -> count becomes 2
        assertEquals(2, updatedOpponents[TeammatePairTracker.createPairKey("p1", "p5")])

        // Verify SessionEntity persists manual edit metadata cleanly
        val entity = SessionEntity(
            id = 42,
            playerIdsJson = "[\"Arjun\",\"Kiran\",\"Varun\",\"Rohit\",\"Rahul\",\"Naveen\",\"Sai\",\"Ajay\"]",
            teamCount = 2,
            overallFairnessScore = confirmed.finalFairness.evaluation.overallScore,
            wasManuallyEdited = confirmed.wasModified,
            originalArrangementJson = "{\"teams\":[]}"
        )
        assertTrue(entity.wasManuallyEdited)
        assertEquals("{\"teams\":[]}", entity.originalArrangementJson)
    }

    @Test
    fun test14_undoHistoryRespectsMaxLimit() {
        val manager = createManager()
        // Perform 60 alternating swaps (exceeding MAX_HISTORY_SIZE = 50)
        repeat(60) {
            assertTrue(manager.swapPlayers("p1", "p5").success)
        }
        assertEquals(ManualTeamEditManager.MAX_HISTORY_SIZE, manager.getState().undoCount)
    }
}
