package com.example.util

data class ConstraintValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object PlayerConstraintValidator {

    fun validate(
        activePlayers: List<Player>,
        numTeams: Int,
        constraints: Map<String, PlayerMatchConstraint>,
        groups: List<LockedPlayerGroup>
    ): ConstraintValidationResult {
        if (numTeams < 2) {
            return ConstraintValidationResult(false, "Number of teams must be at least 2.")
        }

        val excluded = activePlayers.filter { constraints[it.id]?.isExcluded == true }
        val eligible = activePlayers.filter { constraints[it.id]?.isExcluded != true }

        if (eligible.isEmpty()) {
            return ConstraintValidationResult(
                false,
                "Cannot generate teams. All eligible players are excluded. Add at least one eligible player."
            )
        }

        if (eligible.size < numTeams) {
            return ConstraintValidationResult(
                false,
                "Cannot generate teams. There are not enough eligible players (${eligible.size}) for the selected team configuration ($numTeams teams)."
            )
        }

        val activePlayerMap = activePlayers.associateBy { it.id }

        // Validate Groups
        for (group in groups) {
            // Check players exist
            val unknownIds = group.playerIds.filter { !activePlayerMap.containsKey(it) }
            if (unknownIds.isNotEmpty()) {
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. Fixed group '${group.name}' contains players not in the current roster."
                )
            }

            // Check distinct players
            if (group.playerIds.distinct().size < 2) {
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. Fixed group '${group.name}' must contain at least two distinct players."
                )
            }

            // Check no excluded players in group
            val excludedInGroup = group.playerIds.filter { constraints[it]?.isExcluded == true }
            if (excludedInGroup.isNotEmpty()) {
                val names = excludedInGroup.mapNotNull { activePlayerMap[it]?.name }
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. Fixed group '${group.name}' contains excluded player(s): ${names.joinToString(", ")}. Un-exclude them or remove them from the group."
                )
            }

            // Check conflicting team locks within group
            val lockedTeamsInGroup = group.playerIds.mapNotNull { id ->
                val c = constraints[id]
                if (c?.isLocked == true && c.lockedTeamIndex != null) c.lockedTeamIndex else null
            }.distinct()

            if (lockedTeamsInGroup.size > 1) {
                val teamLabels = lockedTeamsInGroup.map { "Team " + (('A' + it).toString()) }
                val names = group.playerIds.mapNotNull { activePlayerMap[it]?.name }
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. Players in fixed group '${group.name}' (${names.joinToString(", ")}) are locked to different teams (${teamLabels.joinToString(", ")})."
                )
            }
        }

        // Check conflicting groups (player in multiple groups)
        val allGroupPlayerIds = groups.flatMap { it.playerIds }
        val duplicates = allGroupPlayerIds.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            val dupNames = duplicates.keys.mapNotNull { activePlayerMap[it]?.name }
            return ConstraintValidationResult(
                false,
                "Cannot generate teams. Player(s) ${dupNames.joinToString(", ")} belong to multiple fixed groups."
            )
        }

        // Joker Availability Check
        if (eligible.size % numTeams != 0) {
            val jokerCandidates = eligible.filter { player ->
                val c = constraints[player.id]
                val isLocked = c?.isLocked == true && c.lockedTeamIndex != null
                val inGroup = groups.any { g -> player.id in g.playerIds }
                !isLocked && !inGroup
            }

            if (jokerCandidates.isEmpty()) {
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. An odd player count requires 1 Joker, but no eligible players are available because all players are locked to teams or part of fixed groups."
                )
            }
        }

        // Target Capacities
        val poolSize = eligible.size - (if (eligible.size % numTeams != 0) 1 else 0)
        val targetCapacities = IntArray(numTeams) { i ->
            poolSize / numTeams + (if (i < poolSize % numTeams) 1 else 0)
        }

        // Effective Team Locks (direct locks + groups containing a locked player)
        val effectiveLocks = mutableMapOf<String, Int>()

        // 1. Direct locks
        for (player in eligible) {
            val c = constraints[player.id]
            if (c?.isLocked == true && c.lockedTeamIndex != null) {
                effectiveLocks[player.id] = c.lockedTeamIndex
            }
        }

        // 2. Propagate group locks
        for (group in groups) {
            val lockedTeam = group.playerIds.firstNotNullOfOrNull { effectiveLocks[it] }
            if (lockedTeam != null) {
                for (id in group.playerIds) {
                    effectiveLocks[id] = lockedTeam
                }
            }
        }

        // Check locked players per team
        val remainingCapacities = targetCapacities.clone()
        for (teamIndex in 0 until numTeams) {
            val lockedCount = effectiveLocks.values.count { it == teamIndex }
            if (lockedCount > targetCapacities[teamIndex]) {
                val teamLabel = "Team " + (('A' + teamIndex).toString())
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. $teamLabel has more locked players ($lockedCount) than its target capacity (${targetCapacities[teamIndex]})."
                )
            }
            remainingCapacities[teamIndex] -= lockedCount
        }

        // Check Floating Groups (groups with no locked team)
        val floatingGroups = groups.filter { group ->
            group.playerIds.none { effectiveLocks.containsKey(it) }
        }

        for (fg in floatingGroups) {
            val maxCap = remainingCapacities.maxOrNull() ?: 0
            if (fg.playerIds.size > maxCap) {
                return ConstraintValidationResult(
                    false,
                    "Cannot generate teams. Fixed group '${fg.name}' (${fg.playerIds.size} players) exceeds the maximum available team target capacity ($maxCap)."
                )
            }
        }

        // Bin packing check for floating groups
        val floatingSizes = floatingGroups.map { it.playerIds.size }.sortedDescending()
        if (!canFitGroups(floatingSizes, 0, remainingCapacities)) {
            return ConstraintValidationResult(
                false,
                "Cannot generate teams. The remaining capacities of the teams cannot accommodate all fixed groups without splitting them."
            )
        }

        return ConstraintValidationResult(true)
    }

    private fun canFitGroups(
        groupSizes: List<Int>,
        groupIndex: Int,
        remCap: IntArray
    ): Boolean {
        if (groupIndex >= groupSizes.size) return true
        val size = groupSizes[groupIndex]
        for (i in remCap.indices) {
            if (remCap[i] >= size) {
                remCap[i] -= size
                if (canFitGroups(groupSizes, groupIndex + 1, remCap)) {
                    remCap[i] += size
                    return true
                }
                remCap[i] += size
            }
        }
        return false
    }
}
