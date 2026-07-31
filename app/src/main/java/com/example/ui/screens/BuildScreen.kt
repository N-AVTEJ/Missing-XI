package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BuildScreen(viewModel: AppViewModel, onNavigateToLibrary: () -> Unit) {
    val totalPlayersInput by viewModel.buildTotalPlayersInput.collectAsState()
    val playersList by viewModel.buildPlayersList.collectAsState()
    val searchQuery by viewModel.buildSearchQuery.collectAsState()
    val duplicateError by viewModel.buildDuplicateError.collectAsState()
    val emptyFieldError by viewModel.buildEmptyFieldError.collectAsState()
    val hasStartedBuilding by viewModel.hasStartedBuilding.collectAsState()
    val latestSession by viewModel.latestSession.collectAsState()

    val filteredPlayers = playersList.mapIndexed { index, name -> index to name }
        .filter { it.second.contains(searchQuery, ignoreCase = true) }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Text(
                text = "PLAYER ROSTER BUILDER",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!hasStartedBuilding) {
                // Starter Options
                if (latestSession != null) {
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.continueWithLastSession() },
                        cornerRadius = 16
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = NeonGreen, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Continue With Last Players", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Restore players from your last shuffle", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                GlassyCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToLibrary() },
                    cornerRadius = 16
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "Library", tint = NeonBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Select From Player Library", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Choose from your saved players", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                GlassyCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.startBuildingFresh() },
                    cornerRadius = 16
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Create New Players", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Start fresh and enter names manually", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Meta Configuration Card
            GlassyCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Total Players Input
                    OutlinedTextField(
                        value = totalPlayersInput,
                        onValueChange = { viewModel.updateBuildTotalPlayers(it) },
                        label = { Text("Total Players to Generate", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedLabelColor = NeonBlue,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("total_players_input")
                    )

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateBuildSearchQuery(it) },
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
                        modifier = Modifier.fillMaxWidth().testTag("search_players_input")
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Live stats & Errors
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Live Counter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonBlue.copy(alpha = 0.15f))
                        .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, contentDescription = "Count", tint = NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${playersList.size} Players Active",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Error Badge
                val currentError = duplicateError ?: emptyFieldError
                AnimatedVisibility(
                    visible = currentError != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CrimsonHot.copy(alpha = 0.15f))
                            .border(1.dp, CrimsonHot.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = CrimsonHot, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentError ?: "",
                                color = CrimsonHot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player List with Animations
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    count = filteredPlayers.size,
                    key = { filteredPlayers[it].first } // Key by original index for stable animations
                ) { listIndex ->
                    val (originalIndex, playerName) = filteredPlayers[listIndex]
                    
                    Box(modifier = Modifier.animateItemPlacement()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            // Player Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonBlue.copy(alpha = 0.2f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${originalIndex + 1}", color = NeonBlue, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Name Input
                            OutlinedTextField(
                                value = playerName,
                                onValueChange = { viewModel.updateBuildPlayerName(originalIndex, it) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedBorderColor = NeonBlue.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Remove Button
                            IconButton(
                                onClick = { viewModel.removeBuildPlayer(originalIndex) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CrimsonHot.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Player",
                                    tint = CrimsonHot
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.addBuildPlayer() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Manually", tint = NeonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Player Manually", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            } // Close else block
        }
    }
}


