package com.example.util

data class OpponentPairAnalysis(
    val totalOpponentPairs: Int = 0,
    val newOpponentPairs: Int = 0,
    val repeatedOpponentPairs: Int = 0,
    val historicalRepeatOccurrences: Int = 0,
    val maxHistoricalOpponentCount: Int = 0,
    val pairDetails: List<CandidatePairDetail> = emptyList()
)

object OpponentPairTracker {

    fun getOpponentPairs(teamsPlayerIds: List<List<String>>): List<String> {
        val pairs = mutableListOf<String>()
        val numTeams = teamsPlayerIds.size
        for (i in 0 until numTeams) {
            for (j in i + 1 until numTeams) {
                val teamA = teamsPlayerIds[i]
                val teamB = teamsPlayerIds[j]
                for (playerA in teamA) {
                    for (playerB in teamB) {
                        pairs.add(TeammatePairTracker.createPairKey(playerA, playerB))
                    }
                }
            }
        }
        return pairs
    }

    fun updateOpponentHistory(
        currentCounts: Map<String, Int>,
        acceptedTeamsPlayerIds: List<List<String>>
    ): Map<String, Int> {
        val updatedMap = currentCounts.toMutableMap()
        val opponentPairs = getOpponentPairs(acceptedTeamsPlayerIds)
        for (pairKey in opponentPairs) {
            updatedMap[pairKey] = (updatedMap[pairKey] ?: 0) + 1
        }
        return updatedMap
    }

    fun analyzeOpponentPairs(
        teamsPlayerIds: List<List<String>>,
        opponentPairCounts: Map<String, Int>
    ): OpponentPairAnalysis {
        val candidatePairKeys = getOpponentPairs(teamsPlayerIds)

        if (candidatePairKeys.isEmpty()) {
            return OpponentPairAnalysis()
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

            val prevCount = opponentPairCounts[pairKey] ?: 0
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

        return OpponentPairAnalysis(
            totalOpponentPairs = totalPairs,
            newOpponentPairs = newPairs,
            repeatedOpponentPairs = repeatedPairs,
            historicalRepeatOccurrences = historicalRepeatOccurrences,
            maxHistoricalOpponentCount = maxHistoricalPairCount,
            pairDetails = pairDetails
        )
    }

    fun getOpponentStatistics(counts: Map<String, Int>): PairStatistics {
        return TeammatePairTracker.calculatePairStatistics(counts)
    }
}
