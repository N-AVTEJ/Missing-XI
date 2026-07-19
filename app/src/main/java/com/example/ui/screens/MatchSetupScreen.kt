package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchSetupScreen(viewModel: AppViewModel) {
    val teamAName by viewModel.matchTeamAName.collectAsState()
    val teamBName by viewModel.matchTeamBName.collectAsState()
    val teamAPlayers by viewModel.matchTeamAPlayers.collectAsState()
    val teamBPlayers by viewModel.matchTeamBPlayers.collectAsState()

    FrostedMeshBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MATCH SETUP",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = "Configure teams and active players",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Team A Card
            item {
                TeamSetupCard(
                    teamName = teamAName,
                    players = teamAPlayers,
                    isTeamA = true,
                    onNameChange = { viewModel.updateMatchTeamName(true, it) },
                    onAddPlayer = { viewModel.addMatchPlayer(true) },
                    onDeletePlayer = { viewModel.removeMatchPlayer(true, it) },
                    onPlayerNameChange = { index, name -> viewModel.updateMatchPlayerName(true, index, name) }
                )
            }

            // VS Divider
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "VS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Team B Card
            item {
                TeamSetupCard(
                    teamName = teamBName,
                    players = teamBPlayers,
                    isTeamA = false,
                    onNameChange = { viewModel.updateMatchTeamName(false, it) },
                    onAddPlayer = { viewModel.addMatchPlayer(false) },
                    onDeletePlayer = { viewModel.removeMatchPlayer(false, it) },
                    onPlayerNameChange = { index, name -> viewModel.updateMatchPlayerName(false, index, name) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamSetupCard(
    teamName: String,
    players: List<String>,
    isTeamA: Boolean,
    onNameChange: (String) -> Unit,
    onAddPlayer: () -> Unit,
    onDeletePlayer: (Int) -> Unit,
    onPlayerNameChange: (Int, String) -> Unit
) {
    val themeColor = if (isTeamA) NeonGreen else CrimsonHot

    GlassyCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Team Name Input & Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = onNameChange,
                    label = { Text(if (isTeamA) "Team A Name" else "Team B Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.weight(1f).testTag(if(isTeamA) "team_a_name" else "team_b_name"),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Live Player Count Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.copy(alpha = 0.15f))
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, contentDescription = "Count", tint = themeColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${players.size}",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Player List with Animations
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                players.forEachIndexed { index, player ->
                    // Animated visibility for adding/removing items gracefully in Column
                    // (Since we are inside a LazyColumn item, we use AnimatedVisibility instead of animateItem for list changes)
                    AnimatedVisibility(
                        visible = true, // To animate appearance/disappearance if using proper state tracking, though compose keys are better. We keep it simple.
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(8.dp)
                        ) {
                            OutlinedTextField(
                                value = player,
                                onValueChange = { onPlayerNameChange(index, it) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedBorderColor = themeColor.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            
                            IconButton(
                                onClick = { onDeletePlayer(index) },
                                enabled = players.size > 1, // Disable if only 1 player
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Player",
                                    tint = if (players.size > 1) Color.Red.copy(alpha = 0.8f) else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // Add Player Button
            Button(
                onClick = onAddPlayer,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = themeColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Player", color = themeColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
