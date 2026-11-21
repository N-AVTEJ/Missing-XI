package com.example.util

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class TossAttemptOutcome {
    data class Ready(val tossResult: TossResult) : TossAttemptOutcome()
    data class Rejected(val reason: String) : TossAttemptOutcome()
}

/**
 * Pure Kotlin toss logic, lifecycle transition validation, and match team serialization.
 * Completely independent of team fairness scores, player skill ratings, or previous tosses.
 */
object MatchTossEngine {

    const val FLIP_ANIMATION_DURATION_MS = 2400L

    /**
     * Generates a fair, equal-probability (50/50) Heads or Tails outcome.
     * Uses Math.random() < 0.5 by default and accepts an injectable random provider for deterministic testing.
     */
    fun flipCoin(randomProvider: () -> Double = { Math.random() }): TossChoice {
        return if (randomProvider() < 0.5) TossChoice.HEADS else TossChoice.TAILS
    }

    /**
     * Validates inputs and generates the toss result ONCE before animation begins.
     */
    fun prepareTossAttempt(
        matchId: String?,
        userChoice: TossChoice?,
        currentMatchStatus: MatchStatus,
        currentTossStatus: TossStatus,
        nextAttemptNumber: Int,
        isStandalone: Boolean = false,
        randomProvider: () -> Double = { Math.random() },
        timestampProvider: () -> Long = { System.currentTimeMillis() }
    ): TossAttemptOutcome {
        if (currentTossStatus == TossStatus.IN_PROGRESS) {
            return TossAttemptOutcome.Rejected("Coin flip is already in progress.")
        }
        if (userChoice == null) {
            return TossAttemptOutcome.Rejected("Please select Heads or Tails before flipping the coin.")
        }
        if (!isStandalone) {
            if (matchId.isNullOrBlank()) {
                return TossAttemptOutcome.Rejected("Missing match ID. Please confirm teams before tossing.")
            }
            if (currentMatchStatus == MatchStatus.COMPLETED) {
                return TossAttemptOutcome.Rejected("This match is already completed. Start a new match to toss again.")
            }
            if (currentMatchStatus != MatchStatus.TEAMS_CONFIRMED && currentMatchStatus != MatchStatus.TOSS_COMPLETED) {
                return TossAttemptOutcome.Rejected("Teams must be confirmed before starting the coin toss.")
            }
        }

        val outcome = flipCoin(randomProvider)
        val won = userChoice == outcome
        val safeMatchId = if (matchId.isNullOrBlank()) "standalone" else matchId

        val toss = TossResult(
            id = UUID.randomUUID().toString(),
            matchId = safeMatchId,
            selectedBy = "USER",
            userChoice = userChoice,
            result = outcome,
            wonToss = won,
            createdAt = timestampProvider(),
            status = TossStatus.IN_PROGRESS,
            attemptNumber = nextAttemptNumber.coerceAtLeast(1)
        )
        return TossAttemptOutcome.Ready(toss)
    }

    fun isValidTransition(from: MatchStatus, to: MatchStatus): Boolean {
        return from.canTransitionTo(to)
    }

    fun canFinalizeMatch(status: MatchStatus): Boolean {
        return status == MatchStatus.TOSS_COMPLETED
    }

    fun serializeTeamsJson(teams: List<GeneratedTeam>, joker: String?): String {
        val root = JSONObject()
        val teamsArr = JSONArray()
        teams.forEach { team ->
            teamsArr.put(
                JSONObject().apply {
                    put("teamNumber", team.teamNumber)
                    put("name", team.name)
                    put("players", JSONArray(team.players))
                    put("playerIds", JSONArray(team.playerIds))
                    put("totalStrength", team.totalStrength)
                    put("averageStrength", team.averageStrength)
                    put("maxPlayerRating", team.maxPlayerRating)
                    put("minPlayerRating", team.minPlayerRating)
                }
            )
        }
        root.put("teams", teamsArr)
        if (!joker.isNullOrBlank()) {
            root.put("joker", joker)
        }
        return root.toString()
    }

    fun parseTeamsJson(json: String): Pair<List<GeneratedTeam>, String?> {
        if (json.isBlank()) return emptyList<GeneratedTeam>() to null
        return try {
            val root = JSONObject(json)
            val joker = if (root.has("joker") && !root.isNull("joker")) {
                root.optString("joker").takeIf { it.isNotBlank() }
            } else null

            val teamsArr = root.optJSONArray("teams") ?: return emptyList<GeneratedTeam>() to joker
            val teams = mutableListOf<GeneratedTeam>()
            for (i in 0 until teamsArr.length()) {
                val obj = teamsArr.getJSONObject(i)
                val playersArr = obj.optJSONArray("players") ?: JSONArray()
                val players = (0 until playersArr.length()).map { playersArr.getString(it) }
                val idsArr = obj.optJSONArray("playerIds") ?: JSONArray()
                val playerIds = (0 until idsArr.length()).map { idsArr.getString(it) }
                teams.add(
                    GeneratedTeam(
                        teamNumber = obj.optInt("teamNumber", i + 1),
                        name = obj.optString("name", "Team ${'A' + i}"),
                        players = players,
                        playerIds = playerIds,
                        totalStrength = obj.optInt("totalStrength", 0),
                        averageStrength = obj.optDouble("averageStrength", 0.0),
                        maxPlayerRating = obj.optInt("maxPlayerRating", 0),
                        minPlayerRating = obj.optInt("minPlayerRating", 0)
                    )
                )
            }
            teams to joker
        } catch (_: Exception) {
            emptyList<GeneratedTeam>() to null
        }
    }
}
