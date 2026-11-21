package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?
    
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions")
    fun getSessionCount(): Flow<Int>

    @Query("SELECT * FROM sessions WHERE matchId = :matchId LIMIT 1")
    suspend fun getSessionByMatchId(matchId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Int): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)
}
