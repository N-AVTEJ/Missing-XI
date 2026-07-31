package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.FuchsiaAccent
import com.example.ui.theme.GoldStar
import com.example.ui.theme.NeonBlue

@Composable
fun JokerCard(
    jokerName: String,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("joker_card"),
        cornerRadius = 20,
        borderBrush = Brush.linearGradient(
            colors = listOf(FuchsiaAccent.copy(alpha = 0.6f), GoldStar.copy(alpha = 0.4f))
        ),
        backgroundBrush = Brush.linearGradient(
            colors = listOf(FuchsiaAccent.copy(alpha = 0.12f), GoldStar.copy(alpha = 0.05f))
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(FuchsiaAccent, GoldStar)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Joker",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "OFFICIAL JOKER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Neutral Wild Card Player",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FuchsiaAccent.copy(alpha = 0.2f))
                        .border(1.dp, FuchsiaAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "JOKER",
                        color = FuchsiaAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, FuchsiaAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Player",
                    tint = GoldStar,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = jokerName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun JokerRotationSection(
    currentJoker: String?,
    remainingPlayers: List<String>,
    previousJokers: List<String>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Current Joker Card
        if (!currentJoker.isNullOrBlank()) {
            JokerCard(jokerName = currentJoker)
        }

        // 2. Remaining Players Card (Who haven't been Joker yet in current cycle)
        GlassyCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("joker_remaining_card"),
            cornerRadius = 20
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Remaining Candidates",
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REMAINING JOKER CANDIDATES",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlue.copy(alpha = 0.15f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${remainingPlayers.size} Remaining",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "Players who haven't served as Joker in current rotation cycle:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (remainingPlayers.isEmpty()) {
                    Text(
                        text = "All players have been Joker once! Cycle will reset on next shuffle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldStar,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(remainingPlayers) { player ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = NeonBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = player,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Previous Jokers History Card
        GlassyCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("joker_history_card"),
            cornerRadius = 20
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Joker History",
                            tint = FuchsiaAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PREVIOUS JOKERS HISTORY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    if (previousJokers.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("clear_joker_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = CrimsonHot,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (previousJokers.isEmpty()) {
                    Text(
                        text = "No previous Jokers recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        previousJokers.reversed().forEachIndexed { index, player ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${previousJokers.size - index}",
                                        color = FuchsiaAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = player,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Joker",
                                    tint = FuchsiaAccent.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

