package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlin.math.roundToInt

/**
 * Read-only Match Details screen displaying stored teams, Joker, excluded players,
 * expandable historical Fairness Breakdown, chronological Toss History, and safe match deletion.
 *
 * Opening a historical match NEVER regenerates teams, triggers a toss, recalculates fairness,
 * or modifies player/pair statistics.
 */
@Composable
fun MatchDetailsScreen(
    viewModel: AppViewModel,
    matchId: String,
    onNavigateBack: () -> Unit
) {
    val details by viewModel.selectedMatchDetails.collectAsState()
    var isFairnessExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetails(matchId)
    }

    BackHandler {
        onNavigateBack()
    }

    FrostedMeshBackground {
        val activeDetails = details
        if (activeDetails == null || activeDetails.id != matchId) {
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
                            text = "Match details unavailable",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "This match may have been removed from history.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        NeonButton(
                            text = "Back to History",
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            glowingColor = IndigoAccent
                        )
                    }
                }
            }
        } else {
            val isCompleted = activeDetails.status == MatchStatus.COMPLETED

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .testTag("match_details_list"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Action Header
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("match_details_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Match History",
                                tint = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = activeDetails.displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                ),
                                modifier = Modifier.testTag("match_details_title")
                            )
                            Text(
                                text = dateFormatter.format(Date(activeDetails.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_match_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Match",
                                tint = CrimsonHot
                            )
                        }
                    }
                }

                // Status & Read-Only Indicator Pill Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (isCompleted) NeonGreen.copy(alpha = 0.16f)
                                    else GoldStar.copy(alpha = 0.16f)
                                )
                                .border(
                                    1.dp,
                                    if (isCompleted) NeonGreen.copy(alpha = 0.45f)
                                    else GoldStar.copy(alpha = 0.45f),
                                    RoundedCornerShape(99.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Match Status",
                                    tint = if (isCompleted) NeonGreen else GoldStar,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeDetails.status.displayLabel.uppercase(),
                                    color = if (isCompleted) NeonGreen else GoldStar,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        if (activeDetails.wasManuallyEdited) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(NeonBlue.copy(alpha = 0.16f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "MANUALLY EDITED",
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Fairness Overview + Expandable Historical Fairness Breakdown
                item {
                    val breakdown = activeDetails.fairnessBreakdown
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_details_fairness_card"),
                        cornerRadius = 22
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFairnessExpanded = !isFairnessExpanded }
                                    .testTag("fairness_breakdown_toggle"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "Fairness Breakdown",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "FAIRNESS",
                                            color = NeonGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.5.sp
                                        )
                                        Text(
                                            text = activeDetails.fairnessScore?.let { "$it / 100" }
                                                ?: "Fairness data unavailable",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isFairnessExpanded) "Hide Breakdown" else "Fairness Breakdown",
                                        color = NeonBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isFairnessExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand Fairness Breakdown",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Summary meta chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoPill(
                                    text = "Profile: ${activeDetails.fairnessProfile ?: "Legacy"}",
                                    accent = IndigoAccent
                                )
                                if (!activeDetails.qualityGateOutcome.isNullOrBlank()) {
                                    val isBestAvail = activeDetails.qualityGateOutcome.equals("Best Available", ignoreCase = true)
                                    InfoPill(
                                        text = "Quality Gate: ${activeDetails.qualityGateOutcome}",
                                        accent = if (isBestAvail) GoldStar else NeonGreen
                                    )
                                }
                            }

                            // Expandable stored historical breakdown
                            AnimatedVisibility(visible = isFairnessExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .testTag("fairness_breakdown_content"),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    if (breakdown == null) {
                                        Text(
                                            text = "Fairness breakdown unavailable for this match.",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            modifier = Modifier.testTag("fairness_breakdown_unavailable")
                                        )
                                    } else {
                                        BreakdownRow(
                                            label = "Overall Fairness Score",
                                            value = "${breakdown.overallScore.roundToInt()}/100 (${breakdown.fairnessRating})"
                                        )
                                        BreakdownRow(
                                            label = "Teammate Variety Score",
                                            value = "${breakdown.teammateVarietyScore.roundToInt()}/100"
                                        )
                                        BreakdownRow(
                                            label = "Opponent Variety Score",
                                            value = "${breakdown.opponentVarietyScore.roundToInt()}/100"
                                        )
                                        BreakdownRow(
                                            label = "Team Strength Score",
                                            value = "${breakdown.teamStrengthScore.roundToInt()}/100"
                                        )
                                        BreakdownRow(
                                            label = "Joker Fairness Score",
                                            value = "${breakdown.jokerFairnessScore.roundToInt()}/100"
                                        )
                                        BreakdownRow(
                                            label = "Teammate Penalty",
                                            value = breakdown.teammatePenalty?.toString() ?: "Unavailable"
                                        )
                                        BreakdownRow(
                                            label = "Opponent Penalty",
                                            value = breakdown.opponentPenalty?.toString() ?: "Unavailable"
                                        )
                                        BreakdownRow(
                                            label = "Team Strength Difference",
                                            value = breakdown.teamStrengthDifference?.let { "$it pts" } ?: "Unavailable"
                                        )
                                        BreakdownRow(
                                            label = "Quality Gate Outcome",
                                            value = breakdown.qualityGateOutcome ?: "Unavailable"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Stored Teams Section (2, 3, 4+ teams)
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
                            text = "CONFIRMED TEAMS (${activeDetails.teams.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                items(activeDetails.teams.size, key = { "detail_team_${activeDetails.teams[it].teamNumber}" }) { index ->
                    val team = activeDetails.teams[index]
                    val accentColor = when (index % 4) {
                        0 -> NeonGreen
                        1 -> NeonBlue
                        2 -> IndigoAccent
                        else -> GoldStar
                    }

                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("details_team_card_${team.teamNumber}"),
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
                                        text = team.name.uppercase(),
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
                                team.players.forEach { playerName ->
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
                                            text = playerName,
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

                // Separate Joker Card (when present)
                if (!activeDetails.joker.isNullOrBlank()) {
                    item {
                        GlassyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("details_joker_card"),
                            cornerRadius = 18
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Joker Player",
                                    tint = FuchsiaAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "JOKER PLAYER",
                                        color = FuchsiaAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = activeDetails.joker,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Separate Excluded Players Card (when present)
                if (activeDetails.excludedPlayers.isNotEmpty()) {
                    item {
                        GlassyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("details_excluded_players_card"),
                            cornerRadius = 18
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Excluded Players",
                                        tint = CrimsonHot,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "EXCLUDED PLAYERS (${activeDetails.excludedPlayers.size})",
                                        color = CrimsonHot,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Text(
                                    text = activeDetails.excludedPlayers.joinToString(", "),
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Toss Result & Chronological Toss History Card
                item {
                    val latestToss = activeDetails.latestToss
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("details_toss_section"),
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Toss",
                                        tint = GoldStar,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "TOSS",
                                        color = GoldStar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                if (activeDetails.tossHistory.isNotEmpty()) {
                                    Text(
                                        text = "${activeDetails.tossHistory.size} Attempt${if (activeDetails.tossHistory.size == 1) "" else "s"}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            if (latestToss == null) {
                                Text(
                                    text = "No toss recorded",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.testTag("details_no_toss_recorded")
                                )
                            } else {
                                BreakdownRow("Your choice", latestToss.userChoice.displayName)
                                BreakdownRow("Coin result", latestToss.result.displayName)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Outcome", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        text = if (latestToss.wonToss) "Won" else "Lost",
                                        color = if (latestToss.wonToss) NeonGreen else CrimsonHot,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Full Chronological Toss History
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Toss History",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "TOSS HISTORY",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                val maxAttempt = activeDetails.tossHistory.maxOfOrNull { it.attemptNumber } ?: 0
                                activeDetails.tossHistory.sortedBy { it.attemptNumber }.forEach { attempt ->
                                    val isLatest = attempt.attemptNumber == maxAttempt
                                    val outcomeColor = if (attempt.wonToss) NeonGreen else CrimsonHot
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isLatest) Color.White.copy(alpha = 0.06f)
                                                else Color.White.copy(alpha = 0.025f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Attempt ${attempt.attemptNumber}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                if (isLatest) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(GoldStar.copy(alpha = 0.2f))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "LATEST",
                                                            color = GoldStar,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = timeFormatter.format(Date(attempt.createdAt)),
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = "Your choice: ${attempt.userChoice.displayName}  •  Result: ${attempt.result.displayName}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Outcome: ${if (attempt.wonToss) "Won" else "Lost"}",
                                            color = outcomeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Metadata Footer
                item {
                    GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BreakdownRow("Match ID", activeDetails.id.take(16))
                            BreakdownRow("Created", dateFormatter.format(Date(activeDetails.createdAt)))
                            if (activeDetails.completedAt != null) {
                                BreakdownRow("Completed", dateFormatter.format(Date(activeDetails.completedAt)))
                            }
                            BreakdownRow("Participating Players", "${activeDetails.totalParticipatingPlayers}")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Delete Match Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text(
                            text = "Delete ${activeDetails.displayTitle}?",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Deleting this match will permanently remove this match record and all associated toss history. Player profiles, global fairness settings, and other saved matches will remain intact."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteMatchRecord(activeDetails.id) {
                                    onNavigateBack()
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = CrimsonHot),
                            modifier = Modifier.testTag("confirm_delete_match_button")
                        ) {
                            Text("Delete Match", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoPill(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
