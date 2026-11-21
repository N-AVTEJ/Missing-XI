package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.FuchsiaAccent
import com.example.ui.theme.GoldStar
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.MatchStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchFinalizationScreen(
    viewModel: AppViewModel,
    matchId: String,
    onNavigateBack: () -> Unit,
    onStartNewMatch: () -> Unit
) {
    val summary by viewModel.reopenedMatchSummary.collectAsState()
    var showDetails by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    // Read-only load from SQLite — never triggers a new toss or modifies stats/teams
    LaunchedEffect(matchId) {
        viewModel.loadMatchSummary(matchId)
    }

    BackHandler {
        onNavigateBack()
    }

    FrostedMeshBackground {
        val activeSummary = summary
        if (activeSummary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Match record unavailable",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "This match could not be found in saved history.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        NeonButton(
                            text = "Back",
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            glowingColor = IndigoAccent
                        )
                    }
                }
            }
        } else {
            val isCompleted = activeSummary.matchStatus == MatchStatus.COMPLETED

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar + Match Completed Banner
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("finalization_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (isCompleted) NeonGreen.copy(alpha = 0.18f)
                                    else GoldStar.copy(alpha = 0.18f)
                                )
                                .border(
                                    1.dp,
                                    if (isCompleted) NeonGreen.copy(alpha = 0.5f)
                                    else GoldStar.copy(alpha = 0.5f),
                                    RoundedCornerShape(99.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Match Status",
                                    tint = if (isCompleted) NeonGreen else GoldStar,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isCompleted) "MATCH COMPLETED" else activeSummary.matchStatus.displayLabel.uppercase(),
                                    color = if (isCompleted) NeonGreen else GoldStar,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.testTag("match_finalization_status_badge")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // Confirmed Teams Section (read-only)
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Confirmed Teams",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CONFIRMED TEAMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        if (activeSummary.wasManuallyEdited) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonBlue.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "EDITED",
                                    color = NeonBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                items(activeSummary.teams.size, key = { activeSummary.teams[it].teamNumber }) { index ->
                    val team = activeSummary.teams[index]
                    val accentColor = when (index % 4) {
                        0 -> NeonGreen
                        1 -> NeonBlue
                        2 -> IndigoAccent
                        else -> GoldStar
                    }

                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("finalized_team_card_${team.teamNumber}"),
                        cornerRadius = 20
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(accentColor)
                                    )
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                                Text(
                                    text = "${team.players.size} Players",
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                team.players.forEach { player ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Player",
                                            tint = accentColor.copy(alpha = 0.75f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = player,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (team.totalStrength > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Team Strength: ${team.totalStrength}",
                                        color = NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Avg: ${String.format(Locale.US, "%.2f", team.averageStrength)}",
                                        color = NeonBlue,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional Joker Card
                if (!activeSummary.joker.isNullOrBlank()) {
                    item {
                        GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Joker",
                                    tint = FuchsiaAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Joker Player: ${activeSummary.joker}",
                                    color = FuchsiaAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Toss Result Summary Card (or graceful legacy fallback)
                item {
                    val latestToss = activeSummary.latestToss
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("finalized_toss_result_card"),
                        cornerRadius = 22
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Toss Result",
                                        tint = GoldStar,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "TOSS RESULT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldStar,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                if (activeSummary.tossHistory.size > 1) {
                                    Text(
                                        text = "${activeSummary.tossHistory.size} Attempts",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            if (latestToss == null) {
                                Text(
                                    text = "No toss recorded.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("no_toss_recorded_text")
                                )
                            } else {
                                val accent = if (latestToss.wonToss) NeonGreen else CrimsonHot
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = latestToss.result.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp,
                                            letterSpacing = 2.sp
                                        )
                                        Text(
                                            text = "Your choice: ${latestToss.userChoice.displayName}  •  Coin result: ${latestToss.result.displayName}",
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.copy(alpha = 0.18f))
                                            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (latestToss.wonToss) "YOU WON!" else "YOU LOST",
                                            color = accent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable [ View Match Details ] Section
                item {
                    OutlinedButton(
                        onClick = { showDetails = !showDetails },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(NeonBlue.copy(alpha = 0.55f))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("view_match_details_button")
                    ) {
                        Text(
                            text = if (showDetails) "Hide Match Details" else "View Match Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Match Details",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "MATCH DETAILS",
                                        color = NeonBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    DetailMetricRow("Status", activeSummary.matchStatus.displayLabel)
                                    DetailMetricRow("Created", dateFormatter.format(Date(activeSummary.createdAt)))
                                    if (activeSummary.completedAt > 0L) {
                                        DetailMetricRow("Completed", dateFormatter.format(Date(activeSummary.completedAt)))
                                    }
                                    if (activeSummary.fairnessScore > 0) {
                                        DetailMetricRow("Fairness Score", "${activeSummary.fairnessScore}/100 (${activeSummary.fairnessRating})")
                                    }
                                    if (activeSummary.fairnessProfile.isNotBlank()) {
                                        DetailMetricRow("Fairness Profile", activeSummary.fairnessProfile)
                                    }
                                    if (activeSummary.qualityGateOutcome.isNotBlank()) {
                                        DetailMetricRow("Quality Gate", activeSummary.qualityGateOutcome)
                                    }
                                }
                            }

                            if (activeSummary.tossHistory.isNotEmpty()) {
                                val maxAttempt = activeSummary.tossHistory.maxOfOrNull { it.attemptNumber } ?: 0
                                GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "TOSS ATTEMPTS (${activeSummary.tossHistory.size})",
                                            color = GoldStar,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                        activeSummary.tossHistory.sortedBy { it.attemptNumber }.forEach { attempt ->
                                            val isLatest = attempt.attemptNumber == maxAttempt
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.03f))
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Attempt #${attempt.attemptNumber}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                        if (isLatest) {
                                                            Text(
                                                                text = "LATEST",
                                                                color = GoldStar,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "${attempt.userChoice.displayName} → ${attempt.result.displayName} • ${timeFormatter.format(Date(attempt.createdAt))}",
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text(
                                                    text = if (attempt.wonToss) "WON" else "LOST",
                                                    color = if (attempt.wonToss) NeonGreen else CrimsonHot,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // [ New Match ] Action Button
                item {
                    NeonButton(
                        text = "New Match",
                        onClick = {
                            viewModel.startNewMatch()
                            onStartNewMatch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_match_button"),
                        glowingColor = NeonGreen
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
