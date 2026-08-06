import re

with open('app/src/main/java/com/example/util/TeammatePairTracker.kt', 'r') as f:
    lines = f.readlines()

# find where "fun calculateFairnessScore" is defined and remove it and everything after
idx = -1
for i, line in enumerate(lines):
    if "fun calculateFairnessScore" in line:
        idx = i
        break

if idx != -1:
    lines = lines[:idx-4]
    
# Find the last closing brace
with open('app/src/main/java/com/example/util/TeammatePairTracker.kt', 'w') as f:
    f.writelines(lines)
    f.write("""
    /**
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
}
""")
