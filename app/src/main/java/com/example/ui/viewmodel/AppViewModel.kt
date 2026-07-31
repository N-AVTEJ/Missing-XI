package com.example.ui.viewmodel

import android.content.Context
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseService
import com.example.data.model.LineupEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import com.example.util.CandidatePairAnalysis
import com.example.util.PairStatistics
import com.example.util.Player
import com.example.util.TeammatePairTracker
import org.json.JSONArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class PlayerSlot(
    val index: Int,
    val positionLabel: String,
    val xPercent: Float, // Relative X coordinate on pitch (0.0 to 1.0)
    val yPercent: Float, // Relative Y coordinate on pitch (0.0 to 1.0)
    var name: String = ""
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.lineupDao(),
        database.tossDao(),
        database.playerDao(),
        database.sessionDao()
    )
    val firebaseService = FirebaseService()

    // Database Flows
    val savedLineups: StateFlow<List<LineupEntity>> = repository.allLineups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTosses: StateFlow<List<TossEntity>> = repository.allTosses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivePlayers = repository.allActivePlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyUsedPlayers = repository.recentlyUsedPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestSession = MutableStateFlow<com.example.data.model.SessionEntity?>(null)

    init {
        loadLatestSession()
    }

    private fun loadLatestSession() {
        viewModelScope.launch {
            val session = repository.getLatestSession()
            latestSession.value = session
        }
    }

    // Active Builder Draft State
    val teamName = MutableStateFlow("My Dream XI")
    val selectedFormation = MutableStateFlow("4-3-3")
    val selectedSport = MutableStateFlow("Football") // "Football" or "Cricket"
    val playerSlots = MutableStateFlow<List<PlayerSlot>>(emptyList())

    // UI Status
    val saveMessage = MutableStateFlow("")

    // Active Toss State
    val isCoinFlipping = MutableStateFlow(false)
    val selectedTossChoice = MutableStateFlow("Heads") // Heads or Tails
    val tossResult = MutableStateFlow<String?>(null) // Heads or Tails
    val tossStatusMessage = MutableStateFlow("Choose Heads or Tails to start the toss")

    // Theme Config state
    val pitchThemeColor = MutableStateFlow("Neon Green") // "Neon Green", "Neon Blue", "Crimson Hot"

    // Match Setup State
    val matchTeamAName = MutableStateFlow("Team Alpha")
    val matchTeamBName = MutableStateFlow("Team Beta")
    val matchTeamAPlayers = MutableStateFlow(listOf("Player A1", "Player A2", "Player A3", "Player A4"))
    val matchTeamBPlayers = MutableStateFlow(listOf("Player B1", "Player B2", "Player B3", "Player B4"))

    fun updateMatchTeamName(isTeamA: Boolean, name: String) {
        if (isTeamA) matchTeamAName.value = name else matchTeamBName.value = name
    }

    fun addMatchPlayer(isTeamA: Boolean) {
        val playersFlow = if (isTeamA) matchTeamAPlayers else matchTeamBPlayers
        val currentList = playersFlow.value.toMutableList()
        currentList.add("New Player")
        playersFlow.value = currentList
    }

    fun removeMatchPlayer(isTeamA: Boolean, index: Int) {
        val playersFlow = if (isTeamA) matchTeamAPlayers else matchTeamBPlayers
        val currentList = playersFlow.value.toMutableList()
        if (currentList.size > 1 && index in currentList.indices) { // Minimum 1 player validation
            currentList.removeAt(index)
            playersFlow.value = currentList
        }
    }

    fun updateMatchPlayerName(isTeamA: Boolean, index: Int, newName: String) {
        val playersFlow = if (isTeamA) matchTeamAPlayers else matchTeamBPlayers
        val currentList = playersFlow.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = newName
            playersFlow.value = currentList
        }
    }

    // Dynamic Build Screen State
    val buildTotalPlayersInput = MutableStateFlow("11")
    val buildPlayersList = MutableStateFlow<List<String>>(List(11) { "Player ${it + 1}" })
    val buildSearchQuery = MutableStateFlow("")
    val buildDuplicateError = MutableStateFlow<String?>(null)
    val buildEmptyFieldError = MutableStateFlow<String?>(null)
    val hasStartedBuilding = MutableStateFlow(false)

    fun startBuildingFresh() {
        hasStartedBuilding.value = true
        // Keep the default 11 players
    }

    fun continueWithLastSession() {
        viewModelScope.launch {
            val session = latestSession.value ?: return@launch
            try {
                val jsonArray = org.json.JSONArray(session.playerIdsJson)
                val players = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    players.add(jsonArray.getString(i))
                }
                if (players.isNotEmpty()) {
                    buildPlayersList.value = players
                    buildTotalPlayersInput.value = players.size.toString()
                    hasStartedBuilding.value = true
                    validateDuplicates(players)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addPlayersFromLibrary(players: List<String>) {
        if (players.isNotEmpty()) {
            buildPlayersList.value = players
            buildTotalPlayersInput.value = players.size.toString()
            hasStartedBuilding.value = true
            validateDuplicates(players)
        }
    }

    fun togglePlayerFavorite(player: com.example.data.model.PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player.copy(isFavorite = !player.isFavorite))
        }
    }

    fun archivePlayer(player: com.example.data.model.PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player.copy(isArchived = true))
        }
    }

    fun deletePlayer(player: com.example.data.model.PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player.id)
        }
    }

    fun updateBuildTotalPlayers(total: String) {
        buildTotalPlayersInput.value = total
        val count = total.toIntOrNull() ?: return
        if (count > 0 && count <= 100) {
            val currentList = buildPlayersList.value
            if (count > currentList.size) {
                val newList = currentList.toMutableList()
                for (i in currentList.size until count) {
                    newList.add("Player ${i + 1}")
                }
                buildPlayersList.value = newList
            } else if (count < currentList.size) {
                buildPlayersList.value = currentList.take(count)
            }
            validateDuplicates(buildPlayersList.value)
        }
    }

    fun addBuildPlayer() {
        val currentList = buildPlayersList.value.toMutableList()
        currentList.add("New Player ${currentList.size + 1}")
        buildPlayersList.value = currentList
        buildTotalPlayersInput.value = currentList.size.toString()
        validateDuplicates(currentList)
    }

    fun removeBuildPlayer(index: Int) {
        val currentList = buildPlayersList.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            buildPlayersList.value = currentList
            buildTotalPlayersInput.value = currentList.size.toString()
            validateDuplicates(currentList)
        }
    }

    fun updateBuildPlayerName(index: Int, name: String) {
        val currentList = buildPlayersList.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = name
            buildPlayersList.value = currentList
            validateDuplicates(currentList)
        }
    }

    fun updateBuildSearchQuery(query: String) {
        buildSearchQuery.value = query
    }

    private fun validateDuplicates(list: List<String>) {
        val nonBlank = list.filter { it.isNotBlank() }
        val duplicates = nonBlank.groupingBy { it.trim().lowercase() }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            buildDuplicateError.value = "Warning: Duplicate names found!"
        } else {
            buildDuplicateError.value = null
        }
        
        if (list.any { it.isBlank() }) {
            buildEmptyFieldError.value = "Warning: Player name cannot be empty!"
        } else {
            buildEmptyFieldError.value = null
        }
    }

    // Team Configuration State
    val configNumberOfTeams = MutableStateFlow("2")

    fun updateConfigNumberOfTeams(numTeamsStr: String) {
        configNumberOfTeams.value = numTeamsStr
    }
    
    data class TeamConfigState(val playersPerTeam: Int = 0, val remainingPlayers: Int = 0, val error: String? = null)
    
    data class GeneratedTeam(
        val teamNumber: Int,
        val name: String,
        val players: List<String>,
        val playerIds: List<String> = emptyList()
    )

    data class ShuffleSession(
        val shuffleNumber: Int,
        val timestamp: Long,
        val teams: List<GeneratedTeam>,
        val players: List<String>,
        val joker: String?
    )

    val generatedTeams = MutableStateFlow<List<GeneratedTeam>>(emptyList())
    private var nextShuffleNumber = 1
    val sessionHistory = MutableStateFlow<List<ShuffleSession>>(emptyList())

    val teammatePairCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pairStatistics: StateFlow<PairStatistics> = teammatePairCounts.map { counts ->
        TeammatePairTracker.calculatePairStatistics(counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PairStatistics())

    val candidatePairAnalysis = MutableStateFlow<CandidatePairAnalysis?>(null)

    val generatedSignaturesSet = MutableStateFlow<Set<String>>(emptySet())
    val duplicatesPrevented = MutableStateFlow(0)
    val currentShuffleNumber = MutableStateFlow(0)
    val uniqueTeamsGenerated = MutableStateFlow(0)

    fun generateArrangementSignature(teams: List<GeneratedTeam>, joker: String?): String {
        val sortedTeams = teams.map { team ->
            team.players.map { it.trim() }.sorted().joinToString(",")
        }.sorted()
        val teamsStr = sortedTeams.joinToString(" | ")
        val jokerStr = if (!joker.isNullOrBlank()) " [JOKER: ${joker.trim()}]" else ""
        return "$teamsStr$jokerStr"
    }
    
    private val jokerPrefs = application.getSharedPreferences("joker_rotation_prefs", Context.MODE_PRIVATE)

    val previousJokersHistory = MutableStateFlow<List<String>>(emptyList())
    val currentCycleJokers = MutableStateFlow<List<String>>(emptyList())
    val jokerPlayer = MutableStateFlow<String?>(null)

    val remainingJokerCandidates: StateFlow<List<String>> = combine(buildPlayersList, currentCycleJokers) { players, cycle ->
        val active = players.map { it.trim() }.filter { it.isNotBlank() }
        val cycleSet = cycle.toSet()
        val remaining = active.filter { it !in cycleSet }
        if (remaining.isEmpty() && active.isNotEmpty()) {
            active
        } else {
            remaining
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun loadJokerHistory() {
        val jsonHistoryStr = jokerPrefs.getString("previous_jokers_history", "[]") ?: "[]"
        val jsonCycleStr = jokerPrefs.getString("current_cycle_jokers", "[]") ?: "[]"
        val curJoker = jokerPrefs.getString("current_joker", null)

        val historyList = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonHistoryStr)
            for (i in 0 until arr.length()) historyList.add(arr.getString(i))
        } catch (e: Exception) { e.printStackTrace() }

        val cycleList = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonCycleStr)
            for (i in 0 until arr.length()) cycleList.add(arr.getString(i))
        } catch (e: Exception) { e.printStackTrace() }

        previousJokersHistory.value = historyList
        currentCycleJokers.value = cycleList
        jokerPlayer.value = curJoker
    }

    private fun saveJokerHistory() {
        val jsonHistory = JSONArray(previousJokersHistory.value).toString()
        val jsonCycle = JSONArray(currentCycleJokers.value).toString()
        jokerPrefs.edit()
            .putString("previous_jokers_history", jsonHistory)
            .putString("current_cycle_jokers", jsonCycle)
            .putString("current_joker", jokerPlayer.value)
            .apply()
    }

    fun clearJokerHistory() {
        previousJokersHistory.value = emptyList()
        currentCycleJokers.value = emptyList()
        jokerPlayer.value = null
        jokerPrefs.edit().clear().apply()
    }

    val teamConfigState = combine(configNumberOfTeams, buildPlayersList) { numTeamsStr, players ->
        val numTeams = numTeamsStr.toIntOrNull() ?: 0
        val totalPlayers = players.size
        
        if (numTeams <= 1) {
            TeamConfigState(0, 0, "Number of teams must be at least 2.")
        } else if (numTeams > totalPlayers) {
            TeamConfigState(0, 0, "Number of teams cannot exceed total active players.")
        } else {
            TeamConfigState(totalPlayers / numTeams, totalPlayers % numTeams, null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeamConfigState(0,0,null))

    fun shuffleTeams() {
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
    }

    fun clearSessionHistory() {
        sessionHistory.value = emptyList()
        teammatePairCounts.value = emptyMap()
        candidatePairAnalysis.value = null
        generatedSignaturesSet.value = emptySet()
        duplicatesPrevented.value = 0
        currentShuffleNumber.value = 0
        uniqueTeamsGenerated.value = 0
        nextShuffleNumber = 1
    }

    init {
        loadJokerHistory()
        generatePlayersForFormation("Football", "4-3-3")
    }

    fun updateTeamName(name: String) {
        teamName.value = name
    }

    fun updateSportType(sport: String) {
        selectedSport.value = sport
        if (sport == "Cricket") {
            generatePlayersForFormation("Cricket", "Standard")
        } else {
            generatePlayersForFormation("Football", selectedFormation.value)
        }
    }

    fun updateFormation(formation: String) {
        selectedFormation.value = formation
        generatePlayersForFormation("Football", formation)
    }

    fun updatePlayerName(index: Int, name: String) {
        val currentList = playerSlots.value.toMutableList()
        val targetIndex = currentList.indexOfFirst { it.index == index }
        if (targetIndex != -1) {
            currentList[targetIndex] = currentList[targetIndex].copy(name = name)
            playerSlots.value = currentList
        }
    }

    private fun generatePlayersForFormation(sport: String, formation: String) {
        if (sport == "Cricket") {
            playerSlots.value = listOf(
                PlayerSlot(0, "WK", 0.5f, 0.9f, "Player 1"),
                PlayerSlot(1, "Bowler", 0.35f, 0.75f, "Player 2"),
                PlayerSlot(2, "Bowler", 0.65f, 0.75f, "Player 3"),
                PlayerSlot(3, "All-Rounder", 0.2f, 0.55f, "Player 4"),
                PlayerSlot(4, "All-Rounder", 0.5f, 0.55f, "Player 5"),
                PlayerSlot(5, "All-Rounder", 0.8f, 0.55f, "Player 6"),
                PlayerSlot(6, "Batsman", 0.15f, 0.3f, "Player 7"),
                PlayerSlot(7, "Batsman", 0.4f, 0.3f, "Player 8"),
                PlayerSlot(8, "Batsman", 0.6f, 0.3f, "Player 9"),
                PlayerSlot(9, "Batsman", 0.85f, 0.3f, "Player 10"),
                PlayerSlot(10, "Captain", 0.5f, 0.15f, "Player 11")
            )
            return
        }

        // Football formations
        when (formation) {
            "4-3-3" -> {
                playerSlots.value = listOf(
                    PlayerSlot(0, "GK", 0.5f, 0.88f, "Goalkeeper"),
                    PlayerSlot(1, "LB", 0.15f, 0.68f, "L. Defender"),
                    PlayerSlot(2, "CB", 0.38f, 0.72f, "C. Defender L"),
                    PlayerSlot(3, "CB", 0.62f, 0.72f, "C. Defender R"),
                    PlayerSlot(4, "RB", 0.85f, 0.68f, "R. Defender"),
                    PlayerSlot(5, "LCM", 0.25f, 0.45f, "Midfielder L"),
                    PlayerSlot(6, "CM", 0.5f, 0.48f, "Playmaker"),
                    PlayerSlot(7, "RCM", 0.75f, 0.45f, "Midfielder R"),
                    PlayerSlot(8, "LW", 0.2f, 0.22f, "Winger L"),
                    PlayerSlot(9, "ST", 0.5f, 0.18f, "Striker"),
                    PlayerSlot(10, "RW", 0.8f, 0.22f, "Winger R")
                )
            }
            "4-4-2" -> {
                playerSlots.value = listOf(
                    PlayerSlot(0, "GK", 0.5f, 0.88f, "Goalkeeper"),
                    PlayerSlot(1, "LB", 0.15f, 0.68f, "Def L"),
                    PlayerSlot(2, "CB", 0.38f, 0.72f, "Def CL"),
                    PlayerSlot(3, "CB", 0.62f, 0.72f, "Def CR"),
                    PlayerSlot(4, "RB", 0.85f, 0.68f, "Def R"),
                    PlayerSlot(5, "LM", 0.15f, 0.45f, "Mid L"),
                    PlayerSlot(6, "CM", 0.38f, 0.48f, "Mid CL"),
                    PlayerSlot(7, "CM", 0.62f, 0.48f, "Mid CR"),
                    PlayerSlot(8, "RM", 0.85f, 0.45f, "Mid R"),
                    PlayerSlot(9, "ST", 0.35f, 0.2f, "Striker L"),
                    PlayerSlot(10, "ST", 0.65f, 0.2f, "Striker R")
                )
            }
            "3-5-2" -> {
                playerSlots.value = listOf(
                    PlayerSlot(0, "GK", 0.5f, 0.88f, "Goalkeeper"),
                    PlayerSlot(1, "CB", 0.25f, 0.7f, "CB Left"),
                    PlayerSlot(2, "CB", 0.5f, 0.72f, "CB Center"),
                    PlayerSlot(3, "CB", 0.75f, 0.7f, "CB Right"),
                    PlayerSlot(4, "LWB", 0.12f, 0.48f, "Wingback L"),
                    PlayerSlot(5, "CM", 0.34f, 0.46f, "Midfielder L"),
                    PlayerSlot(6, "DM", 0.5f, 0.54f, "Def Midfielder"),
                    PlayerSlot(7, "CM", 0.66f, 0.46f, "Midfielder R"),
                    PlayerSlot(8, "RWB", 0.88f, 0.48f, "Wingback R"),
                    PlayerSlot(9, "ST", 0.35f, 0.2f, "Striker L"),
                    PlayerSlot(10, "ST", 0.65f, 0.2f, "Striker R")
                )
            }
        }
    }

    fun saveActiveLineup() {
        viewModelScope.launch {
            val playersString = playerSlots.value.joinToString(", ") { "${it.positionLabel}:${it.name}" }
            val newEntity = LineupEntity(
                teamName = teamName.value,
                formation = if (selectedSport.value == "Cricket") "Cricket Lineup" else selectedFormation.value,
                sportType = selectedSport.value,
                playersJson = playersString
            )
            repository.insertLineup(newEntity)
            saveMessage.value = "Lineup saved to history!"

            // Sync with Firebase Firestore if user is logged in
            firebaseService.syncLineupToCloud(
                teamName = teamName.value,
                formation = newEntity.formation,
                players = playersString
            )

            delay(2000)
            saveMessage.value = ""
        }
    }

    fun deleteLineup(id: Int) {
        viewModelScope.launch {
            repository.deleteLineup(id)
        }
    }

    fun triggerToss() {
        if (isCoinFlipping.value) return

        viewModelScope.launch {
            isCoinFlipping.value = true
            tossResult.value = null
            tossStatusMessage.value = "Flipping coin..."

            // Simulate high-fidelity premium coin spinning delays
            delay(1500)

            val outcomes = listOf("Heads", "Tails")
            val selected = selectedTossChoice.value
            val outcome = outcomes.random()

            val won = selected.equals(outcome, ignoreCase = true)
            val newToss = TossEntity(
                choice = selected,
                result = outcome,
                hasWon = won
            )

            repository.insertToss(newToss)

            tossResult.value = outcome
            tossStatusMessage.value = if (won) {
                "Congratulations! You won the toss!"
            } else {
                "Oh, you lost the toss. Better luck next match!"
            }
            isCoinFlipping.value = false
        }
    }

    fun clearAllTosses() {
        viewModelScope.launch {
            repository.clearTossHistory()
        }
    }
}
