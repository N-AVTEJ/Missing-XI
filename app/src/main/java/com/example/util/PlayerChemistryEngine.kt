package com.example.util

import com.example.data.model.PlayerEntity

data class FavoriteInfo(
    val idOrName: String,
    val matchCount: Int
)

data class DashboardStats(
    val mostActivePlayer: PlayerEntity? = null,
    val leastActivePlayer: PlayerEntity? = null,
    val highestSkillPlayer: PlayerEntity? = null,
    val mostFrequentJoker: PlayerEntity? = null,
    val averageMatchesPerPlayer: Double = 0.0,
    val averageSkillRating: Double = 0.0,
    val totalRegisteredPlayers: Int = 0,
    val totalMatchesGenerated: Int = 0
)

enum class PlayerSortOption(val displayName: String) {
    ALPHABETICAL("Alphabetically (A-Z)"),
    RECENTLY_PLAYED("Recently Played"),
    MOST_MATCHES("Most Matches"),
    HIGHEST_SKILL("Highest Skill"),
    FAVORITES_FIRST("Favorites First"),
    LEAST_ACTIVE("Least Active")
}

object PlayerChemistryEngine {

    fun getFavoriteTeammate(
        playerKey: String,
        teammatePairCounts: Map<String, Int>
    ): FavoriteInfo? {
        if (playerKey.isBlank() || teammatePairCounts.isEmpty()) return null
        var maxCount = 0
        var favoriteKey: String? = null

        for ((pairKey, count) in teammatePairCounts) {
            if (count <= 0) continue
            val parts = pairKey.split("|")
            if (parts.size == 2) {
                val (p1, p2) = parts
                if (p1.equals(playerKey, ignoreCase = true) || p1 == playerKey) {
                    if (count > maxCount) {
                        maxCount = count
                        favoriteKey = p2
                    }
                } else if (p2.equals(playerKey, ignoreCase = true) || p2 == playerKey) {
                    if (count > maxCount) {
                        maxCount = count
                        favoriteKey = p1
                    }
                }
            }
        }

        return if (favoriteKey != null && maxCount > 0) {
            FavoriteInfo(favoriteKey, maxCount)
        } else null
    }

    fun getFavoriteOpponent(
        playerKey: String,
        opponentPairCounts: Map<String, Int>
    ): FavoriteInfo? {
        if (playerKey.isBlank() || opponentPairCounts.isEmpty()) return null
        var maxCount = 0
        var favoriteKey: String? = null

        for ((pairKey, count) in opponentPairCounts) {
            if (count <= 0) continue
            val parts = pairKey.split("|")
            if (parts.size == 2) {
                val (p1, p2) = parts
                if (p1.equals(playerKey, ignoreCase = true) || p1 == playerKey) {
                    if (count > maxCount) {
                        maxCount = count
                        favoriteKey = p2
                    }
                } else if (p2.equals(playerKey, ignoreCase = true) || p2 == playerKey) {
                    if (count > maxCount) {
                        maxCount = count
                        favoriteKey = p1
                    }
                }
            }
        }

        return if (favoriteKey != null && maxCount > 0) {
            FavoriteInfo(favoriteKey, maxCount)
        } else null
    }

    fun getTotalTeammatesCount(
        playerKey: String,
        teammatePairCounts: Map<String, Int>
    ): Int {
        if (playerKey.isBlank() || teammatePairCounts.isEmpty()) return 0
        val partners = mutableSetOf<String>()

        for ((pairKey, count) in teammatePairCounts) {
            if (count <= 0) continue
            val parts = pairKey.split("|")
            if (parts.size == 2) {
                val (p1, p2) = parts
                if (p1.equals(playerKey, ignoreCase = true) || p1 == playerKey) {
                    partners.add(p2)
                } else if (p2.equals(playerKey, ignoreCase = true) || p2 == playerKey) {
                    partners.add(p1)
                }
            }
        }
        return partners.size
    }

    fun getTotalOpponentsCount(
        playerKey: String,
        opponentPairCounts: Map<String, Int>
    ): Int {
        if (playerKey.isBlank() || opponentPairCounts.isEmpty()) return 0
        val opponents = mutableSetOf<String>()

        for ((pairKey, count) in opponentPairCounts) {
            if (count <= 0) continue
            val parts = pairKey.split("|")
            if (parts.size == 2) {
                val (p1, p2) = parts
                if (p1.equals(playerKey, ignoreCase = true) || p1 == playerKey) {
                    opponents.add(p2)
                } else if (p2.equals(playerKey, ignoreCase = true) || p2 == playerKey) {
                    opponents.add(p1)
                }
            }
        }
        return opponents.size
    }

    fun calculateDashboardStats(
        players: List<PlayerEntity>,
        totalSessionsGenerated: Int
    ): DashboardStats {
        if (players.isEmpty()) {
            return DashboardStats(totalMatchesGenerated = totalSessionsGenerated)
        }

        val mostActive = players.maxByOrNull { it.matchesPlayed.coerceAtLeast(it.totalMatches) }
        val leastActive = players.minByOrNull { it.matchesPlayed.coerceAtLeast(it.totalMatches) }
        val highestSkill = players.maxByOrNull { it.skillRating }
        val mostJoker = players.filter { (it.matchesAsJoker.coerceAtLeast(it.totalTimesJoker)) > 0 }
            .maxByOrNull { it.matchesAsJoker.coerceAtLeast(it.totalTimesJoker) }

        val totalMatchesSum = players.sumOf { it.matchesPlayed.coerceAtLeast(it.totalMatches) }
        val avgMatches = totalMatchesSum.toDouble() / players.size.toDouble()
        val avgSkill = players.map { it.skillRating }.average()

        return DashboardStats(
            mostActivePlayer = mostActive,
            leastActivePlayer = leastActive,
            highestSkillPlayer = highestSkill,
            mostFrequentJoker = mostJoker,
            averageMatchesPerPlayer = avgMatches,
            averageSkillRating = avgSkill,
            totalRegisteredPlayers = players.size,
            totalMatchesGenerated = totalSessionsGenerated
        )
    }

    fun sortPlayers(
        players: List<PlayerEntity>,
        sortOption: PlayerSortOption
    ): List<PlayerEntity> {
        return when (sortOption) {
            PlayerSortOption.ALPHABETICAL -> players.sortedBy { it.displayName.lowercase() }
            PlayerSortOption.RECENTLY_PLAYED -> players.sortedWith(
                compareByDescending<PlayerEntity> { it.lastPlayedAt.coerceAtLeast(it.lastUsedAt) }
                    .thenBy { it.displayName.lowercase() }
            )
            PlayerSortOption.MOST_MATCHES -> players.sortedWith(
                compareByDescending<PlayerEntity> { it.matchesPlayed.coerceAtLeast(it.totalMatches) }
                    .thenBy { it.displayName.lowercase() }
            )
            PlayerSortOption.HIGHEST_SKILL -> players.sortedWith(
                compareByDescending<PlayerEntity> { it.skillRating }
                    .thenBy { it.displayName.lowercase() }
            )
            PlayerSortOption.FAVORITES_FIRST -> players.sortedWith(
                compareByDescending<PlayerEntity> { it.isFavorite }
                    .thenBy { it.displayName.lowercase() }
            )
            PlayerSortOption.LEAST_ACTIVE -> players.sortedWith(
                compareBy<PlayerEntity> { it.matchesPlayed.coerceAtLeast(it.totalMatches) }
                    .thenBy { it.displayName.lowercase() }
            )
        }
    }
}
