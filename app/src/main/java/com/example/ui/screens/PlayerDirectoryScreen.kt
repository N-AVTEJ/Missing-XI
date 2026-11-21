package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.FuchsiaAccent
import com.example.ui.theme.GoldStar
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.PlayerAnalyticsService
import com.example.util.PlayerDirectoryItem
import com.example.util.PlayerDirectorySortOption
import com.example.util.PlayerStatusFilter

/**
 * Dedicated Player Directory & Analytics entry screen.
 * Search, active/archived filter, and sort state live in [AppViewModel] / PlayerDirectoryStore
 * so returning from a Player Profile always preserves the user's previous view.
 */
@Composable
fun PlayerDirectoryScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onOpenPlayerProfile: (String) -> Unit
) {
    val uiState by viewModel.playerDirectoryUiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_directory_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "PLAYER DIRECTORY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.8.sp
                            )
                        )
                        Text(
                            text = "${uiState.totalActiveCount} Active • ${uiState.totalArchivedCount} Archived",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = { showAddPlayerDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonBlue.copy(alpha = 0.16f))
                        .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("add_player_directory_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Player",
                        tint = NeonBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar + Sort Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.filter.searchQuery,
                    onValueChange = { viewModel.setPlayerDirectorySearch(it) },
                    placeholder = { Text("Search by player name...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search players", tint = Color.Gray)
                    },
                    trailingIcon = {
                        if (uiState.filter.searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.setPlayerDirectorySearch("") },
                                modifier = Modifier.testTag("clear_player_search_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_directory_search_input")
                )

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                1.dp,
                                if (uiState.filter.sortOption != PlayerDirectorySortOption.ALPHABETICAL) {
                                    IndigoAccent.copy(alpha = 0.7f)
                                } else {
                                    Color.White.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("player_directory_sort_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort players",
                            tint = NeonBlue
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF141C2E))
                    ) {
                        PlayerDirectorySortOption.entries.forEach { option ->
                            val selected = option == uiState.filter.sortOption
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayLabel,
                                        color = if (selected) NeonBlue else Color.White,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonBlue)
                                    }
                                },
                                onClick = {
                                    viewModel.setPlayerDirectorySortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active / Archived / All Filter Chips + Active Sort Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerStatusFilter.entries.forEach { status ->
                    val isSelected = uiState.filter.statusFilter == status
                    val count = when (status) {
                        PlayerStatusFilter.ACTIVE -> uiState.totalActiveCount
                        PlayerStatusFilter.ARCHIVED -> uiState.totalArchivedCount
                        PlayerStatusFilter.ALL -> uiState.totalPlayersCount
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPlayerDirectoryStatusFilter(status) },
                        label = {
                            Text(
                                text = "${status.displayLabel} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (status) {
                                PlayerStatusFilter.ACTIVE -> NeonGreen.copy(alpha = 0.2f)
                                PlayerStatusFilter.ARCHIVED -> GoldStar.copy(alpha = 0.2f)
                                PlayerStatusFilter.ALL -> NeonBlue.copy(alpha = 0.2f)
                            },
                            selectedLabelColor = when (status) {
                                PlayerStatusFilter.ACTIVE -> NeonGreen
                                PlayerStatusFilter.ARCHIVED -> GoldStar
                                PlayerStatusFilter.ALL -> NeonBlue
                            },
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = Color.LightGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.12f),
                            selectedBorderColor = when (status) {
                                PlayerStatusFilter.ACTIVE -> NeonGreen.copy(alpha = 0.6f)
                                PlayerStatusFilter.ARCHIVED -> GoldStar.copy(alpha = 0.6f)
                                PlayerStatusFilter.ALL -> NeonBlue.copy(alpha = 0.6f)
                            }
                        ),
                        modifier = Modifier.testTag("player_filter_${status.name.lowercase()}")
                    )
                }

                // Current sort indicator chip
                Surface(
                    color = IndigoAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { showSortMenu = true }
                ) {
                    Text(
                        text = "Sort: ${uiState.filter.sortOption.displayLabel}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Error state banner if any
            uiState.errorMessage?.let { err ->
                Surface(
                    color = CrimsonHot.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrimsonHot.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = err,
                        color = CrimsonHot,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Directory Body: Loading / Empty / Filtered Empty / List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                }

                uiState.totalPlayersCount == 0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = NeonBlue.copy(alpha = 0.6f),
                                    modifier = Modifier.size(52.dp)
                                )
                                Text(
                                    text = "No players yet.",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add players to your persistent library or generate a match to start tracking individual analytics and teammate chemistry.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showAddPlayerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("empty_add_first_player_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Player", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (uiState.filter.statusFilter == PlayerStatusFilter.ARCHIVED && uiState.filter.searchQuery.isBlank()) {
                                        "No archived players."
                                    } else {
                                        "No matching players found."
                                    },
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Adjust your search or status filter to see other players in the library.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (uiState.filter.hasActiveSearchOrFilter) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearPlayerDirectoryFilters() },
                                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("clear_player_directory_filters_btn")
                                    ) {
                                        Text("Reset Filters", color = NeonBlue, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.items, key = { it.player.id }) { item ->
                            PlayerDirectoryCard(
                                item = item,
                                onClick = { onOpenPlayerProfile(item.player.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPlayerDialog) {
        AddLibraryPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onCreate = { name, nickname, rating, onError ->
                viewModel.createLibraryPlayer(
                    displayName = name,
                    skillRating = rating,
                    nickname = nickname
                ) { result ->
                    result.onSuccess {
                        showAddPlayerDialog = false
                    }.onFailure { err ->
                        onError(err.message ?: "Could not add player.")
                    }
                }
            }
        )
    }
}

@Composable
private fun PlayerDirectoryCard(
    item: PlayerDirectoryItem,
    onClick: () -> Unit
) {
    val player = item.player
    val initials = player.displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { player.displayName.take(2).uppercase() }

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("player_directory_card_${player.id}"),
        cornerRadius = 16
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (player.isArchived) {
                            Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                        } else {
                            Brush.linearGradient(listOf(IndigoAccent, NeonBlue))
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (player.isArchived) Color.Gray.copy(alpha = 0.5f) else NeonBlue.copy(alpha = 0.7f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = if (player.isArchived) Color.LightGray else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info & Stats
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = player.displayName,
                        color = if (player.isArchived) Color.LightGray else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (player.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = GoldStar,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (player.isArchived) {
                        Surface(
                            color = GoldStar.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GoldStar.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "ARCHIVED",
                                color = GoldStar,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skill Rating Pill
                    Surface(
                        color = GoldStar.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = "Skill ${player.skillRating}/10",
                            color = GoldStar,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Matches Played Pill
                    Surface(
                        color = NeonGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = "${item.matchesPlayed} ${if (item.matchesPlayed == 1) "Match" else "Matches"}",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Joker Pill
                    Surface(
                        color = FuchsiaAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = "${item.matchesAsJoker} Joker",
                            color = FuchsiaAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open player profile",
                tint = NeonBlue.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AddLibraryPlayerDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, nickname: String?, skillRating: Int, onError: (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var skillRating by remember { mutableFloatStateOf(PlayerAnalyticsService.DEFAULT_SKILL_RATING.toFloat()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141C2E),
        title = {
            Text(
                text = "Add Player",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Player Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_player_name_input")
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (Optional)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skill Rating", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = "${skillRating.toInt()} / 10",
                            color = GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Slider(
                        value = skillRating,
                        onValueChange = { skillRating = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldStar,
                            activeTrackColor = GoldStar,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                errorText?.let { err ->
                    Text(text = err, color = CrimsonHot, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(name, nickname.takeIf { it.isNotBlank() }, skillRating.toInt()) { err ->
                        errorText = err
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_add_player_btn")
            ) {
                Text("Save Player", color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        }
    )
}
