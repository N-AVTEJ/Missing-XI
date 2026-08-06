import re

with open('app/src/main/java/com/example/util/TeammatePairTracker.kt', 'r') as f:
    content = f.read()

new_funcs = """    /**
     * Calculates a deterministic Fairness Score between 0 and 100.
     * 100 = completely fresh teammate pairings
     * Lower values indicate more repeated teammate pairings.
     */
    fun calculateFairnessScore(analysis: CandidatePairAnalysis): Int {
        if (analysis.totalPairs == 0) return 100
        if (analysis.penaltyResult.totalPenalty == 0) return 100
        
        val newP = analysis.newPairs.toDouble()
        val penalty = analysis.penaltyResult.totalPenalty.toDouble()
        
        val score = (newP / (newP + penalty)) * 100.0
        return score.toInt().coerceIn(0, 100)
    }

    /**
     * Maps a Fairness Score to a readable Fairness Rating.
     */
    fun getFairnessRating(score: Int): String {
        return when {
            score >= 98 -> "Excellent"
            score >= 90 -> "Very Good"
            score >= 80 -> "Good"
            score >= 70 -> "Average"
            else -> "Poor"
        }
    }
}"""

content = content.replace("}\n", new_funcs + "\n")
# Ensure we don't accidentally double the closing brace.
content = re.sub(r'\}\s*\}\s*$', '}\n', content)

with open('app/src/main/java/com/example/util/TeammatePairTracker.kt', 'w') as f:
    f.write(content)
