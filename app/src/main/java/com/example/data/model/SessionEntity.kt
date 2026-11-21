package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["matchStatus"]),
        Index(value = ["completedAt"]),
        Index(value = ["matchId"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerIdsJson: String, // Ordered JSON array of player names (or IDs)
    val teamCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val overallFairnessScore: Double = 0.0,
    val fairnessRating: String = "",
    val teammateVarietyScore: Double = 0.0,
    val opponentVarietyScore: Double = 0.0,
    val teamStrengthScore: Double = 0.0,
    val jokerFairnessScore: Double = 0.0,
    val fairnessProfile: String = "Legacy Result",
    val settingsSnapshotJson: String = "",
    val fairnessThresholdUsed: Double = 70.0,
    val qualityGateOutcome: String = "",
    val lockedPlayerIdsJson: String = "[]",
    val fixedGroupsJson: String = "[]",
    val excludedPlayerIdsJson: String = "[]",
    val wasManuallyEdited: Boolean = false,
    val originalArrangementJson: String = "",
    val matchId: String = UUID.randomUUID().toString(),
    val matchStatus: String = "TEAMS_GENERATED",
    val finalTeamsJson: String = "",
    val completedAt: Long = 0L,
    val latestTossChoice: String = "",
    val latestTossResult: String = "",
    val latestTossWon: Boolean = false,
    val matchNumber: Int? = null,
    val totalEligiblePlayers: Int = 0,
    val teammatePenalty: Int? = null,
    val opponentPenalty: Int? = null,
    val teamStrengthDifference: Int? = null,
    val excludedPlayerNamesJson: String = "[]",
    val hasFairnessBreakdown: Boolean = false
)
