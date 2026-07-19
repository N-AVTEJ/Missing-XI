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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToss(toss: TossEntity)

    @Query("DELETE FROM tosses")
    suspend fun clearAllTosses()
}
