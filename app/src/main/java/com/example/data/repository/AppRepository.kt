package com.example.data.repository

import com.example.data.db.LineupDao
import com.example.data.db.TossDao
import com.example.data.model.LineupEntity
import com.example.data.model.TossEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val lineupDao: LineupDao,
    private val tossDao: TossDao
) {
    val allLineups: Flow<List<LineupEntity>> = lineupDao.getAllLineups()
    val allTosses: Flow<List<TossEntity>> = tossDao.getAllTosses()

    suspend fun insertLineup(lineup: LineupEntity) {
        lineupDao.insertLineup(lineup)
    }

    suspend fun deleteLineup(id: Int) {
        lineupDao.deleteLineupById(id)
    }

    suspend fun insertToss(toss: TossEntity) {
        tossDao.insertToss(toss)
    }

    suspend fun clearTossHistory() {
        tossDao.clearAllTosses()
    }
}
