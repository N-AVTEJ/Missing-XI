package com.example.ui.viewmodel

import kotlin.math.roundToInt
import android.content.Context
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseService
import com.example.data.model.LineupEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import com.example.util.CandidatePairAnalysis
import com.example.util.FairnessConfig
import com.example.util.FairnessMode
import com.example.util.FairnessSettings
import com.example.util.GeneratedTeam
import com.example.util.MatchFairnessConfig
import com.example.util.MatchFairnessProfile
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
        runDiagnostics()
    }

    private fun runDiagnostics() {
        viewModelScope.launch {
            try {
                // Wait briefly for initial flows to emit
                kotlinx.coroutines.delay(1000)
                
                val playerCount = allActivePlayers.value.size
                val tossCount = savedTosses.value.size
                val lineupCount = savedLineups.value.size
                val hasSession = if (latestSession.value != null) 1 else 0
                
                android.util.Log.d("PersistenceDiagnostics", "Database opened successfully")
                android.util.Log.d("PersistenceDiagnostics", "Existing player count: $playerCount")
                android.util.Log.d("PersistenceDiagnostics", "Existing match history (lineups) count: $lineupCount")
                android.util.Log.d("PersistenceDiagnostics", "Existing match history (tosses) count: $tossCount")
                android.util.Log.d("PersistenceDiagnostics", "Existing session count: $hasSession")
                
                if (playerCount == 0 && lineupCount == 0) {
                    android.util.Log.d("PersistenceDiagnostics", "Zero records found. This is normal if it is a fresh install or if the app's data was cleared by Android (e.g. uninstallation). Data is stored in private internal storage which is wiped on uninstall.")
                }
            } catch (e: Exception) {
                android.util.Log.e("PersistenceDiagnostics", "Error running diagnostics: ${e.message}")
            }
        }
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
    
    
    data class GenerationDiagnostics(
        val candidatesGenerated: Int,
        val candidatesRejected: Int,
        val retryCount: Int,
        val generationTimeMs: Long,
        val averageCandidatePenalty: Double,
        val bestCandidateRank: Int = 1,
        val lowestPenaltyFound: Int = 0,
        val highestPenaltyFound: Int = 0,
        val winningCandidatePenalty: Int = 0,
        val winningCandidateFairnessScore: Int = 0,
        val fairnessTarget: Int = com.example.util.FairnessConfig.MINIMUM_OVERALL_FAIRNESS_SCORE.toInt(),
        val qualityGateStatus: String = "",
        val additionalAttempts: Int = 0,
        val failedCriteria: List<String> = emptyList(),
        val fairnessMode: String = FairnessMode.BALANCED.name,
        val activeSettings: FairnessSettings = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS
    )

    data class GeneratedCandidate(
        val candidateId: String,
        val teams: List<GeneratedTeam>,
        val joker: String?,
        val signature: String,
        val pairAnalysis: com.example.util.CandidatePairAnalysis,
        val penaltyAnalysis: com.example.util.CandidatePenaltyResult,
        val opponentAnalysis: com.example.util.OpponentPairAnalysis,
        val strengthAnalysis: com.example.util.CandidateStrengthAnalysis = com.example.util.CandidateStrengthAnalysis(),
        val updatedCycle: List<String>,
        val updatedHistory: List<String>,
        val generatedAt: Long = System.currentTimeMillis(),
        val fairnessScore: Int = 0,
        val fairnessRating: String = "",
        val fairnessEvaluation: com.example.util.FairnessEvaluation = com.example.util.FairnessEvaluation(),
        val qualityResult: com.example.util.FairnessQualityResult = com.example.util.FairnessQualityResult()
    )

    val isGeneratingCandidates = MutableStateFlow(false)
    val candidateGenerationProgress = MutableStateFlow(0)
    val candidateGenerationTarget = MutableStateFlow(0)
    val candidatesGeneratedList = MutableStateFlow<List<GeneratedCandidate>>(emptyList())
    val generationDiagnostics = MutableStateFlow<GenerationDiagnostics?>(null)
    val currentStrengthAnalysis = MutableStateFlow<com.example.util.CandidateStrengthAnalysis?>(null)

    val playerSortOption = MutableStateFlow(com.example.util.PlayerSortOption.FAVORITES_FIRST)

    fun setPlayerSortOption(option: com.example.util.PlayerSortOption) {
        playerSortOption.value = option
    }

    val sortedActivePlayers: StateFlow<List<PlayerEntity>> = combine(
        allActivePlayers,
        playerSortOption
    ) { players, sortOpt ->
        com.example.util.PlayerChemistryEngine.sortPlayers(players, sortOpt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<com.example.util.DashboardStats> = combine(
        allActivePlayers,
        repository.sessionCount
    ) { players, sessionCount ->
        com.example.util.PlayerChemistryEngine.calculateDashboardStats(players, sessionCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.util.DashboardStats())

    val highestRatedPlayer: StateFlow<PlayerEntity?> = allActivePlayers.map { players ->
        players.maxByOrNull { it.skillRating }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val averagePlayerSkill: StateFlow<Double> = allActivePlayers.map { players ->
        if (players.isNotEmpty()) players.map { it.skillRating }.average() else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val teamStrengthDistribution: StateFlow<Map<String, Int>> = currentStrengthAnalysis.map { analysis ->
        if (analysis != null) {
            val dist = mutableMapOf<String, Int>()
            analysis.teamStrengths.forEachIndexed { idx, str ->
                dist["Team " + (('A' + idx).toString())] = str
            }
            dist
        } else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun updatePlayerSkill(player: PlayerEntity, rating: Int) {
        val clamped = rating.coerceIn(1, 10)
        viewModelScope.launch {
            repository.updatePlayer(player.copy(skillRating = clamped))
        }
    }

    data class TeamConfigState(val playersPerTeam: Int = 0, val remainingPlayers: Int = 0, val error: String? = null)

    data class ShuffleSession(
        val shuffleNumber: Int,
        val timestamp: Long,
        val teams: List<GeneratedTeam>,
        val players: List<String>,
        val joker: String?,
        val fairnessProfile: String = "Legacy Result",
        val qualityGateOutcome: String = "Passed",
        val fairnessScore: Int = 0,
        val targetScore: Int = 70
    )

    // Match-Level Fairness Profile State
    val matchFairnessConfig = MutableStateFlow(MatchFairnessConfig())
    val appliedMatchProfile = MutableStateFlow(MatchFairnessProfile.GLOBAL_DEFAULT)
    val appliedSettingsSnapshot = MutableStateFlow(FairnessConfig.DEFAULT_FAIRNESS_SETTINGS)

    fun setMatchFairnessProfile(profile: MatchFairnessProfile) {
        if (isGeneratingCandidates.value) return
        val current = matchFairnessConfig.value
        val updated = if (profile == MatchFairnessProfile.CUSTOM && current.profile != MatchFairnessProfile.CUSTOM) {
            val baseline = current.resolveSettings(fairnessSettings.value)
            current.copy(
                profile = profile,
                customSettings = baseline.copy(fairnessMode = FairnessMode.CUSTOM)
            )
        } else {
            current.copy(profile = profile)
        }
        matchFairnessConfig.value = updated
    }

    fun updateMatchCustomMinScore(score: Double) {
        if (isGeneratingCandidates.value) return
        val current = matchFairnessConfig.value
        val updatedCustom = current.customSettings.copy(
            minimumOverallFairnessScore = score.coerceIn(0.0, 100.0),
            fairnessMode = FairnessMode.CUSTOM
        )
        matchFairnessConfig.value = current.copy(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = updatedCustom
        )
    }

    fun updateMatchCustomMaxStrengthDiff(diff: Int) {
        if (isGeneratingCandidates.value) return
        val current = matchFairnessConfig.value
        val updatedCustom = current.customSettings.copy(
            maximumTeamStrengthDifference = diff.coerceAtLeast(0),
            fairnessMode = FairnessMode.CUSTOM
        )
        matchFairnessConfig.value = current.copy(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = updatedCustom
        )
    }

    fun updateMatchCustomMaxTeammatePenalty(penalty: Int) {
        if (isGeneratingCandidates.value) return
        val current = matchFairnessConfig.value
        val updatedCustom = current.customSettings.copy(
            maximumTeammatePenalty = penalty.coerceAtLeast(0),
            fairnessMode = FairnessMode.CUSTOM
        )
        matchFairnessConfig.value = current.copy(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = updatedCustom
        )
    }

    fun updateMatchCustomMaxOpponentPenalty(penalty: Int) {
        if (isGeneratingCandidates.value) return
        val current = matchFairnessConfig.value
        val updatedCustom = current.customSettings.copy(
            maximumOpponentPenalty = penalty.coerceAtLeast(0),
            fairnessMode = FairnessMode.CUSTOM
        )
        matchFairnessConfig.value = current.copy(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = updatedCustom
        )
    }

    fun updateMatchCustomMaxRetries(attempts: Int) {
        if (isGeneratingCandidates.value) return
        val current = matchFairnessConfig.value
        val updatedCustom = current.customSettings.copy(
            maxAdditionalGenerationAttempts = attempts.coerceIn(0, 20),
            fairnessMode = FairnessMode.CUSTOM
        )
        matchFairnessConfig.value = current.copy(
            profile = MatchFairnessProfile.CUSTOM,
            customSettings = updatedCustom
        )
    }

    val generatedTeams = MutableStateFlow<List<GeneratedTeam>>(emptyList())
    private var nextShuffleNumber = 1
    val sessionHistory = MutableStateFlow<List<ShuffleSession>>(emptyList())

    val teammatePairCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pairStatistics: StateFlow<PairStatistics> = teammatePairCounts.map { counts ->
        TeammatePairTracker.calculatePairStatistics(counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PairStatistics())

    val candidatePairAnalysis = MutableStateFlow<CandidatePairAnalysis?>(null)
    val currentFairnessScore = MutableStateFlow(0)
    val currentFairnessRating = MutableStateFlow("")
    val candidateFairnessEvaluation = MutableStateFlow<com.example.util.FairnessEvaluation?>(null)
    val candidateQualityResult = MutableStateFlow<com.example.util.FairnessQualityResult?>(null)
    val additionalGenerationAttempts = MutableStateFlow(0)
    val candidatesEvaluatedCount = MutableStateFlow(0)
    val bestFairnessScore = MutableStateFlow(0.0)
    
    val opponentPairCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val opponentStatistics: StateFlow<PairStatistics> = opponentPairCounts.map { counts ->
        com.example.util.OpponentPairTracker.getOpponentStatistics(counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PairStatistics())

    val candidateOpponentAnalysis = MutableStateFlow<com.example.util.OpponentPairAnalysis?>(null)
    

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

    // Fairness Settings Persistence & State
    private val fairnessPrefs = application.getSharedPreferences("fairness_settings_prefs", Context.MODE_PRIVATE)

    private fun loadFairnessSettings(): FairnessSettings {
        val modeStr = fairnessPrefs.getString("fairness_mode", FairnessMode.BALANCED.name) ?: FairnessMode.BALANCED.name
        val mode = try {
            FairnessMode.valueOf(modeStr)
        } catch (e: Exception) {
            FairnessMode.BALANCED
        }

        val minScore = fairnessPrefs.getFloat(
            "min_fairness_score",
            FairnessConfig.DEFAULT_FAIRNESS_SETTINGS.minimumOverallFairnessScore.toFloat()
        ).toDouble()
        val maxDiff = fairnessPrefs.getInt(
            "max_strength_diff",
            FairnessConfig.DEFAULT_FAIRNESS_SETTINGS.maximumTeamStrengthDifference
        )
        val maxTmPenalty = fairnessPrefs.getInt(
            "max_teammate_penalty",
            FairnessConfig.DEFAULT_FAIRNESS_SETTINGS.maximumTeammatePenalty
        )
        val maxOppPenalty = fairnessPrefs.getInt(
            "max_opponent_penalty",
            FairnessConfig.DEFAULT_FAIRNESS_SETTINGS.maximumOpponentPenalty
        )
        val maxAttempts = fairnessPrefs.getInt(
            "max_additional_attempts",
            FairnessConfig.DEFAULT_FAIRNESS_SETTINGS.maxAdditionalGenerationAttempts
        )

        return FairnessSettings(
            minimumOverallFairnessScore = minScore,
            maximumTeamStrengthDifference = maxDiff,
            maximumTeammatePenalty = maxTmPenalty,
            maximumOpponentPenalty = maxOppPenalty,
            maxAdditionalGenerationAttempts = maxAttempts,
            fairnessMode = mode
        )
    }

    private fun saveFairnessSettings(settings: FairnessSettings) {
        fairnessPrefs.edit()
            .putString("fairness_mode", settings.fairnessMode.name)
            .putFloat("min_fairness_score", settings.minimumOverallFairnessScore.toFloat())
            .putInt("max_strength_diff", settings.maximumTeamStrengthDifference)
            .putInt("max_teammate_penalty", settings.maximumTeammatePenalty)
            .putInt("max_opponent_penalty", settings.maximumOpponentPenalty)
            .putInt("max_additional_attempts", settings.maxAdditionalGenerationAttempts)
            .apply()
    }

    val fairnessSettings = MutableStateFlow(loadFairnessSettings())

    fun selectFairnessMode(mode: FairnessMode) {
        val newSettings = FairnessConfig.getPreset(mode)
        fairnessSettings.value = newSettings
        saveFairnessSettings(newSettings)
    }

    fun updateMinimumFairnessScore(score: Double) {
        val clamped = score.coerceIn(0.0, 100.0)
        val cur = fairnessSettings.value
        val detected = FairnessConfig.detectMode(
            minScore = clamped,
            maxStrengthDiff = cur.maximumTeamStrengthDifference,
            maxTeammatePenalty = cur.maximumTeammatePenalty,
            maxOpponentPenalty = cur.maximumOpponentPenalty,
            maxAttempts = cur.maxAdditionalGenerationAttempts
        )
        val updated = cur.copy(minimumOverallFairnessScore = clamped, fairnessMode = detected)
        fairnessSettings.value = updated
        saveFairnessSettings(updated)
    }

    fun updateMaximumTeamStrengthDifference(diff: Int) {
        val validDiff = diff.coerceAtLeast(0)
        val cur = fairnessSettings.value
        val detected = FairnessConfig.detectMode(
            minScore = cur.minimumOverallFairnessScore,
            maxStrengthDiff = validDiff,
            maxTeammatePenalty = cur.maximumTeammatePenalty,
            maxOpponentPenalty = cur.maximumOpponentPenalty,
            maxAttempts = cur.maxAdditionalGenerationAttempts
        )
        val updated = cur.copy(maximumTeamStrengthDifference = validDiff, fairnessMode = detected)
        fairnessSettings.value = updated
        saveFairnessSettings(updated)
    }

    fun updateMaximumTeammatePenalty(penalty: Int) {
        val validPenalty = penalty.coerceAtLeast(0)
        val cur = fairnessSettings.value
        val detected = FairnessConfig.detectMode(
            minScore = cur.minimumOverallFairnessScore,
            maxStrengthDiff = cur.maximumTeamStrengthDifference,
            maxTeammatePenalty = validPenalty,
            maxOpponentPenalty = cur.maximumOpponentPenalty,
            maxAttempts = cur.maxAdditionalGenerationAttempts
        )
        val updated = cur.copy(maximumTeammatePenalty = validPenalty, fairnessMode = detected)
        fairnessSettings.value = updated
        saveFairnessSettings(updated)
    }

    fun updateMaximumOpponentPenalty(penalty: Int) {
        val validPenalty = penalty.coerceAtLeast(0)
        val cur = fairnessSettings.value
        val detected = FairnessConfig.detectMode(
            minScore = cur.minimumOverallFairnessScore,
            maxStrengthDiff = cur.maximumTeamStrengthDifference,
            maxTeammatePenalty = cur.maximumTeammatePenalty,
            maxOpponentPenalty = validPenalty,
            maxAttempts = cur.maxAdditionalGenerationAttempts
        )
        val updated = cur.copy(maximumOpponentPenalty = validPenalty, fairnessMode = detected)
        fairnessSettings.value = updated
        saveFairnessSettings(updated)
    }

    fun updateMaxAdditionalGenerationAttempts(attempts: Int) {
        val validAttempts = attempts.coerceIn(0, 20)
        val cur = fairnessSettings.value
        val detected = FairnessConfig.detectMode(
            minScore = cur.minimumOverallFairnessScore,
            maxStrengthDiff = cur.maximumTeamStrengthDifference,
            maxTeammatePenalty = cur.maximumTeammatePenalty,
            maxOpponentPenalty = cur.maximumOpponentPenalty,
            maxAttempts = validAttempts
        )
        val updated = cur.copy(maxAdditionalGenerationAttempts = validAttempts, fairnessMode = detected)
        fairnessSettings.value = updated
        saveFairnessSettings(updated)
    }

    fun resetFairnessSettingsToDefault() {
        val defaults = FairnessConfig.DEFAULT_FAIRNESS_SETTINGS
        fairnessSettings.value = defaults
        saveFairnessSettings(defaults)
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
        if (isGeneratingCandidates.value) return
        val numTeams = configNumberOfTeams.value.toIntOrNull() ?: 0
        val activePlayersMap = allActivePlayers.value.associateBy { it.displayName }
        val activePlayerObjects = buildPlayersList.value.mapIndexed { index, name ->
            val cleanName = name.trim()
            val dbPlayer = activePlayersMap[cleanName]
            Player(
                id = dbPlayer?.id ?: "player_${index + 1}",
                name = cleanName,
                skillRating = dbPlayer?.skillRating ?: 5
            )
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
        
        // 1. Capture immutable snapshot of match configuration & global settings
        val activeMatchConfig = matchFairnessConfig.value
        val globalSettingsSnapshot = fairnessSettings.value
        val effectiveSettings = activeMatchConfig.resolveSettings(globalSettingsSnapshot)
        val selectedProfile = activeMatchConfig.profile
        val profileDisplayName = selectedProfile.displayName

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            var candidatesRejected = 0
            var retryCount = 0
            
            val currentPairCounts = teammatePairCounts.value
            val currentOpponentCounts = opponentPairCounts.value

            val generatedCandidates = mutableListOf<GeneratedCandidate>()
            var totalCandidatesRejected = 0
            var totalRetryCount = 0
            var attemptsMade = 0
            var qualityGatePassed = false
            var winningCandidate: GeneratedCandidate? = null

            android.util.Log.d("FAIRNESS", "[FAIRNESS] Initial candidates target: $candidateTarget, Profile: $profileDisplayName, Mode: ${effectiveSettings.fairnessMode.name}, Max Retries: ${effectiveSettings.maxAdditionalGenerationAttempts}")

            for (attempt in 0..effectiveSettings.maxAdditionalGenerationAttempts) {
                attemptsMade = attempt
                if (attempt > 0) {
                    android.util.Log.d("FAIRNESS", "[FAIRNESS] Quality gate FAILED. Generating additional candidates (Attempt $attempt/${effectiveSettings.maxAdditionalGenerationAttempts})")
                }

                var batchRetryCount = 0
                val batchCandidatesCount = generatedCandidates.size

                while (generatedCandidates.size < batchCandidatesCount + candidateTarget && batchRetryCount < maxRetries) {
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
                        val strAnalysis = com.example.util.TeamStrengthEngine.calculateTeamStrength(players)
                        GeneratedTeam(
                            teamNumber = index + 1,
                            name = "Team " + (('A' + index).toString()),
                            players = players.map { it.name },
                            playerIds = players.map { it.id },
                            totalStrength = strAnalysis.totalStrength,
                            averageStrength = strAnalysis.averageStrength,
                            maxPlayerRating = strAnalysis.maxPlayerRating,
                            minPlayerRating = strAnalysis.minPlayerRating
                        )
                    }

                    val strengthAnalysis = com.example.util.TeamStrengthEngine.evaluateCandidateStrength(candidateTeams)
                    val sig = generateArrangementSignature(candidateTeams, tempJoker)

                    if (existingSigs.contains(sig) || generatedCandidates.any { it.signature == sig }) {
                        totalCandidatesRejected++
                        batchRetryCount++
                        totalRetryCount++
                    } else {
                        val acceptedTeamsPlayerIds = candidateTeams.map { it.playerIds }
                        val pairAnalysis = com.example.util.TeammatePairTracker.analyzeCandidatePairs(
                            acceptedTeamsPlayerIds,
                            currentPairCounts
                        )
                        val opponentAnalysis = com.example.util.OpponentPairTracker.analyzeOpponentPairs(
                            acceptedTeamsPlayerIds,
                            currentOpponentCounts
                        )
                        val penaltyAnalysis = pairAnalysis.penaltyResult 

                        val evaluation = com.example.util.FairnessRankingEngine.evaluateCandidateFairness(
                            pairAnalysis = pairAnalysis,
                            opponentAnalysis = opponentAnalysis,
                            strengthAnalysis = strengthAnalysis,
                            joker = tempJoker,
                            activePlayers = activePlayerNames,
                            cycleJokers = tempCycle
                        )
                        val fScore = evaluation.overallScore.roundToInt()
                        val fRating = com.example.util.FairnessRankingEngine.getFairnessRating(evaluation.overallScore)
                        val qResult = com.example.util.FairnessQualityResult.evaluate(evaluation, effectiveSettings)

                        generatedCandidates.add(
                            GeneratedCandidate(
                                candidateId = java.util.UUID.randomUUID().toString(),
                                teams = candidateTeams,
                                joker = tempJoker,
                                signature = sig,
                                pairAnalysis = pairAnalysis,
                                penaltyAnalysis = penaltyAnalysis,
                                opponentAnalysis = opponentAnalysis,
                                strengthAnalysis = strengthAnalysis,
                                updatedCycle = tempCycle,
                                updatedHistory = tempHistory,
                                fairnessScore = fScore,
                                fairnessRating = fRating,
                                fairnessEvaluation = evaluation,
                                qualityResult = qResult
                            )
                        )
                        candidateGenerationProgress.value = generatedCandidates.size
                    }
                }

                // Re-rank candidates
                generatedCandidates.shuffle()
                generatedCandidates.sortWith { c1, c2 ->
                    com.example.util.FairnessRankingEngine.compareCandidates(c1.fairnessEvaluation, c2.fairnessEvaluation)
                }

                val topCandidate = generatedCandidates.firstOrNull()
                if (topCandidate != null) {
                    val qResult = com.example.util.FairnessQualityResult.evaluate(topCandidate.fairnessEvaluation, effectiveSettings)
                    android.util.Log.d("FAIRNESS", "[FAIRNESS] Attempt $attempt Best score: ${topCandidate.fairnessScore}, Passed: ${qResult.passed}")

                    if (qResult.passed) {
                        qualityGatePassed = true
                        winningCandidate = topCandidate.copy(qualityResult = qResult)
                        android.util.Log.d("FAIRNESS", "[FAIRNESS] Quality gate: PASSED")
                        break
                    } else {
                        android.util.Log.d("FAIRNESS", "[FAIRNESS] Quality gate: FAILED (${qResult.failedChecks.joinToString("; ")})")
                    }
                }
            }

            val endTime = System.currentTimeMillis()

            val rankedCandidates = generatedCandidates.mapIndexed { index, candidate ->
                candidate.copy(
                    fairnessEvaluation = candidate.fairnessEvaluation.copy(rankingPosition = index + 1)
                )
            }

            val finalChosen = if (qualityGatePassed && winningCandidate != null) {
                winningCandidate
            } else {
                val top = rankedCandidates.firstOrNull()
                if (top != null) {
                    val evalResult = com.example.util.FairnessQualityResult.evaluate(top.fairnessEvaluation, effectiveSettings)
                    top.copy(
                        qualityResult = evalResult.copy(qualityLabel = com.example.util.FairnessQualityLabel.BEST_AVAILABLE)
                    )
                } else null
            }

            if (finalChosen != null) {
                android.util.Log.d("FAIRNESS", "[FAIRNESS] Final candidate accepted. Quality: ${finalChosen.qualityResult.qualityLabel.name}, Score: ${finalChosen.fairnessScore}")
            }

            val avgPenalty = if (rankedCandidates.isNotEmpty()) rankedCandidates.map { it.penaltyAnalysis.totalPenalty.toDouble() }.average() else 0.0
            val lowestPenalty = rankedCandidates.minOfOrNull { it.penaltyAnalysis.totalPenalty } ?: 0
            val highestPenalty = rankedCandidates.maxOfOrNull { it.penaltyAnalysis.totalPenalty } ?: 0

            candidatesEvaluatedCount.value = rankedCandidates.size
            bestFairnessScore.value = finalChosen?.fairnessEvaluation?.overallScore ?: 0.0
            additionalGenerationAttempts.value = attemptsMade
            
            generationDiagnostics.value = GenerationDiagnostics(
                candidatesGenerated = rankedCandidates.size,
                candidatesRejected = totalCandidatesRejected,
                retryCount = totalRetryCount,
                generationTimeMs = endTime - startTime,
                averageCandidatePenalty = avgPenalty,
                bestCandidateRank = 1,
                lowestPenaltyFound = lowestPenalty,
                highestPenaltyFound = highestPenalty,
                winningCandidatePenalty = finalChosen?.penaltyAnalysis?.totalPenalty ?: 0,
                winningCandidateFairnessScore = finalChosen?.fairnessScore ?: 0,
                fairnessTarget = effectiveSettings.minimumOverallFairnessScore.toInt(),
                qualityGateStatus = finalChosen?.qualityResult?.qualityLabel?.name ?: "UNKNOWN",
                additionalAttempts = attemptsMade,
                failedCriteria = finalChosen?.qualityResult?.failedChecks ?: emptyList(),
                fairnessMode = profileDisplayName,
                activeSettings = effectiveSettings
            )

            candidatesGeneratedList.value = rankedCandidates

            // Back to main thread for applying the chosen candidate
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (finalChosen != null) {
                    applyCandidateToState(
                        chosen = finalChosen, 
                        activePlayerNames = activePlayerNames, 
                        numTeams = numTeams, 
                        existingSigs = existingSigs,
                        profile = selectedProfile,
                        settingsSnapshot = effectiveSettings
                    )
                    duplicatesPrevented.value += totalCandidatesRejected 
                }
                isGeneratingCandidates.value = false
            }
        }
    }

    private fun applyCandidateToState(
        chosen: GeneratedCandidate, 
        activePlayerNames: List<String>, 
        numTeams: Int,
        existingSigs: MutableSet<String>,
        profile: MatchFairnessProfile,
        settingsSnapshot: FairnessSettings
    ) {
        appliedMatchProfile.value = profile
        appliedSettingsSnapshot.value = settingsSnapshot

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

        val qualityOutcome = if (chosen.qualityResult.passed) "Passed" else "Best Available"

        val session = ShuffleSession(
            shuffleNumber = shuffleNum,
            timestamp = System.currentTimeMillis(),
            teams = chosen.teams,
            players = activePlayerNames,
            joker = chosen.joker,
            fairnessProfile = profile.displayName,
            qualityGateOutcome = qualityOutcome,
            fairnessScore = chosen.fairnessScore,
            targetScore = settingsSnapshot.minimumOverallFairnessScore.toInt()
        )
        sessionHistory.value = listOf(session) + sessionHistory.value

        candidatePairAnalysis.value = chosen.pairAnalysis
        candidateOpponentAnalysis.value = chosen.opponentAnalysis
        currentStrengthAnalysis.value = chosen.strengthAnalysis
        candidateFairnessEvaluation.value = chosen.fairnessEvaluation
        candidateQualityResult.value = chosen.qualityResult
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
        )

        // Save session and players to database
        viewModelScope.launch {
            val jsonArray = org.json.JSONArray(activePlayerNames)
            val dbSession = com.example.data.model.SessionEntity(
                playerIdsJson = jsonArray.toString(),
                teamCount = numTeams,
                timestamp = System.currentTimeMillis(),
                overallFairnessScore = chosen.fairnessEvaluation.overallScore,
                fairnessRating = chosen.fairnessRating,
                teammateVarietyScore = chosen.fairnessEvaluation.teammateScore,
                opponentVarietyScore = chosen.fairnessEvaluation.opponentScore,
                teamStrengthScore = chosen.fairnessEvaluation.strengthScore,
                jokerFairnessScore = chosen.fairnessEvaluation.jokerScore,
                fairnessProfile = profile.displayName,
                settingsSnapshotJson = settingsSnapshot.toSummary(),
                fairnessThresholdUsed = settingsSnapshot.minimumOverallFairnessScore,
                qualityGateOutcome = qualityOutcome
            )
            repository.insertSession(dbSession)
            loadLatestSession() // Update latest session state

            val currentTime = System.currentTimeMillis()
            activePlayerNames.forEach { playerName ->
                val existingPlayer = repository.getPlayerByName(playerName)
                val isJoker = playerName == chosen.joker

                val favTeammate = com.example.util.PlayerChemistryEngine.getFavoriteTeammate(playerName, teammatePairCounts.value)
                val favOpponent = com.example.util.PlayerChemistryEngine.getFavoriteOpponent(playerName, opponentPairCounts.value)
                val totalTeammatesCount = com.example.util.PlayerChemistryEngine.getTotalTeammatesCount(playerName, teammatePairCounts.value)
                val totalOpponentsCount = com.example.util.PlayerChemistryEngine.getTotalOpponentsCount(playerName, opponentPairCounts.value)

                if (existingPlayer != null) {
                    val prevLastPlayed = if (existingPlayer.lastPlayedAt > 0) existingPlayer.lastPlayedAt else existingPlayer.lastUsedAt
                    val gap = if (prevLastPlayed > 0) currentTime - prevLastPlayed else 0L
                    val longestGap = if (gap > existingPlayer.longestGapSincePlayed) gap else existingPlayer.longestGapSincePlayed

                    val updatedMatches = existingPlayer.matchesPlayed + 1
                    val updatedTotalMatches = existingPlayer.totalMatches + 1
                    val updatedJoker = if (isJoker) existingPlayer.matchesAsJoker + 1 else existingPlayer.matchesAsJoker
                    val updatedTotalJoker = if (isJoker) existingPlayer.totalTimesJoker + 1 else existingPlayer.totalTimesJoker

                    repository.updatePlayer(
                        existingPlayer.copy(
                            lastUsedAt = currentTime,
                            lastPlayedAt = currentTime,
                            totalMatches = updatedTotalMatches,
                            matchesPlayed = updatedMatches,
                            totalTimesJoker = updatedTotalJoker,
                            matchesAsJoker = updatedJoker,
                            currentPlayStreak = existingPlayer.currentPlayStreak + 1,
                            longestGapSincePlayed = longestGap,
                            totalTeammates = totalTeammatesCount,
                            totalOpponents = totalOpponentsCount,
                            favoriteTeammateId = favTeammate?.idOrName,
                            favoriteOpponentId = favOpponent?.idOrName,
                            updatedAt = currentTime
                        )
                    )
                } else {
                    repository.insertPlayer(
                        com.example.data.model.PlayerEntity(
                            displayName = playerName,
                            totalMatches = 1,
                            matchesPlayed = 1,
                            totalTimesJoker = if (isJoker) 1 else 0,
                            matchesAsJoker = if (isJoker) 1 else 0,
                            currentPlayStreak = 1,
                            lastUsedAt = currentTime,
                            lastPlayedAt = currentTime,
                            totalTeammates = totalTeammatesCount,
                            totalOpponents = totalOpponentsCount,
                            favoriteTeammateId = favTeammate?.idOrName,
                            favoriteOpponentId = favOpponent?.idOrName,
                            updatedAt = currentTime
                        )
                    )
                }
            }
        }
    }

    fun clearSessionHistory() {
        sessionHistory.value = emptyList()
        teammatePairCounts.value = emptyMap()
        opponentPairCounts.value = emptyMap()
        candidatePairAnalysis.value = null
        candidateOpponentAnalysis.value = null
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
