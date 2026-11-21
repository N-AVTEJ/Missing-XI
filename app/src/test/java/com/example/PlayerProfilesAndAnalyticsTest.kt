package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity
import com.example.data.repository.AppRepository
import com.example.util.GeneratedTeam
import com.example.util.MatchHistoryStore
import com.example.util.MatchStatus
import com.example.util.MatchTossEngine
import com.example.util.Player
import com.example.util.PlayerAnalyticsService
import com.example.util.PlayerDirectorySortOption
import com.example.util.PlayerDirectoryStore
import com.example.util.PlayerRemovalResult
import com.example.util.PlayerStatusFilter
import com.example.util.TeamStrengthEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerProfilesAndAnalyticsTest {

    private lateinit var context: Context
    private lateinit var dbName: String
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var directoryStore: PlayerDirectoryStore
    private lateinit var matchHistoryStore: MatchHistoryStore

    private val baseTime = 1_795_500_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "test_player_profiles_${UUID.randomUUID()}.db"
        db = openDiskDatabase(dbName)
        repository = createRepository(db)
        directoryStore = PlayerDirectoryStore(repository)
        matchHistoryStore = MatchHistoryStore(repository)
    }

    @After
    fun tearDown() {
        if (db.isOpen) db.close()
        context.deleteDatabase(dbName)
    }

    private fun openDiskDatabase(name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11
            )
            .build()
    }

    private fun createRepository(database: AppDatabase): AppRepository {
        return AppRepository(
            lineupDao = database.lineupDao(),
            tossDao = database.tossDao(),
            playerDao = database.playerDao(),
            sessionDao = database.sessionDao(),
            database = database
        )
    }

    private suspend fun seedPlayer(
        id: String = UUID.randomUUID().toString(),
        name: String,
        skillRating: Int = PlayerAnalyticsService.DEFAULT_SKILL_RATING,
        isArchived: Boolean = false,
        createdAt: Long = baseTime - 10_000L
    ): PlayerEntity {
        val entity = PlayerEntity(
            id = id,
            displayName = name,
            skillRating = skillRating,
            isArchived = isArchived,
            createdAt = createdAt,
            lastUsedAt = 0L,
            lastPlayedAt = 0L,
            updatedAt = createdAt
        )
        repository.insertPlayer(entity)
        return entity
    }

    private suspend fun seedMatch(
        matchId: String = UUID.randomUUID().toString(),
        matchNumber: Int? = 1,
        timestamp: Long = baseTime,
        status: MatchStatus = MatchStatus.COMPLETED,
        teams: List<GeneratedTeam>,
        joker: PlayerEntity? = null,
        excludedPlayers: List<PlayerEntity> = emptyList(),
        fairnessScore: Double = 87.5,
        teamStrengthScore: Double = 90.0
    ): String {
        val participants = (teams.flatMap { it.players } + listOfNotNull(joker?.displayName)).distinct()
        val finalTeamsJson = MatchTossEngine.serializeTeamsJson(
            teams = teams,
            joker = joker?.displayName,
            jokerId = joker?.id
        )
        repository.insertSession(
            SessionEntity(
                playerIdsJson = JSONArray(participants).toString(),
                teamCount = teams.size,
                timestamp = timestamp,
                overallFairnessScore = fairnessScore,
                fairnessRating = "Excellent",
                teammateVarietyScore = 88.0,
                opponentVarietyScore = 85.0,
                teamStrengthScore = teamStrengthScore,
                jokerFairnessScore = 92.0,
                fairnessProfile = "Balanced",
                qualityGateOutcome = "Passed",
                excludedPlayerIdsJson = JSONArray(excludedPlayers.map { it.id }).toString(),
                excludedPlayerNamesJson = JSONArray(excludedPlayers.map { it.displayName }).toString(),
                matchId = matchId,
                matchStatus = status.name,
                finalTeamsJson = finalTeamsJson,
                completedAt = if (status == MatchStatus.COMPLETED) timestamp + 60_000L else 0L,
                matchNumber = matchNumber,
                totalEligiblePlayers = participants.size,
                teammatePenalty = 1,
                opponentPenalty = 1,
                teamStrengthDifference = 1,
                hasFairnessBreakdown = true
            )
        )
        return matchId
    }

    private fun teamOf(teamNumber: Int, name: String, vararg players: PlayerEntity): GeneratedTeam {
        val strengths = players.map { it.skillRating }
        val total = strengths.sum()
        val avg = if (strengths.isNotEmpty()) total.toDouble() / strengths.size else 0.0
        return GeneratedTeam(
            teamNumber = teamNumber,
            name = name,
            players = players.map { it.displayName },
            playerIds = players.map { it.id },
            totalStrength = total,
            averageStrength = avg,
            maxPlayerRating = strengths.maxOrNull() ?: 0,
            minPlayerRating = strengths.minOrNull() ?: 0
        )
    }

    @Test
    fun scenario_1_and_12_new_player_with_no_match_history_shows_not_enough_data_and_default_skill() = runBlocking {
        val rookie = seedPlayer(id = "p_rookie", name = "Aarav")

        val analytics = directoryStore.loadPlayerProfileSync(rookie.id)
        assertNotNull(analytics)
        assertEquals(5, analytics!!.player.skillRating)
        assertFalse(analytics.player.isArchived)
        assertEquals(0, analytics.totalMatchesPlayed)
        assertEquals(0, analytics.matchesAsJoker)
        assertEquals(0, analytics.totalTeammateAppearances)
        assertEquals(0, analytics.totalOpponentAppearances)
        assertTrue(analytics.topTeammates.isEmpty())
        assertTrue(analytics.topOpponents.isEmpty())
        assertTrue(analytics.matchTimeline.isEmpty())
        assertEquals("Not enough data", analytics.mostFrequentTeammateSummary())
        assertEquals("Not enough data", analytics.mostFrequentOpponentSummary())
    }

    @Test
    fun scenario_2_multiple_confirmed_matches_ignores_drafts_unconfirmed_shuffles_and_excluded_players() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Rahul", skillRating = 8)
        val p2 = seedPlayer(id = "p2", name = "Kiran", skillRating = 7)
        val p3 = seedPlayer(id = "p3", name = "Arjun", skillRating = 6)
        val p4 = seedPlayer(id = "p4", name = "Naveen", skillRating = 7)
        val pExcluded = seedPlayer(id = "p5", name = "Vikram", skillRating = 5)

        // 3 valid accepted/confirmed lifecycle states
        seedMatch(
            matchId = "m_confirmed",
            matchNumber = 1,
            timestamp = baseTime + 1_000L,
            status = MatchStatus.TEAMS_CONFIRMED,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4)),
            excludedPlayers = listOf(pExcluded)
        )
        seedMatch(
            matchId = "m_tossed",
            matchNumber = 2,
            timestamp = baseTime + 2_000L,
            status = MatchStatus.TOSS_COMPLETED,
            teams = listOf(teamOf(1, "Team A", p1, p3), teamOf(2, "Team B", p2, p4))
        )
        seedMatch(
            matchId = "m_completed",
            matchNumber = 3,
            timestamp = baseTime + 3_000L,
            status = MatchStatus.COMPLETED,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4))
        )

        // Should NOT be counted: DRAFT & modern unconfirmed TEAMS_GENERATED candidate
        seedMatch(
            matchId = "m_draft",
            matchNumber = 4,
            timestamp = baseTime + 4_000L,
            status = MatchStatus.DRAFT,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4))
        )
        seedMatch(
            matchId = "m_unconfirmed_shuffle",
            matchNumber = 5,
            timestamp = baseTime + 5_000L,
            status = MatchStatus.TEAMS_GENERATED,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4))
        )

        // Duplicate matchId row should never double-count
        seedMatch(
            matchId = "m_completed",
            matchNumber = 3,
            timestamp = baseTime + 3_000L,
            status = MatchStatus.COMPLETED,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4))
        )

        val rahulAnalytics = directoryStore.loadPlayerProfileSync(p1.id)!!
        assertEquals(3, rahulAnalytics.totalMatchesPlayed)
        assertEquals(0, rahulAnalytics.matchesAsJoker)
        assertEquals(3, rahulAnalytics.matchTimeline.size)
        // Newest first timeline ordering
        assertEquals(listOf("m_completed", "m_tossed", "m_confirmed"), rahulAnalytics.matchTimeline.map { it.matchId })

        // Excluded player must have 0 matches played
        val excludedAnalytics = directoryStore.loadPlayerProfileSync(pExcluded.id)!!
        assertEquals(0, excludedAnalytics.totalMatchesPlayed)
        assertTrue(excludedAnalytics.matchTimeline.isEmpty())
    }

    @Test
    fun scenario_3_player_as_joker_several_times_counts_accurately_and_follows_joker_rules() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Dev")
        val p2 = seedPlayer(id = "p2", name = "Rohan")
        val p3 = seedPlayer(id = "p3", name = "Siddharth")
        val p4 = seedPlayer(id = "p4", name = "Meera")
        val p5 = seedPlayer(id = "p5", name = "Ananya")

        // Dev is Joker in 2 matches, regular team member in 1 match
        seedMatch(
            matchId = "m_joker_1",
            matchNumber = 1,
            timestamp = baseTime + 1_000L,
            teams = listOf(teamOf(1, "Team A", p2, p3), teamOf(2, "Team B", p4, p5)),
            joker = p1
        )
        seedMatch(
            matchId = "m_joker_2",
            matchNumber = 2,
            timestamp = baseTime + 2_000L,
            teams = listOf(teamOf(1, "Team A", p2, p4), teamOf(2, "Team B", p3, p5)),
            joker = p1
        )
        seedMatch(
            matchId = "m_regular",
            matchNumber = 3,
            timestamp = baseTime + 3_000L,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4)),
            joker = p5
        )

        val devAnalytics = directoryStore.loadPlayerProfileSync(p1.id)!!
        assertEquals(3, devAnalytics.totalMatchesPlayed)
        assertEquals(2, devAnalytics.matchesAsJoker)
        // Only the 1 regular team match records fixed-team teammate/opponent appearances
        assertEquals(1, devAnalytics.totalTeammateAppearances)
        assertEquals(2, devAnalytics.totalOpponentAppearances)
        assertEquals(2, devAnalytics.matchTimeline.count { it.isJoker })
        assertEquals("Joker", devAnalytics.matchTimeline.first { it.matchId == "m_joker_2" }.teamAssigned)
    }

    @Test
    fun scenario_4_and_5_repeated_teammates_and_opponents_top_5_excludes_deleted_references_and_separates_roles() = runBlocking {
        val focus = seedPlayer(id = "p_focus", name = "Rahul")
        val tm1 = seedPlayer(id = "tm1", name = "Ajay")
        val tm2 = seedPlayer(id = "tm2", name = "Bala")
        val tm3 = seedPlayer(id = "tm3", name = "Charan")
        val tm4 = seedPlayer(id = "tm4", name = "Deepak")
        val tm5 = seedPlayer(id = "tm5", name = "Eshan")
        val tm6 = seedPlayer(id = "tm6", name = "Farhan")

        val opp1 = seedPlayer(id = "opp1", name = "Gaurav")
        val opp2 = seedPlayer(id = "opp2", name = "Harsh")
        val opp3 = seedPlayer(id = "opp3", name = "Ishaan")
        val opp4 = seedPlayer(id = "opp4", name = "Jatin")
        val opp5 = seedPlayer(id = "opp5", name = "Kabir")
        val opp6 = seedPlayer(id = "opp6", name = "Laksh")

        // Deleted/invalid player NOT in DB
        val ghostPlayer = PlayerEntity(id = "deleted_ghost_id", displayName = "Ghost")

        // 4 matches with Ajay, 3 with Bala, 2 with Charan/Deepak/Eshan, 1 with Farhan (so top 5 excludes Farhan)
        // Plus ghostPlayer on Rahul's team — must be excluded from chemistry!
        seedMatch(
            matchId = "m1", matchNumber = 1, timestamp = baseTime + 1_000L,
            teams = listOf(teamOf(1, "Team A", focus, tm1, tm2, ghostPlayer), teamOf(2, "Team B", opp1, opp2, opp3))
        )
        seedMatch(
            matchId = "m2", matchNumber = 2, timestamp = baseTime + 2_000L,
            teams = listOf(teamOf(1, "Team A", focus, tm1, tm2, tm3), teamOf(2, "Team B", opp1, opp2, opp4))
        )
        seedMatch(
            matchId = "m3", matchNumber = 3, timestamp = baseTime + 3_000L,
            teams = listOf(teamOf(1, "Team A", focus, tm1, tm2, tm3, tm4), teamOf(2, "Team B", opp1, opp4, opp5))
        )
        seedMatch(
            matchId = "m4", matchNumber = 4, timestamp = baseTime + 4_000L,
            teams = listOf(teamOf(1, "Team A", focus, tm1, tm4, tm5), teamOf(2, "Team B", opp1, opp2, opp5, opp6))
        )
        seedMatch(
            matchId = "m5", matchNumber = 5, timestamp = baseTime + 5_000L,
            teams = listOf(teamOf(1, "Team A", focus, tm5, tm6), teamOf(2, "Team B", opp3, opp4))
        )

        val analytics = directoryStore.loadPlayerProfileSync(focus.id)!!
        assertEquals(5, analytics.totalMatchesPlayed)

        // Top 5 Teammates (ghostPlayer excluded, Farhan (1) excluded as 6th)
        assertEquals(5, analytics.topTeammates.size)
        assertEquals(listOf("tm1", "tm2", "tm3", "tm4", "tm5"), analytics.topTeammates.map { it.playerId })
        assertEquals(listOf(4, 3, 2, 2, 2), analytics.topTeammates.map { it.sharedMatches })
        assertFalse(analytics.topTeammates.any { it.playerId == ghostPlayer.id })
        // Total recorded valid teammate appearances = 4 + 3 + 2 + 2 + 2 + 1 = 14 (ghost excluded)
        assertEquals(14, analytics.totalTeammateAppearances)

        // Top 5 Opponents strictly separated from teammates
        assertEquals(5, analytics.topOpponents.size)
        assertEquals("opp1", analytics.topOpponents.first().playerId)
        assertEquals(4, analytics.topOpponents.first().sharedMatches)
        assertTrue(analytics.topOpponents.none { it.playerId.startsWith("tm") })
        assertTrue(analytics.topTeammates.none { it.playerId.startsWith("opp") })
        // opp1=4, opp2=3, opp4=3, opp3=2, opp5=2, opp6=1 => total = 15
        assertEquals(15, analytics.totalOpponentAppearances)
    }

    @Test
    fun scenario_6_two_teammates_with_equal_frequency_share_same_rank_and_display_both_in_summary() = runBlocking {
        val focus = seedPlayer(id = "p_focus", name = "Rahul")
        val tmZ = seedPlayer(id = "tm_z", name = "Zoya")
        val tmA = seedPlayer(id = "tm_a", name = "Amit")
        val tmC = seedPlayer(id = "tm_c", name = "Chetan")
        val opp = seedPlayer(id = "opp_1", name = "Ravi")

        // Amit (2), Zoya (2), Chetan (1)
        seedMatch(
            matchId = "m1", matchNumber = 1, timestamp = baseTime + 1_000L,
            teams = listOf(teamOf(1, "Team A", focus, tmZ, tmA), teamOf(2, "Team B", opp))
        )
        seedMatch(
            matchId = "m2", matchNumber = 2, timestamp = baseTime + 2_000L,
            teams = listOf(teamOf(1, "Team A", focus, tmZ, tmA, tmC), teamOf(2, "Team B", opp))
        )

        val analytics = directoryStore.loadPlayerProfileSync(focus.id)!!
        assertEquals(3, analytics.topTeammates.size)
        // Alphabetical tie-break: Amit before Zoya, both rank #1; Chetan rank #3
        assertEquals(listOf("Amit", "Zoya", "Chetan"), analytics.topTeammates.map { it.displayName })
        assertEquals(listOf(1, 1, 3), analytics.topTeammates.map { it.rank })
        assertEquals(2, analytics.mostFrequentTeammates.size)
        assertEquals("Amit & Zoya (2 matches)", analytics.mostFrequentTeammateSummary())
    }

    @Test
    fun scenario_7_skill_rating_edit_validates_persists_updates_future_strength_and_never_mutates_historical_fairness() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Rahul", skillRating = 5)
        val p2 = seedPlayer(id = "p2", name = "Kiran", skillRating = 5)
        val p3 = seedPlayer(id = "p3", name = "Arjun", skillRating = 5)
        val p4 = seedPlayer(id = "p4", name = "Naveen", skillRating = 5)

        val matchId = seedMatch(
            matchId = "m_hist_fairness",
            matchNumber = 1,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4)),
            fairnessScore = 91.5,
            teamStrengthScore = 100.0
        )

        // Validation rejects out-of-range ratings
        assertTrue(directoryStore.updatePlayerSkillRating(p1.id, 0).isFailure)
        assertTrue(directoryStore.updatePlayerSkillRating(p1.id, 11).isFailure)

        // Valid update succeeds
        val updateResult = directoryStore.updatePlayerSkillRating(p1.id, 10)
        assertTrue(updateResult.isSuccess)

        // Simulate app restart by reopening SQLite DB from disk
        db.close()
        db = openDiskDatabase(dbName)
        repository = createRepository(db)
        directoryStore = PlayerDirectoryStore(repository)
        matchHistoryStore = MatchHistoryStore(repository)

        // Updated rating persisted & immediately impacts future TeamStrengthEngine calculation
        val reloadedP1 = repository.getPlayerById(p1.id)!!
        assertEquals(10, reloadedP1.skillRating)
        val futureTeamStrength = TeamStrengthEngine.calculateTeamStrength(
            listOf(Player(reloadedP1.id, reloadedP1.displayName, reloadedP1.skillRating), Player(p2.id, p2.displayName, p2.skillRating))
        )
        assertEquals(15, futureTeamStrength.totalStrength)

        // Historical match fairness & stored team strengths MUST remain untouched!
        val historicalSession = repository.getSessionByMatchId(matchId)!!
        assertEquals(91.5, historicalSession.overallFairnessScore, 0.0001)
        assertEquals(100.0, historicalSession.teamStrengthScore, 0.0001)

        val historicalDetails = matchHistoryStore.getMatchDetails(matchId)!!
        assertEquals(92, historicalDetails.fairnessScore)
        assertEquals(10, historicalDetails.teams.first().totalStrength) // Originally 5 + 5 = 10, NOT 15!
    }

    @Test
    fun scenario_8_and_9_archiving_preserves_historical_records_excludes_from_new_generation_and_blocks_destructive_delete() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Rahul", skillRating = 8)
        val p2 = seedPlayer(id = "p2", name = "Kiran", skillRating = 7)
        val p3 = seedPlayer(id = "p3", name = "Arjun", skillRating = 6)
        val p4 = seedPlayer(id = "p4", name = "Naveen", skillRating = 7)
        val unusedPlayer = seedPlayer(id = "p_unused", name = "GhostFree", skillRating = 5)

        val matchId = seedMatch(
            matchId = "m_archive_test",
            matchNumber = 1,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4))
        )

        // Attempting safe delete on a player referenced by history MUST archive, NEVER permanently delete
        val removalResult = directoryStore.removePlayerSafely(p2.id)
        assertTrue(removalResult is PlayerRemovalResult.ArchivedBecauseReferenced)
        val archivedKiran = repository.getPlayerById(p2.id)
        assertNotNull(archivedKiran)
        assertTrue(archivedKiran!!.isArchived)

        // Unused player with zero match history can still be safely removed
        val unusedRemoval = directoryStore.removePlayerSafely(unusedPlayer.id)
        assertTrue(unusedRemoval is PlayerRemovalResult.Deleted)
        assertNull(repository.getPlayerById(unusedPlayer.id))

        // Scenario 9: Archived player is excluded from new team generation selections by default
        val selectableForNewMatch = directoryStore.getSelectablePlayersForNewMatch()
        assertFalse(selectableForNewMatch.any { it.id == p2.id })
        assertEquals(3, selectableForNewMatch.size)

        // Scenario 8: Archived player remains visible in historical match details & teammate chemistry
        val matchDetails = matchHistoryStore.getMatchDetails(matchId)!!
        assertTrue(matchDetails.teams.flatMap { it.players }.contains("Kiran"))

        val rahulAnalytics = directoryStore.loadPlayerProfileSync(p1.id)!!
        assertEquals(1, rahulAnalytics.topTeammates.size)
        assertEquals("Kiran", rahulAnalytics.topTeammates.first().displayName)
        assertTrue(rahulAnalytics.topTeammates.first().isArchived)

        // Archived player's own profile & stats remain intact
        val kiranAnalytics = directoryStore.loadPlayerProfileSync(p2.id)!!
        assertTrue(kiranAnalytics.player.isArchived)
        assertEquals(1, kiranAnalytics.totalMatchesPlayed)
        assertEquals(1, kiranAnalytics.matchTimeline.size)

        // Restoring returns Kiran to new team generation selections while preserving stats
        assertTrue(directoryStore.restorePlayer(p2.id))
        val selectableAfterRestore = directoryStore.getSelectablePlayersForNewMatch()
        assertTrue(selectableAfterRestore.any { it.id == p2.id })
        assertEquals(1, directoryStore.loadPlayerProfileSync(p2.id)!!.totalMatchesPlayed)
    }

    @Test
    fun scenario_10_deleted_match_is_removed_from_derived_statistics_consistently() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Rahul")
        val p2 = seedPlayer(id = "p2", name = "Kiran")
        val p3 = seedPlayer(id = "p3", name = "Arjun")
        val p4 = seedPlayer(id = "p4", name = "Naveen")

        val m1 = seedMatch(
            matchId = "m_keep", matchNumber = 1, timestamp = baseTime + 1_000L,
            teams = listOf(teamOf(1, "Team A", p1, p2), teamOf(2, "Team B", p3, p4))
        )
        val m2 = seedMatch(
            matchId = "m_delete", matchNumber = 2, timestamp = baseTime + 2_000L,
            teams = listOf(teamOf(1, "Team A", p1, p3), teamOf(2, "Team B", p2, p4))
        )

        val beforeDelete = directoryStore.loadPlayerProfileSync(p1.id)!!
        assertEquals(2, beforeDelete.totalMatchesPlayed)
        assertEquals(2, beforeDelete.totalTeammateAppearances)
        assertEquals(4, beforeDelete.totalOpponentAppearances)

        // Delete m2 transactionally
        assertTrue(matchHistoryStore.deleteMatch(m2))

        val afterDelete = directoryStore.loadPlayerProfileSync(p1.id)!!
        assertEquals(1, afterDelete.totalMatchesPlayed)
        assertEquals(1, afterDelete.totalTeammateAppearances)
        assertEquals(2, afterDelete.totalOpponentAppearances)
        assertEquals(listOf(m1), afterDelete.matchTimeline.map { it.matchId })
        assertEquals(listOf("p2"), afterDelete.topTeammates.map { it.playerId })
        assertEquals(setOf("p3", "p4"), afterDelete.topOpponents.map { it.playerId }.toSet())
    }

    @Test
    fun scenario_11_legacy_match_with_missing_optional_fields_loads_without_crashing_or_fabricating_pairs() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Rahul")
        val p2 = seedPlayer(id = "p2", name = "Kiran")

        // Insert legacy pre-v9 session with empty finalTeamsJson, null matchNumber, no breakdown
        repository.insertSession(
            SessionEntity(
                playerIdsJson = JSONArray(listOf("Rahul", "Kiran")).toString(),
                teamCount = 2,
                timestamp = baseTime - 50_000L,
                overallFairnessScore = 0.0,
                fairnessRating = "",
                fairnessProfile = "Legacy Result",
                matchId = "legacy_match_42",
                matchStatus = MatchStatus.TEAMS_GENERATED.name,
                finalTeamsJson = "",
                completedAt = 0L,
                matchNumber = null,
                hasFairnessBreakdown = false
            )
        )

        val analytics = directoryStore.loadPlayerProfileSync(p1.id)!!
        assertEquals(1, analytics.totalMatchesPlayed)
        assertEquals(0, analytics.matchesAsJoker)
        // Must NOT fabricate teammates/opponents when finalTeamsJson is absent
        assertEquals(0, analytics.totalTeammateAppearances)
        assertEquals(0, analytics.totalOpponentAppearances)
        assertEquals("Not enough data", analytics.mostFrequentTeammateSummary())
        assertEquals("Not enough data", analytics.mostFrequentOpponentSummary())

        assertEquals(1, analytics.matchTimeline.size)
        val entry = analytics.matchTimeline.first()
        assertEquals("Legacy Match", entry.matchDisplayTitle)
        assertNull(entry.teamAssigned)
        assertTrue(entry.teammates.isEmpty())
        assertTrue(entry.opponents.isEmpty())
        assertNull(entry.fairnessScore)
    }

    @Test
    fun directory_search_filter_sort_and_state_preservation_across_profile_navigation() = runBlocking {
        val p1 = seedPlayer(id = "p1", name = "Zara", skillRating = 9)
        val p2 = seedPlayer(id = "p2", name = "Amit", skillRating = 6)
        val p3 = seedPlayer(id = "p3", name = "Bala", skillRating = 10)
        val p4 = seedPlayer(id = "p4", name = "Carlos", skillRating = 7, isArchived = true)

        // Amit plays 2 matches, Zara plays 1 match, Bala plays 0 matches
        seedMatch(
            matchId = "m1", matchNumber = 1,
            teams = listOf(teamOf(1, "Team A", p2), teamOf(2, "Team B", p1))
        )
        seedMatch(
            matchId = "m2", matchNumber = 2,
            teams = listOf(teamOf(1, "Team A", p2), teamOf(2, "Team B", p4))
        )

        // Default Active + Alphabetical
        var state = directoryStore.refreshSync()
        assertEquals(3, state.totalActiveCount)
        assertEquals(1, state.totalArchivedCount)
        assertEquals(listOf("Amit", "Bala", "Zara"), state.items.map { it.player.displayName })

        // Sort Highest Skill
        directoryStore.setSortOption(PlayerDirectorySortOption.HIGHEST_SKILL)
        state = directoryStore.refreshSync()
        assertEquals(listOf("Bala", "Zara", "Amit"), state.items.map { it.player.displayName })

        // Sort Most Matches
        directoryStore.setSortOption(PlayerDirectorySortOption.MOST_MATCHES)
        state = directoryStore.refreshSync()
        assertEquals(listOf("Amit", "Zara", "Bala"), state.items.map { it.player.displayName })

        // Sort Least Matches
        directoryStore.setSortOption(PlayerDirectorySortOption.LEAST_MATCHES)
        state = directoryStore.refreshSync()
        assertEquals(listOf("Bala", "Zara", "Amit"), state.items.map { it.player.displayName })

        // Filter Archived
        directoryStore.setStatusFilter(PlayerStatusFilter.ARCHIVED)
        state = directoryStore.refreshSync()
        assertEquals(listOf("Carlos"), state.items.map { it.player.displayName })

        // Search + All Filter preserved after opening a player profile
        directoryStore.setStatusFilter(PlayerStatusFilter.ALL)
        directoryStore.setSearchQuery("ar") // Matches Zara & Carlos
        directoryStore.setSortOption(PlayerDirectorySortOption.ALPHABETICAL)
        state = directoryStore.refreshSync()
        assertEquals(listOf("Carlos", "Zara"), state.items.map { it.player.displayName })

        // Simulate navigating to Carlos's profile and returning
        val profile = directoryStore.loadPlayerProfileSync(p4.id)
        assertNotNull(profile)
        assertEquals("Carlos", profile!!.player.displayName)

        // Directory search/filter/sort state MUST remain preserved on return
        assertEquals("ar", directoryStore.filter.value.searchQuery)
        assertEquals(PlayerStatusFilter.ALL, directoryStore.filter.value.statusFilter)
        assertEquals(PlayerDirectorySortOption.ALPHABETICAL, directoryStore.filter.value.sortOption)
    }

    @Test
    fun unique_player_ids_distinguish_players_even_with_duplicate_display_names() = runBlocking {
        val rahulOne = seedPlayer(id = "id_rahul_1", name = "Rahul", skillRating = 9)
        val rahulTwo = seedPlayer(id = "id_rahul_2", name = "Rahul", skillRating = 4)
        val partner = seedPlayer(id = "id_partner", name = "Kiran", skillRating = 7)
        val rival = seedPlayer(id = "id_rival", name = "Arjun", skillRating = 6)

        // Only rahulOne plays in m1 and m2; rahulTwo plays in 0 matches
        seedMatch(
            matchId = "m_id_1", matchNumber = 1,
            teams = listOf(teamOf(1, "Team A", rahulOne, partner), teamOf(2, "Team B", rival))
        )
        seedMatch(
            matchId = "m_id_2", matchNumber = 2,
            teams = listOf(teamOf(1, "Team A", rahulOne, partner), teamOf(2, "Team B", rival))
        )

        val statsOne = directoryStore.loadPlayerProfileSync(rahulOne.id)!!
        val statsTwo = directoryStore.loadPlayerProfileSync(rahulTwo.id)!!

        assertEquals(2, statsOne.totalMatchesPlayed)
        assertEquals(2, statsOne.topTeammates.first().sharedMatches)
        assertEquals(0, statsTwo.totalMatchesPlayed)
        assertTrue(statsTwo.topTeammates.isEmpty())
    }
}
