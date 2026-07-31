package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerIdsJson: String, // Ordered JSON array of player names (or IDs)
    val teamCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
