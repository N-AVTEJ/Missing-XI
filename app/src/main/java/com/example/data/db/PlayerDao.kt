package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE isArchived = 0 ORDER BY displayName ASC")
    fun getAllActivePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE isArchived = 0 ORDER BY lastUsedAt DESC")
    fun getRecentlyUsedPlayers(): Flow<List<PlayerEntity>>
    
    @Query("SELECT * FROM players WHERE displayName = :displayName LIMIT 1")
    suspend fun getPlayerByName(displayName: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: String): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deletePlayerById(id: String)
}
