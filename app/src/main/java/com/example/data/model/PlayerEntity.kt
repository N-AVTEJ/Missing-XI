package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val nickname: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val totalMatches: Int = 0,
    val totalTimesJoker: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val skillRating: Int = 5,
    val matchesPlayed: Int = 0,
    val matchesAsJoker: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val longestGapSincePlayed: Long = 0L,
    val currentPlayStreak: Int = 0,
    val totalTeammates: Int = 0,
    val totalOpponents: Int = 0,
    val favoriteTeammateId: String? = null,
    val favoriteOpponentId: String? = null,
    val lastPlayedAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
