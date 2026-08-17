package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerEntity
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.GoldStar
import com.example.ui.theme.FuchsiaAccent
import com.example.ui.viewmodel.AppViewModel
import com.example.util.DashboardStats
import com.example.util.PlayerChemistryEngine
import com.example.util.PlayerSortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerPickerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: ((String) -> Unit)? = null
) {
    val allActivePlayers by viewModel.allActivePlayers.collectAsState()
    val sortedActivePlayers by viewModel.sortedActivePlayers.collectAsState()
    val recentlyUsedPlayers by viewModel.recentlyUsedPlayers.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val currentSortOption by viewModel.playerSortOption.collectAsState()
    val teammatePairCounts by viewModel.teammatePairCounts.collectAsState()
    val opponentPairCounts by viewModel.opponentPairCounts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val selectedPlayers = remember { mutableStateListOf<String>() }
    var detailPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var showDashboard by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Improved Search Filter Logic
    val filteredPlayers = sortedActivePlayers.filter { player ->
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) true
        else {
            val nameMatch = player.displayName.lowercase().contains(query)
            val nickMatch = player.nickname?.lowercase()?.contains(query) == true
            val skillMatch = query == "skill ${player.skillRating}" || query == "${player.skillRating}"
            val favMatch = (query == "fav" || query == "favorite") && player.isFavorite
            val recentMatch = query == "recent" && player.lastUsedAt > 0
            nameMatch || nickMatch || skillMatch || favMatch || recentMatch
        }
    }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "PLAYER LIBRARY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showDashboard = !showDashboard }) {
                        Icon(
                            imageVector = if (showDashboard) Icons.Default.BarChart else Icons.Default.InsertChartOutlined,
                            contentDescription = "Toggle Dashboard",
                            tint = if (showDashboard) NeonBlue else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Statistics Dashboard
            AnimatedVisibility(visible = showDashboard) {
                StatisticsDashboardCard(
                    dashboardStats = dashboardStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Search Bar & Sort Dropdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search (Name, Skill, Favorite...)", color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = NeonBlue,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Options", tint = NeonBlue)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF1A2234))
                    ) {
                        PlayerSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        color = if (option == currentSortOption) NeonBlue else Color.White,
                                        fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setPlayerSortOption(option)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (option == currentSortOption) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonBlue)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Quick Sort Option Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PlayerSortOption.entries) { option ->
                    val isSelected = option == currentSortOption
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPlayerSortOption(option) },
                        label = { Text(option.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonBlue.copy(alpha = 0.2f),
                            selectedLabelColor = NeonBlue,
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = NeonBlue
                        )
                    )
                }
            }

            // Player List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPlayers, key = { it.id }) { player ->
                    val isSelected = selectedPlayers.contains(player.displayName)
                    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
                    val matchesCount = player.matchesPlayed.coerceAtLeast(player.totalMatches)
                    val lastPlayed = player.lastPlayedAt.coerceAtLeast(player.lastUsedAt)
                    val lastPlayedStr = if (lastPlayed > 0) dateFormat.format(Date(lastPlayed)) else "Never"

                    val favoriteTeammate = remember(player, teammatePairCounts) {
                        PlayerChemistryEngine.getFavoriteTeammate(player.displayName, teammatePairCounts)
                    }

                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelected) selectedPlayers.remove(player.displayName)
                                    else selectedPlayers.add(player.displayName)
                                },
                                onLongClick = {
                                    detailPlayer = player
                                }
                            ),
                        cornerRadius = 12
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) selectedPlayers.add(player.displayName)
                                        else selectedPlayers.remove(player.displayName)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NeonBlue,
                                        uncheckedColor = Color.Gray,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = player.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )

                                        if (!player.nickname.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "(${player.nickname})",
                                                color = NeonBlue,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Skill Rating Badge
                                        Text(
                                            text = "⭐ ${player.skillRating}/10",
                                            color = GoldStar,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GoldStar.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "🏏 $matchesCount matches",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Text("•", color = Color.Gray, fontSize = 11.sp)
                                        Text(
                                            text = "Last: $lastPlayedStr",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )

                                        if (favoriteTeammate != null) {
                                            Text("•", color = Color.Gray, fontSize = 11.sp)
                                            Text(
                                                text = "🤝 ${favoriteTeammate.idOrName} (${favoriteTeammate.matchCount}m)",
                                                color = NeonGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Quick Skill Adjustment (- / +)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (player.skillRating > 1) {
                                                viewModel.updatePlayerSkill(player, player.skillRating - 1)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Text(
                                        text = "${player.skillRating}",
                                        color = GoldStar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (player.skillRating < 10) {
                                                viewModel.updatePlayerSkill(player, player.skillRating + 1)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Profile / Info Icon
                                IconButton(
                                    onClick = {
                                        if (onNavigateToProfile != null) {
                                            onNavigateToProfile(player.id)
                                        } else {
                                            detailPlayer = player
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Player Profile",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.togglePlayerFavorite(player) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (player.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (player.isFavorite) GoldStar else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Selection Action Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x99121824)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Selected", color = Color.Gray, fontSize = 12.sp)
                        Text("${selectedPlayers.size} Players", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    NeonButton(
                        text = "Continue",
                        onClick = {
                            viewModel.addPlayersFromLibrary(selectedPlayers.toList())
                            onNavigateBack()
                        },
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Long-Press or Info Click Player Details Dialog
        detailPlayer?.let { player ->
            PlayerDetailsDialog(
                player = player,
                teammatePairCounts = teammatePairCounts,
                opponentPairCounts = opponentPairCounts,
                onDismiss = { detailPlayer = null },
                onUpdateSkill = { newSkill ->
                    viewModel.updatePlayerSkill(player, newSkill)
                    detailPlayer = player.copy(skillRating = newSkill)
                },
                onToggleFavorite = {
                    viewModel.togglePlayerFavorite(player)
                    detailPlayer = player.copy(isFavorite = !player.isFavorite)
                },
                onViewFullProfile = if (onNavigateToProfile != null) {
                    {
                        val pid = player.id
                        detailPlayer = null
                        onNavigateToProfile(pid)
                    }
                } else null
            )
        }
    }
}

@Composable
fun StatisticsDashboardCard(
    dashboardStats: DashboardStats,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier,
        cornerRadius = 16
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATISTICS DASHBOARD",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Generated ${dashboardStats.totalMatchesGenerated} Matches",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Most Active
                StatMiniCard(
                    title = "Most Active 👑",
                    value = dashboardStats.mostActivePlayer?.displayName ?: "None",
                    subtext = "${dashboardStats.mostActivePlayer?.matchesPlayed ?: 0} matches",
                    valueColor = GoldStar,
                    modifier = Modifier.weight(1f)
                )

                // Least Active
                StatMiniCard(
                    title = "Least Active 💤",
                    value = dashboardStats.leastActivePlayer?.displayName ?: "None",
                    subtext = "${dashboardStats.leastActivePlayer?.matchesPlayed ?: 0} matches",
                    valueColor = Color.LightGray,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Highest Skill
                StatMiniCard(
                    title = "Highest Skill ⭐",
                    value = dashboardStats.highestSkillPlayer?.displayName ?: "None",
                    subtext = "Rating: ⭐${dashboardStats.highestSkillPlayer?.skillRating ?: 0}",
                    valueColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )

                // Most Frequent Joker
                StatMiniCard(
                    title = "Most Joker 🃏",
                    value = dashboardStats.mostFrequentJoker?.displayName ?: "None",
                    subtext = "${dashboardStats.mostFrequentJoker?.matchesAsJoker ?: 0} times",
                    valueColor = FuchsiaAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary Footer Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Avg Skill: ⭐ ${String.format(Locale.US, "%.1f", dashboardStats.averageSkillRating)}",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Avg Matches: ${String.format(Locale.US, "%.1f", dashboardStats.averageMatchesPerPlayer)}",
                    color = NeonBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Players: ${dashboardStats.totalRegisteredPlayers}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatMiniCard(
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(10.dp)
    ) {
        Column {
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(subtext, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun PlayerDetailsDialog(
    player: PlayerEntity,
    teammatePairCounts: Map<String, Int>,
    opponentPairCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onUpdateSkill: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onViewFullProfile: (() -> Unit)? = null
) {
    var skillValue by remember(player.id, player.skillRating) { mutableFloatStateOf(player.skillRating.toFloat()) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val lastPlayed = player.lastPlayedAt.coerceAtLeast(player.lastUsedAt)
    val lastPlayedStr = if (lastPlayed > 0) dateFormat.format(Date(lastPlayed)) else "Never"
    val matchesCount = player.matchesPlayed.coerceAtLeast(player.totalMatches)
    val jokerCount = player.matchesAsJoker.coerceAtLeast(player.totalTimesJoker)

    val favTeammate = remember(player, teammatePairCounts) {
        PlayerChemistryEngine.getFavoriteTeammate(player.displayName, teammatePairCounts)
    }

    val favOpponent = remember(player, opponentPairCounts) {
        PlayerChemistryEngine.getFavoriteOpponent(player.displayName, opponentPairCounts)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161F30),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = player.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (player.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (player.isFavorite) GoldStar else Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!player.nickname.isNullOrBlank()) {
                    Text("Nickname: \"${player.nickname}\"", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Skill Slider Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skill Rating", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = "⭐ ${skillValue.toInt()} / 10",
                            color = GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = skillValue,
                        onValueChange = {
                            skillValue = it
                            onUpdateSkill(it.toInt())
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldStar,
                            activeTrackColor = GoldStar,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Stats Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PLAYER STATISTICS & CHEMISTRY", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Matches Played:", color = Color.Gray, fontSize = 12.sp)
                        Text("$matchesCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Times Joker:", color = Color.Gray, fontSize = 12.sp)
                        Text("$jokerCount", color = FuchsiaAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Favorite Teammate:", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            favTeammate?.let { "${it.idOrName} (${it.matchCount}m)" } ?: player.favoriteTeammateId ?: "None",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Favorite Opponent:", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            favOpponent?.let { "${it.idOrName} (${it.matchCount}m)" } ?: player.favoriteOpponentId ?: "None",
                            color = FuchsiaAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Played:", color = Color.Gray, fontSize = 12.sp)
                        Text(lastPlayedStr, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onViewFullProfile != null) {
                    TextButton(onClick = onViewFullProfile) {
                        Text("Full Profile", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
