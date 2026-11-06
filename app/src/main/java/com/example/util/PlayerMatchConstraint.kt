package com.example.util

data class PlayerMatchConstraint(
    val playerId: String,
    val isLocked: Boolean = false,
    val lockedTeamIndex: Int? = null, // 0 for Team A (Team 1), 1 for Team B (Team 2), etc.
    val isExcluded: Boolean = false
) {
    fun getLockedTeamLabel(): String? {
        return lockedTeamIndex?.let { "Team " + (('A' + it).toString()) }
    }
}

data class LockedPlayerGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Group",
    val playerIds: List<String> = emptyList()
)
