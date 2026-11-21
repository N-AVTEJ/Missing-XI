package com.example.util

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.data.model.SessionEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Dedicated Match History store handling SQLite-level search, combined filters,
 * consistent sorting (with null-safe fairness handling), pagination, lazy read-only
 * match details loading, and transactional match deletion.
 *
 * Keeps all database queries outside UI composables.
 */
class MatchHistoryStore(
    private val repository: AppRepository,
    private val clockProvider: () -> Long = { System.currentTimeMillis() },
    private val pageSize: Int = DEFAULT_PAGE_SIZE
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        private const val MILLIS_PER_DAY = 86_400_000L
    }

    private val _uiState = MutableStateFlow(MatchHistoryUiState(pageSize = pageSize))
    val uiState: StateFlow<MatchHistoryUiState> = _uiState.asStateFlow()

    private val _selectedMatchDetails = MutableStateFlow<MatchDetailsData?>(null)
    val selectedMatchDetails: StateFlow<MatchDetailsData?> = _selectedMatchDetails.asStateFlow()

    suspend fun loadMatchHistory() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            val profiles = repository.getDistinctFairnessProfiles()
                .filter { it.isNotBlank() && !it.equals("Legacy Result", ignoreCase = true) }

            val filter = _uiState.value.filter
            val (countQuery, pageQuery) = buildQueries(filter, limit = pageSize, offset = 0)
            val totalCount = repository.countSessionsFiltered(countQuery)
            val rows = repository.querySessionsPaginated(pageQuery)
            val items = rows.map { it.toHistoryItem() }

            _uiState.value = _uiState.value.copy(
                items = items,
                totalCount = totalCount,
                currentPage = 1,
                hasMorePages = items.size < totalCount,
                isLoading = false,
                isLoadingMore = false,
                availableProfiles = profiles,
                errorMessage = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false,
                errorMessage = "Could not load match history: ${e.message ?: "Database error"}"
            )
        }
    }

    suspend fun loadNextPage() {
        val current = _uiState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMorePages) return

        _uiState.value = current.copy(isLoadingMore = true, errorMessage = null)
        try {
            val offset = current.items.size
            val (_, pageQuery) = buildQueries(current.filter, limit = pageSize, offset = offset)
            val nextRows = repository.querySessionsPaginated(pageQuery)
            val nextItems = nextRows.map { it.toHistoryItem() }
            val merged = (current.items + nextItems).distinctBy { it.id }

            _uiState.value = _uiState.value.copy(
                items = merged,
                currentPage = current.currentPage + 1,
                hasMorePages = merged.size < current.totalCount && nextItems.isNotEmpty(),
                isLoadingMore = false,
                errorMessage = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingMore = false,
                errorMessage = "Could not load more matches: ${e.message ?: "Database error"}"
            )
        }
    }

    suspend fun searchMatches(query: String) {
        val updated = _uiState.value.filter.copy(searchQuery = query)
        _uiState.value = _uiState.value.copy(filter = updated)
        loadMatchHistory()
    }

    suspend fun applyMatchFilters(
        statusFilter: MatchStatusFilter = _uiState.value.filter.statusFilter,
        fairnessProfile: String? = _uiState.value.filter.fairnessProfile,
        dateRange: MatchDateRangeFilter = _uiState.value.filter.dateRange
    ) {
        val updated = _uiState.value.filter.copy(
            statusFilter = statusFilter,
            fairnessProfile = fairnessProfile?.takeIf { it.isNotBlank() },
            dateRange = dateRange
        )
        _uiState.value = _uiState.value.copy(filter = updated)
        loadMatchHistory()
    }

    suspend fun clearFilters() {
        val currentSort = _uiState.value.filter.sortOrder
        _uiState.value = _uiState.value.copy(
            filter = MatchHistoryFilter(sortOrder = currentSort)
        )
        loadMatchHistory()
    }

    suspend fun sortMatches(sortOrder: MatchSortOrder) {
        val updated = _uiState.value.filter.copy(sortOrder = sortOrder)
        _uiState.value = _uiState.value.copy(filter = updated)
        loadMatchHistory()
    }

    suspend fun refreshHistory() {
        loadMatchHistory()
    }

    /**
     * Fetches full read-only Match Details and chronological Toss History lazily from SQLite.
     * NEVER generates teams, NEVER triggers a toss, NEVER recalculates fairness, and NEVER mutates stats.
     */
    suspend fun getMatchDetails(matchId: String): MatchDetailsData? {
        if (matchId.isBlank()) {
            _selectedMatchDetails.value = null
            return null
        }

        val session = repository.getSessionByMatchId(matchId)
        if (session == null) {
            _selectedMatchDetails.value = null
            return null
        }

        val (parsedTeams, parsedJoker) = MatchTossEngine.parseTeamsJson(session.finalTeamsJson)
        val resolvedTeams = if (parsedTeams.isNotEmpty()) {
            parsedTeams
        } else {
            reconstructLegacyTeams(session.playerIdsJson, session.teamCount)
        }

        val excludedPlayers = resolveExcludedPlayers(session)
        val tossHistory = repository.getTossesForMatchSync(session.matchId)
            .map { it.toHistoryRecord() }
            .sortedBy { it.attemptNumber }

        val hasStoredFairness = session.hasFairnessBreakdown ||
            (session.overallFairnessScore > 0.0 && session.fairnessRating.isNotBlank())

        val normalizedProfile = session.fairnessProfile
            .takeIf { it.isNotBlank() && !it.equals("Legacy Result", ignoreCase = true) }

        val breakdown = if (hasStoredFairness) {
            HistoricalFairnessBreakdown(
                overallScore = session.overallFairnessScore,
                fairnessRating = session.fairnessRating.ifBlank { "Recorded" },
                teammateVarietyScore = session.teammateVarietyScore,
                opponentVarietyScore = session.opponentVarietyScore,
                teamStrengthScore = session.teamStrengthScore,
                jokerFairnessScore = session.jokerFairnessScore,
                teammatePenalty = session.teammatePenalty,
                opponentPenalty = session.opponentPenalty,
                teamStrengthDifference = session.teamStrengthDifference,
                qualityGateOutcome = session.qualityGateOutcome.takeIf { it.isNotBlank() },
                fairnessProfile = normalizedProfile
            )
        } else {
            null
        }

        val details = MatchDetailsData(
            id = session.matchId,
            dbSessionId = session.id,
            matchNumber = session.matchNumber,
            createdAt = session.timestamp,
            completedAt = session.completedAt.takeIf { it > 0L },
            status = MatchStatus.fromString(session.matchStatus),
            teams = resolvedTeams,
            joker = parsedJoker,
            excludedPlayers = excludedPlayers,
            fairnessScore = if (hasStoredFairness) session.overallFairnessScore.roundToInt() else null,
            fairnessProfile = normalizedProfile,
            qualityGateOutcome = session.qualityGateOutcome.takeIf { it.isNotBlank() },
            fairnessBreakdown = breakdown,
            wasManuallyEdited = session.wasManuallyEdited,
            tossHistory = tossHistory
        )

        _selectedMatchDetails.value = details
        return details
    }

    /**
     * Transactionally deletes a single match and its dependent toss history without
     * affecting player profiles, global fairness settings, or unrelated matches.
     */
    suspend fun deleteMatch(matchId: String): Boolean {
        if (matchId.isBlank()) return false
        return try {
            val deleted = repository.deleteMatchTransactionally(matchId)
            if (deleted) {
                if (_selectedMatchDetails.value?.id == matchId) {
                    _selectedMatchDetails.value = null
                }
                loadMatchHistory()
            }
            deleted
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to delete match: ${e.message ?: "Database error"}"
            )
            false
        }
    }

    private fun buildQueries(
        filter: MatchHistoryFilter,
        limit: Int,
        offset: Int
    ): Pair<SimpleSQLiteQuery, SimpleSQLiteQuery> {
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        // 1. Search (case-insensitive across player names, match number, and match ID)
        val rawSearch = filter.searchQuery.trim()
        if (rawSearch.isNotEmpty()) {
            val likeArg = "%${rawSearch.lowercase()}%"
            val normalizedNum = rawSearch
                .removePrefix("#")
                .removePrefix("match")
                .removePrefix("MATCH")
                .trim()
                .removePrefix("#")
                .trim()
                .toIntOrNull()

            if (normalizedNum != null) {
                whereClauses.add(
                    "(LOWER(playerIdsJson) LIKE ? OR LOWER(finalTeamsJson) LIKE ? OR LOWER(excludedPlayerNamesJson) LIKE ? OR LOWER(matchId) LIKE ? OR matchNumber = ?)"
                )
                args.add(likeArg)
                args.add(likeArg)
                args.add(likeArg)
                args.add(likeArg)
                args.add(normalizedNum)
            } else {
                whereClauses.add(
                    "(LOWER(playerIdsJson) LIKE ? OR LOWER(finalTeamsJson) LIKE ? OR LOWER(excludedPlayerNamesJson) LIKE ? OR LOWER(matchId) LIKE ?)"
                )
                args.add(likeArg)
                args.add(likeArg)
                args.add(likeArg)
                args.add(likeArg)
            }
        }

        // 2. Status filter
        when (filter.statusFilter) {
            MatchStatusFilter.ALL -> Unit
            MatchStatusFilter.COMPLETED -> {
                whereClauses.add("matchStatus = ?")
                args.add(MatchStatus.COMPLETED.name)
            }
            MatchStatusFilter.TEAMS_CONFIRMED -> {
                whereClauses.add("matchStatus = ?")
                args.add(MatchStatus.TEAMS_CONFIRMED.name)
            }
            MatchStatusFilter.TOSS_COMPLETED -> {
                whereClauses.add("matchStatus = ?")
                args.add(MatchStatus.TOSS_COMPLETED.name)
            }
            MatchStatusFilter.PENDING -> {
                whereClauses.add("matchStatus IN (?, ?, ?)")
                args.add(MatchStatus.TEAMS_GENERATED.name)
                args.add(MatchStatus.TEAMS_CONFIRMED.name)
                args.add(MatchStatus.TOSS_COMPLETED.name)
            }
        }

        // 3. Fairness Profile filter
        if (!filter.fairnessProfile.isNullOrBlank()) {
            whereClauses.add("LOWER(fairnessProfile) = ?")
            args.add(filter.fairnessProfile.trim().lowercase())
        }

        // 4. Date Range filter
        val now = clockProvider()
        val cutoffMillis: Long? = when (filter.dateRange) {
            MatchDateRangeFilter.ALL_TIME -> null
            MatchDateRangeFilter.TODAY -> {
                Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            MatchDateRangeFilter.LAST_7_DAYS -> now - (7L * MILLIS_PER_DAY)
            MatchDateRangeFilter.LAST_30_DAYS -> now - (30L * MILLIS_PER_DAY)
        }
        if (cutoffMillis != null) {
            whereClauses.add("timestamp >= ?")
            args.add(cutoffMillis)
        }

        val whereSql = if (whereClauses.isEmpty()) {
            ""
        } else {
            " WHERE " + whereClauses.joinToString(" AND ")
        }

        // 5. Consistent sorting — matches missing fairness data are always placed last on fairness sorts
        val hasFairnessExpr = "(CASE WHEN (hasFairnessBreakdown = 1 OR (overallFairnessScore > 0.0 AND fairnessRating != '')) THEN 0 ELSE 1 END)"
        val orderBySql = when (filter.sortOrder) {
            MatchSortOrder.NEWEST_FIRST -> " ORDER BY timestamp DESC, id DESC"
            MatchSortOrder.OLDEST_FIRST -> " ORDER BY timestamp ASC, id ASC"
            MatchSortOrder.HIGHEST_FAIRNESS -> " ORDER BY $hasFairnessExpr ASC, overallFairnessScore DESC, timestamp DESC"
            MatchSortOrder.LOWEST_FAIRNESS -> " ORDER BY $hasFairnessExpr ASC, overallFairnessScore ASC, timestamp DESC"
        }

        val countQuery = SimpleSQLiteQuery(
            "SELECT COUNT(*) FROM sessions$whereSql",
            args.toTypedArray()
        )

        val pageArgs = args.toMutableList().apply {
            add(limit)
            add(offset)
        }
        val pageQuery = SimpleSQLiteQuery(
            "SELECT * FROM sessions$whereSql$orderBySql LIMIT ? OFFSET ?",
            pageArgs.toTypedArray()
        )

        return countQuery to pageQuery
    }

    private fun SessionEntity.toHistoryItem(): MatchHistoryItem {
        val (parsedTeams, parsedJoker) = MatchTossEngine.parseTeamsJson(finalTeamsJson)
        val teamSizes = parsedTeams.map { it.name to it.players.size }
        val resolvedEligibleCount = when {
            totalEligiblePlayers > 0 -> totalEligiblePlayers
            parsedTeams.isNotEmpty() -> parsedTeams.sumOf { it.players.size } + if (!parsedJoker.isNullOrBlank()) 1 else 0
            else -> parseJsonStringList(playerIdsJson).size
        }
        val hasStoredFairness = hasFairnessBreakdown || (overallFairnessScore > 0.0 && fairnessRating.isNotBlank())
        val normalizedProfile = fairnessProfile
            .takeIf { it.isNotBlank() && !it.equals("Legacy Result", ignoreCase = true) }

        val tossRes = TossChoice.fromString(latestTossResult)
        val tossCh = TossChoice.fromString(latestTossChoice)

        return MatchHistoryItem(
            id = matchId.ifBlank { "legacy_match_$id" },
            dbSessionId = id,
            matchNumber = matchNumber,
            createdAt = timestamp,
            completedAt = completedAt.takeIf { it > 0L },
            status = MatchStatus.fromString(matchStatus),
            teamCount = if (parsedTeams.isNotEmpty()) parsedTeams.size else teamCount.coerceAtLeast(1),
            totalEligiblePlayers = resolvedEligibleCount,
            teamSizesSummary = teamSizes,
            fairnessScore = if (hasStoredFairness) overallFairnessScore.roundToInt() else null,
            fairnessProfile = normalizedProfile,
            tossChoice = tossCh,
            tossResult = tossRes,
            tossWon = if (tossRes != null) latestTossWon else null,
            wasManuallyEdited = wasManuallyEdited
        )
    }

    private suspend fun resolveExcludedPlayers(session: SessionEntity): List<String> {
        val storedNames = parseJsonStringList(session.excludedPlayerNamesJson)
        if (storedNames.isNotEmpty()) return storedNames

        val storedIds = parseJsonStringList(session.excludedPlayerIdsJson)
        if (storedIds.isEmpty()) return emptyList()

        return storedIds.map { id ->
            repository.getPlayerById(id)?.displayName ?: id
        }
    }

    private fun reconstructLegacyTeams(playerIdsJson: String, teamCount: Int): List<GeneratedTeam> {
        val safeTeamCount = teamCount.coerceAtLeast(1)
        val players = parseJsonStringList(playerIdsJson)
        if (players.isEmpty()) return emptyList()
        val buckets = List(safeTeamCount) { mutableListOf<String>() }
        players.forEachIndexed { index, name ->
            buckets[index % safeTeamCount].add(name)
        }
        return buckets.mapIndexed { idx, members ->
            GeneratedTeam(
                teamNumber = idx + 1,
                name = "Team ${'A' + idx}",
                players = members,
                playerIds = members
            )
        }
    }

    private fun parseJsonStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun TossEntity.toHistoryRecord(): TossHistoryRecord {
        return TossHistoryRecord(
            id = tossUuid.ifBlank { "toss_$id" },
            matchId = matchId,
            attemptNumber = attemptNumber.coerceAtLeast(1),
            userChoice = TossChoice.fromString(choice) ?: TossChoice.HEADS,
            result = TossChoice.fromString(result) ?: TossChoice.HEADS,
            wonToss = hasWon,
            createdAt = timestamp
        )
    }
}
