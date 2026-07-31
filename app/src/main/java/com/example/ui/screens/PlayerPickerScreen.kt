package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.GoldStar
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPickerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val allActivePlayers by viewModel.allActivePlayers.collectAsState()
    val recentlyUsedPlayers by viewModel.recentlyUsedPlayers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val selectedPlayers = remember { mutableStateListOf<String>() }

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var currentSection = ""
                items(displayList, key = { it.id }) { player ->
                    // Determine Section
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
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedPlayers.remove(player.displayName)
                                else selectedPlayers.add(player.displayName)
                            },
                        cornerRadius = 12
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = player.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (!player.nickname.isNullOrBlank()) {
                                    Text(
                                        text = player.nickname,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            // Quick Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { 
                                        viewModel.togglePlayerFavorite(player)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (player.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (player.isFavorite) GoldStar else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.archivePlayer(player) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deletePlayer(player) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = com.example.ui.theme.CrimsonHot, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
