package com.example.util

import java.util.UUID

enum class TossChoice(val displayName: String) {
    HEADS("Heads"),
    TAILS("Tails");

    companion object {
        fun fromString(value: String?): TossChoice? {
            return when (value?.trim()?.uppercase()) {
                "HEADS" -> HEADS
                "TAILS" -> TAILS
                else -> null
            }
        }
    }
}

enum class TossStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

enum class MatchStatus(val displayLabel: String) {
    DRAFT("Draft"),
    TEAMS_GENERATED("Teams Generated"),
    TEAMS_CONFIRMED("Teams Confirmed"),
    TOSS_COMPLETED("Toss Completed"),
    COMPLETED("Completed");

    fun canTransitionTo(next: MatchStatus): Boolean {
        return when (this) {
            DRAFT -> next == TEAMS_GENERATED
            TEAMS_GENERATED -> next == TEAMS_GENERATED || next == TEAMS_CONFIRMED
            TEAMS_CONFIRMED -> next == TEAMS_CONFIRMED || next == TOSS_COMPLETED || next == TEAMS_GENERATED
            TOSS_COMPLETED -> next == TOSS_COMPLETED || next == COMPLETED
            COMPLETED -> false
        }
    }

    companion object {
        fun fromString(value: String?): MatchStatus {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
                ?: TEAMS_GENERATED
        }
    }
}

data class TossResult(
    val id: String = UUID.randomUUID().toString(),
    val matchId: String,
    val selectedBy: String = "USER",
    val userChoice: TossChoice,
    val result: TossChoice,
    val wonToss: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val status: TossStatus = TossStatus.COMPLETED,
    val attemptNumber: Int = 1
)

data class TossHistoryRecord(
    val id: String,
    val matchId: String,
    val attemptNumber: Int,
    val userChoice: TossChoice,
    val result: TossChoice,
    val wonToss: Boolean,
    val createdAt: Long
)

data class MatchTossUiState(
    val matchId: String? = null,
    val matchStatus: MatchStatus = MatchStatus.DRAFT,
    val selectedChoice: TossChoice? = null,
    val tossStatus: TossStatus = TossStatus.NOT_STARTED,
    val pendingResult: TossChoice? = null,
    val latestTossResult: TossResult? = null,
    val matchTossHistory: List<TossHistoryRecord> = emptyList(),
    val flipSequenceNumber: Int = 0,
    val errorMessage: String? = null,
    val isStandaloneMode: Boolean = false
)

data class MatchSummaryData(
    val matchId: String,
    val sessionDbId: Int = 0,
    val matchStatus: MatchStatus,
    val teams: List<GeneratedTeam>,
    val joker: String? = null,
    val fairnessScore: Int = 0,
    val fairnessRating: String = "",
    val fairnessProfile: String = "",
    val qualityGateOutcome: String = "",
    val wasManuallyEdited: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L,
    val tossHistory: List<TossHistoryRecord> = emptyList()
) {
    val latestToss: TossHistoryRecord?
        get() = tossHistory.maxByOrNull { it.attemptNumber }
}
