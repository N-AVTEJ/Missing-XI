package com.example.util

data class FairnessEvaluation(
    val overallScore: Double = 0.0,
    val teammateScore: Double = 0.0,
    val opponentScore: Double = 0.0,
    val strengthScore: Double = 0.0,
    val jokerScore: Double = 0.0,
    val teammatePenalty: Int = 0,
    val opponentPenalty: Int = 0,
    val strengthDifference: Int = 0,
    val newTeammatePairs: Int = 0,
    val rankingPosition: Int = 1
)

object FairnessRankingEngine {

    // Configurable Component Weights (Named Constants)
    const val WEIGHT_TEAMMATE_VARIETY: Double = 0.40
    const val WEIGHT_OPPONENT_VARIETY: Double = 0.20
    const val WEIGHT_TEAM_STRENGTH: Double = 0.30
    const val WEIGHT_JOKER_FAIRNESS: Double = 0.10

    /**
     * 3. Teammate Variety Score
     * Returns 100 if no repeated teammate pairs / penalties.
     * Decreases progressively based on amount and severity of repetition.
     */
    fun calculateTeammateVarietyScore(
        pairAnalysis: CandidatePairAnalysis
    ): Double {
        if (pairAnalysis.totalPairs == 0 || pairAnalysis.penaltyResult.totalPenalty == 0) {
            return 100.0
        }
        val penalty = pairAnalysis.penaltyResult.totalPenalty.toDouble()
        val newPairs = pairAnalysis.newPairs.toDouble()
        val score = (newPairs / (newPairs + penalty)) * 100.0
        return score.coerceIn(0.0, 100.0)
    }

    /**
     * 4. Opponent Variety Score
     * Returns 100 for completely new opponent relationships.
     * Repeated opponent relationships reduce score.
     */
    fun calculateOpponentVarietyScore(
        opponentAnalysis: OpponentPairAnalysis
    ): Double {
        if (opponentAnalysis.totalOpponentPairs == 0 ||
            opponentAnalysis.repeatedOpponentPairs == 0 ||
            opponentAnalysis.historicalRepeatOccurrences == 0
        ) {
            return 100.0
        }
        val repeatOccurrences = opponentAnalysis.historicalRepeatOccurrences.toDouble()
        val newOpponentPairs = opponentAnalysis.newOpponentPairs.toDouble()
        val score = (newOpponentPairs / (newOpponentPairs + repeatOccurrences)) * 100.0
        return score.coerceIn(0.0, 100.0)
    }

    /**
     * 5. Team Strength Score
     * Difference of 0 receives 100.
     * Larger differences progressively reduce score. Works for any number of teams.
     */
    fun calculateTeamStrengthScore(
        strengthAnalysis: CandidateStrengthAnalysis
    ): Double {
        val diff = strengthAnalysis.strengthDifference
        if (diff <= 0) return 100.0
        val score = (100.0 - (diff * 12.0)).coerceIn(0.0, 100.0)
        return score
    }

    /**
     * 6. Joker Fairness Score
     * Returns 100 if no Joker required or if Joker assigned to player not yet Joker in current cycle.
     * Minimizes Joker assignment imbalance when rotation repeats.
     */
    fun calculateJokerFairnessScore(
        joker: String?,
        activePlayers: List<String>,
        cycleJokers: List<String>,
        playerJokerCounts: Map<String, Int> = emptyMap()
    ): Double {
        if (joker.isNullOrBlank()) {
            return 100.0
        }

        val cycleSet = cycleJokers.toSet()
        val wasInCycle = cycleSet.contains(joker)

        if (!wasInCycle) {
            return 100.0
        }

        val assignedCount = playerJokerCounts[joker] ?: 0
        val minCount = activePlayers.minOfOrNull { playerJokerCounts[it] ?: 0 } ?: 0
        val diff = assignedCount - minCount
        if (diff <= 0) return 100.0

        val score = (100.0 - (diff * 25.0)).coerceIn(0.0, 100.0)
        return score
    }

    /**
     * 7. Overall Score
     * Weighted average of normalized scores.
     */
    fun calculateOverallFairnessScore(
        teammateScore: Double,
        opponentScore: Double,
        strengthScore: Double,
        jokerScore: Double
    ): Double {
        val overall = (teammateScore * WEIGHT_TEAMMATE_VARIETY) +
                (opponentScore * WEIGHT_OPPONENT_VARIETY) +
                (strengthScore * WEIGHT_TEAM_STRENGTH) +
                (jokerScore * WEIGHT_JOKER_FAIRNESS)
        return overall.coerceIn(0.0, 100.0)
    }

    /**
     * Complete Candidate Fairness Evaluation
     */
    fun evaluateCandidateFairness(
        pairAnalysis: CandidatePairAnalysis,
        opponentAnalysis: OpponentPairAnalysis,
        strengthAnalysis: CandidateStrengthAnalysis,
        joker: String?,
        activePlayers: List<String>,
        cycleJokers: List<String>,
        playerJokerCounts: Map<String, Int> = emptyMap()
    ): FairnessEvaluation {
        val teammateScore = calculateTeammateVarietyScore(pairAnalysis)
        val opponentScore = calculateOpponentVarietyScore(opponentAnalysis)
        val strengthScore = calculateTeamStrengthScore(strengthAnalysis)
        val jokerScore = calculateJokerFairnessScore(joker, activePlayers, cycleJokers, playerJokerCounts)

        val overallScore = calculateOverallFairnessScore(
            teammateScore = teammateScore,
            opponentScore = opponentScore,
            strengthScore = strengthScore,
            jokerScore = jokerScore
        )

        return FairnessEvaluation(
            overallScore = overallScore,
            teammateScore = teammateScore,
            opponentScore = opponentScore,
            strengthScore = strengthScore,
            jokerScore = jokerScore,
            teammatePenalty = pairAnalysis.penaltyResult.totalPenalty,
            opponentPenalty = opponentAnalysis.historicalRepeatOccurrences,
            strengthDifference = strengthAnalysis.strengthDifference,
            newTeammatePairs = pairAnalysis.newPairs,
            rankingPosition = 1
        )
    }

    fun getFairnessRating(overallScore: Double): String {
        return when {
            overallScore >= 95.0 -> "Excellent"
            overallScore >= 88.0 -> "Very Good"
            overallScore >= 78.0 -> "Good"
            overallScore >= 68.0 -> "Fair"
            else -> "Needs Improvement"
        }
    }

    /**
     * 8. Candidate Ranking Comparator
     * Priority order:
     * 1. Highest Overall Fairness Score
     * 2. Lowest Teammate Penalty
     * 3. Lowest Opponent Penalty
     * 4. Lowest Team Strength Difference
     * 5. Highest New Teammate Pairs
     */
    fun compareCandidates(c1Evaluation: FairnessEvaluation, c2Evaluation: FairnessEvaluation): Int {
        val scoreCmp = c2Evaluation.overallScore.compareTo(c1Evaluation.overallScore)
        if (scoreCmp != 0) return scoreCmp

        val tmPenaltyCmp = c1Evaluation.teammatePenalty.compareTo(c2Evaluation.teammatePenalty)
        if (tmPenaltyCmp != 0) return tmPenaltyCmp

        val oppPenaltyCmp = c1Evaluation.opponentPenalty.compareTo(c2Evaluation.opponentPenalty)
        if (oppPenaltyCmp != 0) return oppPenaltyCmp

        val strDiffCmp = c1Evaluation.strengthDifference.compareTo(c2Evaluation.strengthDifference)
        if (strDiffCmp != 0) return strDiffCmp

        val newPairsCmp = c2Evaluation.newTeammatePairs.compareTo(c1Evaluation.newTeammatePairs)
        if (newPairsCmp != 0) return newPairsCmp

        return 0
    }
}
