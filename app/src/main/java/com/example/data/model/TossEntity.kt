package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tosses")
data class TossEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val choice: String, // "Heads" or "Tails"
    val result: String, // "Heads" or "Tails"
    val hasWon: Boolean
)
