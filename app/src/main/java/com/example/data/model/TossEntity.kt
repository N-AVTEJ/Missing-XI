package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tosses")
data class TossEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val choice: String, // "Heads" or "Tails"
    val result: String, // "Heads" or "Tails"
    val hasWon: Boolean,
    val tossUuid: String = UUID.randomUUID().toString(),
    val matchId: String = "",
    val attemptNumber: Int = 1,
    val selectedBy: String = "USER",
    val status: String = "COMPLETED"
)
