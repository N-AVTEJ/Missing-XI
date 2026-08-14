package com.example.util

data class GeneratedTeam(
    val teamNumber: Int,
    val name: String,
    val players: List<String>,
    val playerIds: List<String> = emptyList(),
    val totalStrength: Int = 0,
    val averageStrength: Double = 0.0,
    val maxPlayerRating: Int = 0,
    val minPlayerRating: Int = 0
)

data class TeamStrengthAnalysis(
    val totalStrength: Int = 0,
    val averageStrength: Double = 0.0,
    val maxPlayerRating: Int = 0,
    val minPlayerRating: Int = 0
)

data class CandidateStrengthAnalysis(
    val teamStrengths: List<Int> = emptyList(),
    val strengthDifference: Int = 0,
    val averageTeamStrength: Double = 0.0,
    val strongestTeamStrength: Int = 0,
    val weakestTeamStrength: Int = 0,
    val strongestTeamName: String = "",
    val weakestTeamName: String = ""
)

object TeamStrengthEngine {

    fun calculateTeamStrength(players: List<Player>): TeamStrengthAnalysis {
        if (players.isEmpty()) return TeamStrengthAnalysis()
        val total = players.sumOf { it.skillRating }
        val avg = total.toDouble() / players.size
        val max = players.maxOfOrNull { it.skillRating } ?: 0
        val min = players.minOfOrNull { it.skillRating } ?: 0
        return TeamStrengthAnalysis(
            totalStrength = total,
            averageStrength = avg,
            maxPlayerRating = max,
            minPlayerRating = min
        )
    }

    fun calculateStrengthDifference(teamStrengths: List<Int>): Int {
        if (teamStrengths.isEmpty()) return 0
        val max = teamStrengths.maxOrNull() ?: 0
        val min = teamStrengths.minOrNull() ?: 0
        return max - min
    }

    fun getAverageTeamStrength(teamStrengths: List<Int>): Double {
        if (teamStrengths.isEmpty()) return 0.0
        return teamStrengths.average()
    }

    fun evaluateCandidateStrength(
        candidateTeams: List<GeneratedTeam>
    ): CandidateStrengthAnalysis {
        if (candidateTeams.isEmpty()) return CandidateStrengthAnalysis()
        val strengths = candidateTeams.map { it.totalStrength }
        val maxStrength = strengths.maxOrNull() ?: 0
        val minStrength = strengths.minOrNull() ?: 0
        val diff = maxStrength - minStrength
        val avg = if (strengths.isNotEmpty()) strengths.average() else 0.0

        val strongest = candidateTeams.maxByOrNull { it.totalStrength }
        val weakest = candidateTeams.minByOrNull { it.totalStrength }

        return CandidateStrengthAnalysis(
            teamStrengths = strengths,
            strengthDifference = diff,
            averageTeamStrength = avg,
            strongestTeamStrength = maxStrength,
            weakestTeamStrength = minStrength,
            strongestTeamName = strongest?.name ?: "",
            weakestTeamName = weakest?.name ?: ""
        )
    }
}
