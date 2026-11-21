package com.example.util

import java.util.Locale

enum class MatchStatusFilter(val displayLabel: String) {
    ALL("All"),
    COMPLETED("Completed"),
    TEAMS_CONFIRMED("Teams Confirmed"),
    TOSS_COMPLETED("Toss Completed"),
    PENDING("Pending")
}

enum class MatchDateRangeFilter(val displayLabel: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

enum class MatchSortOrder(val displayLabel: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    HIGHEST_FAIRNESS("Highest Fairness"),
    LOWEST_FAIRNESS("Lowest Fairness")
}

data class MatchHistoryFilter(
    val searchQuery: String = "",
    val statusFilter: MatchStatusFilter = MatchStatusFilter.ALL,
    val fairnessProfile: String? = null,
    val dateRange: MatchDateRangeFilter = MatchDateRangeFilter.ALL_TIME,
    val sortOrder: MatchSortOrder = MatchSortOrder.NEWEST_FIRST
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            statusFilter != MatchStatusFilter.ALL ||
            !fairnessProfile.isNullOrBlank() ||
            dateRange != MatchDateRangeFilter.ALL_TIME
}

/**
 * Compact summary item rendered on the Match History dashboard.
 * Uses the existing matchId as primary identifier and avoids eager loading of full toss history.
 */
data class MatchHistoryItem(
    val id: String,
    val dbSessionId: Int = 0,
    val matchNumber: Int? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
    val status: MatchStatus,
    val teamCount: Int,
    val totalEligiblePlayers: Int,
    val teamSizesSummary: List<Pair<String, Int>> = emptyList(),
    val fairnessScore: Int? = null,
    val fairnessProfile: String? = null,
    val tossChoice: TossChoice? = null,
    val tossResult: TossChoice? = null,
    val tossWon: Boolean? = null,
    val wasManuallyEdited: Boolean = false
) {
    val displayTitle: String
        get() = if (matchNumber != null && matchNumber > 0) {
            String.format(Locale.US, "Match #%03d", matchNumber)
        } else {
            "Legacy Match"
        }

    val isLegacy: Boolean
        get() = matchNumber == null
}

/**
 * Historical fairness evaluation snapshot preserved from when the match was created/confirmed.
 * Never recalculated from current player ratings or current global settings.
 */
data class HistoricalFairnessBreakdown(
    val overallScore: Double,
    val fairnessRating: String,
    val teammateVarietyScore: Double,
    val opponentVarietyScore: Double,
    val teamStrengthScore: Double,
    val jokerFairnessScore: Double,
    val teammatePenalty: Int?,
    val opponentPenalty: Int?,
    val teamStrengthDifference: Int?,
    val qualityGateOutcome: String?,
    val fairnessProfile: String?
)

/**
 * Read-only detailed match model fetched on demand when opening Match Details.
 */
data class MatchDetailsData(
    val id: String,
    val dbSessionId: Int = 0,
    val matchNumber: Int? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
    val status: MatchStatus,
    val teams: List<GeneratedTeam>,
    val joker: String? = null,
    val excludedPlayers: List<String> = emptyList(),
    val fairnessScore: Int? = null,
    val fairnessProfile: String? = null,
    val qualityGateOutcome: String? = null,
    val fairnessBreakdown: HistoricalFairnessBreakdown? = null,
    val wasManuallyEdited: Boolean = false,
    val tossHistory: List<TossHistoryRecord> = emptyList()
) {
    val displayTitle: String
        get() = if (matchNumber != null && matchNumber > 0) {
            String.format(Locale.US, "MATCH #%03d", matchNumber)
        } else {
            "LEGACY MATCH"
        }

    val latestToss: TossHistoryRecord?
        get() = tossHistory.maxByOrNull { it.attemptNumber }

    val totalParticipatingPlayers: Int
        get() = teams.sumOf { it.players.size } + if (!joker.isNullOrBlank()) 1 else 0
}

data class MatchHistoryUiState(
    val items: List<MatchHistoryItem> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 0,
    val pageSize: Int = MatchHistoryStore.DEFAULT_PAGE_SIZE,
    val hasMorePages: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val filter: MatchHistoryFilter = MatchHistoryFilter(),
    val availableProfiles: List<String> = emptyList(),
    val errorMessage: String? = null
)
