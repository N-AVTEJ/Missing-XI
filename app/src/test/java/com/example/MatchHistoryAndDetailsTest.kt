package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import com.example.util.GeneratedTeam
import com.example.util.MatchDateRangeFilter
import com.example.util.MatchHistoryStore
import com.example.util.MatchSortOrder
import com.example.util.MatchStatus
import com.example.util.MatchStatusFilter
import com.example.util.MatchTossCoordinator
import com.example.util.MatchTossEngine
import com.example.util.TossChoice
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchHistoryAndDetailsTest {

    private lateinit var context: Context
    private lateinit var dbName: String
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var store: MatchHistoryStore

    private val baseTime = 1_795_200_000_000L // Fixed reference clock for deterministic date-range tests
    private val oneDayMs = 86_400_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "test_history_${UUID.randomUUID()}.db"
        db = openDiskDatabase(dbName)
        repository = createRepository(db)
        store = MatchHistoryStore(
            repository = repository,
            clockProvider = { baseTime },
            pageSize = MatchHistoryStore.DEFAULT_PAGE_SIZE
        )
    }

    @After
    fun tearDown() {
        if (db.isOpen) db.close()
        context.deleteDatabase(dbName)
    }

    private fun openDiskDatabase(name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11)
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

    private suspend fun insertSampleMatch(
        matchId: String = UUID.randomUUID().toString(),
        matchNumber: Int? = 1,
        timestamp: Long = baseTime,
        completedAt: Long = timestamp + 60_000L,
        status: MatchStatus = MatchStatus.COMPLETED,
        teams: List<GeneratedTeam> = listOf(
            GeneratedTeam(1, "Team A", listOf("Rahul", "Kiran"), listOf("p1", "p2"), 14, 7.0, 8, 6),
            GeneratedTeam(2, "Team B", listOf("Arjun", "Naveen"), listOf("p3", "p4"), 13, 6.5, 7, 6)
        ),
        joker: String? = null,
        excludedNames: List<String> = emptyList(),
        fairnessScore: Double = 86.0,
        fairnessRating: String = "Excellent",
        fairnessProfile: String = "Balanced",
        qualityGateOutcome: String = "Passed",
        teammatePenalty: Int? = 2,
        opponentPenalty: Int? = 1,
        strengthDiff: Int? = 1,
        hasFairnessBreakdown: Boolean = true,
        latestTossChoice: String = "HEADS",
        latestTossResult: String = "HEADS",
        latestTossWon: Boolean = true
    ): String {
        val participating = (teams.flatMap { it.players } + listOfNotNull(joker)).distinct()
        val playerIdsJson = org.json.JSONArray(participating).toString()
        val excludedNamesJson = org.json.JSONArray(excludedNames).toString()
        val finalTeamsJson = MatchTossEngine.serializeTeamsJson(teams, joker)

        repository.insertSession(
            SessionEntity(
                playerIdsJson = playerIdsJson,
                teamCount = teams.size,
                timestamp = timestamp,
                overallFairnessScore = fairnessScore,
                fairnessRating = fairnessRating,
                teammateVarietyScore = if (hasFairnessBreakdown) 88.0 else 0.0,
                opponentVarietyScore = if (hasFairnessBreakdown) 84.0 else 0.0,
                teamStrengthScore = if (hasFairnessBreakdown) 90.0 else 0.0,
                jokerFairnessScore = if (hasFairnessBreakdown) 100.0 else 0.0,
                fairnessProfile = fairnessProfile,
                qualityGateOutcome = qualityGateOutcome,
                matchId = matchId,
                matchStatus = status.name,
                finalTeamsJson = finalTeamsJson,
                completedAt = if (status == MatchStatus.COMPLETED) completedAt else 0L,
                latestTossChoice = latestTossChoice,
                latestTossResult = latestTossResult,
                latestTossWon = latestTossWon,
                matchNumber = matchNumber,
                totalEligiblePlayers = participating.size,
                teammatePenalty = teammatePenalty,
                opponentPenalty = opponentPenalty,
                teamStrengthDifference = strengthDiff,
                excludedPlayerNamesJson = excludedNamesJson,
                hasFairnessBreakdown = hasFairnessBreakdown
            )
        )
        return matchId
    }

    @Test
    fun `1_loading_match_history_and_pagination_loads_20_initially_then_next_page`() = runBlocking {
        // Insert 25 matches
        for (i in 1..25) {
            insertSampleMatch(
                matchNumber = i,
                timestamp = baseTime - ((25 - i) * 1000L),
                fairnessScore = 70.0 + (i % 20)
            )
        }

        store.loadMatchHistory()
        val page1 = store.uiState.value
        assertEquals(25, page1.totalCount)
        assertEquals(20, page1.items.size)
        assertTrue(page1.hasMorePages)
        assertEquals(25, page1.items.first().matchNumber) // newest first

        store.loadNextPage()
        val page2 = store.uiState.value
        assertEquals(25, page2.items.size)
        assertFalse(page2.hasMorePages)
        assertEquals(1, page2.items.last().matchNumber)
    }

    @Test
    fun `2_3_4_sorting_by_newest_oldest_highest_and_lowest_fairness_handles_missing_scores_consistently`() = runBlocking {
        val mOld = insertSampleMatch(matchNumber = 1, timestamp = baseTime - 3 * oneDayMs, fairnessScore = 72.0)
        val mMid = insertSampleMatch(matchNumber = 2, timestamp = baseTime - 2 * oneDayMs, fairnessScore = 94.0)
        val mNew = insertSampleMatch(matchNumber = 3, timestamp = baseTime - 1 * oneDayMs, fairnessScore = 81.0)
        val mLegacyNoScore = insertSampleMatch(
            matchNumber = null,
            timestamp = baseTime,
            fairnessScore = 0.0,
            fairnessRating = "",
            fairnessProfile = "Legacy Result",
            hasFairnessBreakdown = false,
            teammatePenalty = null,
            opponentPenalty = null,
            strengthDiff = null
        )

        // Newest first
        store.sortMatches(MatchSortOrder.NEWEST_FIRST)
        assertEquals(listOf(mLegacyNoScore, mNew, mMid, mOld), store.uiState.value.items.map { it.id })

        // Oldest first
        store.sortMatches(MatchSortOrder.OLDEST_FIRST)
        assertEquals(listOf(mOld, mMid, mNew, mLegacyNoScore), store.uiState.value.items.map { it.id })

        // Highest fairness -> scored descending, missing score ALWAYS last (never treated as 0 mixed in)
        store.sortMatches(MatchSortOrder.HIGHEST_FAIRNESS)
        assertEquals(listOf(mMid, mNew, mOld, mLegacyNoScore), store.uiState.value.items.map { it.id })
        assertNull(store.uiState.value.items.last().fairnessScore)

        // Lowest fairness -> scored ascending (72, 81, 94), missing score STILL last (NOT falsely ranked as 0)
        store.sortMatches(MatchSortOrder.LOWEST_FAIRNESS)
        assertEquals(listOf(mOld, mNew, mMid, mLegacyNoScore), store.uiState.value.items.map { it.id })
    }

    @Test
    fun `5_and_6_searching_by_player_name_and_match_number_is_case_insensitive`() = runBlocking {
        val m1 = insertSampleMatch(
            matchNumber = 24,
            teams = listOf(
                GeneratedTeam(1, "Team A", listOf("Rahul", "Varun"), listOf("p1", "p2")),
                GeneratedTeam(2, "Team B", listOf("Arjun", "Sai"), listOf("p3", "p4"))
            )
        )
        val m2 = insertSampleMatch(
            matchNumber = 25,
            teams = listOf(
                GeneratedTeam(1, "Team A", listOf("Deepak", "Vikram"), listOf("p5", "p6")),
                GeneratedTeam(2, "Team B", listOf("Rohan", "Karthik"), listOf("p7", "p8"))
            )
        )

        // Search by player name (mixed case)
        store.searchMatches("rAhUl")
        assertEquals(listOf(m1), store.uiState.value.items.map { it.id })

        // Search by match number with # and leading zero
        store.searchMatches("#024")
        assertEquals(listOf(m1), store.uiState.value.items.map { it.id })

        // Search by plain match number
        store.searchMatches("25")
        assertEquals(listOf(m2), store.uiState.value.items.map { it.id })

        // Search by match ID prefix
        store.searchMatches(m2.take(8))
        assertEquals(listOf(m2), store.uiState.value.items.map { it.id })
    }

    @Test
    fun `7_8_9_filtering_by_status_profile_date_range_and_combining_all_filters`() = runBlocking {
        val mStrictRecentCompleted = insertSampleMatch(
            matchNumber = 10,
            timestamp = baseTime - 2 * oneDayMs,
            status = MatchStatus.COMPLETED,
            fairnessProfile = "Strict"
        )
        val mStrictOldCompleted = insertSampleMatch(
            matchNumber = 11,
            timestamp = baseTime - 20 * oneDayMs,
            status = MatchStatus.COMPLETED,
            fairnessProfile = "Strict"
        )
        val mBalancedConfirmed = insertSampleMatch(
            matchNumber = 12,
            timestamp = baseTime - 1 * oneDayMs,
            status = MatchStatus.TEAMS_CONFIRMED,
            fairnessProfile = "Balanced"
        )
        val mCasualTossDone = insertSampleMatch(
            matchNumber = 13,
            timestamp = baseTime - 1 * oneDayMs,
            status = MatchStatus.TOSS_COMPLETED,
            fairnessProfile = "Casual"
        )

        // Status filter
        store.applyMatchFilters(statusFilter = MatchStatusFilter.TEAMS_CONFIRMED)
        assertEquals(listOf(mBalancedConfirmed), store.uiState.value.items.map { it.id })

        store.applyMatchFilters(statusFilter = MatchStatusFilter.TOSS_COMPLETED)
        assertEquals(listOf(mCasualTossDone), store.uiState.value.items.map { it.id })

        // Profile filter alone
        store.clearFilters()
        store.applyMatchFilters(fairnessProfile = "Strict")
        assertEquals(setOf(mStrictRecentCompleted, mStrictOldCompleted), store.uiState.value.items.map { it.id }.toSet())

        // Combined: Status=COMPLETED + Profile=Strict + Date=LAST_7_DAYS
        store.applyMatchFilters(
            statusFilter = MatchStatusFilter.COMPLETED,
            fairnessProfile = "Strict",
            dateRange = MatchDateRangeFilter.LAST_7_DAYS
        )
        assertEquals(listOf(mStrictRecentCompleted), store.uiState.value.items.map { it.id })
    }

    @Test
    fun `10_11_12_13_14_15_match_details_displays_multiple_teams_joker_excluded_fairness_breakdown_and_toss_history`() = runBlocking {
        val threeTeams = listOf(
            GeneratedTeam(1, "Team A", listOf("Rahul", "Kiran"), listOf("p1", "p2"), 15, 7.5, 8, 7),
            GeneratedTeam(2, "Team B", listOf("Arjun", "Naveen"), listOf("p3", "p4"), 14, 7.0, 8, 6),
            GeneratedTeam(3, "Team C", listOf("Sai", "Ajay"), listOf("p5", "p6"), 14, 7.0, 7, 7)
        )
        val matchId = insertSampleMatch(
            matchNumber = 24,
            teams = threeTeams,
            joker = "Varun",
            excludedNames = listOf("Rohit", "Dev"),
            fairnessScore = 86.0,
            fairnessRating = "Excellent",
            fairnessProfile = "Balanced",
            qualityGateOutcome = "Passed",
            teammatePenalty = 3,
            opponentPenalty = 2,
            strengthDiff = 1,
            hasFairnessBreakdown = true,
            latestTossChoice = "TAILS",
            latestTossResult = "TAILS",
            latestTossWon = true
        )

        repository.insertToss(
            TossEntity(
                timestamp = baseTime + 1000L,
                choice = "Heads",
                result = "Tails",
                hasWon = false,
                matchId = matchId,
                attemptNumber = 1
            )
        )
        repository.insertToss(
            TossEntity(
                timestamp = baseTime + 5000L,
                choice = "Tails",
                result = "Tails",
                hasWon = true,
                matchId = matchId,
                attemptNumber = 2
            )
        )

        val details = store.getMatchDetails(matchId)
        assertNotNull(details)
        assertEquals("MATCH #024", details!!.displayTitle)
        assertEquals(3, details.teams.size)
        assertEquals("Varun", details.joker)
        assertEquals(listOf("Rohit", "Dev"), details.excludedPlayers)

        // Historical fairness breakdown
        val bd = details.fairnessBreakdown
        assertNotNull(bd)
        assertEquals(86.0, bd!!.overallScore, 0.01)
        assertEquals(88.0, bd.teammateVarietyScore, 0.01)
        assertEquals(84.0, bd.opponentVarietyScore, 0.01)
        assertEquals(90.0, bd.teamStrengthScore, 0.01)
        assertEquals(100.0, bd.jokerFairnessScore, 0.01)
        assertEquals(3, bd.teammatePenalty)
        assertEquals(2, bd.opponentPenalty)
        assertEquals(1, bd.teamStrengthDifference)
        assertEquals("Passed", bd.qualityGateOutcome)

        // Chronological toss history with latest identified
        assertEquals(2, details.tossHistory.size)
        assertEquals(1, details.tossHistory[0].attemptNumber)
        assertFalse(details.tossHistory[0].wonToss)
        assertEquals(2, details.tossHistory[1].attemptNumber)
        assertTrue(details.tossHistory[1].wonToss)
        assertEquals(2, details.latestToss?.attemptNumber)
    }

    @Test
    fun `16_legacy_records_without_breakdown_or_toss_or_match_number_render_safely_without_fabricated_values`() = runBlocking {
        val legacyId = "legacy_match_42"
        repository.insertSession(
            SessionEntity(
                playerIdsJson = "[\"A\",\"B\",\"C\",\"D\"]",
                teamCount = 2,
                timestamp = baseTime - 10 * oneDayMs,
                overallFairnessScore = 0.0,
                fairnessRating = "",
                fairnessProfile = "Legacy Result",
                qualityGateOutcome = "",
                matchId = legacyId,
                matchStatus = MatchStatus.TEAMS_GENERATED.name,
                finalTeamsJson = "",
                completedAt = 0L,
                matchNumber = null,
                hasFairnessBreakdown = false
            )
        )

        store.loadMatchHistory()
        val item = store.uiState.value.items.single()
        assertEquals("Legacy Match", item.displayTitle)
        assertNull(item.fairnessScore)
        assertNull(item.fairnessProfile)
        assertNull(item.tossResult)

        val details = store.getMatchDetails(legacyId)!!
        assertEquals("LEGACY MATCH", details.displayTitle)
        assertNull(details.fairnessBreakdown)
        assertNull(details.latestToss)
        assertTrue(details.tossHistory.isEmpty())
        assertEquals(2, details.teams.size)
    }

    @Test
    fun `17_18_19_deleting_match_removes_only_selected_match_and_its_tosses_while_preserving_players_and_other_matches`() = runBlocking {
        // Seed player profile
        repository.insertPlayer(
            PlayerEntity(
                id = "player_rahul",
                displayName = "Rahul",
                matchesPlayed = 5,
                totalMatches = 5,
                skillRating = 8
            )
        )

        val matchToDelete = insertSampleMatch(matchNumber = 1)
        val matchToKeep = insertSampleMatch(matchNumber = 2)

        repository.insertToss(TossEntity(choice = "Heads", result = "Heads", hasWon = true, matchId = matchToDelete, attemptNumber = 1))
        repository.insertToss(TossEntity(choice = "Tails", result = "Heads", hasWon = false, matchId = matchToDelete, attemptNumber = 2))
        repository.insertToss(TossEntity(choice = "Tails", result = "Tails", hasWon = true, matchId = matchToKeep, attemptNumber = 1))

        val deleted = store.deleteMatch(matchToDelete)
        assertTrue(deleted)

        // Selected match & its tosses are gone
        assertNull(repository.getSessionByMatchId(matchToDelete))
        assertTrue(repository.getTossesForMatchSync(matchToDelete).isEmpty())

        // Unrelated match & its tosses remain intact
        assertNotNull(repository.getSessionByMatchId(matchToKeep))
        assertEquals(1, repository.getTossesForMatchSync(matchToKeep).size)

        // Player profile & career statistics remain completely intact
        val player = repository.getPlayerById("player_rahul")
        assertNotNull(player)
        assertEquals(5, player!!.matchesPlayed)
        assertEquals(8, player.skillRating)
    }

    @Test
    fun `20_viewing_match_history_and_details_never_mutates_player_stats_or_creates_tosses`() = runBlocking {
        repository.insertPlayer(
            PlayerEntity(id = "p_kiran", displayName = "Kiran", matchesPlayed = 3, totalMatches = 3, skillRating = 7)
        )
        val matchId = insertSampleMatch(matchNumber = 7)
        repository.insertToss(TossEntity(choice = "Heads", result = "Heads", hasWon = true, matchId = matchId, attemptNumber = 1))

        // Repeatedly browse history and open details
        repeat(3) {
            store.loadMatchHistory()
            store.getMatchDetails(matchId)
        }

        val playerAfter = repository.getPlayerById("p_kiran")!!
        assertEquals(3, playerAfter.matchesPlayed)
        assertEquals(1, repository.getTossesForMatchSync(matchId).size)
    }

    @Test
    fun `end_to_end_lifecycle_create_complete_search_filter_details_restart_and_delete`() = runBlocking {
        // 1. Seed player library
        repository.insertPlayer(PlayerEntity(id = "p_rahul", displayName = "Rahul", matchesPlayed = 1, totalMatches = 1, skillRating = 8))

        // Create and complete Match #1 via MatchTossCoordinator
        val coordinator = MatchTossCoordinator(repository)
        val match1Teams = listOf(
            GeneratedTeam(1, "Team A", listOf("Rahul", "Kiran"), listOf("p_rahul", "p_kiran"), 15, 7.5, 8, 7),
            GeneratedTeam(2, "Team B", listOf("Arjun", "Naveen"), listOf("p_arjun", "p_naveen"), 14, 7.0, 7, 7)
        )
        val match1Id = insertSampleMatch(
            matchNumber = 1,
            timestamp = baseTime - oneDayMs,
            status = MatchStatus.TEAMS_GENERATED,
            teams = match1Teams,
            fairnessScore = 89.0,
            fairnessProfile = "Strict",
            qualityGateOutcome = "Passed",
            teammatePenalty = 1,
            opponentPenalty = 0,
            strengthDiff = 1,
            latestTossChoice = "",
            latestTossResult = "",
            latestTossWon = false
        )
        coordinator.onTeamsGenerated(match1Id)
        assertTrue(coordinator.confirmTeamsForToss(match1Id, 0, match1Teams, null).isSuccess)
        coordinator.selectChoice(TossChoice.HEADS)
        coordinator.executeCoinFlip(randomProvider = { 0.8 }, animationDurationMs = 0L, timestampProvider = { baseTime - oneDayMs + 1000L }).getOrThrow()
        coordinator.prepareFlipAgain()
        coordinator.selectChoice(TossChoice.TAILS)
        coordinator.executeCoinFlip(randomProvider = { 0.9 }, animationDurationMs = 0L, timestampProvider = { baseTime - oneDayMs + 2000L }).getOrThrow()
        coordinator.finalizeMatch(timestampProvider = { baseTime - oneDayMs + 3000L }).getOrThrow()

        // Create a second unrelated match
        val match2Id = insertSampleMatch(
            matchNumber = 2,
            timestamp = baseTime,
            status = MatchStatus.COMPLETED,
            teams = listOf(
                GeneratedTeam(1, "Team A", listOf("Dev", "Vikram"), listOf("p_dev", "p_vik")),
                GeneratedTeam(2, "Team B", listOf("Rohan", "Ajay"), listOf("p_roh", "p_aj"))
            ),
            fairnessScore = 76.0,
            fairnessProfile = "Casual"
        )
        repository.insertToss(TossEntity(choice = "Heads", result = "Heads", hasWon = true, matchId = match2Id, attemptNumber = 1))

        // 2-7. Find in History, Search by player, Combined Filters, Open Details, Verify Breakdown & Toss History
        store.loadMatchHistory()
        assertEquals(2, store.uiState.value.totalCount)

        store.searchMatches("rahul")
        assertEquals(listOf(match1Id), store.uiState.value.items.map { it.id })

        store.clearFilters()
        store.applyMatchFilters(
            statusFilter = MatchStatusFilter.COMPLETED,
            fairnessProfile = "Strict",
            dateRange = MatchDateRangeFilter.LAST_7_DAYS
        )
        assertEquals(listOf(match1Id), store.uiState.value.items.map { it.id })

        val details = store.getMatchDetails(match1Id)!!
        assertEquals(89.0, details.fairnessBreakdown!!.overallScore, 0.01)
        assertEquals(2, details.tossHistory.size)
        assertEquals(TossChoice.TAILS, details.latestToss?.result)
        assertTrue(details.latestToss?.wonToss == true)

        // 8. Simulate full app restart by closing & reopening SQLite DB from disk
        db.close()
        db = openDiskDatabase(dbName)
        repository = createRepository(db)
        val restartedStore = MatchHistoryStore(repository, clockProvider = { baseTime })

        restartedStore.loadMatchHistory()
        assertEquals(2, restartedStore.uiState.value.totalCount)
        val restartedDetails = restartedStore.getMatchDetails(match1Id)!!
        assertEquals(MatchStatus.COMPLETED, restartedDetails.status)
        assertEquals(2, restartedDetails.tossHistory.size)
        assertEquals(89.0, restartedDetails.fairnessBreakdown!!.overallScore, 0.01)

        // 9. Delete match1 and verify match2, match2's toss, and Rahul's player profile remain intact
        assertTrue(restartedStore.deleteMatch(match1Id))
        assertEquals(1, restartedStore.uiState.value.totalCount)
        assertEquals(listOf(match2Id), restartedStore.uiState.value.items.map { it.id })
        assertTrue(repository.getTossesForMatchSync(match1Id).isEmpty())
        assertEquals(1, repository.getTossesForMatchSync(match2Id).size)
        assertNotNull(repository.getPlayerById("p_rahul"))
    }
}
