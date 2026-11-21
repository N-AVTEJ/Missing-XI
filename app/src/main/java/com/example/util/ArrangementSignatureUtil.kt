package com.example.util

object ArrangementSignatureUtil {

    /**
     * Generates the canonical arrangement signature used across candidate duplicate
     * detection and manual team editing duplicate detection.
     */
    fun generateSignature(teams: List<GeneratedTeam>, joker: String?): String {
        val sortedTeams = teams.map { team ->
            team.players.map { it.trim() }.sorted().joinToString(",")
        }.sorted()
        val teamsStr = sortedTeams.joinToString(" | ")
        val jokerStr = if (!joker.isNullOrBlank()) " [JOKER: ${joker.trim()}]" else ""
        return "$teamsStr$jokerStr"
    }

    /**
     * Returns the canonical team identifier for a GeneratedTeam.
     */
    fun teamIdFor(teamNumber: Int): String = "team_$teamNumber"

    /**
     * Checks whether a GeneratedTeam matches a caller-provided team identifier
     * (either canonical "team_1" or display name "Team A").
     */
    fun matchesTeamId(team: GeneratedTeam, teamId: String): Boolean {
        val trimmed = teamId.trim()
        return teamIdFor(team.teamNumber).equals(trimmed, ignoreCase = true) ||
            team.name.equals(trimmed, ignoreCase = true) ||
            team.teamNumber.toString() == trimmed
    }
}
