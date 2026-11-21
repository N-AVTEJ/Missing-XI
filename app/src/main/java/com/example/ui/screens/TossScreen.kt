package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
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
import com.example.ui.components.RealisticCoinFlipView
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.GoldStar
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.MatchStatus
import com.example.util.TossChoice
import com.example.util.TossHistoryRecord
import com.example.util.TossStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TossScreen(
    viewModel: AppViewModel,
    onMatchFinalized: (String) -> Unit = {},
    onOpenCompletedSummary: (String) -> Unit = {}
) {
    val tossState by viewModel.matchTossUiState.collectAsState()
    val savedTosses by viewModel.savedTosses.collectAsState()
    val generatedTeams by viewModel.generatedTeams.collectAsState()

    val isFlipping = tossState.tossStatus == TossStatus.IN_PROGRESS
    val isMatchCompleted = !tossState.isStandaloneMode && tossState.matchStatus == MatchStatus.COMPLETED
    val hasCompletedToss = tossState.tossStatus == TossStatus.COMPLETED && tossState.latestTossResult != null
    val canSelectChoice = !isFlipping && !isMatchCompleted
    val canFlip = tossState.selectedChoice != null && !isFlipping && !isMatchCompleted

    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.syncTossScreen()
    }

    // Prevent accidental back navigation while coin flip animation is running
    BackHandler(enabled = isFlipping) {
        // Intentionally consume back press until animation completes
    }

    FrostedMeshBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (tossState.isStandaloneMode) IndigoAccent.copy(alpha = 0.2f)
                                else if (isMatchCompleted) NeonGreen.copy(alpha = 0.18f)
                                else GoldStar.copy(alpha = 0.18f)
                            )
                            .border(
                                1.dp,
                                if (tossState.isStandaloneMode) IndigoAccent.copy(alpha = 0.45f)
                                else if (isMatchCompleted) NeonGreen.copy(alpha = 0.45f)
                                else GoldStar.copy(alpha = 0.5f),
                                RoundedCornerShape(99.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = when {
                                tossState.isStandaloneMode -> "QUICK COIN TOSS"
                                isMatchCompleted -> "MATCH COMPLETED"
                                else -> "MATCH READY"
                            },
                            color = when {
                                tossState.isStandaloneMode -> IndigoAccent
                                isMatchCompleted -> NeonGreen
                                else -> GoldStar
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.testTag("toss_header_badge")
                        )
                    }

                    Text(
                        text = if (isMatchCompleted) "Toss Completed" else "Who wins the toss?",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (tossState.isStandaloneMode && generatedTeams.isNotEmpty()) {
                        Text(
                            text = "Confirm your teams in the Teams tab to attach this toss to your match.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    } else if (!tossState.isStandaloneMode && !isMatchCompleted) {
                        Text(
                            text = "Teams are locked & confirmed. Call Heads or Tails to begin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Error Alert Banner (e.g., DB write failure or invalid action)
            item {
                AnimatedVisibility(
                    visible = !tossState.errorMessage.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CrimsonHot.copy(alpha = 0.16f))
                            .border(1.dp, CrimsonHot.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("toss_error_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Toss Error",
                                tint = CrimsonHot,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tossState.errorMessage.orEmpty(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearTossError() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Realistic 3D Animated Coin
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    RealisticCoinFlipView(
                        tossStatus = tossState.tossStatus,
                        targetResult = tossState.pendingResult ?: tossState.latestTossResult?.result,
                        flipSequenceNumber = tossState.flipSequenceNumber,
                        modifier = Modifier.testTag("interactive_coin_view")
                    )
                }
            }

            // Result Display OR Heads/Tails Selection
            if (hasCompletedToss) {
                val result = tossState.latestTossResult!!
                item {
                    TossOutcomeCard(
                        userChoice = result.userChoice,
                        coinResult = result.result,
                        wonToss = result.wonToss,
                        attemptNumber = result.attemptNumber
                    )
                }

                // Post-Toss Action Controls: Continue + Flip Again
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!tossState.isStandaloneMode) {
                            val activeMatchId = tossState.matchId
                            if (isMatchCompleted && !activeMatchId.isNullOrBlank()) {
                                NeonButton(
                                    text = "View Match Summary",
                                    onClick = { onOpenCompletedSummary(activeMatchId) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("view_completed_match_summary_button"),
                                    glowingColor = NeonGreen,
                                    enabled = true
                                )
                            } else {
                                NeonButton(
                                    text = "Continue",
                                    onClick = {
                                        viewModel.finalizeCurrentMatch { finalizedId ->
                                            onMatchFinalized(finalizedId)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("toss_continue_button"),
                                    glowingColor = NeonGreen,
                                    enabled = !isFlipping
                                )
                            }
                        }

                        if (!isMatchCompleted) {
                            OutlinedButton(
                                onClick = { viewModel.prepareFlipAgain() },
                                enabled = !isFlipping,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldStar),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = !isFlipping).copy(
                                    brush = SolidColor(GoldStar.copy(alpha = 0.65f))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("toss_flip_again_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Flip Again",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Flip Again",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Heads / Tails Selection Cards (mutually exclusive, unselected initially)
                item {
                    GlassyCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = if (isFlipping) "COIN IN THE AIR..." else "SELECT YOUR CALL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TossChoiceCard(
                                    choice = TossChoice.HEADS,
                                    isSelected = tossState.selectedChoice == TossChoice.HEADS,
                                    enabled = canSelectChoice,
                                    accentColor = GoldStar,
                                    icon = Icons.Default.Star,
                                    onClick = { viewModel.selectTossChoice(TossChoice.HEADS) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("toss_choice_heads")
                                )
                                TossChoiceCard(
                                    choice = TossChoice.TAILS,
                                    isSelected = tossState.selectedChoice == TossChoice.TAILS,
                                    enabled = canSelectChoice,
                                    accentColor = IndigoAccent,
                                    icon = Icons.Default.Shield,
                                    onClick = { viewModel.selectTossChoice(TossChoice.TAILS) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("toss_choice_tails")
                                )
                            }
                        }
                    }
                }

                // FLIP COIN Primary Action Button (disabled until choice selected and during flip)
                item {
                    NeonButton(
                        text = when {
                            isFlipping -> "FLIPPING COIN..."
                            tossState.selectedChoice == null -> "SELECT HEADS OR TAILS"
                            else -> "FLIP COIN"
                        },
                        onClick = { viewModel.executeCoinFlip() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flip_coin_button"),
                        glowingColor = if (tossState.selectedChoice == TossChoice.HEADS) GoldStar else IndigoAccent,
                        enabled = canFlip
                    )
                }
            }

            // Current Match Toss History (chronological with Latest badge)
            if (tossState.matchTossHistory.isNotEmpty()) {
                item {
                    MatchTossHistoryCard(
                        history = tossState.matchTossHistory,
                        timeFormatter = timeFormatter
                    )
                }
            }

            // All-Time Toss Stats Summary (preserved from existing app)
            item {
                val total = savedTosses.size
                val wins = savedTosses.count { it.hasWon }
                val losses = total - wins

                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ALL-TIME TOSS STATS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
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

@Composable
private fun TossChoiceCard(
    choice: TossChoice,
    isSelected: Boolean,
    enabled: Boolean,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                if (isSelected) accentColor.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.03f)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = choice.displayName,
                    tint = if (isSelected) accentColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = choice.name,
                    color = if (isSelected) Color.White else Color.LightGray,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Selected",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TossOutcomeCard(
    userChoice: TossChoice,
    coinResult: TossChoice,
    wonToss: Boolean,
    attemptNumber: Int
) {
    val resultAccent = if (wonToss) NeonGreen else CrimsonHot

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("toss_result_card"),
        cornerRadius = 24
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = coinResult.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = if (coinResult == TossChoice.HEADS) GoldStar else Color(0xFFA5B4FC),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.testTag("toss_outcome_face")
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(resultAccent.copy(alpha = 0.18f))
                    .border(1.5.dp, resultAccent.copy(alpha = 0.6f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 20.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (wonToss) "YOU WON!" else "YOU LOST",
                    color = resultAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.testTag("toss_win_loss_label")
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your choice", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userChoice.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Coin result", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = coinResult.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Attempt", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "#$attemptNumber",
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchTossHistoryCard(
    history: List<TossHistoryRecord>,
    timeFormatter: SimpleDateFormat
) {
    val latestAttempt = history.maxOfOrNull { it.attemptNumber } ?: 0

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_toss_history_card"),
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
                        imageVector = Icons.Default.History,
                        contentDescription = "Match Toss History",
                        tint = GoldStar,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "MATCH TOSS HISTORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldStar,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "${history.size} Attempt${if (history.size == 1) "" else "s"}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            history.sortedByDescending { it.attemptNumber }.forEach { record ->
                val isLatest = record.attemptNumber == latestAttempt
                val badgeColor = if (record.wonToss) NeonGreen else CrimsonHot

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isLatest) Color.White.copy(alpha = 0.06f)
                            else Color.White.copy(alpha = 0.02f)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Attempt #${record.attemptNumber}",
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
                            text = "Choice: ${record.userChoice.displayName}  •  Result: ${record.result.displayName}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = timeFormatter.format(Date(record.createdAt)),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.16f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (record.wonToss) "WON" else "LOST",
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
