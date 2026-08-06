import re

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'r') as f:
    content = f.read()

# Add fairnessScore and fairnessRating to GeneratedCandidate
old_generated_candidate = """    data class GeneratedCandidate(
        val candidateId: String,
        val teams: List<GeneratedTeam>,
        val joker: String?,
        val signature: String,
        val pairAnalysis: com.example.util.CandidatePairAnalysis,
        val penaltyAnalysis: com.example.util.CandidatePenaltyResult,
        val updatedCycle: List<String>,
        val updatedHistory: List<String>,
        val generatedAt: Long = System.currentTimeMillis()
    )"""

new_generated_candidate = """    data class GeneratedCandidate(
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
content = content.replace(old_generated_candidate, new_generated_candidate)

# Add rank info to GenerationDiagnostics
old_diag = """    data class GenerationDiagnostics(
        val candidatesGenerated: Int,
        val candidatesRejected: Int,
        val retryCount: Int,
        val generationTimeMs: Long,
        val averageCandidatePenalty: Double
    )"""

new_diag = """    data class GenerationDiagnostics(
        val candidatesGenerated: Int,
        val candidatesRejected: Int,
        val retryCount: Int,
        val generationTimeMs: Long,
        val averageCandidatePenalty: Double,
        val bestCandidateRank: Int = 1,
        val lowestPenaltyFound: Int = 0,
        val highestPenaltyFound: Int = 0,
        val winningCandidatePenalty: Int = 0,
        val winningCandidateFairnessScore: Int = 0
    )"""
content = content.replace(old_diag, new_diag)

# Update shuffleTeams
old_generation = """                    val penaltyAnalysis = pairAnalysis.penaltyResult 
                    
                    generatedCandidates.add(
                        GeneratedCandidate(
                            candidateId = java.util.UUID.randomUUID().toString(),
                            teams = candidateTeams,
                            joker = tempJoker,
                            signature = sig,
                            pairAnalysis = pairAnalysis,
                            penaltyAnalysis = penaltyAnalysis,
                            updatedCycle = tempCycle,
                            updatedHistory = tempHistory
                        )
                    )
                    candidateGenerationProgress.value = generatedCandidates.size
                }
            }

            val endTime = System.currentTimeMillis()
            val avgPenalty = if (generatedCandidates.isNotEmpty()) generatedCandidates.map { it.penaltyAnalysis.totalPenalty }.average() else 0.0

            generationDiagnostics.value = GenerationDiagnostics(
                candidatesGenerated = generatedCandidates.size,
                candidatesRejected = candidatesRejected,
                retryCount = retryCount,
                generationTimeMs = endTime - startTime,
                averageCandidatePenalty = avgPenalty
            )

            candidatesGeneratedList.value = generatedCandidates

            // Back to main thread for applying the "first" candidate as the chosen one for now
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (generatedCandidates.isNotEmpty()) {
                    val chosen = generatedCandidates.first()
                    applyCandidateToState(chosen, activePlayerNames, numTeams, existingSigs)
                    duplicatesPrevented.value += candidatesRejected // Just to keep the existing stats accurate
                }
                isGeneratingCandidates.value = false
            }"""

new_generation = """                    val penaltyAnalysis = pairAnalysis.penaltyResult 
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
                    )
                    candidateGenerationProgress.value = generatedCandidates.size
                }
            }

            val endTime = System.currentTimeMillis()
            
            // Randomize tied candidates by shuffling before sorting
            generatedCandidates.shuffle()
            generatedCandidates.sortWith(Comparator { c1, c2 ->
                var cmp = c1.penaltyAnalysis.totalPenalty.compareTo(c2.penaltyAnalysis.totalPenalty)
                if (cmp != 0) return@Comparator cmp
                cmp = c2.pairAnalysis.newPairs.compareTo(c1.pairAnalysis.newPairs)
                if (cmp != 0) return@Comparator cmp
                cmp = c1.penaltyAnalysis.highestPairPenalty.compareTo(c2.penaltyAnalysis.highestPairPenalty)
                cmp
            })

            val avgPenalty = if (generatedCandidates.isNotEmpty()) generatedCandidates.map { it.penaltyAnalysis.totalPenalty }.average() else 0.0
            val lowestPenalty = generatedCandidates.minOfOrNull { it.penaltyAnalysis.totalPenalty } ?: 0
            val highestPenalty = generatedCandidates.maxOfOrNull { it.penaltyAnalysis.totalPenalty } ?: 0
            val chosen = generatedCandidates.firstOrNull()
            
            generationDiagnostics.value = GenerationDiagnostics(
                candidatesGenerated = generatedCandidates.size,
                candidatesRejected = candidatesRejected,
                retryCount = retryCount,
                generationTimeMs = endTime - startTime,
                averageCandidatePenalty = avgPenalty,
                bestCandidateRank = 1,
                lowestPenaltyFound = lowestPenalty,
                highestPenaltyFound = highestPenalty,
                winningCandidatePenalty = chosen?.penaltyAnalysis?.totalPenalty ?: 0,
                winningCandidateFairnessScore = chosen?.fairnessScore ?: 0
            )

            candidatesGeneratedList.value = generatedCandidates

            // Back to main thread for applying the chosen candidate
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (chosen != null) {
                    applyCandidateToState(chosen, activePlayerNames, numTeams, existingSigs)
                    duplicatesPrevented.value += candidatesRejected 
                }
                isGeneratingCandidates.value = false
            }"""
content = content.replace(old_generation, new_generation)

# Now we need to make sure chosen.fairnessScore and rating are preserved for the session.
# We also need to add properties to the ViewModel to store current fairness rating & score so we can display it.
# Add them in AppViewModel
state_additions = """    val candidatePairAnalysis = MutableStateFlow(com.example.util.CandidatePairAnalysis())
    val currentFairnessScore = MutableStateFlow(0)
    val currentFairnessRating = MutableStateFlow("")"""

content = content.replace("    val candidatePairAnalysis = MutableStateFlow(com.example.util.CandidatePairAnalysis())", state_additions)

old_apply = """        candidatePairAnalysis.value = chosen.pairAnalysis"""
new_apply = """        candidatePairAnalysis.value = chosen.pairAnalysis
        currentFairnessScore.value = chosen.fairnessScore
        currentFairnessRating.value = chosen.fairnessRating"""
content = content.replace(old_apply, new_apply)

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'w') as f:
    f.write(content)
