package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TossEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TossDao {
    @Query("SELECT * FROM tosses ORDER BY timestamp DESC")
    fun getAllTosses(): Flow<List<TossEntity>>

    @Query("SELECT * FROM tosses WHERE matchId = :matchId ORDER BY attemptNumber ASC, timestamp ASC")
    fun getTossesForMatch(matchId: String): Flow<List<TossEntity>>

    @Query("SELECT * FROM tosses WHERE matchId = :matchId ORDER BY attemptNumber ASC, timestamp ASC")
    suspend fun getTossesForMatchSync(matchId: String): List<TossEntity>

    @Query("SELECT * FROM tosses WHERE matchId = :matchId ORDER BY attemptNumber DESC, timestamp DESC LIMIT 1")
    suspend fun getLatestTossForMatch(matchId: String): TossEntity?

    @Query("SELECT MAX(attemptNumber) FROM tosses WHERE matchId = :matchId")
    suspend fun getMaxAttemptNumberForMatch(matchId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToss(toss: TossEntity): Long

    @Query("DELETE FROM tosses WHERE matchId = :matchId")
    suspend fun deleteTossesByMatchId(matchId: String): Int

    @Query("DELETE FROM tosses")
    suspend fun clearAllTosses()
}
