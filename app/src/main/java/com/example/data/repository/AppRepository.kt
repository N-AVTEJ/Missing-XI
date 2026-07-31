package com.example.data.repository

import com.example.data.db.LineupDao
import com.example.data.db.PlayerDao
import com.example.data.db.SessionDao
import com.example.data.db.TossDao
import com.example.data.model.LineupEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity
import com.example.data.model.TossEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val lineupDao: LineupDao,
    private val tossDao: TossDao,
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao
) {
    val allLineups: Flow<List<LineupEntity>> = lineupDao.getAllLineups()
    val allTosses: Flow<List<TossEntity>> = tossDao.getAllTosses()
    val allActivePlayers: Flow<List<PlayerEntity>> = playerDao.getAllActivePlayers()
    val recentlyUsedPlayers: Flow<List<PlayerEntity>> = playerDao.getRecentlyUsedPlayers()

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

    suspend fun getPlayerByName(name: String): PlayerEntity? {
        return playerDao.getPlayerByName(name)
    }

    suspend fun insertPlayer(player: PlayerEntity) {
        playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }
    
    suspend fun deletePlayer(id: String) {
        playerDao.deletePlayerById(id)
    }

    suspend fun getLatestSession(): SessionEntity? {
        return sessionDao.getLatestSession()
    }

    suspend fun insertSession(session: SessionEntity) {
        sessionDao.insertSession(session)
    }
}
