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
    val isArchived: Boolean = false
)
