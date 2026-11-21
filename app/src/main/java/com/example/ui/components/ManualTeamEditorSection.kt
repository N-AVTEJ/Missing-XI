package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.ArrangementSignatureUtil
import com.example.util.ArrangementValidationResult
import com.example.util.GeneratedTeam
import com.example.util.ManualEditState
import kotlin.math.roundToInt

@Composable
fun ManualTeamEditorSection(
    state: ManualEditState,
    onPlayerTap: (String) -> Unit,
    onMovePlayer: (playerId: String, destinationTeamId: String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetChanges: () -> Unit,
    onToggleTemporaryImbalance: (Boolean) -> Unit,
    onDismissError: () -> Unit,
    onValidateForConfirm: () -> ArrangementValidationResult?,
    onConfirmTeams: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showFinalConfirmDialog by remember { mutableStateOf(false) }
    var pendingConfirmValidation by remember { mutableStateOf<ArrangementValidationResult?>(null) }

    val beforeScore = state.originalFairness.fairnessScore
    val afterScore = state.currentFairness.fairnessScore
    val scoreDelta = afterScore - beforeScore

    val selectedPlayer = state.selectedPlayerId?.let { state.playerMap[it] }
    val selectedTeamIndex = state.selectedPlayerId?.let { pid ->
        state.currentArrangement.teams.indexOfFirst { pid in it.playerIds }
    } ?: -1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("manual_team_editor_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Card
        GlassyCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Teams",
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "EDIT TEAMS",
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Tap two players to swap, or select one to move",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = onCancelEdit,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("exit_manual_edit_btn")
                    ) {
                        Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Allow Temporary Imbalance Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Temporary Imbalance",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (state.allowTemporaryImbalance) {
                                "Multi-step moves enabled; balance sizes before confirming"
                            } else {
                                "Preserves balanced team sizes via atomic swaps"
                            },
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = state.allowTemporaryImbalance,
                        onCheckedChange = onToggleTemporaryImbalance,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonBlue,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("allow_temp_imbalance_switch")
                    )
                }
            }
        }

        // 2. Rejected Operation / Hard Validation Error Banner
        val activeErrors = buildList {
            state.lastOperationError?.let { add(it) }
            state.validationResult.errors.forEach { err ->
                if (err != state.lastOperationError) add(err)
            }
        }
        AnimatedVisibility(
            visible = activeErrors.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonHot.copy(alpha = 0.14f)),
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
                        contentDescription = "Validation Error",
                        tint = CrimsonHot,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Action Blocked",
                            color = CrimsonHot,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        activeErrors.forEach { err ->
                            Text(
                                text = err,
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_edit_error_btn")
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

        // 3. Selected Player Action Bar (Move to Destination Team)
        AnimatedVisibility(
            visible = selectedPlayer != null && selectedTeamIndex >= 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (selectedPlayer != null && selectedTeamIndex >= 0) {
                val sourceTeamName = state.currentArrangement.teams[selectedTeamIndex].name
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonBlue.copy(alpha = 0.14f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonBlue.copy(alpha = 0.6f))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_player_action_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Selected: ${selectedPlayer.name} ($sourceTeamName)",
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Tap another player to Swap",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.currentArrangement.teams.forEachIndexed { idx, team ->
                                if (idx != selectedTeamIndex) {
                                    OutlinedButton(
                                        onClick = {
                                            onMovePlayer(
                                                selectedPlayer.id,
                                                ArrangementSignatureUtil.teamIdFor(team.teamNumber)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.6f))
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("move_to_team_${team.teamNumber}_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Move to ${team.name}",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Move to ${team.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Editable Team Columns / Cards
        val teams = state.currentArrangement.teams
        if (teams.size == 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                teams.forEachIndexed { idx, team ->
                    EditableTeamCard(
                        team = team,
                        accentColor = if (idx == 0) NeonGreen else NeonBlue,
                        state = state,
                        onPlayerTap = onPlayerTap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                teams.forEachIndexed { idx, team ->
                    val accentColor = when (idx % 4) {
                        0 -> NeonGreen
                        1 -> NeonBlue
                        2 -> IndigoAccent
                        else -> GoldStar
                    }
                    EditableTeamCard(
                        team = team,
                        accentColor = accentColor,
                        state = state,
                        onPlayerTap = onPlayerTap,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Joker read-only reminder (if present)
        if (!state.currentArrangement.joker.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FuchsiaAccent.copy(alpha = 0.1f))
                    .border(1.dp, FuchsiaAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Joker",
                        tint = FuchsiaAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Joker: ${state.currentArrangement.joker}",
                        color = FuchsiaAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Fixed Role",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        // 5. Undo / Redo Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = state.canUndo,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.DarkGray
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = state.canUndo).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (state.canUndo) NeonBlue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("manual_undo_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.undoCount > 0) "Undo (${state.undoCount})" else "Undo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = onRedo,
                enabled = state.canRedo,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.DarkGray
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = state.canRedo).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (state.canRedo) NeonBlue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("manual_redo_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.redoCount > 0) "Redo (${state.redoCount})" else "Redo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // 6. Live Fairness Score & Comparison Card
        GlassyCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_fairness_card"),
            cornerRadius = 18
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$afterScore/100",
                                color = when {
                                    afterScore >= 80 -> NeonGreen
                                    afterScore >= 65 -> GoldStar
                                    else -> CrimsonHot
                                },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (state.hasUnsavedChanges) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val deltaPrefix = if (scoreDelta > 0) "+" else ""
                                val deltaColor = when {
                                    scoreDelta > 0 -> NeonGreen
                                    scoreDelta < 0 -> CrimsonHot
                                    else -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(deltaColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Before: $beforeScore ($deltaPrefix$scoreDelta)",
                                        color = deltaColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (state.currentFairness.qualityResult.passed) NeonGreen.copy(alpha = 0.15f)
                                else GoldStar.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (state.currentFairness.qualityResult.passed) state.currentFairness.fairnessRating else "Below Target",
                            color = if (state.currentFairness.qualityResult.passed) NeonGreen else GoldStar,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill("Strength Diff", "${state.currentFairness.strengthAnalysis.strengthDifference}", NeonBlue)
                    MetricPill("Teammate Pen.", "${state.currentFairness.evaluation.teammatePenalty}", IndigoAccent)
                    MetricPill("Opponent Pen.", "${state.currentFairness.evaluation.opponentPenalty}", FuchsiaAccent)
                }
            }
        }

        // 7. Duplicate Arrangement Warning Banner
        if (state.isDuplicateArrangement) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GoldStar.copy(alpha = 0.15f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GoldStar)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_duplicate_warning_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Duplicate arrangement",
                        tint = GoldStar,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "This arrangement has already been used.",
                            color = GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "You can still confirm it, and it will not increase your unique arrangements count.",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 8. Non-Blocking Fairness & Temporary Imbalance Warnings
        val nonDuplicateWarnings = state.validationResult.warnings.filter {
            it != "This arrangement has already been used."
        }
        if (nonDuplicateWarnings.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GoldStar.copy(alpha = 0.1f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(GoldStar.copy(alpha = 0.5f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_fairness_warnings_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Fairness Warning",
                            tint = GoldStar,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fairness & Balance Warnings",
                            color = GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    nonDuplicateWarnings.forEach { warning ->
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

        // 9. Reset Changes & Confirm Teams Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (state.hasUnsavedChanges) {
                        showResetConfirmDialog = true
                    }
                },
                enabled = state.hasUnsavedChanges,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CrimsonHot,
                    disabledContentColor = Color.DarkGray
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = state.hasUnsavedChanges).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (state.hasUnsavedChanges) CrimsonHot.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("manual_reset_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Changes",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val check = onValidateForConfirm()
                    if (check != null && check.valid) {
                        pendingConfirmValidation = check
                        showFinalConfirmDialog = true
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("manual_confirm_btn")
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

    // Reset Changes Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text("Reset Changes?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Restore the originally generated teams from this shuffle? Your manual edits will be discarded without modifying match history.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetChanges()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonHot),
                    modifier = Modifier.testTag("confirm_reset_changes_btn")
                ) {
                    Text("Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Keep Editing", color = Color.Gray)
                }
            }
        )
    }

    // Final Confirmation Modal Dialog
    if (showFinalConfirmDialog) {
        val confirmWarnings = pendingConfirmValidation?.warnings ?: state.validationResult.warnings
        AlertDialog(
            onDismissRequest = { showFinalConfirmDialog = false },
            containerColor = SurfaceDark,
            modifier = Modifier.testTag("confirm_final_teams_dialog"),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Final Teams", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Summary of final teams
                    state.currentArrangement.teams.forEach { team ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(team.name, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("STR ${team.totalStrength} • ${team.players.size} Players", color = NeonBlue, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = team.players.joinToString(", "),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Fairness Delta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fairness Score", color = Color.LightGray, fontSize = 12.sp)
                        Text(
                            text = if (state.hasUnsavedChanges) "$beforeScore → $afterScore/100" else "$afterScore/100",
                            color = if (afterScore >= beforeScore) NeonGreen else GoldStar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (confirmWarnings.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldStar.copy(alpha = 0.12f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("Warnings", color = GoldStar, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            confirmWarnings.forEach { w ->
                                Text("• $w", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalConfirmDialog = false
                        onConfirmTeams()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.testTag("confirm_save_teams_btn")
                ) {
                    Text("Confirm & Save", color = DeepBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalConfirmDialog = false }) {
                    Text("Keep Editing", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun EditableTeamCard(
    team: GeneratedTeam,
    accentColor: Color,
    state: ManualEditState,
    onPlayerTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier.testTag("editable_team_card_${team.teamNumber}"),
        cornerRadius = 18
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = team.name,
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${team.players.size}P • ${team.totalStrength}★",
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                team.playerIds.forEachIndexed { idx, playerId ->
                    val playerName = team.players.getOrNull(idx) ?: state.playerMap[playerId]?.name ?: playerId
                    val skill = state.playerMap[playerId]?.skillRating ?: 5
                    val isSelected = state.selectedPlayerId == playerId
                    val isLocked = state.constraints[playerId]?.isLocked == true
                    val group = state.fixedGroups.firstOrNull { playerId in it.playerIds }

                    val bgColor = when {
                        isSelected -> NeonBlue.copy(alpha = 0.25f)
                        isLocked -> NeonGreen.copy(alpha = 0.08f)
                        group != null -> FuchsiaAccent.copy(alpha = 0.08f)
                        else -> Color.White.copy(alpha = 0.04f)
                    }
                    val borderColor = when {
                        isSelected -> NeonBlue
                        isLocked -> NeonGreen.copy(alpha = 0.4f)
                        group != null -> FuchsiaAccent.copy(alpha = 0.4f)
                        else -> Color.White.copy(alpha = 0.08f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { onPlayerTap(playerId) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("edit_player_$playerId"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playerName,
                                color = if (isSelected) NeonBlue else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${skill}★",
                                color = GoldStar,
                                fontSize = 10.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (group != null) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Fixed Group ${group.name}",
                                    tint = FuchsiaAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Selected",
                                    tint = NeonBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
