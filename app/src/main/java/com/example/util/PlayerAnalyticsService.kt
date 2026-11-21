package com.example.util

import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity
import org.json.JSONArray
import kotlin.math.roundToInt

/**
 * Canonical, deterministic, pure-function Player Statistics & Analytics aggregation service.
 *
 * Guarantees:
 * 1. Identifies players strictly by unique player IDs (using unambiguous name lookup ONLY as a
 *    fallback for legacy records that predate ID persistence).
 * 2. Never counts the same match twice (deduplicates by canonical matchId).
 * 3. Excludes DRAFT sessions and unconfirmed TEAMS_GENERATED shuffles (only confirmed/accepted
 *    matches and pre-v9 legacy matches are counted).
 * 4. Excludes players marked as excluded in a match from participation, teammate, and opponent stats.
 * 5. Handles Joker participation according to Missing XI's Joker rules.
 * 6. Excludes invalid, deleted, or unavailable player references from teammate/opponent chemistry
 *    while preserving archived players that still exist in the player library.
 * 7. Never crashes on legacy or partially populated records and never fabricates missing team splits.
 * 8. Recomputes cleanly from canonical SQLite match records so deleting a match automatically
 *    updates derived statistics without stale or double-counted totals.
 */
object PlayerAnalyticsService {

    const val MIN_SKILL_RATING = 1
    const val MAX_SKILL_RATING = 10
    const val DEFAULT_SKILL_RATING = 5
    const val TOP_RELATIONSHIPS_LIMIT = 5

    private data class ResolvedMember(
        val playerId: String?,
        val displayName: String
    )

    fun validateSkillRating(rating: Int): Result<Int> {
        return if (rating in MIN_SKILL_RATING..MAX_SKILL_RATING) {
            Result.success(rating)
        } else {
            Result.failure(
                IllegalArgumentException(
                    "Skill rating must be an integer between $MIN_SKILL_RATING and $MAX_SKILL_RATING."
                )
            )
        }
    }

    /**
     * Determines whether a stored [SessionEntity] represents an accepted/confirmed match
     * eligible for player participation and chemistry statistics.
     */
    fun isCountableMatch(session: SessionEntity): Boolean {
        val status = MatchStatus.fromString(session.matchStatus)
        return when (status) {
            MatchStatus.TEAMS_CONFIRMED,
            MatchStatus.TOSS_COMPLETED,
            MatchStatus.COMPLETED -> true
            MatchStatus.DRAFT -> false
            MatchStatus.TEAMS_GENERATED -> {
                // Pre-v9 legacy records predating the confirmation lifecycle defaulted to TEAMS_GENERATED
                // with legacy_match_ IDs and empty finalTeamsJson.
                session.matchId.startsWith("legacy_match_") && session.finalTeamsJson.isBlank()
            }
        }
    }

    /**
     * Builds derived directory items for all players (active & archived) with filtering and sorting.
     */
    fun buildDirectoryItems(
        allPlayers: List<PlayerEntity>,
        allSessions: List<SessionEntity>,
        filter: PlayerDirectoryFilter = PlayerDirectoryFilter()
    ): List<PlayerDirectoryItem> {
        if (allPlayers.isEmpty()) return emptyList()

        val summaryCounts = aggregateMatchAndJokerCounts(allPlayers, allSessions)

        val statusFiltered = when (filter.statusFilter) {
            PlayerStatusFilter.ACTIVE -> allPlayers.filter { !it.isArchived }
            PlayerStatusFilter.ARCHIVED -> allPlayers.filter { it.isArchived }
            PlayerStatusFilter.ALL -> allPlayers
        }

        val query = filter.searchQuery.trim().lowercase()
        val searchFiltered = if (query.isEmpty()) {
            statusFiltered
        } else {
            statusFiltered.filter { player ->
                player.displayName.lowercase().contains(query) ||
                    (player.nickname?.lowercase()?.contains(query) == true)
            }
        }

        val items = searchFiltered.map { player ->
            val counts = summaryCounts[player.id]
            PlayerDirectoryItem(
                player = player,
                matchesPlayed = counts?.first ?: 0,
                matchesAsJoker = counts?.second ?: 0
            )
        }

        return when (filter.sortOption) {
            PlayerDirectorySortOption.ALPHABETICAL -> items.sortedWith(
                compareBy<PlayerDirectoryItem> { it.player.displayName.lowercase() }
                    .thenBy { it.player.id }
            )
            PlayerDirectorySortOption.HIGHEST_SKILL -> items.sortedWith(
                compareByDescending<PlayerDirectoryItem> { it.player.skillRating }
                    .thenBy { it.player.displayName.lowercase() }
                    .thenBy { it.player.id }
            )
            PlayerDirectorySortOption.MOST_MATCHES -> items.sortedWith(
                compareByDescending<PlayerDirectoryItem> { it.matchesPlayed }
                    .thenBy { it.player.displayName.lowercase() }
                    .thenBy { it.player.id }
            )
            PlayerDirectorySortOption.LEAST_MATCHES -> items.sortedWith(
                compareBy<PlayerDirectoryItem> { it.matchesPlayed }
                    .thenBy { it.player.displayName.lowercase() }
                    .thenBy { it.player.id }
            )
        }
    }

    /**
     * Computes complete individual performance analytics, teammate chemistry, opponent history,
     * and read-only match timeline for [playerId] strictly from canonical match records.
     */
    fun computePlayerProfileAnalytics(
        playerId: String,
        allPlayers: List<PlayerEntity>,
        allSessions: List<SessionEntity>
    ): PlayerProfileAnalytics? {
        val playersById = allPlayers.associateBy { it.id }
        val targetPlayer = playersById[playerId] ?: return null
        val uniqueNameToId = buildUnambiguousNameIndex(allPlayers)

        val canonicalMatches = deduplicateCountableSessions(allSessions)

        var totalMatchesPlayed = 0
        var matchesAsJoker = 0
        var totalTeammateAppearances = 0
        var totalOpponentAppearances = 0

        val teammateCounts = mutableMapOf<String, Int>()
        val opponentCounts = mutableMapOf<String, Int>()
        val timeline = mutableListOf<PlayerMatchTimelineEntry>()

        for (session in canonicalMatches) {
            val excludedIds = resolveExcludedPlayerIds(session, playersById, uniqueNameToId)
            if (playerId in excludedIds) continue

            val snapshot = MatchTossEngine.parseTeamsJsonWithMetadata(session.finalTeamsJson)
            val matchStatus = MatchStatus.fromString(session.matchStatus)
            val storedFairness = if (session.hasFairnessBreakdown || (session.overallFairnessScore > 0.0 && session.fairnessRating.isNotBlank())) {
                session.overallFairnessScore.roundToInt()
            } else {
                null
            }
            val canonicalMatchId = session.matchId.ifBlank { "legacy_match_${session.id}" }

            if (snapshot.teams.isNotEmpty() || !snapshot.jokerId.isNullOrBlank() || !snapshot.jokerName.isNullOrBlank()) {
                val resolvedTeams = snapshot.teams.map { team ->
                    team to resolveTeamMembers(team, playersById, uniqueNameToId, excludedIds)
                }

                val targetTeamEntry = resolvedTeams.firstOrNull { (_, members) ->
                    members.any { it.playerId == playerId }
                }

                val resolvedJokerId = resolveJokerPlayerId(
                    jokerId = snapshot.jokerId,
                    jokerName = snapshot.jokerName,
                    playersById = playersById,
                    uniqueNameToId = uniqueNameToId,
                    excludedIds = excludedIds
                )

                val isTargetJoker = resolvedJokerId == playerId && targetTeamEntry == null

                if (targetTeamEntry != null) {
                    val (assignedTeam, assignedMembers) = targetTeamEntry
                    totalMatchesPlayed++

                    val teammatesInMatch = assignedMembers
                        .filter { it.playerId != playerId }
                        .distinctBy { it.playerId ?: "name:${it.displayName.lowercase()}" }

                    val opponentsInMatch = resolvedTeams
                        .filter { (team, _) -> team.teamNumber != assignedTeam.teamNumber || team.name != assignedTeam.name }
                        .flatMap { (_, members) -> members }
                        .filter { it.playerId != playerId }
                        .distinctBy { it.playerId ?: "name:${it.displayName.lowercase()}" }

                    val teammateIdsInMatch = teammatesInMatch
                        .mapNotNull { it.playerId }
                        .filter { it != playerId && it in playersById }
                        .distinct()

                    val opponentIdsInMatch = opponentsInMatch
                        .mapNotNull { it.playerId }
                        .filter { it != playerId && it in playersById && it !in teammateIdsInMatch }
                        .distinct()

                    for (tmId in teammateIdsInMatch) {
                        teammateCounts[tmId] = (teammateCounts[tmId] ?: 0) + 1
                        totalTeammateAppearances++
                    }

                    for (oppId in opponentIdsInMatch) {
                        opponentCounts[oppId] = (opponentCounts[oppId] ?: 0) + 1
                        totalOpponentAppearances++
                    }

                    timeline.add(
                        PlayerMatchTimelineEntry(
                            matchId = canonicalMatchId,
                            matchNumber = session.matchNumber,
                            matchDate = session.timestamp,
                            teamAssigned = assignedTeam.name.ifBlank { "Team ${assignedTeam.teamNumber}" },
                            isJoker = false,
                            teammates = teammatesInMatch.map { m ->
                                m.playerId?.let { playersById[it]?.displayName } ?: m.displayName
                            },
                            opponents = opponentsInMatch.map { m ->
                                m.playerId?.let { playersById[it]?.displayName } ?: m.displayName
                            },
                            status = matchStatus,
                            fairnessScore = storedFairness
                        )
                    )
                } else if (isTargetJoker) {
                    // Per Missing XI Joker rules: Joker plays across teams (not fixed to a single team),
                    // so Joker increments totalMatchesPlayed & matchesAsJoker without fabricating
                    // fixed-team teammate or opponent pair counts.
                    totalMatchesPlayed++
                    matchesAsJoker++

                    val allTeamPlayers = resolvedTeams.flatMap { (_, members) -> members }
                        .filter { it.playerId != playerId }
                        .distinctBy { it.playerId ?: "name:${it.displayName.lowercase()}" }
                        .map { m -> m.playerId?.let { playersById[it]?.displayName } ?: m.displayName }

                    timeline.add(
                        PlayerMatchTimelineEntry(
                            matchId = canonicalMatchId,
                            matchNumber = session.matchNumber,
                            matchDate = session.timestamp,
                            teamAssigned = "Joker",
                            isJoker = true,
                            teammates = emptyList(),
                            opponents = allTeamPlayers,
                            status = matchStatus,
                            fairnessScore = storedFairness
                        )
                    )
                }
            } else {
                // Legacy match record with no finalTeamsJson: check flat playerIdsJson without
                // reconstructing or inventing team splits.
                val legacyParticipantIds = resolveLegacyParticipantIds(
                    playerIdsJson = session.playerIdsJson,
                    playersById = playersById,
                    uniqueNameToId = uniqueNameToId,
                    excludedIds = excludedIds
                )
                if (playerId in legacyParticipantIds) {
                    totalMatchesPlayed++
                    timeline.add(
                        PlayerMatchTimelineEntry(
                            matchId = canonicalMatchId,
                            matchNumber = session.matchNumber,
                            matchDate = session.timestamp,
                            teamAssigned = null,
                            isJoker = false,
                            teammates = emptyList(),
                            opponents = emptyList(),
                            status = matchStatus,
                            fairnessScore = storedFairness
                        )
                    )
                }
            }
        }

        val rankedTeammates = rankRelationships(teammateCounts, playersById)
        val rankedOpponents = rankRelationships(opponentCounts, playersById)

        val topTeammates = rankedTeammates.take(TOP_RELATIONSHIPS_LIMIT)
        val topOpponents = rankedOpponents.take(TOP_RELATIONSHIPS_LIMIT)

        val mostFrequentTeammates = rankedTeammates.filter { it.rank == 1 }
        val mostFrequentOpponents = rankedOpponents.filter { it.rank == 1 }

        val sortedTimeline = timeline.sortedByDescending { it.matchDate }

        return PlayerProfileAnalytics(
            player = targetPlayer,
            totalMatchesPlayed = totalMatchesPlayed,
            matchesAsJoker = matchesAsJoker,
            totalTeammateAppearances = totalTeammateAppearances,
            totalOpponentAppearances = totalOpponentAppearances,
            uniqueTeammatesCount = rankedTeammates.size,
            uniqueOpponentsCount = rankedOpponents.size,
            mostFrequentTeammates = mostFrequentTeammates,
            mostFrequentOpponents = mostFrequentOpponents,
            topTeammates = topTeammates,
            topOpponents = topOpponents,
            matchTimeline = sortedTimeline
        )
    }

    /**
     * Checks whether [playerId] is referenced in any stored historical match record (regardless of status),
     * used to enforce safe archiving instead of permanent deletion.
     */
    fun isPlayerReferencedInAnyMatch(
        playerId: String,
        allPlayers: List<PlayerEntity>,
        allSessions: List<SessionEntity>
    ): Boolean {
        if (playerId.isBlank() || allSessions.isEmpty()) return false
        val playersById = allPlayers.associateBy { it.id }
        val uniqueNameToId = buildUnambiguousNameIndex(allPlayers)

        for (session in allSessions) {
            val snapshot = MatchTossEngine.parseTeamsJsonWithMetadata(session.finalTeamsJson)
            if (snapshot.teams.any { team ->
                    team.playerIds.any { it == playerId } ||
                        (team.playerIds.isEmpty() && team.players.any { uniqueNameToId[it.trim().lowercase()] == playerId })
                }
            ) {
                return true
            }

            if (snapshot.jokerId == playerId) return true
            if (snapshot.jokerId.isNullOrBlank() && !snapshot.jokerName.isNullOrBlank()) {
                if (uniqueNameToId[snapshot.jokerName.trim().lowercase()] == playerId) return true
            }

            val excludedIds = resolveExcludedPlayerIds(session, playersById, uniqueNameToId)
            if (playerId in excludedIds) return true

            val legacyEntries = parseJsonStringList(session.playerIdsJson)
            for (entry in legacyEntries) {
                if (entry == playerId) return true
                if (entry !in playersById && uniqueNameToId[entry.trim().lowercase()] == playerId) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns only active (non-archived) players eligible for new team generation.
     */
    fun filterSelectablePlayersForTeamGeneration(allPlayers: List<PlayerEntity>): List<PlayerEntity> {
        return allPlayers.filter { !it.isArchived }
    }

    private fun aggregateMatchAndJokerCounts(
        allPlayers: List<PlayerEntity>,
        allSessions: List<SessionEntity>
    ): Map<String, Pair<Int, Int>> {
        val playersById = allPlayers.associateBy { it.id }
        val uniqueNameToId = buildUnambiguousNameIndex(allPlayers)
        val canonicalMatches = deduplicateCountableSessions(allSessions)

        val matchCounts = mutableMapOf<String, Int>()
        val jokerCounts = mutableMapOf<String, Int>()

        for (session in canonicalMatches) {
            val excludedIds = resolveExcludedPlayerIds(session, playersById, uniqueNameToId)
            val snapshot = MatchTossEngine.parseTeamsJsonWithMetadata(session.finalTeamsJson)

            if (snapshot.teams.isNotEmpty() || !snapshot.jokerId.isNullOrBlank() || !snapshot.jokerName.isNullOrBlank()) {
                val participatingTeamIds = mutableSetOf<String>()
                for (team in snapshot.teams) {
                    val members = resolveTeamMembers(team, playersById, uniqueNameToId, excludedIds)
                    for (member in members) {
                        val pid = member.playerId
                        if (pid != null && pid in playersById && pid !in excludedIds) {
                            participatingTeamIds.add(pid)
                        }
                    }
                }

                for (pid in participatingTeamIds) {
                    matchCounts[pid] = (matchCounts[pid] ?: 0) + 1
                }

                val jokerId = resolveJokerPlayerId(
                    jokerId = snapshot.jokerId,
                    jokerName = snapshot.jokerName,
                    playersById = playersById,
                    uniqueNameToId = uniqueNameToId,
                    excludedIds = excludedIds
                )
                if (jokerId != null && jokerId in playersById && jokerId !in participatingTeamIds) {
                    matchCounts[jokerId] = (matchCounts[jokerId] ?: 0) + 1
                    jokerCounts[jokerId] = (jokerCounts[jokerId] ?: 0) + 1
                }
            } else {
                val legacyIds = resolveLegacyParticipantIds(
                    playerIdsJson = session.playerIdsJson,
                    playersById = playersById,
                    uniqueNameToId = uniqueNameToId,
                    excludedIds = excludedIds
                )
                for (pid in legacyIds) {
                    matchCounts[pid] = (matchCounts[pid] ?: 0) + 1
                }
            }
        }

        return allPlayers.associate { player ->
            player.id to Pair(matchCounts[player.id] ?: 0, jokerCounts[player.id] ?: 0)
        }
    }

    private fun deduplicateCountableSessions(allSessions: List<SessionEntity>): List<SessionEntity> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<SessionEntity>()
        for (session in allSessions) {
            if (!isCountableMatch(session)) continue
            val key = session.matchId.ifBlank { "legacy_session_${session.id}" }
            if (seenKeys.add(key)) {
                result.add(session)
            }
        }
        return result
    }

    /**
     * Maps lowercase displayName -> playerId ONLY when a display name is unique across all players.
     * Used strictly as a fallback for legacy records that did not store canonical player IDs.
     */
    private fun buildUnambiguousNameIndex(allPlayers: List<PlayerEntity>): Map<String, String> {
        val grouped = allPlayers.groupBy { it.displayName.trim().lowercase() }
        val map = mutableMapOf<String, String>()
        for ((normalizedName, matches) in grouped) {
            if (normalizedName.isNotBlank() && matches.size == 1) {
                map[normalizedName] = matches.first().id
            }
        }
        return map
    }

    private fun resolveTeamMembers(
        team: GeneratedTeam,
        playersById: Map<String, PlayerEntity>,
        uniqueNameToId: Map<String, String>,
        excludedIds: Set<String>
    ): List<ResolvedMember> {
        val count = maxOf(team.playerIds.size, team.players.size)
        val members = mutableListOf<ResolvedMember>()
        val seenIds = mutableSetOf<String>()

        for (i in 0 until count) {
            val rawId = team.playerIds.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }
            val rawName = team.players.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() }

            val canonicalId: String? = when {
                rawId != null && rawId in playersById -> rawId
                rawId != null && rawName != null && rawId.equals(rawName, ignoreCase = true) -> {
                    // Legacy team where playerIds simply echoed display names
                    uniqueNameToId[rawName.lowercase()]
                }
                rawId != null -> {
                    // An explicit ID that is no longer in playersById (e.g. deleted or invalid ID)
                    null
                }
                rawName != null -> uniqueNameToId[rawName.lowercase()]
                else -> null
            }

            if (canonicalId != null && canonicalId in excludedIds) continue

            val resolvedName = canonicalId?.let { playersById[it]?.displayName }
                ?: rawName
                ?: rawId
                ?: continue

            if (canonicalId != null) {
                if (seenIds.add(canonicalId)) {
                    members.add(ResolvedMember(playerId = canonicalId, displayName = resolvedName))
                }
            } else {
                members.add(ResolvedMember(playerId = null, displayName = resolvedName))
            }
        }
        return members
    }

    private fun resolveJokerPlayerId(
        jokerId: String?,
        jokerName: String?,
        playersById: Map<String, PlayerEntity>,
        uniqueNameToId: Map<String, String>,
        excludedIds: Set<String>
    ): String? {
        val resolved = when {
            !jokerId.isNullOrBlank() && jokerId in playersById -> jokerId
            !jokerId.isNullOrBlank() -> null
            !jokerName.isNullOrBlank() -> uniqueNameToId[jokerName.trim().lowercase()]
            else -> null
        }
        return if (resolved != null && resolved !in excludedIds) resolved else null
    }

    private fun resolveExcludedPlayerIds(
        session: SessionEntity,
        playersById: Map<String, PlayerEntity>,
        uniqueNameToId: Map<String, String>
    ): Set<String> {
        val excluded = mutableSetOf<String>()
        for (rawId in parseJsonStringList(session.excludedPlayerIdsJson)) {
            if (rawId in playersById) {
                excluded.add(rawId)
            } else {
                uniqueNameToId[rawId.trim().lowercase()]?.let { excluded.add(it) }
            }
        }
        for (rawName in parseJsonStringList(session.excludedPlayerNamesJson)) {
            uniqueNameToId[rawName.trim().lowercase()]?.let { excluded.add(it) }
        }
        return excluded
    }

    private fun resolveLegacyParticipantIds(
        playerIdsJson: String,
        playersById: Map<String, PlayerEntity>,
        uniqueNameToId: Map<String, String>,
        excludedIds: Set<String>
    ): Set<String> {
        val participants = mutableSetOf<String>()
        for (entry in parseJsonStringList(playerIdsJson)) {
            val resolved = when {
                entry in playersById -> entry
                else -> uniqueNameToId[entry.trim().lowercase()]
            }
            if (resolved != null && resolved !in excludedIds) {
                participants.add(resolved)
            }
        }
        return participants
    }

    private fun rankRelationships(
        counts: Map<String, Int>,
        playersById: Map<String, PlayerEntity>
    ): List<RelationshipFrequency> {
        val validEntries = counts.entries
            .filter { (id, count) -> count > 0 && id in playersById }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { playersById[it.key]?.displayName?.lowercase() ?: "" }
                    .thenBy { it.key }
            )

        val ranked = mutableListOf<RelationshipFrequency>()
        var previousCount: Int? = null
        var currentRank = 0

        validEntries.forEachIndexed { index, (id, count) ->
            val player = playersById[id] ?: return@forEachIndexed
            val rank = if (previousCount != null && count == previousCount) {
                currentRank
            } else {
                index + 1
            }
            currentRank = rank
            previousCount = count

            ranked.add(
                RelationshipFrequency(
                    playerId = id,
                    displayName = player.displayName,
                    isArchived = player.isArchived,
                    sharedMatches = count,
                    rank = rank
                )
            )
        }
        return ranked
    }

    private fun parseJsonStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).trim().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
