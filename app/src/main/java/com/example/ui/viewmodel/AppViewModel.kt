package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseService
import com.example.data.model.LineupEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val repository = AppRepository(database.lineupDao(), database.tossDao())
    val firebaseService = FirebaseService()

    // Database Flows
    val savedLineups: StateFlow<List<LineupEntity>> = repository.allLineups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTosses: StateFlow<List<TossEntity>> = repository.allTosses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
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
