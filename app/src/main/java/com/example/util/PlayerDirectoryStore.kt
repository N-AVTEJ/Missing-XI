package com.example.util

import com.example.data.model.PlayerEntity
import com.example.data.model.SessionEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Dedicated store managing Player Directory search/filter/sort state (preserved across navigation),
 * Individual Player Profile analytics, validated Skill Rating updates, and safe Player Archiving.
 *
 * Keeps all database queries and aggregation logic cleanly outside UI composables.
 */
class PlayerDirectoryStore(
    private val repository: AppRepository,
    scope: CoroutineScope? = null
) {
    private val _filter = MutableStateFlow(PlayerDirectoryFilter())
    val filter: StateFlow<PlayerDirectoryFilter> = _filter.asStateFlow()

    private val _selectedPlayerId = MutableStateFlow<String?>(null)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _directoryUiState = MutableStateFlow(PlayerDirectoryUiState())
    private val _profileUiState = MutableStateFlow(PlayerProfileUiState())

    val directoryUiState: StateFlow<PlayerDirectoryUiState> = if (scope != null) {
        combine(
            repository.allPlayers,
            repository.allSessions,
            _filter,
            _errorMessage
        ) { players, sessions, activeFilter, errorMsg ->
            buildDirectoryUiState(players, sessions, activeFilter, errorMsg)
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), PlayerDirectoryUiState(isLoading = true))
    } else {
        _directoryUiState.asStateFlow()
    }

    val profileUiState: StateFlow<PlayerProfileUiState> = if (scope != null) {
        combine(
            repository.allPlayers,
            repository.allSessions,
            _selectedPlayerId,
            _statusBannerMessage,
            _errorMessage
        ) { players, sessions, playerId, banner, errorMsg ->
            buildProfileUiState(players, sessions, playerId, banner, errorMsg)
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), PlayerProfileUiState())
    } else {
        _profileUiState.asStateFlow()
    }

    fun setSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun setStatusFilter(statusFilter: PlayerStatusFilter) {
        _filter.value = _filter.value.copy(statusFilter = statusFilter)
    }

    fun setSortOption(sortOption: PlayerDirectorySortOption) {
        _filter.value = _filter.value.copy(sortOption = sortOption)
    }

    fun clearDirectoryFilters() {
        val currentSort = _filter.value.sortOption
        _filter.value = PlayerDirectoryFilter(sortOption = currentSort)
    }

    fun selectPlayerProfile(playerId: String) {
        if (_selectedPlayerId.value != playerId) {
            _statusBannerMessage.value = null
            _errorMessage.value = null
        }
        _selectedPlayerId.value = playerId
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    suspend fun refreshSync(): PlayerDirectoryUiState {
        return try {
            val players = repository.getAllPlayersSync()
            val sessions = repository.getAllSessionsSync()
            val dirState = buildDirectoryUiState(players, sessions, _filter.value, null)
            _directoryUiState.value = dirState

            val selectedId = _selectedPlayerId.value
            if (!selectedId.isNullOrBlank()) {
                _profileUiState.value = buildProfileUiState(
                    players = players,
                    sessions = sessions,
                    playerId = selectedId,
                    banner = _statusBannerMessage.value,
                    errorMsg = null
                )
            }
            dirState
        } catch (e: Exception) {
            val msg = "Could not load players: ${e.message ?: "Database error"}"
            _errorMessage.value = msg
            val failed = _directoryUiState.value.copy(isLoading = false, errorMessage = msg)
            _directoryUiState.value = failed
            failed
        }
    }

    suspend fun loadPlayerProfileSync(playerId: String): PlayerProfileAnalytics? {
        selectPlayerProfile(playerId)
        val players = repository.getAllPlayersSync()
        val sessions = repository.getAllSessionsSync()
        val state = buildProfileUiState(
            players = players,
            sessions = sessions,
            playerId = playerId,
            banner = _statusBannerMessage.value,
            errorMsg = null
        )
        _profileUiState.value = state
        return state.analytics
    }

    suspend fun updatePlayerSkillRating(playerId: String, newRating: Int): Result<Int> {
        val validation = PlayerAnalyticsService.validateSkillRating(newRating)
        if (validation.isFailure) {
            val errMsg = validation.exceptionOrNull()?.message ?: "Invalid skill rating."
            _errorMessage.value = errMsg
            return validation
        }
        return try {
            val updated = repository.updatePlayerSkillRatingTransactionally(playerId, newRating)
            if (updated) {
                _errorMessage.value = null
                _statusBannerMessage.value = "Skill rating updated to $newRating/10 for future matches."
                refreshSync()
                Result.success(newRating)
            } else {
                val err = IllegalStateException("Player not found.")
                _errorMessage.value = err.message
                Result.failure(err)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update skill rating: ${e.message ?: "Database error"}"
            Result.failure(e)
        }
    }

    suspend fun archivePlayer(playerId: String): Boolean {
        return try {
            val updated = repository.setPlayerArchivedTransactionally(playerId, isArchived = true)
            if (updated) {
                _errorMessage.value = null
                _statusBannerMessage.value = "Player archived. Historical match records and stats are preserved."
                refreshSync()
            }
            updated
        } catch (e: Exception) {
            _errorMessage.value = "Failed to archive player: ${e.message ?: "Database error"}"
            false
        }
    }

    suspend fun restorePlayer(playerId: String): Boolean {
        return try {
            val updated = repository.setPlayerArchivedTransactionally(playerId, isArchived = false)
            if (updated) {
                _errorMessage.value = null
                _statusBannerMessage.value = "Player restored to active roster."
                refreshSync()
            }
            updated
        } catch (e: Exception) {
            _errorMessage.value = "Failed to restore player: ${e.message ?: "Database error"}"
            false
        }
    }

    suspend fun createPlayer(
        displayName: String,
        skillRating: Int = PlayerAnalyticsService.DEFAULT_SKILL_RATING,
        nickname: String? = null
    ): Result<PlayerEntity> {
        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Player name cannot be empty."))
        }
        val ratingValidation = PlayerAnalyticsService.validateSkillRating(skillRating)
        if (ratingValidation.isFailure) {
            return Result.failure(ratingValidation.exceptionOrNull()!!)
        }
        val existing = repository.getPlayerByName(trimmedName)
        if (existing != null) {
            return Result.failure(IllegalStateException("A player named \"$trimmedName\" already exists."))
        }
        return try {
            val created = repository.createPlayerTransactionally(
                displayName = trimmedName,
                skillRating = skillRating,
                nickname = nickname?.trim()?.takeIf { it.isNotBlank() }
            )
            refreshSync()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePlayerSafely(playerId: String): PlayerRemovalResult {
        val result = repository.deletePlayerSafely(playerId)
        if (result is PlayerRemovalResult.ArchivedBecauseReferenced) {
            _statusBannerMessage.value = "Player has match history and was archived instead of deleted."
        }
        refreshSync()
        return result
    }

    suspend fun getSelectablePlayersForNewMatch(): List<PlayerEntity> {
        return PlayerAnalyticsService.filterSelectablePlayersForTeamGeneration(
            repository.getAllPlayersSync()
        )
    }

    private fun buildDirectoryUiState(
        players: List<PlayerEntity>,
        sessions: List<SessionEntity>,
        activeFilter: PlayerDirectoryFilter,
        errorMsg: String?
    ): PlayerDirectoryUiState {
        val items = PlayerAnalyticsService.buildDirectoryItems(
            allPlayers = players,
            allSessions = sessions,
            filter = activeFilter
        )
        val activeCount = players.count { !it.isArchived }
        val archivedCount = players.count { it.isArchived }

        return PlayerDirectoryUiState(
            items = items,
            totalPlayersCount = players.size,
            totalActiveCount = activeCount,
            totalArchivedCount = archivedCount,
            filter = activeFilter,
            isLoading = false,
            errorMessage = errorMsg
        )
    }

    private fun buildProfileUiState(
        players: List<PlayerEntity>,
        sessions: List<SessionEntity>,
        playerId: String?,
        banner: String?,
        errorMsg: String?
    ): PlayerProfileUiState {
        if (playerId.isNullOrBlank()) {
            return PlayerProfileUiState(
                analytics = null,
                isLoading = false,
                errorMessage = errorMsg,
                statusBannerMessage = banner
            )
        }

        val analytics = PlayerAnalyticsService.computePlayerProfileAnalytics(
            playerId = playerId,
            allPlayers = players,
            allSessions = sessions
        )

        return PlayerProfileUiState(
            analytics = analytics,
            isLoading = false,
            errorMessage = errorMsg ?: if (analytics == null) "Player not found." else null,
            statusBannerMessage = banner
        )
    }
}
