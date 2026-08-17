package com.example

import com.example.data.model.PlayerEntity
import com.example.util.PlayerChemistryEngine
import com.example.util.PlayerSortOption
import org.junit.Assert.*
import org.junit.Test

class PlayerStatisticsEngineTest {

    @Test
    fun testFavoriteTeammateCalculation() {
        val pairCounts = mapOf(
            "Rahul|Ajay" to 18,
            "Kiran|Rahul" to 5,
            "Ajay|Kiran" to 10
        )

        val favorite = PlayerChemistryEngine.getFavoriteTeammate("Rahul", pairCounts)
        assertNotNull(favorite)
        assertEquals("Ajay", favorite?.idOrName)
        assertEquals(18, favorite?.matchCount)
    }

    @Test
    fun testFavoriteOpponentCalculation() {
        val opponentPairCounts = mapOf(
            "Rahul|Suresh" to 22,
            "Rahul|Kiran" to 14,
            "Ajay|Suresh" to 8
        )

        val favoriteOpponent = PlayerChemistryEngine.getFavoriteOpponent("Rahul", opponentPairCounts)
        assertNotNull(favoriteOpponent)
        assertEquals("Suresh", favoriteOpponent?.idOrName)
        assertEquals(22, favoriteOpponent?.matchCount)
    }

    @Test
    fun testTotalTeammatesAndOpponentsCounts() {
        val teammatePairCounts = mapOf(
            "Rahul|Ajay" to 5,
            "Kiran|Rahul" to 3,
            "Rahul|Vikram" to 2
        )

        val totalTeammates = PlayerChemistryEngine.getTotalTeammatesCount("Rahul", teammatePairCounts)
        assertEquals(3, totalTeammates)

        val opponentPairCounts = mapOf(
            "Rahul|Suresh" to 4,
            "Rahul|Dinesh" to 1
        )
        val totalOpponents = PlayerChemistryEngine.getTotalOpponentsCount("Rahul", opponentPairCounts)
        assertEquals(2, totalOpponents)
    }

    @Test
    fun testDashboardStatsCalculation() {
        val p1 = PlayerEntity(id = "1", displayName = "Rahul", skillRating = 9, matchesPlayed = 20, matchesAsJoker = 2)
        val p2 = PlayerEntity(id = "2", displayName = "Ajay", skillRating = 7, matchesPlayed = 15, matchesAsJoker = 5)
        val p3 = PlayerEntity(id = "3", displayName = "Kiran", skillRating = 5, matchesPlayed = 5, matchesAsJoker = 0)

        val players = listOf(p1, p2, p3)
        val stats = PlayerChemistryEngine.calculateDashboardStats(players, totalSessionsGenerated = 10)

        assertEquals("Rahul", stats.mostActivePlayer?.displayName)
        assertEquals("Kiran", stats.leastActivePlayer?.displayName)
        assertEquals("Rahul", stats.highestSkillPlayer?.displayName)
        assertEquals("Ajay", stats.mostFrequentJoker?.displayName)
        assertEquals(3, stats.totalRegisteredPlayers)
        assertEquals(10, stats.totalMatchesGenerated)
        assertEquals(13.333, stats.averageMatchesPerPlayer, 0.01)
        assertEquals(7.0, stats.averageSkillRating, 0.01)
    }

    @Test
    fun testPlayerSortingOptions() {
        val p1 = PlayerEntity(id = "1", displayName = "Rahul", skillRating = 9, matchesPlayed = 20, isFavorite = false, lastPlayedAt = 100L)
        val p2 = PlayerEntity(id = "2", displayName = "Ajay", skillRating = 7, matchesPlayed = 5, isFavorite = true, lastPlayedAt = 500L)
        val p3 = PlayerEntity(id = "3", displayName = "Bala", skillRating = 10, matchesPlayed = 12, isFavorite = false, lastPlayedAt = 300L)

        val players = listOf(p1, p2, p3)

        val alphaSorted = PlayerChemistryEngine.sortPlayers(players, PlayerSortOption.ALPHABETICAL)
        assertEquals(listOf("Ajay", "Bala", "Rahul"), alphaSorted.map { it.displayName })

        val recentSorted = PlayerChemistryEngine.sortPlayers(players, PlayerSortOption.RECENTLY_PLAYED)
        assertEquals(listOf("Ajay", "Bala", "Rahul"), recentSorted.map { it.displayName })

        val matchesSorted = PlayerChemistryEngine.sortPlayers(players, PlayerSortOption.MOST_MATCHES)
        assertEquals(listOf("Rahul", "Bala", "Ajay"), matchesSorted.map { it.displayName })

        val skillSorted = PlayerChemistryEngine.sortPlayers(players, PlayerSortOption.HIGHEST_SKILL)
        assertEquals(listOf("Bala", "Rahul", "Ajay"), skillSorted.map { it.displayName })

        val favoritesSorted = PlayerChemistryEngine.sortPlayers(players, PlayerSortOption.FAVORITES_FIRST)
        assertEquals("Ajay", favoritesSorted.first().displayName)

        val leastActiveSorted = PlayerChemistryEngine.sortPlayers(players, PlayerSortOption.LEAST_ACTIVE)
        assertEquals("Ajay", leastActiveSorted.first().displayName)
    }
}
