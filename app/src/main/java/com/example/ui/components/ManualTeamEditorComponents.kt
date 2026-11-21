package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.util.*

@Composable
fun ManualTeamEditorOverlay(
    session: ManualEditSession,
    allPlayers: List<Player>,
    constraints: Map<String, PlayerMatchConstraint>,
    fixedGroups: List<LockedPlayerGroup>,
    onMovePlayer: (playerId: String, destinationTeamId: String) -> Boolean,
    onSwapPlayers: (playerAId: String, playerBId: String) -> Boolean,
    onUndo: () -> Boolean,
    onRedo: () -> Boolean,
    onResetChanges: () -> Unit,
    onToggleAllowImbalance: (Boolean) -> Unit,
    onDismissError: () -> Unit,
    onRequestConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler {
        onCancel()
    }

    var interactionMode by remember { mutableStateOf("SWAP") } // "SWAP" or "MOVE"
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }

    val playersById = remember(allPlayers) { allPlayers.associateBy { it.id } }
    val arrangement = session.currentArrangement
    val currentFairness = session.currentFairness
    val originalFairness = session.originalFairness
    val scoreDelta = currentFairness.fairnessScore - originalFairness.fairnessScore

    // Clear selection if selected player is no longer valid
    LaunchedEffect(arrangement) {
        val activeIds = arrangement.teams.flatMap { it.playerIds }.toSet() +
            (arrangement.jokerPlayerId?.let { setOf(it) } ?: emptySet())
        if (selectedPlayerId != null && selectedPlayerId !in activeIds) {
            selectedPlayerId = null
        }
    }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EDIT TEAMS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = NeonGreen,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = if (interactionMode == "SWAP") {
                            if (selectedPlayerId == null) "Tap a player, then tap another to swap atomically"
                            else "Selected: ${playersById[selectedPlayerId]?.name ?: ""} — tap target player to swap"
                        } else {
                            if (selectedPlayerId == null) "Tap a player to choose a destination team"
                            else "Choose destination team for ${playersById[selectedPlayerId]?.name ?: ""}"
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("exit_manual_editor_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel editing",
                        tint = Color.LightGray
                    )
                }
            }

            // Main scrollable body
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Switcher & Undo/Redo Controls
                item {
                    GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Mode Switcher: Swap vs Move
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("SWAP" to "Swap Players", "MOVE" to "Move Player").forEach { (mode, label) ->
                                    val isSelected = interactionMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) IndigoAccent else Color.Transparent)
                                            .clickable {
                                                interactionMode = mode
                                                selectedPlayerId = null
                                            }
                                            .testTag("manual_mode_${mode.lowercase()}_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (mode == "SWAP") Icons.Default.SwapHoriz else Icons.Default.DriveFileMove,
                                                contentDescription = label,
                                                tint = if (isSelected) Color.White else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Undo / Redo Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedPlayerId = null
                                        onUndo()
                                    },
                                    enabled = session.canUndo,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("manual_edit_undo_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NeonBlue,
                                        disabledContentColor = Color.DarkGray
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Undo (${session.undoStack.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedPlayerId = null
                                        onRedo()
                                    },
                                    enabled = session.canRedo,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("manual_edit_redo_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NeonBlue,
                                        disabledContentColor = Color.DarkGray
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Redo",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Redo (${session.redoStack.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Allow Temporary Imbalance Toggle (relevant for Move mode)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Allow temporary team size imbalance",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Useful for multi-step moves; must rebalance before confirming",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = session.allowTemporarySizeImbalance,
                                    onCheckedChange = onToggleAllowImbalance,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GoldStar
                                    ),
                                    modifier = Modifier.testTag("toggle_temp_imbalance_switch")
                                )
                            }
                        }
                    }
                }

                // Live Fairness Score & Diff Card
                item {
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_edit_fairness_card"),
                        cornerRadius = 16
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "LIVE FAIRNESS SCORE",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${currentFairness.fairnessScore}/100",
                                            color = when {
                                                currentFairness.fairnessScore >= 80 -> NeonGreen
                                                currentFairness.fairnessScore >= 65 -> GoldStar
                                                else -> CrimsonHot
                                            },
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = currentFairness.fairnessRating,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }

                                if (session.hasChanges) {
                                    val deltaColor = when {
                                        scoreDelta > 0 -> NeonGreen
                                        scoreDelta < 0 -> CrimsonHot
                                        else -> Color.LightGray
                                    }
                                    val deltaPrefix = if (scoreDelta > 0) "+" else ""
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Before: ${originalFairness.fairnessScore}  →  After: ${currentFairness.fairnessScore}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(deltaColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$deltaPrefix$scoreDelta pts",
                                                color = deltaColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Compact component breakdown row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Strength Diff: ${currentFairness.strengthAnalysis.strengthDifference}",
                                    color = NeonBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Teammate Pen: ${currentFairness.fairnessEvaluation.teammatePenalty}",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Opp Pen: ${currentFairness.fairnessEvaluation.opponentPenalty}",
                                    color = FuchsiaAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Operation Rejection / Hard Validation Error Banner
                val activeError = session.lastOperationError ?: session.strictConfirmationValidation.errors.firstOrNull()
                if (activeError != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CrimsonHot.copy(alpha = 0.15f)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CrimsonHot)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_edit_error_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Edit Error",
                                    tint = CrimsonHot,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeError,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (session.lastOperationError != null) {
                                    IconButton(
                                        onClick = onDismissError,
                                        modifier = Modifier.size(24.dp)
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
                    }
                }

                // Non-Blocking Warnings & Duplicate Arrangement Banner
                val warnings = session.validationResult.warnings
                if (warnings.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = GoldStar.copy(alpha = 0.12f)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(GoldStar.copy(alpha = 0.6f))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_edit_warnings_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Fairness Warning",
                                        tint = GoldStar,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "FAIRNESS & DUPLICATE ADVISORIES",
                                        color = GoldStar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                warnings.forEach { warning ->
                                    Text(
                                        text = "• $warning",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Teams Cards
                items(arrangement.teams.size, key = { arrangement.teams[it].teamId }) { teamIdx ->
                    val team = arrangement.teams[teamIdx]
                    val accentColor = when (teamIdx % 4) {
                        0 -> NeonGreen
                        1 -> NeonBlue
                        2 -> IndigoAccent
                        else -> GoldStar
                    }

                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_edit_team_card_${team.teamId}"),
                        cornerRadius = 18
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = team.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${team.playerIds.size} Players",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "STR ${team.totalStrength}",
                                            color = accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // If Move mode and a player from ANOTHER team is selected, show Move Here button
                            val canMoveSelectedHere = interactionMode == "MOVE" &&
                                selectedPlayerId != null &&
                                selectedPlayerId !in team.playerIds &&
                                selectedPlayerId != arrangement.jokerPlayerId

                            AnimatedVisibility(
                                visible = canMoveSelectedHere,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Button(
                                    onClick = {
                                        val pid = selectedPlayerId
                                        if (pid != null) {
                                            if (onMovePlayer(pid, team.teamId)) {
                                                selectedPlayerId = null
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("move_to_${team.teamId}_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Move to ${team.name}",
                                        tint = DeepBg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Move ${playersById[selectedPlayerId]?.name ?: "Player"} to ${team.name}",
                                        color = DeepBg,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            // Players list
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                team.playerIds.forEachIndexed { idx, playerId ->
                                    val playerName = team.players.getOrNull(idx) ?: playersById[playerId]?.name ?: playerId
                                    val rating = playersById[playerId]?.skillRating ?: 5
                                    val isLocked = constraints[playerId]?.isLocked == true && constraints[playerId]?.lockedTeamIndex != null
                                    val group = fixedGroups.firstOrNull { playerId in it.playerIds }
                                    val isSelected = selectedPlayerId == playerId

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelected -> IndigoAccent.copy(alpha = 0.28f)
                                                    else -> Color.White.copy(alpha = 0.04f)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) IndigoAccent else Color.White.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (interactionMode == "SWAP") {
                                                    val currentSel = selectedPlayerId
                                                    if (currentSel == null) {
                                                        selectedPlayerId = playerId
                                                    } else if (currentSel == playerId) {
                                                        selectedPlayerId = null
                                                    } else {
                                                        onSwapPlayers(currentSel, playerId)
                                                        selectedPlayerId = null
                                                    }
                                                } else {
                                                    selectedPlayerId = if (isSelected) null else playerId
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("editor_player_row_$playerId")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Person,
                                                contentDescription = playerName,
                                                tint = if (isSelected) NeonGreen else accentColor.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = playerName,
                                                color = Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${rating}★",
                                                color = GoldStar,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (isLocked) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeonGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("🔒 Locked", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (group != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(FuchsiaAccent.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("👥 ${group.name}", color = FuchsiaAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Joker Card (Swappable with any unconstrained team player)
                val jokerId = arrangement.jokerPlayerId
                val jokerName = arrangement.jokerPlayerName
                if (jokerId != null && jokerName != null) {
                    item {
                        val isJokerSelected = selectedPlayerId == jokerId
                        GlassyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_edit_joker_card"),
                            cornerRadius = 16
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isJokerSelected) FuchsiaAccent.copy(alpha = 0.25f) else FuchsiaAccent.copy(alpha = 0.08f))
                                    .border(
                                        width = if (isJokerSelected) 1.5.dp else 1.dp,
                                        color = FuchsiaAccent.copy(alpha = if (isJokerSelected) 1f else 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (interactionMode == "SWAP") {
                                            val currentSel = selectedPlayerId
                                            if (currentSel == null) {
                                                selectedPlayerId = jokerId
                                            } else if (currentSel == jokerId) {
                                                selectedPlayerId = null
                                            } else {
                                                onSwapPlayers(currentSel, jokerId)
                                                selectedPlayerId = null
                                            }
                                        } else {
                                            selectedPlayerId = if (isJokerSelected) null else jokerId
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Joker",
                                        tint = FuchsiaAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "JOKER: $jokerName",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Tap in Swap mode to exchange Joker with an unconstrained player",
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Excluded players notice
                if (arrangement.excludedPlayers.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CrimsonHot.copy(alpha = 0.08f))
                                .border(1.dp, CrimsonHot.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Excluded (Cannot Join): ${arrangement.excludedPlayers.joinToString(", ") { it.name }}",
                                color = CrimsonHot,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Sticky Bottom Bar: Reset Changes + Confirm Teams
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedPlayerId = null
                            onResetChanges()
                        },
                        enabled = session.hasChanges,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("manual_edit_reset_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GoldStar,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Changes",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onRequestConfirm,
                        enabled = session.canConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            disabledContainerColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("manual_edit_confirm_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Confirm Teams",
                            tint = DeepBg,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Teams", color = DeepBg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmTeamsDialog(
    preview: AppViewModel.ConfirmationPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val arrangement = preview.arrangement
    val fairness = preview.fairness
    val originalFairness = preview.originalFairness
    val validation = preview.strictValidation

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confirm Final Teams",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                if (preview.wasManuallyEdited) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndigoAccent.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Edited", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fairness Score Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Final Fairness Score", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            text = "${fairness.fairnessScore}/100 (${fairness.fairnessRating})",
                            color = if (fairness.fairnessScore >= 70) NeonGreen else GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (preview.wasManuallyEdited) {
                        Text(
                            text = "Original: ${originalFairness.fairnessScore}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Final Teams List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    arrangement.teams.forEach { team ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(team.name, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("STR ${team.totalStrength}", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = team.players.joinToString(", "),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (!arrangement.jokerPlayerName.isNullOrBlank()) {
                        Text(
                            text = "Joker: ${arrangement.jokerPlayerName}",
                            color = FuchsiaAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Hard Validation Errors (blocks confirm)
                if (!validation.valid) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CrimsonHot.copy(alpha = 0.15f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Hard Constraint Errors:", color = CrimsonHot, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        validation.errors.forEach { err ->
                            Text("• $err", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // Duplicate Arrangement Notice
                if (fairness.isDuplicate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldStar.copy(alpha = 0.18f))
                            .border(1.dp, GoldStar.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "This arrangement has already been used. Confirming will save the match without increasing the unique arrangement count.",
                            color = GoldStar,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Non-blocking Fairness Warnings
                val nonDupWarnings = validation.warnings.filterNot { it.contains("already been used") }
                if (nonDupWarnings.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldStar.copy(alpha = 0.1f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Fairness Warnings:", color = GoldStar, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        nonDupWarnings.forEach { warn ->
                            Text("• $warn", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }

                Text(
                    text = "Confirming will save this match to history and update teammate, opponent, and player statistics once.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = validation.valid,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier.testTag("confirm_save_teams_btn")
            ) {
                Text("Confirm & Save", color = DeepBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
