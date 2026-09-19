package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
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
    val jokerFairnessScore: Double = 0.0
)
