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
    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()
    val allActivePlayers: Flow<List<PlayerEntity>> = playerDao.getAllActivePlayers()
    val archivedPlayers: Flow<List<PlayerEntity>> = playerDao.getArchivedPlayers()
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

    open suspend fun getAllPlayersSync(): List<PlayerEntity> {
        return playerDao.getAllPlayersSync()
    }

    open suspend fun getAllSessionsSync(): List<SessionEntity> {
        return sessionDao.getAllSessionsSync()
    }

    suspend fun insertPlayer(player: PlayerEntity) {
        playerDao.insertPlayer(player)
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    /**
     * Updates a player's skill rating (validated 1..10) transactionally without modifying
     * any historical match records or stored historical fairness/strength values.
     */
    open suspend fun updatePlayerSkillRatingTransactionally(
        playerId: String,
        skillRating: Int,
        updatedAt: Long = System.currentTimeMillis()
    ): Boolean {
        val validated = com.example.util.PlayerAnalyticsService.validateSkillRating(skillRating).getOrThrow()
        return if (database != null) {
            database.withTransaction {
                playerDao.updateSkillRating(playerId, validated, updatedAt) > 0
            }
        } else {
            playerDao.updateSkillRating(playerId, validated, updatedAt) > 0
        }
    }

    /**
     * Archives or restores a player transactionally while preserving all historical relationships
     * and match records.
     */
    open suspend fun setPlayerArchivedTransactionally(
        playerId: String,
        isArchived: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    ): Boolean {
        return if (database != null) {
            database.withTransaction {
                playerDao.setPlayerArchived(playerId, isArchived, updatedAt) > 0
            }
        } else {
            playerDao.setPlayerArchived(playerId, isArchived, updatedAt) > 0
        }
    }

    open suspend fun createPlayerTransactionally(
        displayName: String,
        skillRating: Int = com.example.util.PlayerAnalyticsService.DEFAULT_SKILL_RATING,
        nickname: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ): PlayerEntity {
        val validated = com.example.util.PlayerAnalyticsService.validateSkillRating(skillRating).getOrThrow()
        val entity = PlayerEntity(
            displayName = displayName.trim(),
            nickname = nickname?.trim()?.takeIf { it.isNotBlank() },
            createdAt = createdAt,
            lastUsedAt = 0L,
            lastPlayedAt = 0L,
            updatedAt = createdAt,
            skillRating = validated
        )
        if (database != null) {
            database.withTransaction {
                playerDao.insertPlayer(entity)
            }
        } else {
            playerDao.insertPlayer(entity)
        }
        return entity
    }

    /**
     * Safely handles player removal: if the player is referenced by ANY historical match record,
     * archives the player instead of permanently deleting them so historical records stay intact.
     */
    open suspend fun deletePlayerSafely(
        playerId: String,
        updatedAt: Long = System.currentTimeMillis()
    ): com.example.util.PlayerRemovalResult {
        val action: suspend () -> com.example.util.PlayerRemovalResult = {
            val existing = playerDao.getPlayerById(playerId)
            if (existing == null) {
                com.example.util.PlayerRemovalResult.NotFound
            } else {
                val allPlayers = playerDao.getAllPlayersSync()
                val allSessions = sessionDao.getAllSessionsSync()
                val isReferenced = com.example.util.PlayerAnalyticsService.isPlayerReferencedInAnyMatch(
                    playerId = playerId,
                    allPlayers = allPlayers,
                    allSessions = allSessions
                )
                if (isReferenced) {
                    playerDao.setPlayerArchived(playerId, true, updatedAt)
                    com.example.util.PlayerRemovalResult.ArchivedBecauseReferenced(playerId)
                } else {
                    playerDao.deletePlayerById(playerId)
                    com.example.util.PlayerRemovalResult.Deleted(playerId)
                }
            }
        }
        return if (database != null) {
            database.withTransaction { action() }
        } else {
            action()
        }
    }

    suspend fun deletePlayer(id: String) {
        deletePlayerSafely(id)
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
