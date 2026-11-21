package com.example.util

import com.example.data.model.PlayerEntity

enum class PlayerStatusFilter(val displayLabel: String) {
    ACTIVE("Active"),
    ARCHIVED("Archived"),
    ALL("All")
}

enum class PlayerDirectorySortOption(val displayLabel: String) {
    ALPHABETICAL("Alphabetical (A-Z)"),
    HIGHEST_SKILL("Highest Skill"),
    MOST_MATCHES("Most Matches"),
    LEAST_MATCHES("Least Matches")
}

data class PlayerDirectoryFilter(
    val searchQuery: String = "",
    val statusFilter: PlayerStatusFilter = PlayerStatusFilter.ACTIVE,
    val sortOption: PlayerDirectorySortOption = PlayerDirectorySortOption.ALPHABETICAL
) {
    val hasActiveSearchOrFilter: Boolean
        get() = searchQuery.isNotBlank() || statusFilter != PlayerStatusFilter.ACTIVE
}

data class PlayerDirectoryItem(
    val player: PlayerEntity,
    val matchesPlayed: Int = 0,
    val matchesAsJoker: Int = 0
)

data class RelationshipFrequency(
    val playerId: String,
    val displayName: String,
    val isArchived: Boolean = false,
    val sharedMatches: Int,
    val rank: Int
)

data class PlayerMatchTimelineEntry(
    val matchId: String,
    val matchNumber: Int?,
    val matchDate: Long,
    val teamAssigned: String?,
    val isJoker: Boolean,
    val teammates: List<String>,
    val opponents: List<String>,
    val status: MatchStatus,
    val fairnessScore: Int? = null
) {
    val matchDisplayTitle: String
        get() = when {
            matchNumber != null && matchNumber > 0 -> "Match #${matchNumber.toString().padStart(3, '0')}"
            matchId.startsWith("legacy_match_") -> "Legacy Match"
            matchId.isNotBlank() -> "Match ${matchId.take(8).uppercase()}"
            else -> "Legacy Match"
        }
}

data class PlayerProfileAnalytics(
    val player: PlayerEntity,
    val totalMatchesPlayed: Int = 0,
    val matchesAsJoker: Int = 0,
    val totalTeammateAppearances: Int = 0,
    val totalOpponentAppearances: Int = 0,
    val uniqueTeammatesCount: Int = 0,
    val uniqueOpponentsCount: Int = 0,
    val mostFrequentTeammates: List<RelationshipFrequency> = emptyList(),
    val mostFrequentOpponents: List<RelationshipFrequency> = emptyList(),
    val topTeammates: List<RelationshipFrequency> = emptyList(),
    val topOpponents: List<RelationshipFrequency> = emptyList(),
    val matchTimeline: List<PlayerMatchTimelineEntry> = emptyList()
) {
    val hasMatchData: Boolean
        get() = totalMatchesPlayed > 0

    val hasTeammateChemistryData: Boolean
        get() = topTeammates.isNotEmpty()

    val hasOpponentHistoryData: Boolean
        get() = topOpponents.isNotEmpty()

    fun mostFrequentTeammateSummary(): String {
        if (mostFrequentTeammates.isEmpty()) return "Not enough data"
        val names = mostFrequentTeammates.joinToString(" & ") { it.displayName }
        val count = mostFrequentTeammates.first().sharedMatches
        val suffix = if (count == 1) "match" else "matches"
        return "$names ($count $suffix)"
    }

    fun mostFrequentOpponentSummary(): String {
        if (mostFrequentOpponents.isEmpty()) return "Not enough data"
        val names = mostFrequentOpponents.joinToString(" & ") { it.displayName }
        val count = mostFrequentOpponents.first().sharedMatches
        val suffix = if (count == 1) "match" else "matches"
        return "$names ($count $suffix)"
    }
}

sealed class PlayerRemovalResult {
    data class ArchivedBecauseReferenced(val playerId: String) : PlayerRemovalResult()
    data class Deleted(val playerId: String) : PlayerRemovalResult()
    data object NotFound : PlayerRemovalResult()
}

data class PlayerDirectoryUiState(
    val items: List<PlayerDirectoryItem> = emptyList(),
    val totalPlayersCount: Int = 0,
    val totalActiveCount: Int = 0,
    val totalArchivedCount: Int = 0,
    val filter: PlayerDirectoryFilter = PlayerDirectoryFilter(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class PlayerProfileUiState(
    val analytics: PlayerProfileAnalytics? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusBannerMessage: String? = null
)
