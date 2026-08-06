package com.example.util

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

data class PairPenaltyDetail(
    val pairKey: String,
    val player1Id: String,
    val player2Id: String,
    val previousCount: Int,
    val penalty: Int
)

data class CandidatePenaltyResult(
    val totalPenalty: Int = 0,
    val pairPenaltyCount: Int = 0,
    val highestPairPenalty: Int = 0,
    val averagePenalty: Double = 0.0,
    val penaltyLevel: String = "Excellent",
    val pairBreakdown: List<PairPenaltyDetail> = emptyList()
)

data class CandidatePairAnalysis(
    val totalPairs: Int = 0,
    val newPairs: Int = 0,
    val repeatedPairs: Int = 0,
    val historicalRepeatOccurrences: Int = 0,
    val newPairPercentage: Double = 0.0,
    val repeatedPairPercentage: Double = 0.0,
    val maxHistoricalPairCount: Int = 0,
    val pairDetails: List<CandidatePairDetail> = emptyList(),
    val penaltyResult: CandidatePenaltyResult = CandidatePenaltyResult()
)

object TeammatePairTracker {

    fun createPairKey(idA: String, idB: String): String {
        require(idA != idB) { "A player cannot form a pair with themselves: $idA" }
        return if (idA < idB) {
            "$idA|$idB"
        } else {
            "$idB|$idA"
        }
    }

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

    fun getPairCount(counts: Map<String, Int>, idA: String, idB: String): Int {
        if (idA == idB) return 0
        val pairKey = createPairKey(idA, idB)
        return counts[pairKey] ?: 0
    }

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

        val penaltyResult = calculateCandidatePenalty(teamsPlayerIds, teammatePairCounts)

        return CandidatePairAnalysis(
            totalPairs = totalPairs,
            newPairs = newPairs,
            repeatedPairs = repeatedPairs,
            historicalRepeatOccurrences = historicalRepeatOccurrences,
            newPairPercentage = newPairPercentage,
            repeatedPairPercentage = repeatedPairPercentage,
            maxHistoricalPairCount = maxHistoricalPairCount,
            pairDetails = pairDetails,
            penaltyResult = penaltyResult
        )
    }

    fun getPenaltyLevel(totalPenalty: Int): String {
        return when {
            totalPenalty == 0 -> "Excellent"
            totalPenalty in 1..5 -> "Very Low"
            totalPenalty in 6..15 -> "Low"
            totalPenalty in 16..30 -> "Medium"
            totalPenalty in 31..60 -> "High"
            else -> "Very High"
        }
    }

    fun calculateAveragePenalty(totalPenalty: Int, pairPenaltyCount: Int): Double {
        if (pairPenaltyCount <= 0) return 0.0
        return totalPenalty.toDouble() / pairPenaltyCount.toDouble()
    }

    fun calculateCandidatePenalty(
        teamsPlayerIds: List<List<String>>,
        teammatePairCounts: Map<String, Int>
    ): CandidatePenaltyResult {
        val candidatePairKeys = mutableListOf<String>()
        for (teamPlayerIds in teamsPlayerIds) {
            candidatePairKeys.addAll(getTeamPairs(teamPlayerIds))
        }

        if (candidatePairKeys.isEmpty()) {
            return CandidatePenaltyResult()
        }

        var totalPenalty = 0
        var highestPairPenalty = 0
        val pairBreakdown = mutableListOf<PairPenaltyDetail>()

        for (pairKey in candidatePairKeys) {
            val prevCount = teammatePairCounts[pairKey] ?: 0
            if (prevCount > 0) {
                val penalty = prevCount
                totalPenalty += penalty
                if (penalty > highestPairPenalty) {
                    highestPairPenalty = penalty
                }

                val parts = pairKey.split("|")
                val p1 = parts.getOrElse(0) { "" }
                val p2 = parts.getOrElse(1) { "" }

                pairBreakdown.add(
                    PairPenaltyDetail(
                        pairKey = pairKey,
                        player1Id = p1,
                        player2Id = p2,
                        previousCount = prevCount,
                        penalty = penalty
                    )
                )
            }
        }

        val pairPenaltyCount = pairBreakdown.size
        val averagePenalty = calculateAveragePenalty(totalPenalty, pairPenaltyCount)
        val penaltyLevel = getPenaltyLevel(totalPenalty)

        return CandidatePenaltyResult(
            totalPenalty = totalPenalty,
            pairPenaltyCount = pairPenaltyCount,
            highestPairPenalty = highestPairPenalty,
            averagePenalty = averagePenalty,
            penaltyLevel = penaltyLevel,
            pairBreakdown = pairBreakdown
        )
    }

    fun calculateFairnessScore(analysis: CandidatePairAnalysis): Int {
        if (analysis.totalPairs == 0) return 100
        if (analysis.penaltyResult.totalPenalty == 0) return 100
        
        val newP = analysis.newPairs.toDouble()
        val penalty = analysis.penaltyResult.totalPenalty.toDouble()
        
        val score = (newP / (newP + penalty)) * 100.0
        return score.toInt().coerceIn(0, 100)
    }

    fun getFairnessRating(score: Int): String {
        return when {
            score >= 98 -> "Excellent"
            score >= 90 -> "Very Good"
            score >= 80 -> "Good"
            score >= 70 -> "Average"
            else -> "Poor"
        }
    }
}
