package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    val savedLineups by viewModel.savedLineups.collectAsState()
    val savedTosses by viewModel.savedTosses.collectAsState()

    var activeTab by remember { mutableStateOf("Lineups") } // "Lineups" or "Tosses"

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SAVED HISTORIES",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = IndigoAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // History Tab Switchers (Frosted Glass container with Pill options)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(99.dp))
                    .padding(4.dp)
            ) {
                listOf("Lineups", "Tosses").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (isSelected) IndigoAccent else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (tab == "Lineups") Icons.Default.Groups else Icons.Default.Casino,
                                contentDescription = tab,
                                tint = if (isSelected) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Lists
            if (activeTab == "Lineups") {
                if (savedLineups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "No Squads",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No lineups built yet", color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Create squad configurations in the Build tab!", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).testTag("lineups_history_list"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(savedLineups, key = { it.id }) { lineup ->
                            GlassyCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (lineup.sportType == "Cricket") GoldStar.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = lineup.sportType.uppercase(),
                                                    color = if (lineup.sportType == "Cricket") GoldStar else NeonGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Formation: ${lineup.formation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = lineup.teamName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = dateFormatter.format(Date(lineup.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // Render simplified players chip list
                                        val formattedPlayers = lineup.playersJson.split(", ")
                                            .map { it.substringAfter(":") }
                                            .take(11)
                                            .joinToString(", ")

                                        Text(
                                            text = "Squad: $formattedPlayers",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteLineup(lineup.id) },
                                        modifier = Modifier.testTag("delete_lineup_${lineup.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Lineup",
                                            tint = CrimsonHot.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            } else {
                if (savedTosses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "No Tosses",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No match tosses saved yet", color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Decide lineup status in the Toss tab!", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { viewModel.clearAllTosses() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonHot.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CrimsonHot.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .testTag("clear_toss_history_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear All Toss Histories", color = CrimsonHot, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("tosses_history_list"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(savedTosses) { toss ->
                                GlassyCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 20
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = dateFormatter.format(Date(toss.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Your Choice: ${toss.choice}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "|  Landed on: ${toss.result}",
                                                    color = Color.LightGray,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (toss.hasWon) NeonGreen.copy(alpha = 0.15f) else CrimsonHot.copy(alpha = 0.15f))
                                                .border(1.dp, if (toss.hasWon) NeonGreen.copy(alpha = 0.3f) else CrimsonHot.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (toss.hasWon) "WIN" else "LOSS",
                                                color = if (toss.hasWon) NeonGreen else CrimsonHot,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

