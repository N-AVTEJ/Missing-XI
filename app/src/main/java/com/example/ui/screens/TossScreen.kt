package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.InteractiveCoin
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@Composable
fun TossScreen(viewModel: AppViewModel) {
    val isCoinFlipping by viewModel.isCoinFlipping.collectAsState()
    val selectedTossChoice by viewModel.selectedTossChoice.collectAsState()
    val tossResult by viewModel.tossResult.collectAsState()
    val tossStatusMessage by viewModel.tossStatusMessage.collectAsState()
    val savedTosses by viewModel.savedTosses.collectAsState()

    FrostedMeshBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MATCH KICKOFF TOSS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // Choice selectors
            item {
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Choose Heads or Tails",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Heads", "Tails").forEach { choice ->
                                val isSelected = selectedTossChoice == choice
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) IndigoAccent else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(if (isSelected) IndigoAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.02f))
                                        .clickable(enabled = !isCoinFlipping) {
                                            viewModel.selectedTossChoice.value = choice
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = choice.uppercase(),
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive high-fidelity Coin visualizer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveCoin(
                        isFlipping = isCoinFlipping,
                        result = tossResult,
                        modifier = Modifier.testTag("interactive_coin_view")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Status & Results Card
            item {
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Casino",
                            tint = IndigoAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = tossStatusMessage,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        if (tossResult != null && !isCoinFlipping) {
                            val win = savedTosses.lastOrNull()?.hasWon == true
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (win) NeonGreen.copy(alpha = 0.15f) else CrimsonHot.copy(alpha = 0.15f))
                                    .border(1.dp, if (win) NeonGreen.copy(alpha = 0.4f) else CrimsonHot.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Landed on: $tossResult",
                                    color = if (win) NeonGreen else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Action Trigger
            item {
                NeonButton(
                    text = if (isCoinFlipping) "Flipping..." else "Flip Coin",
                    onClick = { viewModel.triggerToss() },
                    modifier = Modifier.fillMaxWidth().testTag("flip_coin_button"),
                    glowingColor = IndigoAccent,
                    enabled = !isCoinFlipping
                )
            }

            // Toss Stats Summary
            item {
                val total = savedTosses.size
                val wins = savedTosses.count { it.hasWon }
                val losses = total - wins

                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "YOUR MATCH TOSS HISTORY STATS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Flips", color = Color.Gray, fontSize = 12.sp)
                                Text("$total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Column {
                                Text("Wins", color = NeonGreen, fontSize = 12.sp)
                                Text("$wins", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Column {
                                Text("Losses", color = CrimsonHot, fontSize = 12.sp)
                                Text("$losses", color = CrimsonHot, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
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

