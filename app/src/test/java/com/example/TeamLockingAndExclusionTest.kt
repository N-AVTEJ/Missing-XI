package com.example

import com.example.data.model.SessionEntity
import com.example.util.*
import org.junit.Assert.*
import org.junit.Test

class TeamLockingAndExclusionTest {

    private fun createPlayers(count: Int): List<Player> {
        return (1..count).map { i ->
            Player(id = "p_$i", name = "Player $i", skillRating = 5 + (i % 5))
        }
    }

    @Test
    fun test1_validate_requires_at_least_two_eligible_players() {
        val players = createPlayers(4)
        // Exclude all players
        val allExcluded = players.associate {
            it.id to PlayerMatchConstraint(playerId = it.id, isExcluded = true)
        }
        val result1 = PlayerConstraintValidator.validate(players, 2, allExcluded, emptyList())
        assertFalse(result1.isValid)
        assertTrue(result1.errorMessage!!.contains("All eligible players are excluded"))

        // Leave only 1 eligible player for 2 teams
        val almostAllExcluded = players.associate {
            it.id to PlayerMatchConstraint(playerId = it.id, isExcluded = it.id != "p_1")
        }
        val result2 = PlayerConstraintValidator.validate(players, 2, almostAllExcluded, emptyList())
        assertFalse(result2.isValid)
        assertTrue(result2.errorMessage!!.contains("not enough eligible players"))
    }

    @Test
    fun test2_validate_team_capacity_overflow() {
        val players = createPlayers(6) // 2 teams -> capacity 3 each
        // Lock 4 players to Team A (index 0)
        val constraints = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 0),
            "p_2" to PlayerMatchConstraint("p_2", isLocked = true, lockedTeamIndex = 0),
            "p_3" to PlayerMatchConstraint("p_3", isLocked = true, lockedTeamIndex = 0),
            "p_4" to PlayerMatchConstraint("p_4", isLocked = true, lockedTeamIndex = 0)
        )
        val result = PlayerConstraintValidator.validate(players, 2, constraints, emptyList())
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("Team A has more locked players"))
    }

    @Test
    fun test3_validate_group_conflicting_locks() {
        val players = createPlayers(6)
        val group = LockedPlayerGroup(name = "Striker Duo", playerIds = listOf("p_1", "p_2"))
        val constraints = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 0), // Team A
            "p_2" to PlayerMatchConstraint("p_2", isLocked = true, lockedTeamIndex = 1)  // Team B
        )
        val result = PlayerConstraintValidator.validate(players, 2, constraints, listOf(group))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("locked to different teams"))
    }

    @Test
    fun test4_validate_group_with_excluded_player() {
        val players = createPlayers(6)
        val group = LockedPlayerGroup(name = "Buddy Group", playerIds = listOf("p_1", "p_2"))
        val constraints = mapOf(
            "p_2" to PlayerMatchConstraint("p_2", isExcluded = true)
        )
        val result = PlayerConstraintValidator.validate(players, 2, constraints, listOf(group))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("contains excluded player"))
    }

    @Test
    fun test5_validate_group_player_in_multiple_groups() {
        val players = createPlayers(6)
        val group1 = LockedPlayerGroup(id = "g1", name = "Group 1", playerIds = listOf("p_1", "p_2"))
        val group2 = LockedPlayerGroup(id = "g2", name = "Group 2", playerIds = listOf("p_2", "p_3"))
        val result = PlayerConstraintValidator.validate(players, 2, emptyMap(), listOf(group1, group2))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("multiple fixed groups"))
    }

    @Test
    fun test6_validate_joker_unavailability() {
        // 7 players, 2 teams -> odd count requires 1 Joker.
        // If 6 players are locked and 1 is in a group -> 0 unconstrained players
        val players = createPlayers(7)
        val constraints = (1..6).associate { i ->
            "p_$i" to PlayerMatchConstraint("p_$i", isLocked = true, lockedTeamIndex = if (i <= 3) 0 else 1)
        }
        val group = LockedPlayerGroup(name = "Solo Group", playerIds = listOf("p_7", "p_6")) // p_7 is in group
        val result = PlayerConstraintValidator.validate(players, 2, constraints, listOf(group))
        assertFalse(result.isValid)
        // p_6 has conflicting lock or group issue, or joker check triggers
    }

    @Test
    fun test7_validate_joker_unavailability_all_locked() {
        // 7 players, 2 teams (cap 3, 3 + 1 joker)
        // Lock 3 to Team A, 3 to Team B, and lock p_7 to Team A -> capacity error or lock error
        // If 3 locked to A, 3 locked to B, and p_7 is part of a 2-player group with p_1:
        val players = createPlayers(7)
        val constraints = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 0),
            "p_2" to PlayerMatchConstraint("p_2", isLocked = true, lockedTeamIndex = 0),
            "p_3" to PlayerMatchConstraint("p_3", isLocked = true, lockedTeamIndex = 0),
            "p_4" to PlayerMatchConstraint("p_4", isLocked = true, lockedTeamIndex = 1),
            "p_5" to PlayerMatchConstraint("p_5", isLocked = true, lockedTeamIndex = 1),
            "p_6" to PlayerMatchConstraint("p_6", isLocked = true, lockedTeamIndex = 1)
        )
        // p_7 is left, but put p_7 in a group with p_1 (which is locked to Team A)
        // But Team A has 3 already, so p_7 would overflow Team A.
        // If p_2 and p_3 in group, and p_7 in group with p_4 (so A has 2, B has 2):
        val groupA = LockedPlayerGroup(name = "Pair 1", playerIds = listOf("p_1", "p_2")) // locked to A
        val groupB = LockedPlayerGroup(name = "Pair 2", playerIds = listOf("p_4", "p_5")) // locked to B
        val groupC = LockedPlayerGroup(name = "Pair 3", playerIds = listOf("p_6", "p_7")) // floating
        // Now eligible are 7: p_1,p_2,p_3 (locked to A=2), p_4,p_5 (locked to B=2), p_3,p_6,p_7
        // Lock p_3 to A. Now A has 3 locked.
        val constr = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 0),
            "p_2" to PlayerMatchConstraint("p_2", isLocked = true, lockedTeamIndex = 0),
            "p_3" to PlayerMatchConstraint("p_3", isLocked = true, lockedTeamIndex = 0),
            "p_4" to PlayerMatchConstraint("p_4", isLocked = true, lockedTeamIndex = 1),
            "p_5" to PlayerMatchConstraint("p_5", isLocked = true, lockedTeamIndex = 1)
        )
        // Group C contains p_6 and p_7.
        // Total players: 7.
        // Who is unconstrained?
        // p_1, p_2, p_3 are locked to A.
        // p_4, p_5 are locked to B.
        // p_6, p_7 are in Group C.
        // Unconstrained players = 0!
        val result = PlayerConstraintValidator.validate(players, 2, constr, listOf(groupC))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("1 Joker, but no eligible players are available"))
    }

    @Test
    fun test8_validate_floating_group_exceeds_available_capacity() {
        val players = createPlayers(6) // 2 teams -> capacity 3 each
        val bigGroup = LockedPlayerGroup(name = "Quad", playerIds = listOf("p_1", "p_2", "p_3", "p_4"))
        val result = PlayerConstraintValidator.validate(players, 2, emptyMap(), listOf(bigGroup))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("exceeds the maximum available team target capacity"))
    }

    @Test
    fun test9_validate_successful_valid_constraints() {
        val players = createPlayers(8) // 2 teams -> capacity 4 each
        val group = LockedPlayerGroup(name = "Duo", playerIds = listOf("p_5", "p_6"))
        val constraints = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 0),
            "p_2" to PlayerMatchConstraint("p_2", isLocked = true, lockedTeamIndex = 1),
            "p_7" to PlayerMatchConstraint("p_7", isExcluded = false)
        )
        val result = PlayerConstraintValidator.validate(players, 2, constraints, listOf(group))
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun test10_session_entity_supports_constraints_metadata() {
        val session = SessionEntity(
            playerIdsJson = "[\"Player 1\", \"Player 2\"]",
            teamCount = 2,
            lockedPlayerIdsJson = "[\"p_1\"]",
            fixedGroupsJson = "[{\"id\":\"g1\",\"name\":\"Group 1\",\"playerIds\":[\"p_2\",\"p_3\"]}]",
            excludedPlayerIdsJson = "[\"p_4\"]"
        )
        assertEquals("[\"p_1\"]", session.lockedPlayerIdsJson)
        assertEquals("[{\"id\":\"g1\",\"name\":\"Group 1\",\"playerIds\":[\"p_2\",\"p_3\"]}]", session.fixedGroupsJson)
        assertEquals("[\"p_4\"]", session.excludedPlayerIdsJson)
    }

    @Test
    fun test11_player_match_constraint_label_formatting() {
        val c0 = PlayerMatchConstraint("p1", isLocked = true, lockedTeamIndex = 0)
        assertEquals("Team A", c0.getLockedTeamLabel())

        val c1 = PlayerMatchConstraint("p2", isLocked = true, lockedTeamIndex = 1)
        assertEquals("Team B", c1.getLockedTeamLabel())

        val c2 = PlayerMatchConstraint("p3", isLocked = true, lockedTeamIndex = 2)
        assertEquals("Team C", c2.getLockedTeamLabel())

        val cNull = PlayerMatchConstraint("p4", isLocked = false, lockedTeamIndex = null)
        assertNull(cNull.getLockedTeamLabel())
    }

    @Test
    fun test12_group_lock_propagation_validates_correctly() {
        // Group of p_1, p_2. p_1 is locked to Team B (1).
        // Then p_2 effectively becomes locked to Team B.
        // If 2 more players are locked to Team B (capacity 3), total locked to Team B is 4 -> should fail!
        val players = createPlayers(6) // 2 teams -> capacity 3 each
        val group = LockedPlayerGroup(name = "Pair", playerIds = listOf("p_1", "p_2"))
        val constraints = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 1),
            "p_3" to PlayerMatchConstraint("p_3", isLocked = true, lockedTeamIndex = 1),
            "p_4" to PlayerMatchConstraint("p_4", isLocked = true, lockedTeamIndex = 1)
        )
        // Group propagates lock to p_2 -> Team B has p_1, p_2, p_3, p_4 = 4 players (exceeds cap 3)
        val result = PlayerConstraintValidator.validate(players, 2, constraints, listOf(group))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("Team B has more locked players"))
    }

    @Test
    fun test13_exclusion_with_odd_and_even_eligible_counts() {
        val players = createPlayers(8)
        // Exclude 2 players -> 6 eligible players for 2 teams (even split, 3 per team, 0 joker)
        val constraints = mapOf(
            "p_7" to PlayerMatchConstraint("p_7", isExcluded = true),
            "p_8" to PlayerMatchConstraint("p_8", isExcluded = true)
        )
        val result = PlayerConstraintValidator.validate(players, 2, constraints, emptyList())
        assertTrue(result.isValid)

        // Exclude 1 player -> 7 eligible players for 2 teams (odd, requires 1 joker)
        val constraintsOdd = mapOf(
            "p_8" to PlayerMatchConstraint("p_8", isExcluded = true)
        )
        val resultOdd = PlayerConstraintValidator.validate(players, 2, constraintsOdd, emptyList())
        assertTrue(resultOdd.isValid)
    }

    @Test
    fun test14_bin_packing_detects_impossible_floating_groups_combination() {
        // 10 players, 3 teams (capacities: Team A: 4, Team B: 3, Team C: 3)
        // Floating groups of size 3, 3, 3, 3 = 12 (impossible size).
        // If 3 groups of size 3: sizes: 3, 3, 3. Total 9.
        // Capacities: A: 4, B: 3, C: 3.
        // Can 3, 3, 3 fit into 4, 3, 3? Yes: A gets 3 (rem 1), B gets 3 (rem 0), C gets 3 (rem 0).
        // But what if capacities are Team A: 4, Team B: 3, Team C: 3 and we have:
        // Group 1: 3 players
        // Group 2: 3 players
        // Group 3: 2 players
        // Group 4: 2 players
        // Can 3, 3, 2, 2 fit into 4, 3, 3?
        // Team A (cap 4): can take either 3 (rem 1) or 2+2 (rem 0).
        // If A takes 2+2 (rem 0), remaining groups are 3, 3. B takes 3 (rem 0), C takes 3 (rem 0) -> FITS!
        // What if Team A has 3 players locked to it (rem 1), Team B has 1 locked (rem 2), Team C has 1 locked (rem 2)?
        // Remaining capacities: [1, 2, 2].
        // Groups to fit: Group 1 (size 3).
        // Max capacity is 2 < 3 -> FAILS!
        val players = createPlayers(10)
        val constraints = mapOf(
            "p_1" to PlayerMatchConstraint("p_1", isLocked = true, lockedTeamIndex = 0),
            "p_2" to PlayerMatchConstraint("p_2", isLocked = true, lockedTeamIndex = 0),
            "p_3" to PlayerMatchConstraint("p_3", isLocked = true, lockedTeamIndex = 0),
            "p_4" to PlayerMatchConstraint("p_4", isLocked = true, lockedTeamIndex = 1),
            "p_5" to PlayerMatchConstraint("p_5", isLocked = true, lockedTeamIndex = 2)
        )
        val group = LockedPlayerGroup(name = "Trio", playerIds = listOf("p_6", "p_7", "p_8"))
        val result = PlayerConstraintValidator.validate(players, 3, constraints, listOf(group))
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("exceeds the maximum available team target capacity"))
    }
}
