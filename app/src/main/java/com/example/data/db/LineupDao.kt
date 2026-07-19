package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LineupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LineupDao {
    @Query("SELECT * FROM lineups ORDER BY timestamp DESC")
    fun getAllLineups(): Flow<List<LineupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineup(lineup: LineupEntity)

    @Query("DELETE FROM lineups WHERE id = :id")
    suspend fun deleteLineupById(id: Int)
}
