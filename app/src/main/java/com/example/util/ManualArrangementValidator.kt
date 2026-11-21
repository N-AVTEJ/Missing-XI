package com.example.util

import kotlin.math.roundToInt

object ManualArrangementValidator {

    /**
     * Centralized arrangement validation separating hard constraint errors
     * from non-blocking fairness warnings.
     */
    fun validateManualArrangement(
        arrangement: TeamArrangement,
        constraints: MatchConstraints,
        allowTemporaryImbalance: Boolean = constraints.allowTemporaryImbalance
    ): ArrangementValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Validate team structure and team IDs
        if (constraints.numTeams < 2) {
            errors.add("Number of teams must be at least 2.")
            return ArrangementValidationResult(valid = false, errors = errors, warnings = warnings)
        }

        if (arrangement.teams.size != constraints.numTeams) {
            errors.add("Expected ${constraints.numTeams} teams, but found ${arrangement.teams.size}.")
        }

        val seenTeamNumbers = mutableSetOf<Int>()
        val seenTeamNames = mutableSetOf<String>()
        arrangement.teams.forEach { team ->
            if (team.teamNumber < 1 || team.teamNumber > constraints.numTeams) {
                errors.add("Invalid team ID/number: ${team.teamNumber}.")
            }
            if (!seenTeamNumbers.add(team.teamNumber)) {
                errors.add("Duplicate team number detected: ${team.teamNumber}.")
            }
            if (team.name.isBlank() || !seenTeamNames.add(team.name.trim().lowercase())) {
                errors.add("Invalid or duplicate team name: '${team.name}'.")
            }
        }

        val allPlayerMap = constraints.allPlayers.associateBy { it.id }
        val allPlayerByName = constraints.allPlayers.associateBy { it.name.trim() }
        val excludedIds = constraints.allPlayers
            .filter { constraints.playerConstraints[it.id]?.isExcluded == true }
            .map { it.id }
            .toSet()
        val eligibleIds = constraints.allPlayers.map { it.id }.toSet() - excludedIds

        // Resolve Joker ID
        val resolvedJokerId: String? = when {
            !arrangement.jokerId.isNullOrBlank() -> arrangement.jokerId
            !arrangement.joker.isNullOrBlank() -> allPlayerByName[arrangement.joker.trim()]?.id
            else -> null
        }

        if (!arrangement.jokerId.isNullOrBlank() && !allPlayerMap.containsKey(arrangement.jokerId)) {
            errors.add("Unknown Joker player ID: ${arrangement.jokerId}.")
        } else if (!arrangement.joker.isNullOrBlank() && resolvedJokerId == null) {
            errors.add("Unknown Joker player: ${arrangement.joker}.")
        }

        // 2. Check team members: unknown IDs, intra-team duplicates, cross-team duplicates, excluded, joker overlap
        val playerToTeamIndex = mutableMapOf<String, Int>()
        val crossTeamDuplicates = mutableSetOf<String>()

        arrangement.teams.forEachIndexed { teamIndex, team ->
            val seenInTeam = mutableSetOf<String>()
            team.playerIds.forEach { playerId ->
                if (!allPlayerMap.containsKey(playerId)) {
                    errors.add("Unknown player ID '$playerId' in ${team.name}.")
                }
                if (!seenInTeam.add(playerId)) {
                    val name = allPlayerMap[playerId]?.name ?: playerId
                    errors.add("Duplicate player '$name' inside ${team.name}.")
                }
                val previousTeamIndex = playerToTeamIndex.putIfAbsent(playerId, teamIndex)
                if (previousTeamIndex != null && previousTeamIndex != teamIndex && crossTeamDuplicates.add(playerId)) {
                    val name = allPlayerMap[playerId]?.name ?: playerId
                    errors.add("Player '$name' cannot appear in multiple teams.")
                }
                if (playerId in excludedIds) {
                    val name = allPlayerMap[playerId]?.name ?: playerId
                    errors.add("Excluded player '$name' cannot be assigned to ${team.name}.")
                }
                if (resolvedJokerId != null && playerId == resolvedJokerId) {
                    val name = allPlayerMap[playerId]?.name ?: playerId
                    errors.add("Player '$name' cannot be assigned both to ${team.name} and as Joker.")
                }
            }
        }

        // 3. Validate Joker rules
        if (resolvedJokerId != null) {
            val jokerName = allPlayerMap[resolvedJokerId]?.name ?: arrangement.joker ?: resolvedJokerId
            if (resolvedJokerId in excludedIds) {
                errors.add("Excluded player '$jokerName' cannot be assigned as Joker.")
            }
            val jokerConstraint = constraints.playerConstraints[resolvedJokerId]
            if (jokerConstraint?.isLocked == true && jokerConstraint.lockedTeamIndex != null) {
                errors.add("Locked player '$jokerName' cannot be assigned as Joker.")
            }
            if (constraints.fixedGroups.any { resolvedJokerId in it.playerIds }) {
                errors.add("Player '$jokerName' belongs to a fixed group and cannot be assigned as Joker.")
            }
            if (eligibleIds.size % constraints.numTeams == 0) {
                errors.add("Joker '$jokerName' is invalid when eligible players (${eligibleIds.size}) divide evenly across ${constraints.numTeams} teams.")
            }
        }

        // 4. Verify every eligible player appears exactly once (across teams + joker)
        val assignedEligibleIds = playerToTeamIndex.keys + setOfNotNull(resolvedJokerId)
        val missingEligible = eligibleIds - assignedEligibleIds
        if (missingEligible.isNotEmpty()) {
            val names = missingEligible.map { allPlayerMap[it]?.name ?: it }.sorted()
            errors.add("Eligible player(s) missing from arrangement: ${names.joinToString(", ")}.")
        }

        // 5. Team capacity & balanced distribution rules
        val totalInTeams = arrangement.teams.sumOf { it.playerIds.size }
        if (arrangement.teams.isNotEmpty() && constraints.numTeams >= 2) {
            val minExpected = totalInTeams / constraints.numTeams
            val remainder = totalInTeams % constraints.numTeams
            val maxExpected = if (remainder > 0) minExpected + 1 else minExpected
            val isBalanced = arrangement.teams.all { it.playerIds.size in minExpected..maxExpected }

            if (!isBalanced) {
                val sizeSummary = arrangement.teams.joinToString(", ") { "${it.name}: ${it.playerIds.size}" }
                val expectedSummary = if (remainder > 0) {
                    "$minExpected–$maxExpected per team"
                } else {
                    "$minExpected per team"
                }
                if (allowTemporaryImbalance) {
                    warnings.add("Temporary team size imbalance ($sizeSummary). Balance teams ($expectedSummary) before confirming.")
                } else {
                    errors.add("Team sizes are unbalanced ($sizeSummary). Expected $expectedSummary.")
                }
            }
        }

        // 6. Respect team locks
        constraints.playerConstraints.forEach { (playerId, constraint) ->
            if (!constraint.isExcluded && constraint.isLocked && constraint.lockedTeamIndex != null) {
                val expectedTeamIdx = constraint.lockedTeamIndex
                val actualTeamIdx = playerToTeamIndex[playerId]
                val name = allPlayerMap[playerId]?.name ?: playerId
                val expectedTeamLabel = constraint.getLockedTeamLabel() ?: "Team ${expectedTeamIdx + 1}"
                if (actualTeamIdx != expectedTeamIdx) {
                    errors.add("Locked player '$name' must remain on $expectedTeamLabel.")
                }
            }
        }

        // 7. Respect fixed groups (all members must remain on the exact same team)
        constraints.fixedGroups.forEach { group ->
            if (group.playerIds.size >= 2) {
                val teamIndices = group.playerIds.map { playerToTeamIndex[it] }.distinct()
                if (teamIndices.size != 1 || teamIndices.first() == null) {
                    errors.add("Fixed group '${group.name}' cannot be split across teams.")
                }
            }
        }

        return ArrangementValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Evaluates non-blocking fairness warnings comparing original generated arrangement
     * to the current manually edited arrangement.
     */
    fun evaluateFairnessWarnings(
        original: FairnessEvaluation,
        current: FairnessEvaluation,
        settings: FairnessSettings,
        qualityResult: FairnessQualityResult,
        isDuplicate: Boolean
    ): List<String> {
        val warnings = mutableListOf<String>()

        if (isDuplicate) {
            warnings.add("This arrangement has already been used.")
        }

        val beforeScore = original.overallScore.roundToInt()
        val afterScore = current.overallScore.roundToInt()

        if (afterScore < beforeScore) {
            warnings.add("Fairness score decreased from $beforeScore to $afterScore.")
        }

        if (current.teammatePenalty > original.teammatePenalty) {
            warnings.add("This change increases repeated teammate combinations (Penalty: ${original.teammatePenalty} → ${current.teammatePenalty}).")
        }

        if (current.opponentPenalty > original.opponentPenalty) {
            warnings.add("This change increases repeated opponent matchups (Penalty: ${original.opponentPenalty} → ${current.opponentPenalty}).")
        }

        if (current.strengthDifference > settings.maximumTeamStrengthDifference) {
            warnings.add("Team Strength Difference: ${current.strengthDifference} (Recommended maximum: ${settings.maximumTeamStrengthDifference}).")
        }

        if (!qualityResult.overallScorePassed) {
            warnings.add("Fairness score ($afterScore) is below target threshold (${settings.minimumOverallFairnessScore.toInt()}).")
        }

        return warnings.distinct()
    }
}
