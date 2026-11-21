package com.example.util

import kotlin.math.roundToInt

data class TeamArrangement(
    val teams: List<GeneratedTeam>,
    val jokerPlayerId: String? = null,
    val jokerPlayerName: String? = null,
    val excludedPlayers: List<Player> = emptyList()
)

data class MatchConstraints(
    val allPlayers: List<Player>,
    val constraints: Map<String, PlayerMatchConstraint> = emptyMap(),
    val fixedGroups: List<LockedPlayerGroup> = emptyList(),
    val allowTemporarySizeImbalance: Boolean = false,
    val requireJokerForRemainder: Boolean = true
)

data class ArrangementValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ManualFairnessSnapshot(
    val pairAnalysis: CandidatePairAnalysis = CandidatePairAnalysis(),
    val opponentAnalysis: OpponentPairAnalysis = OpponentPairAnalysis(),
    val strengthAnalysis: CandidateStrengthAnalysis = CandidateStrengthAnalysis(),
    val fairnessEvaluation: FairnessEvaluation = FairnessEvaluation(),
    val fairnessScore: Int = 0,
    val fairnessRating: String = "",
    val qualityResult: FairnessQualityResult = FairnessQualityResult(),
    val signature: String = "",
    val isDuplicate: Boolean = false
)

data class ManualEditContext(
    val matchConstraints: MatchConstraints,
    val historicalTeammatePairCounts: Map<String, Int>,
    val historicalOpponentPairCounts: Map<String, Int>,
    val cycleJokers: List<String>,
    val effectiveSettings: FairnessSettings,
    val existingSignatures: Set<String>
)

data class ManualEditSession(
    val originalArrangement: TeamArrangement,
    val currentArrangement: TeamArrangement,
    val undoStack: List<TeamArrangement> = emptyList(),
    val redoStack: List<TeamArrangement> = emptyList(),
    val allowTemporarySizeImbalance: Boolean = false,
    val originalFairness: ManualFairnessSnapshot = ManualFairnessSnapshot(),
    val currentFairness: ManualFairnessSnapshot = ManualFairnessSnapshot(),
    val validationResult: ArrangementValidationResult = ArrangementValidationResult(valid = true),
    val strictConfirmationValidation: ArrangementValidationResult = ArrangementValidationResult(valid = true),
    val lastOperationError: String? = null
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val hasChanges: Boolean get() = currentArrangement != originalArrangement
    val canConfirm: Boolean get() = strictConfirmationValidation.valid
}

data class ManualEditActionResult(
    val session: ManualEditSession,
    val succeeded: Boolean,
    val errorMessage: String? = null
)

object ManualTeamEditor {

    const val MAX_HISTORY_LIMIT = 50

    /**
     * Canonical arrangement signature matching the existing duplicate-detection system.
     */
    fun generateArrangementSignature(teams: List<GeneratedTeam>, joker: String?): String {
        val sortedTeams = teams.map { team ->
            team.players.map { it.trim() }.sorted().joinToString(",")
        }.sorted()
        val teamsStr = sortedTeams.joinToString(" | ")
        val jokerStr = if (!joker.isNullOrBlank()) " [JOKER: ${joker.trim()}]" else ""
        return "$teamsStr$jokerStr"
    }

    /**
     * Rebuilds GeneratedTeam objects from player IDs while preserving each player's skill rating
     * and recalculating strength statistics via the existing TeamStrengthEngine.
     */
    fun rebuildTeamsWithRatings(
        teams: List<GeneratedTeam>,
        allPlayers: List<Player>
    ): List<GeneratedTeam> {
        val playerById = allPlayers.associateBy { it.id }
        val playerByName = allPlayers.associateBy { it.name }
        return teams.mapIndexed { index, team ->
            val resolvedIds = if (team.playerIds.isNotEmpty()) {
                team.playerIds
            } else {
                team.players.map { name -> playerByName[name]?.id ?: name }
            }
            val teamPlayers = resolvedIds.map { id ->
                playerById[id] ?: Player(id = id, name = id, skillRating = 5)
            }
            val str = TeamStrengthEngine.calculateTeamStrength(teamPlayers)
            team.copy(
                teamNumber = if (team.teamNumber > 0) team.teamNumber else index + 1,
                players = teamPlayers.map { it.name },
                playerIds = teamPlayers.map { it.id },
                totalStrength = str.totalStrength,
                averageStrength = str.averageStrength,
                maxPlayerRating = str.maxPlayerRating,
                minPlayerRating = str.minPlayerRating
            )
        }
    }

    /**
     * Read-only fairness evaluation reusing the existing Missing XI fairness engines.
     * Never mutates historical pair counts, signatures, or persistent state.
     */
    fun evaluateArrangementFairness(
        arrangement: TeamArrangement,
        context: ManualEditContext
    ): ManualFairnessSnapshot {
        val rebuiltTeams = rebuildTeamsWithRatings(arrangement.teams, context.matchConstraints.allPlayers)
        val teamsPlayerIds = rebuiltTeams.map { it.playerIds }

        val strengthAnalysis = TeamStrengthEngine.evaluateCandidateStrength(rebuiltTeams)
        val pairAnalysis = TeammatePairTracker.analyzeCandidatePairs(
            teamsPlayerIds = teamsPlayerIds,
            teammatePairCounts = context.historicalTeammatePairCounts
        )
        val opponentAnalysis = OpponentPairTracker.analyzeOpponentPairs(
            teamsPlayerIds = teamsPlayerIds,
            opponentPairCounts = context.historicalOpponentPairCounts
        )

        val eligiblePlayerNames = context.matchConstraints.allPlayers
            .filter { context.matchConstraints.constraints[it.id]?.isExcluded != true }
            .map { it.name }

        val evaluation = FairnessRankingEngine.evaluateCandidateFairness(
            pairAnalysis = pairAnalysis,
            opponentAnalysis = opponentAnalysis,
            strengthAnalysis = strengthAnalysis,
            joker = arrangement.jokerPlayerName,
            activePlayers = eligiblePlayerNames,
            cycleJokers = context.cycleJokers
        )

        val score = evaluation.overallScore.roundToInt()
        val rating = FairnessRankingEngine.getFairnessRating(evaluation.overallScore)
        val quality = FairnessQualityResult.evaluate(evaluation, context.effectiveSettings)
        val signature = generateArrangementSignature(rebuiltTeams, arrangement.jokerPlayerName)
        val isDuplicate = context.existingSignatures.contains(signature)

        return ManualFairnessSnapshot(
            pairAnalysis = pairAnalysis,
            opponentAnalysis = opponentAnalysis,
            strengthAnalysis = strengthAnalysis,
            fairnessEvaluation = evaluation,
            fairnessScore = score,
            fairnessRating = rating,
            qualityResult = quality,
            signature = signature,
            isDuplicate = isDuplicate
        )
    }

    /**
     * Centralized validation function separating hard constraint errors from non-blocking fairness warnings.
     */
    fun validateManualArrangement(
        arrangement: TeamArrangement,
        constraints: MatchConstraints,
        currentFairness: ManualFairnessSnapshot? = null,
        originalFairness: ManualFairnessSnapshot? = null,
        settings: FairnessSettings? = null,
        existingSignatures: Set<String> = emptySet()
    ): ArrangementValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val teams = arrangement.teams
        val numTeams = teams.size
        if (numTeams < 2) {
            errors.add("At least 2 teams are required.")
        }

        // Verify all team IDs exist and are unique
        val teamIds = teams.map { it.teamId }
        if (teamIds.any { it.isBlank() } || teamIds.distinct().size != teamIds.size) {
            errors.add("All teams must have valid, unique team IDs.")
        }

        val allPlayersById = constraints.allPlayers.associateBy { it.id }
        val excludedIds = constraints.constraints
            .filterValues { it.isExcluded }
            .keys
        val eligiblePlayers = constraints.allPlayers.filter { it.id !in excludedIds }
        val eligibleIds = eligiblePlayers.map { it.id }.toSet()

        // Track assignments across teams
        val seenAcrossTeams = mutableMapOf<String, MutableList<String>>() // playerId -> list of team names

        for (team in teams) {
            val seenInTeam = mutableSetOf<String>()
            for (playerId in team.playerIds) {
                if (!allPlayersById.containsKey(playerId)) {
                    errors.add("Unknown player ID '$playerId' found in ${team.name}.")
                }
                if (playerId in excludedIds) {
                    val name = allPlayersById[playerId]?.name ?: playerId
                    errors.add("Excluded player '$name' cannot be assigned to ${team.name}.")
                }
                if (!seenInTeam.add(playerId)) {
                    val name = allPlayersById[playerId]?.name ?: playerId
                    errors.add("Duplicate player '$name' found inside ${team.name}.")
                }
                seenAcrossTeams.getOrPut(playerId) { mutableListOf() }.add(team.name)
            }
        }

        // Check players appearing in multiple teams
        for ((playerId, assignedTeamNames) in seenAcrossTeams) {
            val distinctTeams = assignedTeamNames.distinct()
            if (distinctTeams.size > 1) {
                val name = allPlayersById[playerId]?.name ?: playerId
                errors.add("Player '$name' is assigned to multiple teams (${distinctTeams.joinToString(", ")}).")
            }
        }

        // Joker validation
        val jokerId = arrangement.jokerPlayerId
        if (jokerId != null) {
            if (!allPlayersById.containsKey(jokerId)) {
                errors.add("Unknown Joker player ID '$jokerId'.")
            }
            if (jokerId in excludedIds) {
                val name = allPlayersById[jokerId]?.name ?: jokerId
                errors.add("Excluded player '$name' cannot be assigned as Joker.")
            }
            if (seenAcrossTeams.containsKey(jokerId)) {
                val name = allPlayersById[jokerId]?.name ?: jokerId
                errors.add("Player '$name' cannot be both a team member and Joker.")
            }
            val jokerConstraint = constraints.constraints[jokerId]
            if (jokerConstraint?.isLocked == true && jokerConstraint.lockedTeamIndex != null) {
                val name = allPlayersById[jokerId]?.name ?: jokerId
                errors.add("Locked player '$name' cannot be assigned as Joker.")
            }
            val jokerGroup = constraints.fixedGroups.firstOrNull { jokerId in it.playerIds }
            if (jokerGroup != null) {
                val name = allPlayersById[jokerId]?.name ?: jokerId
                errors.add("Player '$name' in fixed group '${jokerGroup.name}' cannot be assigned as Joker.")
            }
        }

        val needsJoker = numTeams >= 2 && constraints.requireJokerForRemainder && (eligiblePlayers.size % numTeams != 0)
        if (needsJoker && jokerId == null) {
            errors.add("An odd player distribution requires 1 Joker assignment.")
        } else if (!needsJoker && constraints.requireJokerForRemainder && jokerId != null) {
            errors.add("Even player distribution should not have a Joker assigned.")
        }

        // Verify every eligible player appears exactly once
        val assignedEligibleIds = seenAcrossTeams.keys + (jokerId?.let { setOf(it) } ?: emptySet())
        val missingEligible = eligibleIds - assignedEligibleIds
        if (missingEligible.isNotEmpty()) {
            val names = missingEligible.mapNotNull { allPlayersById[it]?.name }.joinToString(", ")
            errors.add("Eligible player(s) missing from arrangement: $names.")
        }

        // Verify Team Locks
        for (player in eligiblePlayers) {
            val c = constraints.constraints[player.id]
            if (c?.isLocked == true && c.lockedTeamIndex != null) {
                val targetIndex = c.lockedTeamIndex
                if (targetIndex !in teams.indices) {
                    errors.add("Player '${player.name}' is locked to a nonexistent team index $targetIndex.")
                } else if (player.id !in teams[targetIndex].playerIds) {
                    val expectedLabel = teams[targetIndex].name
                    errors.add("Locked player '${player.name}' must remain on $expectedLabel.")
                }
            }
        }

        // Verify Fixed Groups remain together
        for (group in constraints.fixedGroups) {
            val memberTeamIndices = group.playerIds.map { memberId ->
                teams.indexOfFirst { memberId in it.playerIds }
            }
            if (memberTeamIndices.any { it < 0 } || memberTeamIndices.distinct().size > 1) {
                errors.add("Fixed group '${group.name}' players must remain together on the same team.")
            }
        }

        // Verify Team Capacity / Balanced Size Rules
        if (numTeams >= 2) {
            val poolSize = eligiblePlayers.size - (if (jokerId != null) 1 else 0)
            val minExpected = poolSize / numTeams
            val remainder = poolSize % numTeams
            val maxExpected = minExpected + (if (remainder > 0) 1 else 0)
            val actualSizes = teams.map { it.playerIds.size }
            val actualMax = actualSizes.maxOrNull() ?: 0
            val actualMin = actualSizes.minOrNull() ?: 0
            val countOfMax = actualSizes.count { it == maxExpected }
            val expectedCountOfMax = if (remainder == 0) numTeams else remainder

            val sizesValid = actualSizes.all { it in minExpected..maxExpected } &&
                (actualMax - actualMin) <= 1 &&
                countOfMax == expectedCountOfMax &&
                actualSizes.sum() == poolSize

            if (!sizesValid) {
                val sizeSummary = teams.joinToString(", ") { "${it.name}: ${it.playerIds.size}" }
                val expectedSummary = if (minExpected == maxExpected) {
                    "$minExpected players per team"
                } else {
                    "$minExpected–$maxExpected players per team"
                }
                val msg = "Team sizes are imbalanced ($sizeSummary). Expected $expectedSummary."
                if (constraints.allowTemporarySizeImbalance) {
                    warnings.add("$msg Rebalance before confirming.")
                } else {
                    errors.add(msg)
                }
            }
        }

        // Non-blocking Fairness Warnings
        if (currentFairness != null) {
            val eval = currentFairness.fairnessEvaluation
            if (originalFairness != null && currentFairness.fairnessScore < originalFairness.fairnessScore) {
                warnings.add(
                    "Fairness Score dropped (Before: ${originalFairness.fairnessScore}, After: ${currentFairness.fairnessScore})."
                )
            }
            if (originalFairness != null && eval.teammatePenalty > originalFairness.fairnessEvaluation.teammatePenalty) {
                warnings.add("This change increases repeated teammate combinations.")
            } else if (settings != null && eval.teammatePenalty > settings.maximumTeammatePenalty) {
                warnings.add(
                    "Teammate repetition penalty: ${eval.teammatePenalty} (Recommended maximum: ${settings.maximumTeammatePenalty})."
                )
            }
            if (settings != null && eval.strengthDifference > settings.maximumTeamStrengthDifference) {
                warnings.add(
                    "Team Strength Difference: ${eval.strengthDifference} (Recommended maximum: ${settings.maximumTeamStrengthDifference})."
                )
            }
            if (settings != null && eval.opponentPenalty > settings.maximumOpponentPenalty) {
                warnings.add(
                    "Opponent repetition penalty: ${eval.opponentPenalty} (Recommended maximum: ${settings.maximumOpponentPenalty})."
                )
            }
            if (settings != null && eval.overallScore < settings.minimumOverallFairnessScore) {
                warnings.add(
                    "Overall fairness score (${currentFairness.fairnessScore}) is below target (${settings.minimumOverallFairnessScore.toInt()})."
                )
            }
        }

        // Duplicate Arrangement Warning (never blocks editing)
        val sig = currentFairness?.signature ?: generateArrangementSignature(teams, arrangement.jokerPlayerName)
        if (existingSignatures.contains(sig) || currentFairness?.isDuplicate == true) {
            warnings.add("This arrangement has already been used.")
        }

        return ArrangementValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings.distinct()
        )
    }

    /**
     * Initializes a manual editing session from the generated arrangement.
     */
    fun createSession(
        initialArrangement: TeamArrangement,
        context: ManualEditContext,
        allowTemporarySizeImbalance: Boolean = false
    ): ManualEditSession {
        val normalizedTeams = rebuildTeamsWithRatings(initialArrangement.teams, context.matchConstraints.allPlayers)
        val normalized = initialArrangement.copy(teams = normalizedTeams)
        val initialFairness = evaluateArrangementFairness(normalized, context)
        val editingConstraints = context.matchConstraints.copy(
            allowTemporarySizeImbalance = allowTemporarySizeImbalance
        )
        val strictConstraints = context.matchConstraints.copy(
            allowTemporarySizeImbalance = false
        )
        val validation = validateManualArrangement(
            arrangement = normalized,
            constraints = editingConstraints,
            currentFairness = initialFairness,
            originalFairness = initialFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        val strictValidation = validateManualArrangement(
            arrangement = normalized,
            constraints = strictConstraints,
            currentFairness = initialFairness,
            originalFairness = initialFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        return ManualEditSession(
            originalArrangement = normalized,
            currentArrangement = normalized,
            undoStack = emptyList(),
            redoStack = emptyList(),
            allowTemporarySizeImbalance = allowTemporarySizeImbalance,
            originalFairness = initialFairness,
            currentFairness = initialFairness,
            validationResult = validation,
            strictConfirmationValidation = strictValidation,
            lastOperationError = null
        )
    }

    /**
     * Move a player from their current team to destinationTeamId.
     * Validates constraints, prevents duplicates and invalid team IDs, preserves skill ratings,
     * pushes reversible state snapshot to undo stack, clears redo stack, and recalculates fairness read-only.
     */
    fun movePlayer(
        session: ManualEditSession,
        playerId: String,
        destinationTeamId: String,
        context: ManualEditContext
    ): ManualEditActionResult {
        val allPlayersById = context.matchConstraints.allPlayers.associateBy { it.id }
        val player = allPlayersById[playerId]
            ?: return reject(session, "Cannot move player: unknown player ID '$playerId'.")

        // Check excluded
        if (context.matchConstraints.constraints[playerId]?.isExcluded == true) {
            return reject(session, "${player.name} is excluded from this match and cannot be added to a team.")
        }

        val currentTeams = session.currentArrangement.teams
        val destTeamIndex = currentTeams.indexOfFirst {
            it.teamId == destinationTeamId || it.name.equals(destinationTeamId, ignoreCase = true)
        }
        if (destTeamIndex < 0) {
            return reject(session, "Destination team '$destinationTeamId' does not exist.")
        }

        // Check if player is currently Joker
        if (session.currentArrangement.jokerPlayerId == playerId) {
            return reject(session, "${player.name} is currently the Joker. Use Swap Players to exchange the Joker with a team player.")
        }

        val sourceTeamIndex = currentTeams.indexOfFirst { playerId in it.playerIds }
        if (sourceTeamIndex < 0) {
            return reject(session, "${player.name} is not assigned to any team.")
        }
        if (sourceTeamIndex == destTeamIndex) {
            return reject(session, "${player.name} is already on ${currentTeams[destTeamIndex].name}.")
        }

        // Check team lock
        val constraint = context.matchConstraints.constraints[playerId]
        if (constraint?.isLocked == true && constraint.lockedTeamIndex != null && constraint.lockedTeamIndex != destTeamIndex) {
            val lockedLabel = constraint.getLockedTeamLabel() ?: currentTeams[sourceTeamIndex].name
            return reject(session, "${player.name} is locked to $lockedLabel and cannot be moved to ${currentTeams[destTeamIndex].name}.")
        }

        // Check fixed group
        val group = context.matchConstraints.fixedGroups.firstOrNull { playerId in it.playerIds }
        if (group != null) {
            return reject(session, "${player.name} belongs to fixed group '${group.name}' and cannot be moved separately.")
        }

        // Construct updated teams atomically (remove from source, add to destination without duplicates)
        val updatedTeamsRaw = currentTeams.mapIndexed { index, team ->
            when (index) {
                sourceTeamIndex -> {
                    val newIds = team.playerIds.filter { it != playerId }
                    team.copy(playerIds = newIds)
                }
                destTeamIndex -> {
                    val newIds = if (playerId in team.playerIds) team.playerIds else team.playerIds + playerId
                    team.copy(playerIds = newIds)
                }
                else -> team
            }
        }
        val rebuiltTeams = rebuildTeamsWithRatings(updatedTeamsRaw, context.matchConstraints.allPlayers)
        val nextArrangement = session.currentArrangement.copy(teams = rebuiltTeams)

        val editingConstraints = context.matchConstraints.copy(
            allowTemporarySizeImbalance = session.allowTemporarySizeImbalance
        )
        val nextFairness = evaluateArrangementFairness(nextArrangement, context)
        val nextValidation = validateManualArrangement(
            arrangement = nextArrangement,
            constraints = editingConstraints,
            currentFairness = nextFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )

        if (!nextValidation.valid) {
            val baseError = nextValidation.errors.firstOrNull() ?: "Move creates an invalid team arrangement."
            val helpfulMsg = if (!session.allowTemporarySizeImbalance && baseError.contains("imbalanced")) {
                "$baseError Use Swap Players or enable temporary imbalance during editing."
            } else {
                baseError
            }
            return reject(session, helpfulMsg)
        }

        return commitEdit(session, nextArrangement, nextFairness, nextValidation, context)
    }

    /**
     * Atomically swap two players (between two different teams, or between a team and the Joker).
     * Never produces an intermediate invalid state.
     */
    fun swapPlayers(
        session: ManualEditSession,
        playerAId: String,
        playerBId: String,
        context: ManualEditContext
    ): ManualEditActionResult {
        if (playerAId == playerBId) {
            return reject(session, "Select two different players to swap.")
        }

        val allPlayersById = context.matchConstraints.allPlayers.associateBy { it.id }
        val playerA = allPlayersById[playerAId]
            ?: return reject(session, "Cannot swap: unknown player ID '$playerAId'.")
        val playerB = allPlayersById[playerBId]
            ?: return reject(session, "Cannot swap: unknown player ID '$playerBId'.")

        // Check excluded
        if (context.matchConstraints.constraints[playerAId]?.isExcluded == true) {
            return reject(session, "${playerA.name} is excluded and cannot be swapped into a team.")
        }
        if (context.matchConstraints.constraints[playerBId]?.isExcluded == true) {
            return reject(session, "${playerB.name} is excluded and cannot be swapped into a team.")
        }

        val currentTeams = session.currentArrangement.teams
        val jokerId = session.currentArrangement.jokerPlayerId

        val teamIndexA = currentTeams.indexOfFirst { playerAId in it.playerIds }
        val teamIndexB = currentTeams.indexOfFirst { playerBId in it.playerIds }
        val isAJoker = jokerId == playerAId
        val isBJoker = jokerId == playerBId

        if (teamIndexA < 0 && !isAJoker) {
            return reject(session, "${playerA.name} is not assigned to a team or Joker.")
        }
        if (teamIndexB < 0 && !isBJoker) {
            return reject(session, "${playerB.name} is not assigned to a team or Joker.")
        }
        if (teamIndexA >= 0 && teamIndexA == teamIndexB) {
            return reject(session, "${playerA.name} and ${playerB.name} are already on the same team.")
        }

        // Check Team Locks for both players
        val constraintA = context.matchConstraints.constraints[playerAId]
        if (constraintA?.isLocked == true && constraintA.lockedTeamIndex != null) {
            val label = constraintA.getLockedTeamLabel() ?: "their team"
            return reject(session, "${playerA.name} is locked to $label and cannot be swapped.")
        }
        val constraintB = context.matchConstraints.constraints[playerBId]
        if (constraintB?.isLocked == true && constraintB.lockedTeamIndex != null) {
            val label = constraintB.getLockedTeamLabel() ?: "their team"
            return reject(session, "${playerB.name} is locked to $label and cannot be swapped.")
        }

        // Check Fixed Groups for both players
        val groupA = context.matchConstraints.fixedGroups.firstOrNull { playerAId in it.playerIds }
        if (groupA != null) {
            return reject(session, "${playerA.name} belongs to fixed group '${groupA.name}' and cannot be separated.")
        }
        val groupB = context.matchConstraints.fixedGroups.firstOrNull { playerBId in it.playerIds }
        if (groupB != null) {
            return reject(session, "${playerB.name} belongs to fixed group '${groupB.name}' and cannot be separated.")
        }

        // Perform single atomic swap
        val updatedTeamsRaw: List<GeneratedTeam>
        val nextJokerId: String?
        val nextJokerName: String?

        if (isAJoker && teamIndexB >= 0) {
            // Swap Joker (A) with Team player (B)
            updatedTeamsRaw = currentTeams.mapIndexed { idx, team ->
                if (idx == teamIndexB) {
                    team.copy(playerIds = team.playerIds.map { if (it == playerBId) playerAId else it })
                } else team
            }
            nextJokerId = playerBId
            nextJokerName = playerB.name
        } else if (isBJoker && teamIndexA >= 0) {
            // Swap Team player (A) with Joker (B)
            updatedTeamsRaw = currentTeams.mapIndexed { idx, team ->
                if (idx == teamIndexA) {
                    team.copy(playerIds = team.playerIds.map { if (it == playerAId) playerBId else it })
                } else team
            }
            nextJokerId = playerAId
            nextJokerName = playerA.name
        } else {
            // Swap Team A player with Team B player
            updatedTeamsRaw = currentTeams.mapIndexed { idx, team ->
                when (idx) {
                    teamIndexA -> team.copy(playerIds = team.playerIds.map { if (it == playerAId) playerBId else it })
                    teamIndexB -> team.copy(playerIds = team.playerIds.map { if (it == playerBId) playerAId else it })
                    else -> team
                }
            }
            nextJokerId = jokerId
            nextJokerName = session.currentArrangement.jokerPlayerName
        }

        val rebuiltTeams = rebuildTeamsWithRatings(updatedTeamsRaw, context.matchConstraints.allPlayers)
        val nextArrangement = session.currentArrangement.copy(
            teams = rebuiltTeams,
            jokerPlayerId = nextJokerId,
            jokerPlayerName = nextJokerName
        )

        val editingConstraints = context.matchConstraints.copy(
            allowTemporarySizeImbalance = session.allowTemporarySizeImbalance
        )
        val nextFairness = evaluateArrangementFairness(nextArrangement, context)
        val nextValidation = validateManualArrangement(
            arrangement = nextArrangement,
            constraints = editingConstraints,
            currentFairness = nextFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )

        if (!nextValidation.valid) {
            return reject(session, nextValidation.errors.firstOrNull() ?: "Swap violates match constraints.")
        }

        return commitEdit(session, nextArrangement, nextFairness, nextValidation, context)
    }

    /**
     * Reverse the most recent successful edit.
     */
    fun undo(
        session: ManualEditSession,
        context: ManualEditContext
    ): ManualEditActionResult {
        if (session.undoStack.isEmpty()) {
            return ManualEditActionResult(session.copy(lastOperationError = null), succeeded = false)
        }
        val previous = session.undoStack.last()
        val remainingUndo = session.undoStack.dropLast(1)
        val updatedRedo = (session.redoStack + session.currentArrangement).takeLast(MAX_HISTORY_LIMIT)

        return rebuildSessionForArrangement(
            session = session,
            nextArrangement = previous,
            undoStack = remainingUndo,
            redoStack = updatedRedo,
            context = context
        )
    }

    /**
     * Reapply the most recently undone edit.
     */
    fun redo(
        session: ManualEditSession,
        context: ManualEditContext
    ): ManualEditActionResult {
        if (session.redoStack.isEmpty()) {
            return ManualEditActionResult(session.copy(lastOperationError = null), succeeded = false)
        }
        val restored = session.redoStack.last()
        val remainingRedo = session.redoStack.dropLast(1)
        val updatedUndo = (session.undoStack + session.currentArrangement).takeLast(MAX_HISTORY_LIMIT)

        return rebuildSessionForArrangement(
            session = session,
            nextArrangement = restored,
            undoStack = updatedUndo,
            redoStack = remainingRedo,
            context = context
        )
    }

    /**
     * Restores the originally generated shuffle arrangement without re-shuffling or modifying history.
     */
    fun resetChanges(
        session: ManualEditSession,
        context: ManualEditContext
    ): ManualEditActionResult {
        return rebuildSessionForArrangement(
            session = session,
            nextArrangement = session.originalArrangement,
            undoStack = emptyList(),
            redoStack = emptyList(),
            context = context
        )
    }

    /**
     * Toggles whether temporary team-size imbalances are allowed during editing moves.
     * Note: Final confirmation always enforces strict team size balance.
     */
    fun setAllowTemporarySizeImbalance(
        session: ManualEditSession,
        allow: Boolean,
        context: ManualEditContext
    ): ManualEditSession {
        val editingConstraints = context.matchConstraints.copy(allowTemporarySizeImbalance = allow)
        val strictConstraints = context.matchConstraints.copy(allowTemporarySizeImbalance = false)
        val validation = validateManualArrangement(
            arrangement = session.currentArrangement,
            constraints = editingConstraints,
            currentFairness = session.currentFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        val strictValidation = validateManualArrangement(
            arrangement = session.currentArrangement,
            constraints = strictConstraints,
            currentFairness = session.currentFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        return session.copy(
            allowTemporarySizeImbalance = allow,
            validationResult = validation,
            strictConfirmationValidation = strictValidation,
            lastOperationError = null
        )
    }

    /**
     * Pure-Kotlin JSON serializer for storing original/final team arrangements in SQLite.
     */
    fun serializeArrangementToJson(arrangement: TeamArrangement): String {
        fun esc(s: String): String = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        val teamsJson = arrangement.teams.joinToString(",") { team ->
            val namesJson = team.players.joinToString(",") { "\"${esc(it)}\"" }
            val idsJson = team.playerIds.joinToString(",") { "\"${esc(it)}\"" }
            """{"teamNumber":${team.teamNumber},"name":"${esc(team.name)}","players":[$namesJson],"playerIds":[$idsJson],"totalStrength":${team.totalStrength}}"""
        }
        val jokerJson = arrangement.jokerPlayerName?.let { "\"${esc(it)}\"" } ?: "null"
        return """{"teams":[$teamsJson],"joker":$jokerJson}"""
    }

    private fun commitEdit(
        session: ManualEditSession,
        nextArrangement: TeamArrangement,
        nextFairness: ManualFairnessSnapshot,
        nextValidation: ArrangementValidationResult,
        context: ManualEditContext
    ): ManualEditActionResult {
        val nextUndo = (session.undoStack + session.currentArrangement).takeLast(MAX_HISTORY_LIMIT)
        val strictConstraints = context.matchConstraints.copy(allowTemporarySizeImbalance = false)
        val strictValidation = validateManualArrangement(
            arrangement = nextArrangement,
            constraints = strictConstraints,
            currentFairness = nextFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        val updatedSession = session.copy(
            currentArrangement = nextArrangement,
            undoStack = nextUndo,
            redoStack = emptyList(), // New edit clears redo stack
            currentFairness = nextFairness,
            validationResult = nextValidation,
            strictConfirmationValidation = strictValidation,
            lastOperationError = null
        )
        return ManualEditActionResult(updatedSession, succeeded = true)
    }

    private fun rebuildSessionForArrangement(
        session: ManualEditSession,
        nextArrangement: TeamArrangement,
        undoStack: List<TeamArrangement>,
        redoStack: List<TeamArrangement>,
        context: ManualEditContext
    ): ManualEditActionResult {
        val nextFairness = evaluateArrangementFairness(nextArrangement, context)
        val editingConstraints = context.matchConstraints.copy(
            allowTemporarySizeImbalance = session.allowTemporarySizeImbalance
        )
        val strictConstraints = context.matchConstraints.copy(
            allowTemporarySizeImbalance = false
        )
        val nextValidation = validateManualArrangement(
            arrangement = nextArrangement,
            constraints = editingConstraints,
            currentFairness = nextFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        val strictValidation = validateManualArrangement(
            arrangement = nextArrangement,
            constraints = strictConstraints,
            currentFairness = nextFairness,
            originalFairness = session.originalFairness,
            settings = context.effectiveSettings,
            existingSignatures = context.existingSignatures
        )
        val updated = session.copy(
            currentArrangement = nextArrangement,
            undoStack = undoStack,
            redoStack = redoStack,
            currentFairness = nextFairness,
            validationResult = nextValidation,
            strictConfirmationValidation = strictValidation,
            lastOperationError = null
        )
        return ManualEditActionResult(updated, succeeded = true)
    }

    private fun reject(
        session: ManualEditSession,
        message: String
    ): ManualEditActionResult {
        // Invalid operations never modify arrangement or enter undo/redo stacks
        return ManualEditActionResult(
            session = session.copy(lastOperationError = message),
            succeeded = false,
            errorMessage = message
        )
    }
}
