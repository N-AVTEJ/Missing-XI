package com.example.util

data class Player(
    val id: String,
    val name: String
)

data class PairStatistics(
    val totalPairsRecorded: Int = 0,
    val uniquePairsCount: Int = 0,
    val totalPairOccurrences: Int = 0,
    val repeatedPairOccurrences: Int = 0,
    val maxRepeatedPairCount: Int = 0
)

object TeammatePairTracker {

    /**
     * Creates a canonical pair key by sorting two player IDs lexicographically.
     * Example: createPairKey("player_7", "player_2") -> "player_2|player_7"
     */
    fun createPairKey(idA: String, idB: String): String {
        require(idA != idB) { "A player cannot form a pair with themselves: $idA" }
        return if (idA < idB) {
            "$idA|$idB"
        } else {
            "$idB|$idA"
        }
    }

    /**
     * Generates all unique teammate pair keys for players in a single team.
     * Number of pairs generated = k * (k - 1) / 2
     */
    fun getTeamPairs(playerIds: List<String>): List<String> {
        val pairs = mutableListOf<String>()
        val distinctIds = playerIds.distinct()
        val n = distinctIds.size
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                pairs.add(createPairKey(distinctIds[i], distinctIds[j]))
            }
        }
        return pairs
    }

    /**
     * Increments pair counts for all teammate pairs across accepted teams.
     */
    fun updateTeammatePairCounts(
        currentCounts: Map<String, Int>,
        acceptedTeamsPlayerIds: List<List<String>>
    ): Map<String, Int> {
        val updatedMap = currentCounts.toMutableMap()
        for (teamPlayerIds in acceptedTeamsPlayerIds) {
            val teamPairs = getTeamPairs(teamPlayerIds)
            for (pairKey in teamPairs) {
                updatedMap[pairKey] = (updatedMap[pairKey] ?: 0) + 1
            }
        }
        return updatedMap
    }

    /**
     * Gets the count of how many times two players have been teammates.
     */
    fun getPairCount(counts: Map<String, Int>, idA: String, idB: String): Int {
        if (idA == idB) return 0
        val pairKey = createPairKey(idA, idB)
        return counts[pairKey] ?: 0
    }

    /**
     * Calculates pair statistics from the teammate pair counts map.
     */
    fun calculatePairStatistics(counts: Map<String, Int>): PairStatistics {
        val activeEntries = counts.filterValues { it > 0 }
        if (activeEntries.isEmpty()) {
            return PairStatistics()
        }

        val uniquePairs = activeEntries.size
        val totalOccurrences = activeEntries.values.sum()
        val repeatedOccurrences = activeEntries.values.sumOf { count -> (count - 1).coerceAtLeast(0) }
        val maxRepeated = activeEntries.values.maxOrNull() ?: 0

        return PairStatistics(
            totalPairsRecorded = totalOccurrences,
            uniquePairsCount = uniquePairs,
            totalPairOccurrences = totalOccurrences,
            repeatedPairOccurrences = repeatedOccurrences,
            maxRepeatedPairCount = maxRepeated
        )
    }
}
