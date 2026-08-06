import re

with open('app/src/test/java/com/example/TeammatePairTrackerTest.kt', 'r') as f:
    content = f.read()

new_tests = """
    @Test
    fun testFairnessScorePerfectShuffle() {
        val analysis = CandidatePairAnalysis(
            totalPairs = 20,
            newPairs = 20,
            penaltyResult = CandidatePenaltyResult(totalPenalty = 0)
        )
        val score = TeammatePairTracker.calculateFairnessScore(analysis)
        assertEquals(100, score)
        assertEquals("Excellent", TeammatePairTracker.getFairnessRating(score))
    }

    @Test
    fun testFairnessScoreRepeatedPairs() {
        // total pairs = 20, new pairs = 15, repeated pairs = 5, total penalty = 5
        val analysis = CandidatePairAnalysis(
            totalPairs = 20,
            newPairs = 15,
            penaltyResult = CandidatePenaltyResult(totalPenalty = 5)
        )
        val score = TeammatePairTracker.calculateFairnessScore(analysis)
        // 15 / (15 + 5) * 100 = 75
        assertEquals(75, score)
        assertEquals("Average", TeammatePairTracker.getFairnessRating(score))
    }
    
    @Test
    fun testFairnessScoreZeroNewPairs() {
        // total pairs = 20, new pairs = 0, penalty = 20
        val analysis = CandidatePairAnalysis(
            totalPairs = 20,
            newPairs = 0,
            penaltyResult = CandidatePenaltyResult(totalPenalty = 20)
        )
        val score = TeammatePairTracker.calculateFairnessScore(analysis)
        assertEquals(0, score)
        assertEquals("Poor", TeammatePairTracker.getFairnessRating(score))
    }
"""

content = content.replace("    @Test\n    fun testPenaltyLevelsMapping() {", new_tests + "\n    @Test\n    fun testPenaltyLevelsMapping() {")

with open('app/src/test/java/com/example/TeammatePairTrackerTest.kt', 'w') as f:
    f.write(content)
