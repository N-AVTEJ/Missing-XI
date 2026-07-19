package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lineups")
data class LineupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamName: String,
    val formation: String, // e.g. "4-3-3", "4-4-2"
    val sportType: String, // "Football" or "Cricket"
    val playersJson: String, // Comma separated or JSON formatted list of players
    val timestamp: Long = System.currentTimeMillis()
)
