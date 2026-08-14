package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerPickerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val allActivePlayers by viewModel.allActivePlayers.collectAsState()
    val recentlyUsedPlayers by viewModel.recentlyUsedPlayers.collectAsState()
    val highestRatedPlayer by viewModel.highestRatedPlayer.collectAsState()
    val averagePlayerSkill by viewModel.averagePlayerSkill.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val selectedPlayers = remember { mutableStateListOf<String>() }
    var detailPlayer by remember { mutableStateOf<PlayerEntity?>(null) }

    val filteredPlayers = allActivePlayers.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        (it.nickname?.contains(searchQuery, ignoreCase = true) == true)
    }

    val displayList = if (searchQuery.isBlank()) {
        val recentList = recentlyUsedPlayers.take(5)
        val recentNames = recentList.map { it.displayName }.toSet()
        val favorites = allActivePlayers.filter { it.isFavorite && it.displayName !in recentNames }
        val favoriteNames = favorites.map { it.displayName }.toSet()
        val rest = allActivePlayers.filter { it.displayName !in recentNames && it.displayName !in favoriteNames }
        
        val result = mutableListOf<PlayerEntity>()
        result.addAll(recentList)
        result.addAll(favorites)
        result.addAll(rest)
        result
    } else {
        filteredPlayers
    }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
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
            Spacer(modifier = Modifier.height(12.dp))

            // Player Library Statistics Summary Card
            GlassyCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val hrPlayer = highestRatedPlayer
                        val highestText = if (hrPlayer != null) "${hrPlayer.displayName} (⭐${hrPlayer.skillRating})" else "None"
                        Text(
                            text = highestText,
                            color = GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text("Highest Rated", color = Color.Gray, fontSize = 10.sp)
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⭐ ${String.format(Locale.US, "%.1f", averagePlayerSkill)} / 10",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text("Average Skill", color = Color.Gray, fontSize = 10.sp)
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${allActivePlayers.size}",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text("Total Players", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Players", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedLabelColor = NeonBlue,
                    unfocusedLabelColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var currentSection = ""
                items(displayList, key = { it.id }) { player ->
                    val section = if (searchQuery.isNotBlank()) {
                        "Search Results"
                    } else if (recentlyUsedPlayers.take(5).any { it.id == player.id }) {
                        "Recently Used"
                    } else if (player.isFavorite) {
                        "Favorites"
                    } else {
                        "All Players"
                    }

                    if (section != currentSection && searchQuery.isBlank()) {
                        currentSection = section
                        Text(
                            text = section,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    val isSelected = selectedPlayers.contains(player.displayName)
                    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
                    val lastPlayedStr = if (player.totalMatches > 0) dateFormat.format(Date(player.lastUsedAt)) else "Never"

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
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Display Skill Rating Badge
                                        Text(
                                            text = "⭐ ${player.skillRating} / 10",
                                            color = GoldStar,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GoldStar.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "Matches: ${player.totalMatches}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "•",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Last: $lastPlayedStr",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Quick Skill Adjustment Step Buttons (- / +)
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

        // Long Press Player Details / Skill Edit Dialog
        detailPlayer?.let { player ->
            PlayerDetailsDialog(
                player = player,
                onDismiss = { detailPlayer = null },
                onUpdateSkill = { newSkill ->
                    viewModel.updatePlayerSkill(player, newSkill)
                    detailPlayer = player.copy(skillRating = newSkill)
                },
                onToggleFavorite = {
                    viewModel.togglePlayerFavorite(player)
                    detailPlayer = player.copy(isFavorite = !player.isFavorite)
                }
            )
        }
    }
}

@Composable
fun PlayerDetailsDialog(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onUpdateSkill: (Int) -> Unit,
    onToggleFavorite: () -> Unit
) {
    var skillValue by remember(player.id, player.skillRating) { mutableFloatStateOf(player.skillRating.toFloat()) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val lastPlayedStr = if (player.totalMatches > 0) dateFormat.format(Date(player.lastUsedAt)) else "Never"

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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!player.nickname.isNullOrBlank()) {
                    Text("Nickname: ${player.nickname}", color = Color.LightGray, fontSize = 14.sp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 (Beginner)", color = Color.Gray, fontSize = 10.sp)
                        Text("5 (Intermediate)", color = Color.Gray, fontSize = 10.sp)
                        Text("10 (Elite)", color = Color.Gray, fontSize = 10.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Stats Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PLAYER STATISTICS", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Matches Played:", color = Color.Gray, fontSize = 13.sp)
                        Text("${player.totalMatches}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Times Selected as Joker:", color = Color.Gray, fontSize = 13.sp)
                        Text("${player.totalTimesJoker}", color = FuchsiaAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Played:", color = Color.Gray, fontSize = 13.sp)
                        Text(lastPlayedStr, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = NeonBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}
