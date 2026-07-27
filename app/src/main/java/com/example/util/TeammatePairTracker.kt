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

data class CandidatePairDetail(
    val pairKey: String,
    val player1Id: String,
    val player2Id: String,
    val previousCount: Int,
    val isRepeated: Boolean
)

data class CandidatePairAnalysis(
    val totalPairs: Int = 0,
    val newPairs: Int = 0,
    val repeatedPairs: Int = 0,
    val historicalRepeatOccurrences: Int = 0,
    val newPairPercentage: Double = 0.0,
    val repeatedPairPercentage: Double = 0.0,
    val maxHistoricalPairCount: Int = 0,
    val pairDetails: List<CandidatePairDetail> = emptyList()
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

    /**
     * Analyzes candidate arrangement teammate pairs against existing teammate pair history.
     * READ-ONLY: Does NOT mutate or update teammatePairCounts.
     */
    fun analyzeCandidatePairs(
        teamsPlayerIds: List<List<String>>,
        teammatePairCounts: Map<String, Int>
    ): CandidatePairAnalysis {
        val candidatePairKeys = mutableListOf<String>()
        for (teamPlayerIds in teamsPlayerIds) {
            candidatePairKeys.addAll(getTeamPairs(teamPlayerIds))
        }

        if (candidatePairKeys.isEmpty()) {
            return CandidatePairAnalysis()
        }

        val totalPairs = candidatePairKeys.size
        var newPairs = 0
        var repeatedPairs = 0
        var historicalRepeatOccurrences = 0
        var maxHistoricalPairCount = 0

        val pairDetails = mutableListOf<CandidatePairDetail>()

        for (pairKey in candidatePairKeys) {
            val parts = pairKey.split("|")
            val p1 = parts.getOrElse(0) { "" }
            val p2 = parts.getOrElse(1) { "" }
            val prevCount = teammatePairCounts[pairKey] ?: 0
            val isRep = prevCount > 0

            if (isRep) {
                repeatedPairs++
                historicalRepeatOccurrences += prevCount
                if (prevCount > maxHistoricalPairCount) {
                    maxHistoricalPairCount = prevCount
                }
            } else {
                newPairs++
            }

            pairDetails.add(
                CandidatePairDetail(
                    pairKey = pairKey,
                    player1Id = p1,
                    player2Id = p2,
                    previousCount = prevCount,
                    isRepeated = isRep
                )
            )
        }

        val newPairPercentage = if (totalPairs > 0) (newPairs.toDouble() / totalPairs.toDouble()) * 100.0 else 0.0
        val repeatedPairPercentage = if (totalPairs > 0) (repeatedPairs.toDouble() / totalPairs.toDouble()) * 100.0 else 0.0

        return CandidatePairAnalysis(
            totalPairs = totalPairs,
            newPairs = newPairs,
            repeatedPairs = repeatedPairs,
            historicalRepeatOccurrences = historicalRepeatOccurrences,
            newPairPercentage = newPairPercentage,
            repeatedPairPercentage = repeatedPairPercentage,
            maxHistoricalPairCount = maxHistoricalPairCount,
            pairDetails = pairDetails
        )
    }
}
