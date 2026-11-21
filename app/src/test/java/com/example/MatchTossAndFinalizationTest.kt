package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.SessionEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import com.example.util.GeneratedTeam
import com.example.util.MatchStatus
import com.example.util.MatchTossCoordinator
import com.example.util.MatchTossEngine
import com.example.util.TossAttemptOutcome
import com.example.util.TossChoice
import com.example.util.TossStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchTossAndFinalizationTest {

    private lateinit var context: Context
    private lateinit var dbName: String
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var coordinator: MatchTossCoordinator

    private val sampleTeams = listOf(
        GeneratedTeam(
            teamNumber = 1,
            name = "Team A",
            players = listOf("Rahul", "Arjun", "Kiran"),
            playerIds = listOf("p1", "p2", "p3"),
            totalStrength = 21,
            averageStrength = 7.0,
            maxPlayerRating = 8,
            minPlayerRating = 6
        ),
        GeneratedTeam(
            teamNumber = 2,
            name = "Team B",
            players = listOf("Vikram", "Dev", "Rohan"),
            playerIds = listOf("p4", "p5", "p6"),
            totalStrength = 20,
            averageStrength = 6.67,
            maxPlayerRating = 8,
            minPlayerRating = 6
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "test_toss_${UUID.randomUUID()}.db"
        db = openDiskDatabase(dbName)
        repository = createRepository(db)
        coordinator = MatchTossCoordinator(repository)
    }

    @After
    fun tearDown() {
        if (db.isOpen) {
            db.close()
        }
        context.deleteDatabase(dbName)
    }

    private fun openDiskDatabase(name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_8_9)
            .build()
    }

    private fun createRepository(database: AppDatabase): AppRepository {
        return AppRepository(
            lineupDao = database.lineupDao(),
            tossDao = database.tossDao(),
            playerDao = database.playerDao(),
            sessionDao = database.sessionDao()
        )
    }

    private suspend fun seedAndConfirmMatch(
        fairnessScore: Double = 88.5,
        fairnessRating: String = "Excellent",
        profile: String = "Balanced"
    ): String {
        val matchId = UUID.randomUUID().toString()
        coordinator.onTeamsGenerated(matchId)
        val rowId = repository.insertSession(
            SessionEntity(
                playerIdsJson = "[\"Rahul\",\"Arjun\",\"Kiran\",\"Vikram\",\"Dev\",\"Rohan\"]",
                teamCount = 2,
                timestamp = 1_700_000_000_000L,
                overallFairnessScore = fairnessScore,
                fairnessRating = fairnessRating,
                teammateVarietyScore = 90.0,
                opponentVarietyScore = 86.0,
                teamStrengthScore = 92.0,
                jokerFairnessScore = 100.0,
                fairnessProfile = profile,
                qualityGateOutcome = "Passed",
                matchId = matchId,
                matchStatus = MatchStatus.TEAMS_GENERATED.name,
                finalTeamsJson = MatchTossEngine.serializeTeamsJson(sampleTeams, null)
            )
        ).toInt()

        val confirmed = coordinator.confirmTeamsForToss(
            matchId = matchId,
            dbSessionId = rowId,
            teams = sampleTeams,
            joker = null
        )
        assertTrue(confirmed.isSuccess)
        return matchId
    }

    @Test
    fun `1_heads_and_tails_selection_starts_unselected_and_is_mutually_exclusive`() = runBlocking {
        seedAndConfirmMatch()

        assertNull(coordinator.tossUiState.value.selectedChoice)

        coordinator.selectChoice(TossChoice.HEADS)
        assertEquals(TossChoice.HEADS, coordinator.tossUiState.value.selectedChoice)

        coordinator.selectChoice(TossChoice.TAILS)
        assertEquals(TossChoice.TAILS, coordinator.tossUiState.value.selectedChoice)
    }

    @Test
    fun `2_flip_coin_rejected_until_choice_is_selected`() = runBlocking {
        seedAndConfirmMatch()

        val result = coordinator.executeCoinFlip(animationDurationMs = 0L)
        assertTrue(result.isFailure)
        assertEquals(TossStatus.NOT_STARTED, coordinator.tossUiState.value.tossStatus)
        assertNotNull(coordinator.tossUiState.value.errorMessage)
    }

    @Test
    fun `3_random_coin_flip_has_fair_50_50_boundaries_and_distribution`() {
        assertEquals(TossChoice.HEADS, MatchTossEngine.flipCoin { 0.0 })
        assertEquals(TossChoice.HEADS, MatchTossEngine.flipCoin { 0.499999 })
        assertEquals(TossChoice.TAILS, MatchTossEngine.flipCoin { 0.5 })
        assertEquals(TossChoice.TAILS, MatchTossEngine.flipCoin { 0.999999 })

        var headsCount = 0
        val trials = 10_000
        repeat(trials) {
            if (MatchTossEngine.flipCoin() == TossChoice.HEADS) headsCount++
        }
        val headsRatio = headsCount.toDouble() / trials
        assertTrue("Expected near 50/50 split, got $headsRatio", headsRatio in 0.45..0.55)
    }

    @Test
    fun `4_random_result_is_generated_once_per_toss_attempt_before_animation`() = runBlocking {
        seedAndConfirmMatch()
        coordinator.selectChoice(TossChoice.HEADS)

        var randomInvocations = 0
        val flipResult = coordinator.executeCoinFlip(
            randomProvider = {
                randomInvocations++
                0.2 // HEADS
            },
            animationDurationMs = 0L
        )

        assertTrue(flipResult.isSuccess)
        assertEquals(1, randomInvocations)
        assertEquals(TossChoice.HEADS, flipResult.getOrNull()?.result)
    }

    @Test
    fun `5_animation_target_face_matches_final_saved_result`() = runBlocking {
        seedAndConfirmMatch()
        coordinator.selectChoice(TossChoice.TAILS)

        val flipJob = async {
            coordinator.executeCoinFlip(
                randomProvider = { 0.75 }, // TAILS
                animationDurationMs = 80L
            )
        }
        delay(20L)

        // While animating, pendingResult is already locked to TAILS so the 3D coin settles on TAILS (180 deg)
        assertEquals(TossStatus.IN_PROGRESS, coordinator.tossUiState.value.tossStatus)
        assertEquals(TossChoice.TAILS, coordinator.tossUiState.value.pendingResult)

        val completed = flipJob.await().getOrThrow()
        assertEquals(TossChoice.TAILS, completed.result)
        assertEquals(TossChoice.TAILS, coordinator.tossUiState.value.latestTossResult?.result)
        assertEquals(TossChoice.TAILS, coordinator.tossUiState.value.pendingResult)
    }

    @Test
    fun `6_duplicate_taps_during_animation_are_prevented`() = runBlocking {
        val matchId = seedAndConfirmMatch()
        coordinator.selectChoice(TossChoice.HEADS)

        val firstFlip = async {
            coordinator.executeCoinFlip(
                randomProvider = { 0.1 },
                animationDurationMs = 120L
            )
        }
        delay(20L)

        // Attempt duplicate tap while IN_PROGRESS
        val secondFlip = coordinator.executeCoinFlip(
            randomProvider = { 0.9 },
            animationDurationMs = 0L
        )
        assertTrue(secondFlip.isFailure)

        assertTrue(firstFlip.await().isSuccess)
        val savedTosses = repository.getTossesForMatchSync(matchId)
        assertEquals(1, savedTosses.size)
    }

    @Test
    fun `7_win_and_loss_calculation_is_exact_for_all_combinations`() {
        val cases = listOf(
            Triple(TossChoice.HEADS, 0.1, true),
            Triple(TossChoice.HEADS, 0.8, false),
            Triple(TossChoice.TAILS, 0.8, true),
            Triple(TossChoice.TAILS, 0.1, false)
        )
        cases.forEach { (choice, rand, expectedWin) ->
            val outcome = MatchTossEngine.prepareTossAttempt(
                matchId = "m1",
                userChoice = choice,
                currentMatchStatus = MatchStatus.TEAMS_CONFIRMED,
                currentTossStatus = TossStatus.NOT_STARTED,
                nextAttemptNumber = 1,
                randomProvider = { rand }
            )
            assertTrue(outcome is TossAttemptOutcome.Ready)
            val ready = outcome as TossAttemptOutcome.Ready
            assertEquals(expectedWin, ready.tossResult.wonToss)
        }
    }

    @Test
    fun `8_and_9_flip_again_creates_new_attempt_and_retains_full_history`() = runBlocking {
        val matchId = seedAndConfirmMatch()

        // Attempt 1: HEADS -> lands TAILS (Loss)
        coordinator.selectChoice(TossChoice.HEADS)
        val attempt1 = coordinator.executeCoinFlip(
            randomProvider = { 0.8 },
            animationDurationMs = 0L,
            timestampProvider = { 1000L }
        ).getOrThrow()
        assertEquals(1, attempt1.attemptNumber)
        assertFalse(attempt1.wonToss)

        // Flip Again -> choice resets, history kept
        coordinator.prepareFlipAgain()
        assertNull(coordinator.tossUiState.value.selectedChoice)
        assertEquals(TossStatus.NOT_STARTED, coordinator.tossUiState.value.tossStatus)
        assertEquals(1, coordinator.tossUiState.value.matchTossHistory.size)

        // Attempt 2: TAILS -> lands TAILS (Win)
        coordinator.selectChoice(TossChoice.TAILS)
        val attempt2 = coordinator.executeCoinFlip(
            randomProvider = { 0.9 },
            animationDurationMs = 0L,
            timestampProvider = { 2000L }
        ).getOrThrow()
        assertEquals(2, attempt2.attemptNumber)
        assertTrue(attempt2.wonToss)

        // Verify both attempts retained in SQLite chronologically
        val persistedTosses = repository.getTossesForMatchSync(matchId)
        assertEquals(2, persistedTosses.size)
        assertEquals(1, persistedTosses[0].attemptNumber)
        assertEquals("Heads", persistedTosses[0].choice)
        assertEquals("Tails", persistedTosses[0].result)
        assertFalse(persistedTosses[0].hasWon)

        assertEquals(2, persistedTosses[1].attemptNumber)
        assertEquals("Tails", persistedTosses[1].choice)
        assertEquals("Tails", persistedTosses[1].result)
        assertTrue(persistedTosses[1].hasWon)
    }

    @Test
    fun `10_and_11_continue_finalizes_match_and_enforces_lifecycle_transitions`() = runBlocking {
        val matchId = seedAndConfirmMatch()

        // Cannot finalize before toss completes
        assertFalse(MatchTossEngine.canFinalizeMatch(MatchStatus.DRAFT))
        assertFalse(MatchTossEngine.canFinalizeMatch(MatchStatus.TEAMS_GENERATED))
        assertFalse(MatchTossEngine.canFinalizeMatch(MatchStatus.TEAMS_CONFIRMED))
        val prematureFinalize = coordinator.finalizeMatch()
        assertTrue(prematureFinalize.isFailure)

        // Complete toss -> status becomes TOSS_COMPLETED
        coordinator.selectChoice(TossChoice.HEADS)
        coordinator.executeCoinFlip(randomProvider = { 0.25 }, animationDurationMs = 0L).getOrThrow()
        assertEquals(MatchStatus.TOSS_COMPLETED, coordinator.currentMatchStatus.value)

        // Finalize -> status becomes COMPLETED
        val summary = coordinator.finalizeMatch(timestampProvider = { 5000L }).getOrThrow()
        assertEquals(matchId, summary.matchId)
        assertEquals(MatchStatus.COMPLETED, summary.matchStatus)
        assertEquals(5000L, summary.completedAt)
        assertEquals(MatchStatus.COMPLETED, coordinator.currentMatchStatus.value)

        // Cannot toss an already completed match
        coordinator.selectChoice(TossChoice.TAILS)
        val postCompleteFlip = coordinator.executeCoinFlip(animationDurationMs = 0L)
        assertTrue(postCompleteFlip.isFailure)
    }

    @Test
    fun `12_and_13_toss_and_flip_again_never_modify_teams_or_fairness_scores`() = runBlocking {
        val matchId = seedAndConfirmMatch(fairnessScore = 91.2, fairnessRating = "Outstanding", profile = "Strict")
        val beforeSession = repository.getSessionByMatchId(matchId)!!

        // Flip twice and finalize
        coordinator.selectChoice(TossChoice.HEADS)
        coordinator.executeCoinFlip(randomProvider = { 0.7 }, animationDurationMs = 0L).getOrThrow()
        coordinator.prepareFlipAgain()
        coordinator.selectChoice(TossChoice.TAILS)
        coordinator.executeCoinFlip(randomProvider = { 0.8 }, animationDurationMs = 0L).getOrThrow()
        val summary = coordinator.finalizeMatch().getOrThrow()

        val afterSession = repository.getSessionByMatchId(matchId)!!

        // Teams identical
        assertEquals(beforeSession.finalTeamsJson, afterSession.finalTeamsJson)
        assertEquals(sampleTeams, summary.teams)

        // Fairness metrics untouched
        assertEquals(beforeSession.overallFairnessScore, afterSession.overallFairnessScore, 0.0001)
        assertEquals(beforeSession.fairnessRating, afterSession.fairnessRating)
        assertEquals(beforeSession.teammateVarietyScore, afterSession.teammateVarietyScore, 0.0001)
        assertEquals(beforeSession.opponentVarietyScore, afterSession.opponentVarietyScore, 0.0001)
        assertEquals(beforeSession.teamStrengthScore, afterSession.teamStrengthScore, 0.0001)
        assertEquals(beforeSession.jokerFairnessScore, afterSession.jokerFairnessScore, 0.0001)
        assertEquals(beforeSession.fairnessProfile, afterSession.fairnessProfile)
        assertEquals(beforeSession.qualityGateOutcome, afterSession.qualityGateOutcome)
    }

    @Test
    fun `14_and_15_sqlite_persistence_survives_app_restart_and_reopen_creates_no_new_toss`() = runBlocking {
        val matchId = seedAndConfirmMatch()
        coordinator.selectChoice(TossChoice.HEADS)
        coordinator.executeCoinFlip(randomProvider = { 0.1 }, animationDurationMs = 0L, timestampProvider = { 101L }).getOrThrow()
        coordinator.prepareFlipAgain()
        coordinator.selectChoice(TossChoice.TAILS)
        coordinator.executeCoinFlip(randomProvider = { 0.9 }, animationDurationMs = 0L, timestampProvider = { 202L }).getOrThrow()
        coordinator.finalizeMatch(timestampProvider = { 303L }).getOrThrow()

        // Simulate full app restart by closing DB and reopening from disk with brand new coordinator
        db.close()
        db = openDiskDatabase(dbName)
        repository = createRepository(db)
        val restartedCoordinator = MatchTossCoordinator(repository)

        val reopened = restartedCoordinator.loadMatchSummary(matchId)
        assertNotNull(reopened)
        assertEquals(MatchStatus.COMPLETED, reopened!!.matchStatus)
        assertEquals(303L, reopened.completedAt)
        assertEquals(sampleTeams, reopened.teams)
        assertEquals(2, reopened.tossHistory.size)
        assertEquals(2, reopened.latestToss?.attemptNumber)
        assertEquals(TossChoice.TAILS, reopened.latestToss?.result)
        assertTrue(reopened.latestToss?.wonToss == true)

        // Reopening multiple times never adds new toss rows
        restartedCoordinator.loadMatchSummary(matchId)
        restartedCoordinator.loadMatchSummary(matchId)
        assertEquals(2, repository.getTossesForMatchSync(matchId).size)
    }

    @Test
    fun `16_legacy_match_without_toss_reopens_cleanly_with_no_toss_recorded`() = runBlocking {
        val legacyMatchId = "legacy_match_99"
        repository.insertSession(
            SessionEntity(
                playerIdsJson = "[\"A\",\"B\",\"C\",\"D\"]",
                teamCount = 2,
                timestamp = 1_600_000_000_000L,
                matchId = legacyMatchId,
                matchStatus = MatchStatus.TEAMS_GENERATED.name,
                finalTeamsJson = ""
            )
        )

        val summary = coordinator.loadMatchSummary(legacyMatchId)
        assertNotNull(summary)
        assertNull(summary!!.latestToss)
        assertTrue(summary.tossHistory.isEmpty())
        assertEquals(2, summary.teams.size)
    }

    @Test
    fun `17_database_write_failure_does_not_display_toss_as_saved`() = runBlocking {
        val failingRepo = object : AppRepository(
            db.lineupDao(),
            db.tossDao(),
            db.playerDao(),
            db.sessionDao()
        ) {
            override suspend fun insertToss(toss: TossEntity): Long {
                throw IOException("Simulated SQLite disk full error")
            }
        }
        val failingCoordinator = MatchTossCoordinator(failingRepo)
        val matchId = UUID.randomUUID().toString()
        failingCoordinator.onTeamsGenerated(matchId)
        failingCoordinator.confirmTeamsForToss(matchId, 0, sampleTeams, null)
        failingCoordinator.selectChoice(TossChoice.HEADS)

        val result = failingCoordinator.executeCoinFlip(
            randomProvider = { 0.1 },
            animationDurationMs = 0L
        )

        assertTrue(result.isFailure)
        val ui = failingCoordinator.tossUiState.value
        assertEquals(TossStatus.NOT_STARTED, ui.tossStatus)
        assertNull(ui.latestTossResult)
        assertTrue(ui.matchTossHistory.isEmpty())
        assertNotNull(ui.errorMessage)
        assertEquals(MatchStatus.TEAMS_CONFIRMED, failingCoordinator.currentMatchStatus.value)
        assertTrue(db.tossDao().getTossesForMatchSync(matchId).isEmpty())
    }
}
