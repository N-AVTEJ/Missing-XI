package com.example.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.data.db.AppDatabase
import com.example.data.db.LineupDao
import com.example.data.db.PlayerDao
import com.example.data.db.SessionDao
import com.example.data.db.TossDao
import com.example.data.model.LineupEntity
import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity
import com.example.data.model.TossEntity
import kotlinx.coroutines.flow.Flow

open class AppRepository(
    private val lineupDao: LineupDao,
    private val tossDao: TossDao,
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao,
    private val database: AppDatabase? = null
) {
    val allLineups: Flow<List<LineupEntity>> = lineupDao.getAllLineups()
    val allTosses: Flow<List<TossEntity>> = tossDao.getAllTosses()
    val allActivePlayers: Flow<List<PlayerEntity>> = playerDao.getAllActivePlayers()
    val recentlyUsedPlayers: Flow<List<PlayerEntity>> = playerDao.getRecentlyUsedPlayers()
    val sessionCount: Flow<Int> = sessionDao.getSessionCount()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun insertLineup(lineup: LineupEntity) {
        lineupDao.insertLineup(lineup)
    }

    suspend fun deleteLineup(id: Int) {
        lineupDao.deleteLineupById(id)
    }

    open suspend fun insertToss(toss: TossEntity): Long {
        return tossDao.insertToss(toss)
    }

    fun getTossesForMatch(matchId: String): Flow<List<TossEntity>> {
        return tossDao.getTossesForMatch(matchId)
    }

    open suspend fun getTossesForMatchSync(matchId: String): List<TossEntity> {
        return tossDao.getTossesForMatchSync(matchId)
    }

    open suspend fun getLatestTossForMatch(matchId: String): TossEntity? {
        return tossDao.getLatestTossForMatch(matchId)
    }

    open suspend fun getMaxAttemptNumberForMatch(matchId: String): Int {
        return tossDao.getMaxAttemptNumberForMatch(matchId) ?: 0
    }

    suspend fun clearTossHistory() {
        tossDao.clearAllTosses()
    }

    suspend fun getPlayerByName(name: String): PlayerEntity? {
        return playerDao.getPlayerByName(name)
    }

    open suspend fun getPlayerById(id: String): PlayerEntity? {
        return playerDao.getPlayerById(id)
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

    open suspend fun getSessionByMatchId(matchId: String): SessionEntity? {
        return sessionDao.getSessionByMatchId(matchId)
    }

    open suspend fun getSessionById(id: Int): SessionEntity? {
        return sessionDao.getSessionById(id)
    }

    open suspend fun getNextMatchNumber(): Int {
        return (sessionDao.getMaxMatchNumber() ?: 0) + 1
    }

    open suspend fun getDistinctFairnessProfiles(): List<String> {
        return sessionDao.getDistinctFairnessProfiles()
    }

    open suspend fun querySessionsPaginated(query: SupportSQLiteQuery): List<SessionEntity> {
        return sessionDao.querySessionsPaginated(query)
    }

    open suspend fun countSessionsFiltered(query: SupportSQLiteQuery): Int {
        return sessionDao.countSessionsFiltered(query)
    }

    /**
     * Atomically deletes ONLY the specified match record and its associated toss attempts.
     * Preserves all player profiles, global fairness settings, and unrelated matches.
     * Historical aggregate player career statistics are intentionally preserved to avoid
     * introducing inconsistent pair or career totals.
     */
    open suspend fun deleteMatchTransactionally(matchId: String): Boolean {
        if (matchId.isBlank()) return false
        return if (database != null) {
            database.withTransaction {
                tossDao.deleteTossesByMatchId(matchId)
                sessionDao.deleteSessionByMatchId(matchId) > 0
            }
        } else {
            tossDao.deleteTossesByMatchId(matchId)
            sessionDao.deleteSessionByMatchId(matchId) > 0
        }
    }

    open suspend fun insertSession(session: SessionEntity): Long {
        return sessionDao.insertSession(session)
    }

    open suspend fun updateSession(session: SessionEntity) {
        sessionDao.updateSession(session)
    }
}
