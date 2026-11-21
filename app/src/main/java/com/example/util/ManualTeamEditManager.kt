package com.example.util

import kotlin.math.roundToInt

/**
 * Pure Kotlin state manager for Manual Team Editing Mode.
 * Encapsulates player moves, atomic swaps, undo/redo history, reset changes,
 * read-only fairness recalculation, duplicate detection, and hard-constraint validation.
 */
class ManualTeamEditManager(
    initialArrangement: TeamArrangement,
    private val baseConstraints: MatchConstraints,
    private val baselineTeammatePairCounts: Map<String, Int>,
    private val baselineOpponentPairCounts: Map<String, Int>,
    private val baselinePreviousSignatures: Set<String>,
    private val baselineCycleJokers: List<String>,
    private val playerJokerCounts: Map<String, Int> = emptyMap(),
    private val maxHistorySize: Int = MAX_HISTORY_SIZE
) {
    companion object {
        const val MAX_HISTORY_SIZE = 50
    }

    private val playerMap: Map<String, Player> = baseConstraints.allPlayers.associateBy { it.id }
    private val eligiblePlayerNames: List<String> = baseConstraints.allPlayers
        .filter { baseConstraints.playerConstraints[it.id]?.isExcluded != true }
        .map { it.name }

    private val originalArrangement: TeamArrangement = normalizeArrangement(initialArrangement)
    private var currentArrangement: TeamArrangement = originalArrangement

    private var allowTemporaryImbalance: Boolean = baseConstraints.allowTemporaryImbalance
    private var selectedPlayerId: String? = null
    private var lastOperationError: String? = null
    private var lastOperationInfo: String? = null

    private val undoStack = ArrayDeque<TeamArrangement>()
    private val redoStack = ArrayDeque<TeamArrangement>()

    private val originalFairness: ManualEditFairnessSnapshot = evaluateReadOnly(originalArrangement, null)
    private var currentFairness: ManualEditFairnessSnapshot = originalFairness

    fun getState(): ManualEditState {
        val activeConstraints = baseConstraints.copy(allowTemporaryImbalance = allowTemporaryImbalance)
        val validation = ManualArrangementValidator.validateManualArrangement(
            arrangement = currentArrangement,
            constraints = activeConstraints,
            allowTemporaryImbalance = allowTemporaryImbalance
        )
        val combinedWarnings = (validation.warnings + currentFairness.fairnessWarnings).distinct()
        return ManualEditState(
            isEditing = true,
            originalArrangement = originalArrangement,
            currentArrangement = currentArrangement,
            originalFairness = originalFairness,
            currentFairness = currentFairness,
            validationResult = validation.copy(warnings = combinedWarnings),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            undoCount = undoStack.size,
            redoCount = redoStack.size,
            allowTemporaryImbalance = allowTemporaryImbalance,
            hasUnsavedChanges = currentArrangement.signature != originalArrangement.signature ||
                currentArrangement.teams != originalArrangement.teams,
            selectedPlayerId = selectedPlayerId,
            lastOperationError = lastOperationError,
            lastOperationInfo = lastOperationInfo,
            isDuplicateArrangement = currentFairness.isDuplicateArrangement,
            playerMap = playerMap,
            constraints = baseConstraints.playerConstraints,
            fixedGroups = baseConstraints.fixedGroups
        )
    }

    fun selectPlayer(playerId: String?) {
        selectedPlayerId = playerId
        lastOperationError = null
    }

    fun clearLastError() {
        lastOperationError = null
    }

    fun setAllowTemporaryImbalance(allow: Boolean) {
        allowTemporaryImbalance = allow
        lastOperationError = null
    }

    /**
     * Moves a player by ID from their current team to [destinationTeamId].
     * Enforces locks, fixed groups, exclusion, duplicate prevention, and team size rules.
     */
    fun movePlayer(playerId: String, destinationTeamId: String): ManualEditOperationResult {
        val player = playerMap[playerId]
            ?: return reject("Player not found (ID: $playerId).")

        if (baseConstraints.playerConstraints[playerId]?.isExcluded == true) {
            return reject("Cannot add excluded player '${player.name}' to a team.")
        }

        if (currentArrangement.jokerId == playerId ||
            (!currentArrangement.joker.isNullOrBlank() && currentArrangement.joker.equals(player.name, ignoreCase = true))
        ) {
            return reject("Cannot move Joker '${player.name}' into a team.")
        }

        val destIndex = currentArrangement.teams.indexOfFirst {
            ArrangementSignatureUtil.matchesTeamId(it, destinationTeamId)
        }
        if (destIndex == -1) {
            return reject("Destination team '$destinationTeamId' does not exist.")
        }

        val sourceIndex = currentArrangement.teams.indexOfFirst { playerId in it.playerIds }
        if (sourceIndex == -1) {
            return reject("Player '${player.name}' is not currently assigned to any team.")
        }

        if (sourceIndex == destIndex) {
            return reject("'${player.name}' is already on ${currentArrangement.teams[destIndex].name}.")
        }

        // Check team lock constraint
        val constraint = baseConstraints.playerConstraints[playerId]
        if (constraint?.isLocked == true && constraint.lockedTeamIndex != null && constraint.lockedTeamIndex != destIndex) {
            val teamLabel = constraint.getLockedTeamLabel() ?: "Team ${constraint.lockedTeamIndex + 1}"
            return reject("Cannot move '${player.name}': locked to $teamLabel.")
        }

        // Check fixed group constraint
        val group = baseConstraints.fixedGroups.firstOrNull { playerId in it.playerIds }
        if (group != null) {
            return reject("Cannot move '${player.name}' individually: belongs to fixed group '${group.name}'. Players in a fixed group must remain together.")
        }

        // Prevent duplicate assignment in destination
        if (playerId in currentArrangement.teams[destIndex].playerIds) {
            return reject("'${player.name}' is already assigned to ${currentArrangement.teams[destIndex].name}.")
        }

        // Build candidate updated teams
        val updatedTeams = currentArrangement.teams.mapIndexed { idx, team ->
            val newIds = when (idx) {
                sourceIndex -> team.playerIds.filter { it != playerId }
                destIndex -> team.playerIds + playerId
                else -> team.playerIds
            }
            rebuildTeam(team.teamNumber, team.name, newIds)
        }

        val candidateArrangement = currentArrangement.copy(
            teams = updatedTeams,
            signature = ArrangementSignatureUtil.generateSignature(updatedTeams, currentArrangement.joker)
        )

        val validation = ManualArrangementValidator.validateManualArrangement(
            arrangement = candidateArrangement,
            constraints = baseConstraints.copy(allowTemporaryImbalance = allowTemporaryImbalance),
            allowTemporaryImbalance = allowTemporaryImbalance
        )

        if (!validation.valid) {
            val errorMsg = validation.errors.firstOrNull() ?: "Move creates an invalid team arrangement."
            val hint = if (!allowTemporaryImbalance && errorMsg.contains("unbalanced", ignoreCase = true)) {
                " Use Swap Players to preserve team sizes, or enable 'Allow Temporary Imbalance'."
            } else ""
            return reject(errorMsg + hint, validation)
        }

        commitEdit(
            newArrangement = candidateArrangement,
            infoMessage = "Moved ${player.name} to ${updatedTeams[destIndex].name}."
        )
        return ManualEditOperationResult(success = true, message = lastOperationInfo, validationResult = validation)
    }

    /**
     * Atomically swaps two players between their respective teams.
     */
    fun swapPlayers(playerIdA: String, playerIdB: String): ManualEditOperationResult {
        if (playerIdA == playerIdB) {
            return reject("Select two different players to swap.")
        }

        val playerA = playerMap[playerIdA]
            ?: return reject("Player not found (ID: $playerIdA).")
        val playerB = playerMap[playerIdB]
            ?: return reject("Player not found (ID: $playerIdB).")

        if (baseConstraints.playerConstraints[playerIdA]?.isExcluded == true) {
            return reject("Excluded player '${playerA.name}' cannot be swapped into a team.")
        }
        if (baseConstraints.playerConstraints[playerIdB]?.isExcluded == true) {
            return reject("Excluded player '${playerB.name}' cannot be swapped into a team.")
        }

        val teamIndexA = currentArrangement.teams.indexOfFirst { playerIdA in it.playerIds }
        val teamIndexB = currentArrangement.teams.indexOfFirst { playerIdB in it.playerIds }

        if (teamIndexA == -1) {
            return reject("'${playerA.name}' is not assigned to a team and cannot be swapped.")
        }
        if (teamIndexB == -1) {
            return reject("'${playerB.name}' is not assigned to a team and cannot be swapped.")
        }
        if (teamIndexA == teamIndexB) {
            return reject("'${playerA.name}' and '${playerB.name}' are already on the same team.")
        }

        // Check team locks
        val lockA = baseConstraints.playerConstraints[playerIdA]
        if (lockA?.isLocked == true && lockA.lockedTeamIndex != null) {
            val label = lockA.getLockedTeamLabel() ?: "Team ${lockA.lockedTeamIndex + 1}"
            return reject("Cannot swap '${playerA.name}': locked to $label.")
        }
        val lockB = baseConstraints.playerConstraints[playerIdB]
        if (lockB?.isLocked == true && lockB.lockedTeamIndex != null) {
            val label = lockB.getLockedTeamLabel() ?: "Team ${lockB.lockedTeamIndex + 1}"
            return reject("Cannot swap '${playerB.name}': locked to $label.")
        }

        // Check fixed groups
        val groupA = baseConstraints.fixedGroups.firstOrNull { playerIdA in it.playerIds }
        if (groupA != null) {
            return reject("Cannot swap '${playerA.name}': belongs to fixed group '${groupA.name}'. Players in a fixed group must remain together.")
        }
        val groupB = baseConstraints.fixedGroups.firstOrNull { playerIdB in it.playerIds }
        if (groupB != null) {
            return reject("Cannot swap '${playerB.name}': belongs to fixed group '${groupB.name}'. Players in a fixed group must remain together.")
        }

        // Atomic swap
        val updatedTeams = currentArrangement.teams.mapIndexed { idx, team ->
            val newIds = when (idx) {
                teamIndexA -> team.playerIds.map { if (it == playerIdA) playerIdB else it }
                teamIndexB -> team.playerIds.map { if (it == playerIdB) playerIdA else it }
                else -> team.playerIds
            }
            rebuildTeam(team.teamNumber, team.name, newIds)
        }

        val candidateArrangement = currentArrangement.copy(
            teams = updatedTeams,
            signature = ArrangementSignatureUtil.generateSignature(updatedTeams, currentArrangement.joker)
        )

        val validation = ManualArrangementValidator.validateManualArrangement(
            arrangement = candidateArrangement,
            constraints = baseConstraints.copy(allowTemporaryImbalance = allowTemporaryImbalance),
            allowTemporaryImbalance = allowTemporaryImbalance
        )

        if (!validation.valid) {
            return reject(validation.errors.firstOrNull() ?: "Swap creates an invalid team arrangement.", validation)
        }

        commitEdit(
            newArrangement = candidateArrangement,
            infoMessage = "Swapped ${playerA.name} ↔ ${playerB.name}."
        )
        return ManualEditOperationResult(success = true, message = lastOperationInfo, validationResult = validation)
    }

    fun undo(): ManualEditOperationResult {
        if (undoStack.isEmpty()) {
            return ManualEditOperationResult(success = false, message = "Nothing to undo.")
        }
        val previous = undoStack.removeLast()
        redoStack.addLast(currentArrangement)
        currentArrangement = previous
        currentFairness = evaluateReadOnly(currentArrangement, originalFairness.evaluation)
        selectedPlayerId = null
        lastOperationError = null
        lastOperationInfo = "Undid last change."
        return ManualEditOperationResult(success = true, message = lastOperationInfo)
    }

    fun redo(): ManualEditOperationResult {
        if (redoStack.isEmpty()) {
            return ManualEditOperationResult(success = false, message = "Nothing to redo.")
        }
        val next = redoStack.removeLast()
        undoStack.addLast(currentArrangement)
        currentArrangement = next
        currentFairness = evaluateReadOnly(currentArrangement, originalFairness.evaluation)
        selectedPlayerId = null
        lastOperationError = null
        lastOperationInfo = "Redid change."
        return ManualEditOperationResult(success = true, message = lastOperationInfo)
    }

    fun resetChanges(): ManualEditOperationResult {
        currentArrangement = originalArrangement
        undoStack.clear()
        redoStack.clear()
        currentFairness = originalFairness
        selectedPlayerId = null
        lastOperationError = null
        lastOperationInfo = "Restored original generated arrangement."
        return ManualEditOperationResult(success = true, message = lastOperationInfo)
    }

    /**
     * Runs strict confirmation validation (always enforcing balanced team sizes
     * even if temporary imbalance was toggled on during editing).
     */
    fun validateForConfirmation(): ArrangementValidationResult {
        val strictValidation = ManualArrangementValidator.validateManualArrangement(
            arrangement = currentArrangement,
            constraints = baseConstraints.copy(allowTemporaryImbalance = false),
            allowTemporaryImbalance = false
        )
        val combinedWarnings = (strictValidation.warnings + currentFairness.fairnessWarnings).distinct()
        return strictValidation.copy(warnings = combinedWarnings)
    }

    /**
     * Produces the confirmed result payload if hard-constraint validation passes;
     * otherwise records the error and returns null.
     */
    fun buildConfirmedResult(): ConfirmedManualArrangement? {
        val validation = validateForConfirmation()
        if (!validation.valid) {
            lastOperationError = validation.errors.firstOrNull() ?: "Cannot confirm invalid arrangement."
            return null
        }
        val freshFairness = evaluateReadOnly(currentArrangement, originalFairness.evaluation)
        currentFairness = freshFairness
        val wasModified = currentArrangement.signature != originalArrangement.signature ||
            currentArrangement.teams != originalArrangement.teams
        return ConfirmedManualArrangement(
            finalArrangement = currentArrangement,
            originalArrangement = originalArrangement,
            finalFairness = freshFairness,
            validationResult = validation,
            wasModified = wasModified,
            isDuplicateArrangement = freshFairness.isDuplicateArrangement
        )
    }

    private fun commitEdit(newArrangement: TeamArrangement, infoMessage: String) {
        undoStack.addLast(currentArrangement)
        while (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        currentArrangement = newArrangement
        currentFairness = evaluateReadOnly(currentArrangement, originalFairness.evaluation)
        selectedPlayerId = null
        lastOperationError = null
        lastOperationInfo = infoMessage
    }

    private fun reject(
        message: String,
        validation: ArrangementValidationResult? = null
    ): ManualEditOperationResult {
        lastOperationError = message
        return ManualEditOperationResult(success = false, message = message, validationResult = validation)
    }

    private fun normalizeArrangement(arrangement: TeamArrangement): TeamArrangement {
        val rebuiltTeams = arrangement.teams.map { team ->
            val ids = if (team.playerIds.isNotEmpty()) {
                team.playerIds
            } else {
                team.players.mapNotNull { name ->
                    baseConstraints.allPlayers.firstOrNull { it.name == name }?.id
                }
            }
            rebuildTeam(team.teamNumber, team.name, ids)
        }
        val resolvedJokerId = arrangement.jokerId ?: arrangement.joker?.let { jName ->
            baseConstraints.allPlayers.firstOrNull { it.name.equals(jName.trim(), ignoreCase = true) }?.id
        }
        return arrangement.copy(
            teams = rebuiltTeams,
            jokerId = resolvedJokerId,
            signature = ArrangementSignatureUtil.generateSignature(rebuiltTeams, arrangement.joker)
        )
    }

    private fun rebuildTeam(teamNumber: Int, teamName: String, playerIds: List<String>): GeneratedTeam {
        val players = playerIds.mapNotNull { playerMap[it] }
        val strAnalysis = TeamStrengthEngine.calculateTeamStrength(players)
        return GeneratedTeam(
            teamNumber = teamNumber,
            name = teamName,
            players = players.map { it.name },
            playerIds = players.map { it.id },
            totalStrength = strAnalysis.totalStrength,
            averageStrength = strAnalysis.averageStrength,
            maxPlayerRating = strAnalysis.maxPlayerRating,
            minPlayerRating = strAnalysis.minPlayerRating
        )
    }

    /**
     * Read-only fairness recalculation reusing existing Missing XI engines.
     * Never mutates baseline history maps.
     */
    private fun evaluateReadOnly(
        arrangement: TeamArrangement,
        originalEval: FairnessEvaluation?
    ): ManualEditFairnessSnapshot {
        val teamPlayerIds = arrangement.teams.map { it.playerIds }
        val strengthAnalysis = TeamStrengthEngine.evaluateCandidateStrength(arrangement.teams)
        val pairAnalysis = TeammatePairTracker.analyzeCandidatePairs(
            teamsPlayerIds = teamPlayerIds,
            teammatePairCounts = baselineTeammatePairCounts
        )
        val opponentAnalysis = OpponentPairTracker.analyzeOpponentPairs(
            teamsPlayerIds = teamPlayerIds,
            opponentPairCounts = baselineOpponentPairCounts
        )
        val evaluation = FairnessRankingEngine.evaluateCandidateFairness(
            pairAnalysis = pairAnalysis,
            opponentAnalysis = opponentAnalysis,
            strengthAnalysis = strengthAnalysis,
            joker = arrangement.joker,
            activePlayers = eligiblePlayerNames,
            cycleJokers = baselineCycleJokers,
            playerJokerCounts = playerJokerCounts
        )
        val fScore = evaluation.overallScore.roundToInt()
        val fRating = FairnessRankingEngine.getFairnessRating(evaluation.overallScore)
        val qualityResult = FairnessQualityResult.evaluate(evaluation, baseConstraints.fairnessSettings)
        val sig = ArrangementSignatureUtil.generateSignature(arrangement.teams, arrangement.joker)
        val isDuplicate = sig in baselinePreviousSignatures

        val warnings = ManualArrangementValidator.evaluateFairnessWarnings(
            original = originalEval ?: evaluation,
            current = evaluation,
            settings = baseConstraints.fairnessSettings,
            qualityResult = qualityResult,
            isDuplicate = isDuplicate
        )

        return ManualEditFairnessSnapshot(
            pairAnalysis = pairAnalysis,
            opponentAnalysis = opponentAnalysis,
            strengthAnalysis = strengthAnalysis,
            evaluation = evaluation,
            qualityResult = qualityResult,
            fairnessScore = fScore,
            fairnessRating = fRating,
            isDuplicateArrangement = isDuplicate,
            fairnessWarnings = warnings
        )
    }
}
