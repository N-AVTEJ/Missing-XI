import re

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'r') as f:
    content = f.read()

# Add to GeneratedCandidate
old_generated_candidate = """    data class GeneratedCandidate(
        val candidateId: String,
        val teams: List<GeneratedTeam>,
        val joker: String?,
        val signature: String,
        val pairAnalysis: com.example.util.CandidatePairAnalysis,
        val penaltyAnalysis: com.example.util.CandidatePenaltyResult,
        val updatedCycle: List<String>,
        val updatedHistory: List<String>,
        val generatedAt: Long = System.currentTimeMillis(),
        val fairnessScore: Int = 0,
        val fairnessRating: String = ""
    )"""

new_generated_candidate = """    data class GeneratedCandidate(
        val candidateId: String,
        val teams: List<GeneratedTeam>,
        val joker: String?,
        val signature: String,
        val pairAnalysis: com.example.util.CandidatePairAnalysis,
        val penaltyAnalysis: com.example.util.CandidatePenaltyResult,
        val opponentAnalysis: com.example.util.OpponentPairAnalysis,
        val updatedCycle: List<String>,
        val updatedHistory: List<String>,
        val generatedAt: Long = System.currentTimeMillis(),
        val fairnessScore: Int = 0,
        val fairnessRating: String = ""
    )"""
content = content.replace(old_generated_candidate, new_generated_candidate)

# Add properties
old_props = """    val candidatePairAnalysis = MutableStateFlow<CandidatePairAnalysis?>(null)
    val currentFairnessScore = MutableStateFlow(0)
    val currentFairnessRating = MutableStateFlow("")"""
new_props = """    val candidatePairAnalysis = MutableStateFlow<CandidatePairAnalysis?>(null)
    val currentFairnessScore = MutableStateFlow(0)
    val currentFairnessRating = MutableStateFlow("")
    
    val opponentPairCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val opponentStatistics: StateFlow<PairStatistics> = opponentPairCounts.map { counts ->
        com.example.util.OpponentPairTracker.getOpponentStatistics(counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PairStatistics())

    val candidateOpponentAnalysis = MutableStateFlow<com.example.util.OpponentPairAnalysis?>(null)"""
content = content.replace(old_props, new_props)

# In shuffleTeams:
old_shuffle = """            val currentPairCounts = teammatePairCounts.value
            val currentJokerCycle = activeJokerCycle.value.toMutableList()
            val currentJokerHistory = jokerHistory.value.toMutableList()"""
new_shuffle = """            val currentPairCounts = teammatePairCounts.value
            val currentOpponentCounts = opponentPairCounts.value
            val currentJokerCycle = activeJokerCycle.value.toMutableList()
            val currentJokerHistory = jokerHistory.value.toMutableList()"""
content = content.replace(old_shuffle, new_shuffle)

old_shuffle_analysis = """                    val acceptedTeamsPlayerIds = candidateTeams.map { it.playerIds }
                    val pairAnalysis = com.example.util.TeammatePairTracker.analyzeCandidatePairs(
                        acceptedTeamsPlayerIds,
                        currentPairCounts
                    )
                    val penaltyAnalysis = pairAnalysis.penaltyResult 
                    val fScore = com.example.util.TeammatePairTracker.calculateFairnessScore(pairAnalysis)
                    val fRating = com.example.util.TeammatePairTracker.getFairnessRating(fScore)
                    
                    generatedCandidates.add(
                        GeneratedCandidate(
                            candidateId = java.util.UUID.randomUUID().toString(),
                            teams = candidateTeams,
                            joker = tempJoker,
                            signature = sig,
                            pairAnalysis = pairAnalysis,
                            penaltyAnalysis = penaltyAnalysis,
                            updatedCycle = tempCycle,
                            updatedHistory = tempHistory,
                            fairnessScore = fScore,
                            fairnessRating = fRating
                        )
                    )"""
new_shuffle_analysis = """                    val acceptedTeamsPlayerIds = candidateTeams.map { it.playerIds }
                    val pairAnalysis = com.example.util.TeammatePairTracker.analyzeCandidatePairs(
                        acceptedTeamsPlayerIds,
                        currentPairCounts
                    )
                    val opponentAnalysis = com.example.util.OpponentPairTracker.analyzeOpponentPairs(
                        acceptedTeamsPlayerIds,
                        currentOpponentCounts
                    )
                    val penaltyAnalysis = pairAnalysis.penaltyResult 
                    val fScore = com.example.util.TeammatePairTracker.calculateFairnessScore(pairAnalysis)
                    val fRating = com.example.util.TeammatePairTracker.getFairnessRating(fScore)
                    
                    generatedCandidates.add(
                        GeneratedCandidate(
                            candidateId = java.util.UUID.randomUUID().toString(),
                            teams = candidateTeams,
                            joker = tempJoker,
                            signature = sig,
                            pairAnalysis = pairAnalysis,
                            penaltyAnalysis = penaltyAnalysis,
                            opponentAnalysis = opponentAnalysis,
                            updatedCycle = tempCycle,
                            updatedHistory = tempHistory,
                            fairnessScore = fScore,
                            fairnessRating = fRating
                        )
                    )"""
content = content.replace(old_shuffle_analysis, new_shuffle_analysis)


old_apply = """        candidatePairAnalysis.value = chosen.pairAnalysis
        currentFairnessScore.value = chosen.fairnessScore
        currentFairnessRating.value = chosen.fairnessRating

        // Update Teammate Pair Counts ONLY after accepted shuffle and analysis
        val acceptedTeamsPlayerIds = chosen.teams.map { it.playerIds }
        teammatePairCounts.value = com.example.util.TeammatePairTracker.updateTeammatePairCounts(
            teammatePairCounts.value,
            acceptedTeamsPlayerIds
        )"""

new_apply = """        candidatePairAnalysis.value = chosen.pairAnalysis
        candidateOpponentAnalysis.value = chosen.opponentAnalysis
        currentFairnessScore.value = chosen.fairnessScore
        currentFairnessRating.value = chosen.fairnessRating

        // Update Teammate Pair Counts ONLY after accepted shuffle and analysis
        val acceptedTeamsPlayerIds = chosen.teams.map { it.playerIds }
        teammatePairCounts.value = com.example.util.TeammatePairTracker.updateTeammatePairCounts(
            teammatePairCounts.value,
            acceptedTeamsPlayerIds
        )
        
        // Update Opponent History
        opponentPairCounts.value = com.example.util.OpponentPairTracker.updateOpponentHistory(
            opponentPairCounts.value,
            acceptedTeamsPlayerIds
        )"""
content = content.replace(old_apply, new_apply)

old_clear = """        teammatePairCounts.value = emptyMap()
        candidatePairAnalysis.value = null"""
new_clear = """        teammatePairCounts.value = emptyMap()
        opponentPairCounts.value = emptyMap()
        candidatePairAnalysis.value = null
        candidateOpponentAnalysis.value = null"""
content = content.replace(old_clear, new_clear)

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'w') as f:
    f.write(content)
