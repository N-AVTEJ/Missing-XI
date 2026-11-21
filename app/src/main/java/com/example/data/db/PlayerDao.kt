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
    @Query("SELECT * FROM players ORDER BY displayName ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players ORDER BY displayName ASC")
    suspend fun getAllPlayersSync(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE isArchived = 0 ORDER BY displayName ASC")
    fun getAllActivePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE isArchived = 1 ORDER BY displayName ASC")
    fun getArchivedPlayers(): Flow<List<PlayerEntity>>

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

    @Query("UPDATE players SET skillRating = :skillRating, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSkillRating(id: String, skillRating: Int, updatedAt: Long): Int

    @Query("UPDATE players SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPlayerArchived(id: String, isArchived: Boolean, updatedAt: Long): Int

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deletePlayerById(id: String)
}
