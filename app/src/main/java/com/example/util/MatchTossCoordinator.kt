package com.example.util

import com.example.data.model.SessionEntity
import com.example.data.model.TossEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.UUID

/**
 * Coordinates match confirmation, fair Heads/Tails coin tosses, SQLite persistence,
 * match finalization, and read-only reopening of historical matches.
 *
 * Strictly independent of the team fairness engine and player statistics:
 * tossing or re-flipping never mutates teams, fairness scores, or match counts.
 */
class MatchTossCoordinator(
    private val repository: AppRepository
) {
    private val _currentMatchId = MutableStateFlow<String?>(null)
    val currentMatchId: StateFlow<String?> = _currentMatchId.asStateFlow()

    private val _currentMatchStatus = MutableStateFlow(MatchStatus.DRAFT)
    val currentMatchStatus: StateFlow<MatchStatus> = _currentMatchStatus.asStateFlow()

    private val _tossUiState = MutableStateFlow(MatchTossUiState())
    val tossUiState: StateFlow<MatchTossUiState> = _tossUiState.asStateFlow()

    private val _reopenedMatchSummary = MutableStateFlow<MatchSummaryData?>(null)
    val reopenedMatchSummary: StateFlow<MatchSummaryData?> = _reopenedMatchSummary.asStateFlow()

    fun onTeamsGenerated(matchId: String) {
        _currentMatchId.value = matchId
        _currentMatchStatus.value = MatchStatus.TEAMS_GENERATED
        _tossUiState.value = MatchTossUiState(
            matchId = matchId,
            matchStatus = MatchStatus.TEAMS_GENERATED,
            selectedChoice = null,
            tossStatus = TossStatus.NOT_STARTED,
            pendingResult = null,
            latestTossResult = null,
            matchTossHistory = emptyList(),
            flipSequenceNumber = 0,
            errorMessage = null,
            isStandaloneMode = false
        )
    }

    /**
     * Transitions the current match to TEAMS_CONFIRMED (or preserves TOSS_COMPLETED if re-entering toss)
     * and persists the confirmed teams snapshot to SQLite without touching fairness or player stats.
     */
    suspend fun confirmTeamsForToss(
        matchId: String,
        dbSessionId: Int,
        teams: List<GeneratedTeam>,
        joker: String?
    ): Result<String> {
        if (teams.isEmpty()) {
            return Result.failure(IllegalStateException("No teams available to confirm."))
        }
        val currentStatus = _currentMatchStatus.value
        if (currentStatus == MatchStatus.COMPLETED && _currentMatchId.value == matchId) {
            return Result.failure(IllegalStateException("This match is already completed."))
        }

        val targetStatus = if (currentStatus == MatchStatus.TOSS_COMPLETED && _currentMatchId.value == matchId) {
            MatchStatus.TOSS_COMPLETED
        } else {
            MatchStatus.TEAMS_CONFIRMED
        }

        val finalTeamsJson = MatchTossEngine.serializeTeamsJson(teams, joker)

        try {
            val existingSession = repository.getSessionByMatchId(matchId)
                ?: if (dbSessionId > 0) repository.getSessionById(dbSessionId) else null

            if (existingSession != null) {
                repository.updateSession(
                    existingSession.copy(
                        matchId = matchId,
                        matchStatus = targetStatus.name,
                        finalTeamsJson = finalTeamsJson
                    )
                )
            } else {
                val participatingNames = (teams.flatMap { it.players } + listOfNotNull(joker)).distinct()
                repository.insertSession(
                    SessionEntity(
                        playerIdsJson = JSONArray(participatingNames).toString(),
                        teamCount = teams.size,
                        timestamp = System.currentTimeMillis(),
                        matchId = matchId,
                        matchStatus = targetStatus.name,
                        finalTeamsJson = finalTeamsJson
                    )
                )
            }
        } catch (e: Exception) {
            _tossUiState.value = _tossUiState.value.copy(
                errorMessage = "Failed to save confirmed teams: ${e.message ?: "Database error"}"
            )
            return Result.failure(e)
        }

        val existingTosses = try {
            repository.getTossesForMatchSync(matchId).map { it.toHistoryRecord() }
        } catch (_: Exception) {
            emptyList()
        }
        val latestRecord = existingTosses.maxByOrNull { it.attemptNumber }

        _currentMatchId.value = matchId
        _currentMatchStatus.value = targetStatus
        _tossUiState.value = MatchTossUiState(
            matchId = matchId,
            matchStatus = targetStatus,
            selectedChoice = null,
            tossStatus = if (latestRecord != null) TossStatus.COMPLETED else TossStatus.NOT_STARTED,
            pendingResult = latestRecord?.result,
            latestTossResult = latestRecord?.toTossResult(),
            matchTossHistory = existingTosses,
            flipSequenceNumber = _tossUiState.value.flipSequenceNumber,
            errorMessage = null,
            isStandaloneMode = false
        )

        return Result.success(matchId)
    }

    /**
     * Synchronizes Toss screen state when opened directly from navigation.
     */
    suspend fun syncForTossScreen() {
        val state = _tossUiState.value
        if (state.tossStatus == TossStatus.IN_PROGRESS) return

        val activeId = _currentMatchId.value
        val activeStatus = _currentMatchStatus.value

        if (activeId != null && (activeStatus == MatchStatus.TEAMS_CONFIRMED ||
                activeStatus == MatchStatus.TOSS_COMPLETED ||
                activeStatus == MatchStatus.COMPLETED)
        ) {
            val history = try {
                repository.getTossesForMatchSync(activeId).map { it.toHistoryRecord() }
            } catch (_: Exception) {
                state.matchTossHistory
            }
            val latest = history.maxByOrNull { it.attemptNumber }
            _tossUiState.value = state.copy(
                matchId = activeId,
                matchStatus = activeStatus,
                matchTossHistory = history,
                latestTossResult = latest?.toTossResult() ?: state.latestTossResult,
                isStandaloneMode = false,
                errorMessage = null
            )
        } else {
            // Standalone toss mode when user opens Toss tab before confirming teams
            _tossUiState.value = MatchTossUiState(
                matchId = null,
                matchStatus = activeStatus,
                selectedChoice = null,
                tossStatus = TossStatus.NOT_STARTED,
                pendingResult = null,
                latestTossResult = null,
                matchTossHistory = emptyList(),
                flipSequenceNumber = state.flipSequenceNumber,
                errorMessage = null,
                isStandaloneMode = true
            )
        }
    }

    fun selectChoice(choice: TossChoice) {
        val current = _tossUiState.value
        if (current.tossStatus == TossStatus.IN_PROGRESS) return
        if (!current.isStandaloneMode && current.matchStatus == MatchStatus.COMPLETED) return
        _tossUiState.value = current.copy(
            selectedChoice = choice,
            errorMessage = null
        )
    }

    fun clearError() {
        _tossUiState.value = _tossUiState.value.copy(errorMessage = null)
    }

    /**
     * Prepares a fresh toss attempt on the same matchId while keeping all previous toss records intact.
     * Never alters teams, fairness scores, or player stats.
     */
    fun prepareFlipAgain() {
        val current = _tossUiState.value
        if (current.tossStatus == TossStatus.IN_PROGRESS) return
        if (!current.isStandaloneMode && current.matchStatus == MatchStatus.COMPLETED) return

        _tossUiState.value = current.copy(
            selectedChoice = null,
            tossStatus = TossStatus.NOT_STARTED,
            pendingResult = null,
            errorMessage = null
        )
    }

    /**
     * Executes a single fair Heads/Tails coin flip:
     * 1. Validates choice, match status, and prevents duplicate concurrent flips.
     * 2. Generates the 50/50 result ONCE before animation starts.
     * 3. Waits for animation duration so the 3D coin settles on the exact generated result.
     * 4. Persists the toss attempt (and match TOSS_COMPLETED status) to SQLite.
     * 5. If DB persistence fails, reverts state and surfaces an error — never pretending it was saved.
     */
    suspend fun executeCoinFlip(
        randomProvider: () -> Double = { Math.random() },
        animationDurationMs: Long = MatchTossEngine.FLIP_ANIMATION_DURATION_MS,
        timestampProvider: () -> Long = { System.currentTimeMillis() }
    ): Result<TossResult> {
        val current = _tossUiState.value

        val nextAttemptNumber = if (!current.isStandaloneMode && !current.matchId.isNullOrBlank()) {
            val dbMax = try {
                repository.getMaxAttemptNumberForMatch(current.matchId)
            } catch (_: Exception) {
                current.matchTossHistory.maxOfOrNull { it.attemptNumber } ?: 0
            }
            maxOf(dbMax, current.matchTossHistory.maxOfOrNull { it.attemptNumber } ?: 0) + 1
        } else {
            (current.matchTossHistory.maxOfOrNull { it.attemptNumber } ?: 0) + 1
        }

        val outcome = MatchTossEngine.prepareTossAttempt(
            matchId = current.matchId,
            userChoice = current.selectedChoice,
            currentMatchStatus = current.matchStatus,
            currentTossStatus = current.tossStatus,
            nextAttemptNumber = nextAttemptNumber,
            isStandalone = current.isStandaloneMode,
            randomProvider = randomProvider,
            timestampProvider = timestampProvider
        )

        if (outcome is TossAttemptOutcome.Rejected) {
            _tossUiState.value = current.copy(errorMessage = outcome.reason)
            return Result.failure(IllegalStateException(outcome.reason))
        }

        val pendingToss = (outcome as TossAttemptOutcome.Ready).tossResult

        // Lock UI and publish pre-generated target result for the 3D animation
        _tossUiState.value = current.copy(
            tossStatus = TossStatus.IN_PROGRESS,
            pendingResult = pendingToss.result,
            flipSequenceNumber = current.flipSequenceNumber + 1,
            errorMessage = null
        )

        return try {
            if (animationDurationMs > 0L) {
                delay(animationDurationMs)
            }

            val completedToss = pendingToss.copy(status = TossStatus.COMPLETED)
            val entity = TossEntity(
                timestamp = completedToss.createdAt,
                choice = completedToss.userChoice.displayName,
                result = completedToss.result.displayName,
                hasWon = completedToss.wonToss,
                tossUuid = completedToss.id,
                matchId = if (current.isStandaloneMode) "" else completedToss.matchId,
                attemptNumber = completedToss.attemptNumber,
                selectedBy = completedToss.selectedBy,
                status = TossStatus.COMPLETED.name
            )

            // Persist toss to SQLite — must succeed before displaying as saved
            val rowId = repository.insertToss(entity)
            if (rowId <= 0L) {
                throw IllegalStateException("Database rejected toss record insert.")
            }

            // Update match session status to TOSS_COMPLETED (non-standalone)
            val updatedMatchStatus = if (!current.isStandaloneMode && !current.matchId.isNullOrBlank()) {
                val existingSession = repository.getSessionByMatchId(current.matchId)
                    ?: throw IllegalStateException("Match session not found for ID ${current.matchId}")
                repository.updateSession(
                    existingSession.copy(
                        matchStatus = MatchStatus.TOSS_COMPLETED.name,
                        latestTossChoice = completedToss.userChoice.name,
                        latestTossResult = completedToss.result.name,
                        latestTossWon = completedToss.wonToss
                    )
                )
                MatchStatus.TOSS_COMPLETED
            } else {
                current.matchStatus
            }

            val updatedHistory = if (!current.isStandaloneMode && !current.matchId.isNullOrBlank()) {
                repository.getTossesForMatchSync(current.matchId).map { it.toHistoryRecord() }
            } else {
                current.matchTossHistory + TossHistoryRecord(
                    id = completedToss.id,
                    matchId = completedToss.matchId,
                    attemptNumber = completedToss.attemptNumber,
                    userChoice = completedToss.userChoice,
                    result = completedToss.result,
                    wonToss = completedToss.wonToss,
                    createdAt = completedToss.createdAt
                )
            }

            if (!current.isStandaloneMode) {
                _currentMatchStatus.value = updatedMatchStatus
            }

            _tossUiState.value = _tossUiState.value.copy(
                matchStatus = updatedMatchStatus,
                tossStatus = TossStatus.COMPLETED,
                pendingResult = completedToss.result,
                latestTossResult = completedToss,
                matchTossHistory = updatedHistory,
                errorMessage = null
            )

            Result.success(completedToss)
        } catch (ce: CancellationException) {
            // Animation or coroutine interrupted (navigation / lifecycle): reset cleanly without partial save
            _tossUiState.value = _tossUiState.value.copy(
                tossStatus = TossStatus.NOT_STARTED,
                pendingResult = null
            )
            throw ce
        } catch (e: Exception) {
            // Database write failure: NEVER display toss as saved
            _tossUiState.value = _tossUiState.value.copy(
                tossStatus = TossStatus.NOT_STARTED,
                pendingResult = null,
                errorMessage = "Could not save toss result. Please try again. (${e.message ?: "Storage error"})"
            )
            Result.failure(e)
        }
    }

    /**
     * Finalizes the match after at least one toss has been saved (TOSS_COMPLETED -> COMPLETED).
     * Never regenerates teams or alters fairness/stats.
     */
    suspend fun finalizeMatch(
        timestampProvider: () -> Long = { System.currentTimeMillis() }
    ): Result<MatchSummaryData> {
        val current = _tossUiState.value
        val matchId = current.matchId

        if (current.tossStatus == TossStatus.IN_PROGRESS) {
            val msg = "Wait for the coin flip to finish before continuing."
            _tossUiState.value = current.copy(errorMessage = msg)
            return Result.failure(IllegalStateException(msg))
        }
        if (matchId.isNullOrBlank()) {
            val msg = "Missing match ID. Cannot finalize match."
            _tossUiState.value = current.copy(errorMessage = msg)
            return Result.failure(IllegalStateException(msg))
        }
        if (!MatchTossEngine.canFinalizeMatch(_currentMatchStatus.value)) {
            val msg = "Complete the coin toss before finalizing this match."
            _tossUiState.value = current.copy(errorMessage = msg)
            return Result.failure(IllegalStateException(msg))
        }

        val completedAt = timestampProvider()

        return try {
            val session = repository.getSessionByMatchId(matchId)
                ?: throw IllegalStateException("Match record not found in database.")

            val updatedSession = session.copy(
                matchStatus = MatchStatus.COMPLETED.name,
                completedAt = completedAt
            )
            repository.updateSession(updatedSession)

            _currentMatchStatus.value = MatchStatus.COMPLETED
            _tossUiState.value = current.copy(
                matchStatus = MatchStatus.COMPLETED,
                errorMessage = null
            )

            val summary = buildSummaryFromSession(updatedSession)
            _reopenedMatchSummary.value = summary
            Result.success(summary)
        } catch (e: Exception) {
            _tossUiState.value = current.copy(
                errorMessage = "Failed to finalize match: ${e.message ?: "Database error"}"
            )
            Result.failure(e)
        }
    }

    /**
     * Loads any saved match (completed or legacy) in read-only mode.
     * NEVER creates a new toss or alters statistics.
     */
    suspend fun loadMatchSummary(
        matchId: String,
        fallbackTeams: List<GeneratedTeam> = emptyList(),
        fallbackJoker: String? = null
    ): MatchSummaryData? {
        if (matchId.isBlank()) {
            _reopenedMatchSummary.value = null
            return null
        }

        val session = repository.getSessionByMatchId(matchId)
        if (session == null) {
            _reopenedMatchSummary.value = null
            return null
        }

        val summary = buildSummaryFromSession(
            session = session,
            fallbackTeams = fallbackTeams,
            fallbackJoker = fallbackJoker
        )
        _reopenedMatchSummary.value = summary
        return summary
    }

    fun resetForNewMatch() {
        _currentMatchId.value = null
        _currentMatchStatus.value = MatchStatus.DRAFT
        _tossUiState.value = MatchTossUiState()
        _reopenedMatchSummary.value = null
    }

    private suspend fun buildSummaryFromSession(
        session: SessionEntity,
        fallbackTeams: List<GeneratedTeam> = emptyList(),
        fallbackJoker: String? = null
    ): MatchSummaryData {
        val (parsedTeams, parsedJoker) = MatchTossEngine.parseTeamsJson(session.finalTeamsJson)
        val resolvedTeams = when {
            parsedTeams.isNotEmpty() -> parsedTeams
            fallbackTeams.isNotEmpty() -> fallbackTeams
            else -> reconstructLegacyTeams(session.playerIdsJson, session.teamCount)
        }
        val resolvedJoker = parsedJoker ?: fallbackJoker

        val tossHistory = try {
            repository.getTossesForMatchSync(session.matchId).map { it.toHistoryRecord() }
        } catch (_: Exception) {
            emptyList()
        }

        return MatchSummaryData(
            matchId = session.matchId,
            sessionDbId = session.id,
            matchStatus = MatchStatus.fromString(session.matchStatus),
            teams = resolvedTeams,
            joker = resolvedJoker,
            fairnessScore = session.overallFairnessScore.toInt(),
            fairnessRating = session.fairnessRating,
            fairnessProfile = session.fairnessProfile,
            qualityGateOutcome = session.qualityGateOutcome,
            wasManuallyEdited = session.wasManuallyEdited,
            createdAt = session.timestamp,
            completedAt = session.completedAt,
            tossHistory = tossHistory
        )
    }

    private fun reconstructLegacyTeams(playerIdsJson: String, teamCount: Int): List<GeneratedTeam> {
        val safeTeamCount = teamCount.coerceAtLeast(1)
        return try {
            val arr = JSONArray(playerIdsJson)
            val players = (0 until arr.length()).map { arr.getString(it) }
            if (players.isEmpty()) return emptyList()
            val buckets = List(safeTeamCount) { mutableListOf<String>() }
            players.forEachIndexed { index, name ->
                buckets[index % safeTeamCount].add(name)
            }
            buckets.mapIndexed { idx, members ->
                GeneratedTeam(
                    teamNumber = idx + 1,
                    name = "Team ${'A' + idx}",
                    players = members,
                    playerIds = members
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun TossEntity.toHistoryRecord(): TossHistoryRecord {
        return TossHistoryRecord(
            id = tossUuid.ifBlank { "toss_$id" },
            matchId = matchId,
            attemptNumber = attemptNumber.coerceAtLeast(1),
            userChoice = TossChoice.fromString(choice) ?: TossChoice.HEADS,
            result = TossChoice.fromString(result) ?: TossChoice.HEADS,
            wonToss = hasWon,
            createdAt = timestamp
        )
    }

    private fun TossHistoryRecord.toTossResult(): TossResult {
        return TossResult(
            id = id,
            matchId = matchId,
            selectedBy = "USER",
            userChoice = userChoice,
            result = result,
            wonToss = wonToss,
            createdAt = createdAt,
            status = TossStatus.COMPLETED,
            attemptNumber = attemptNumber
        )
    }
}
