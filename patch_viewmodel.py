import re

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'r') as f:
    content = f.read()

# Add new data classes inside AppViewModel
data_classes = """
    data class GenerationDiagnostics(
        val candidatesGenerated: Int,
        val candidatesRejected: Int,
        val retryCount: Int,
        val generationTimeMs: Long,
        val averageCandidatePenalty: Double
    )

    data class GeneratedCandidate(
        val candidateId: String,
        val teams: List<GeneratedTeam>,
        val joker: String?,
        val signature: String,
        val pairAnalysis: com.example.util.CandidatePairAnalysis,
        val penaltyAnalysis: com.example.util.CandidatePenaltyResult,
        val updatedCycle: List<String>,
        val updatedHistory: List<String>,
        val generatedAt: Long = System.currentTimeMillis()
    )

    val isGeneratingCandidates = MutableStateFlow(false)
    val candidateGenerationProgress = MutableStateFlow(0)
    val candidateGenerationTarget = MutableStateFlow(0)
    val candidatesGeneratedList = MutableStateFlow<List<GeneratedCandidate>>(emptyList())
    val generationDiagnostics = MutableStateFlow<GenerationDiagnostics?>(null)
"""

content = content.replace("data class TeamConfigState(", data_classes + "\n    data class TeamConfigState(")

shuffle_teams_old = """    fun shuffleTeams() {
        val numTeams = configNumberOfTeams.value.toIntOrNull() ?: 0
        val activePlayerObjects = buildPlayersList.value.mapIndexed { index, name ->
            Player(id = "player_${index + 1}", name = name.trim())
        }.filter { it.name.isNotBlank() }
        val activePlayerNames = activePlayerObjects.map { it.name }
        if (numTeams < 2 || activePlayerObjects.size < numTeams) return

        val existingSigs = generatedSignaturesSet.value.toMutableSet()
        var attempts = 0
        val maxAttempts = 100

        var chosenTeams: List<GeneratedTeam> = emptyList()
        var chosenJoker: String? = null
        var chosenCycle: List<String> = currentCycleJokers.value
        var chosenHistory: List<String> = previousJokersHistory.value
        var chosenSignature = ""

        while (attempts < maxAttempts) {
            var tempJoker: String? = null
            var tempCycle = currentCycleJokers.value.toMutableList()
            var tempHistory = previousJokersHistory.value.toMutableList()
            val tempPool: List<Player>

            if (activePlayerObjects.size % numTeams != 0) {
                val cycleSet = tempCycle.toSet()
                var eligible = activePlayerObjects.filter { it.name !in cycleSet && it.id !in cycleSet }

                if (eligible.isEmpty()) {
                    tempCycle.clear()
                    eligible = activePlayerObjects
                }

                val pickedJoker = eligible.shuffled().first()
                tempJoker = pickedJoker.name

                tempCycle.add(pickedJoker.name)
                tempHistory.add(pickedJoker.name)

                tempPool = activePlayerObjects.filter { it.id != pickedJoker.id }.shuffled()
            } else {
                tempJoker = null
                tempPool = activePlayerObjects.shuffled()
            }

            val teamsList = List(numTeams) { mutableListOf<Player>() }
            tempPool.forEachIndexed { index, player ->
                teamsList[index % numTeams].add(player)
            }

            val candidateTeams = teamsList.mapIndexed { index, players ->
                GeneratedTeam(
                    teamNumber = index + 1,
                    name = "Team " + (('A' + index).toString()),
                    players = players.map { it.name },
                    playerIds = players.map { it.id }
                )
            }

            val sig = generateArrangementSignature(candidateTeams, tempJoker)

            if (existingSigs.contains(sig)) {
                duplicatesPrevented.value += 1
                attempts++
            } else {
                chosenTeams = candidateTeams
                chosenJoker = tempJoker
                chosenCycle = tempCycle
                chosenHistory = tempHistory
                chosenSignature = sig
                break
            }
        }

        if (chosenSignature.isEmpty() && chosenTeams.isNotEmpty()) {
            chosenSignature = generateArrangementSignature(chosenTeams, chosenJoker)
        }

        jokerPlayer.value = chosenJoker
        currentCycleJokers.value = chosenCycle
        previousJokersHistory.value = chosenHistory
        saveJokerHistory()

        generatedTeams.value = chosenTeams
        if (chosenSignature.isNotEmpty()) {
            existingSigs.add(chosenSignature)
        }
        generatedSignaturesSet.value = existingSigs
        uniqueTeamsGenerated.value = existingSigs.size

        val shuffleNum = nextShuffleNumber++
        currentShuffleNumber.value = shuffleNum

        val session = ShuffleSession(
            shuffleNumber = shuffleNum,
            timestamp = System.currentTimeMillis(),
            teams = chosenTeams,
            players = activePlayerNames,
            joker = chosenJoker
        )
        sessionHistory.value = listOf(session) + sessionHistory.value

        // Analyze Candidate Teammate Pairs against existing history (BEFORE updating pair history)
        val acceptedTeamsPlayerIds = chosenTeams.map { it.playerIds }
        val candidateAnalysis = TeammatePairTracker.analyzeCandidatePairs(
            acceptedTeamsPlayerIds,
            teammatePairCounts.value
        )
        candidatePairAnalysis.value = candidateAnalysis

        // Update Teammate Pair Counts ONLY after accepted shuffle and analysis
        teammatePairCounts.value = TeammatePairTracker.updateTeammatePairCounts(
            teammatePairCounts.value,
            acceptedTeamsPlayerIds
        )

        // Save session and players to database
        viewModelScope.launch {
            val jsonArray = org.json.JSONArray(activePlayerNames)
            val dbSession = com.example.data.model.SessionEntity(
                playerIdsJson = jsonArray.toString(),
                teamCount = numTeams,
                timestamp = System.currentTimeMillis()
            )
            repository.insertSession(dbSession)
            loadLatestSession() // Update latest session state

            activePlayerNames.forEach { playerName ->
                val existingPlayer = repository.getPlayerByName(playerName)
                if (existingPlayer != null) {
                    val isJoker = playerName == chosenJoker
                    repository.updatePlayer(existingPlayer.copy(
                        lastUsedAt = System.currentTimeMillis(),
                        totalMatches = existingPlayer.totalMatches + 1,
                        totalTimesJoker = if (isJoker) existingPlayer.totalTimesJoker + 1 else existingPlayer.totalTimesJoker
                    ))
                } else {
                    val isJoker = playerName == chosenJoker
                    repository.insertPlayer(
                        com.example.data.model.PlayerEntity(
                            displayName = playerName,
                            totalMatches = 1,
                            totalTimesJoker = if (isJoker) 1 else 0
                        )
                    )
                }
            }
        }
    }"""

shuffle_teams_new = """    fun shuffleTeams() {
        if (isGeneratingCandidates.value) return
        val numTeams = configNumberOfTeams.value.toIntOrNull() ?: 0
        val activePlayerObjects = buildPlayersList.value.mapIndexed { index, name ->
            Player(id = "player_${index + 1}", name = name.trim())
        }.filter { it.name.isNotBlank() }
        val activePlayerNames = activePlayerObjects.map { it.name }
        if (numTeams < 2 || activePlayerObjects.size < numTeams) return

        val candidateTarget = when {
            activePlayerObjects.size <= 10 -> 25
            activePlayerObjects.size in 11..16 -> 50
            activePlayerObjects.size in 17..24 -> 100
            else -> 150
        }
        val maxRetries = candidateTarget * 10
        candidateGenerationTarget.value = candidateTarget
        candidateGenerationProgress.value = 0
        isGeneratingCandidates.value = true

        val existingSigs = generatedSignaturesSet.value.toMutableSet()
        val generatedCandidates = mutableListOf<GeneratedCandidate>()
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            var candidatesRejected = 0
            var retryCount = 0
            
            val currentPairCounts = teammatePairCounts.value

            while (generatedCandidates.size < candidateTarget && retryCount < maxRetries) {
                var tempJoker: String? = null
                var tempCycle = currentCycleJokers.value.toMutableList()
                var tempHistory = previousJokersHistory.value.toMutableList()
                val tempPool: List<Player>

                if (activePlayerObjects.size % numTeams != 0) {
                    val cycleSet = tempCycle.toSet()
                    var eligible = activePlayerObjects.filter { it.name !in cycleSet && it.id !in cycleSet }

                    if (eligible.isEmpty()) {
                        tempCycle.clear()
                        eligible = activePlayerObjects
                    }

                    val pickedJoker = eligible.shuffled().first()
                    tempJoker = pickedJoker.name

                    tempCycle.add(pickedJoker.name)
                    tempHistory.add(pickedJoker.name)

                    tempPool = activePlayerObjects.filter { it.id != pickedJoker.id }.shuffled()
                } else {
                    tempJoker = null
                    tempPool = activePlayerObjects.shuffled()
                }

                val teamsList = List(numTeams) { mutableListOf<Player>() }
                tempPool.forEachIndexed { index, player ->
                    teamsList[index % numTeams].add(player)
                }

                val candidateTeams = teamsList.mapIndexed { index, players ->
                    GeneratedTeam(
                        teamNumber = index + 1,
                        name = "Team " + (('A' + index).toString()),
                        players = players.map { it.name },
                        playerIds = players.map { it.id }
                    )
                }

                val sig = generateArrangementSignature(candidateTeams, tempJoker)

                if (existingSigs.contains(sig) || generatedCandidates.any { it.signature == sig }) {
                    candidatesRejected++
                    retryCount++
                } else {
                    val acceptedTeamsPlayerIds = candidateTeams.map { it.playerIds }
                    val pairAnalysis = com.example.util.TeammatePairTracker.analyzeCandidatePairs(
                        acceptedTeamsPlayerIds,
                        currentPairCounts
                    )
                    val penaltyAnalysis = pairAnalysis.penaltyResult 
                    
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
            }
        }
    }

    private fun applyCandidateToState(
        chosen: GeneratedCandidate, 
        activePlayerNames: List<String>, 
        numTeams: Int,
        existingSigs: MutableSet<String>
    ) {
        jokerPlayer.value = chosen.joker
        currentCycleJokers.value = chosen.updatedCycle
        previousJokersHistory.value = chosen.updatedHistory
        saveJokerHistory()

        generatedTeams.value = chosen.teams
        existingSigs.add(chosen.signature)
        generatedSignaturesSet.value = existingSigs
        uniqueTeamsGenerated.value = existingSigs.size

        val shuffleNum = nextShuffleNumber++
        currentShuffleNumber.value = shuffleNum

        val session = ShuffleSession(
            shuffleNumber = shuffleNum,
            timestamp = System.currentTimeMillis(),
            teams = chosen.teams,
            players = activePlayerNames,
            joker = chosen.joker
        )
        sessionHistory.value = listOf(session) + sessionHistory.value

        candidatePairAnalysis.value = chosen.pairAnalysis

        // Update Teammate Pair Counts ONLY after accepted shuffle and analysis
        val acceptedTeamsPlayerIds = chosen.teams.map { it.playerIds }
        teammatePairCounts.value = com.example.util.TeammatePairTracker.updateTeammatePairCounts(
            teammatePairCounts.value,
            acceptedTeamsPlayerIds
        )

        // Save session and players to database
        viewModelScope.launch {
            val jsonArray = org.json.JSONArray(activePlayerNames)
            val dbSession = com.example.data.model.SessionEntity(
                playerIdsJson = jsonArray.toString(),
                teamCount = numTeams,
                timestamp = System.currentTimeMillis()
            )
            repository.insertSession(dbSession)
            loadLatestSession() // Update latest session state

            activePlayerNames.forEach { playerName ->
                val existingPlayer = repository.getPlayerByName(playerName)
                if (existingPlayer != null) {
                    val isJoker = playerName == chosen.joker
                    repository.updatePlayer(existingPlayer.copy(
                        lastUsedAt = System.currentTimeMillis(),
                        totalMatches = existingPlayer.totalMatches + 1,
                        totalTimesJoker = if (isJoker) existingPlayer.totalTimesJoker + 1 else existingPlayer.totalTimesJoker
                    ))
                } else {
                    val isJoker = playerName == chosen.joker
                    repository.insertPlayer(
                        com.example.data.model.PlayerEntity(
                            displayName = playerName,
                            totalMatches = 1,
                            totalTimesJoker = if (isJoker) 1 else 0
                        )
                    )
                }
            }
        }
    }"""

if shuffle_teams_old in content:
    content = content.replace(shuffle_teams_old, shuffle_teams_new)
    with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Could not find shuffle_teams_old in content. Let's try matching chunks.")
