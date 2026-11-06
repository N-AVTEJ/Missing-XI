package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.LockedPlayerGroup
import com.example.util.Player
import com.example.util.PlayerMatchConstraint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerConstraintDialog(
    player: Player,
    numTeams: Int,
    currentConstraint: PlayerMatchConstraint?,
    existingGroups: List<LockedPlayerGroup>,
    onSaveConstraint: (PlayerMatchConstraint) -> Unit,
    onAssignToGroup: (groupId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isLocked by remember { mutableStateOf(currentConstraint?.isLocked == true) }
    var selectedTeamIndex by remember { mutableStateOf(currentConstraint?.lockedTeamIndex) }
    var isExcluded by remember { mutableStateOf(currentConstraint?.isExcluded == true) }

    val currentGroupId = existingGroups.firstOrNull { it.playerIds.contains(player.id) }?.id
    var selectedGroupId by remember { mutableStateOf(currentGroupId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Skill Rating: ${player.skillRating}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldStar
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ID: ${player.id.take(8)}",
                        color = NeonBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Exclude Player
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExcluded) CrimsonHot.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isExcluded) CrimsonHot.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Exclude",
                            tint = if (isExcluded) CrimsonHot else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exclude from Shuffle",
                                color = if (isExcluded) CrimsonHot else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Will not be placed on any team or Joker",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Switch(
                        checked = isExcluded,
                        onCheckedChange = {
                            isExcluded = it
                            if (it) {
                                isLocked = false
                                selectedTeamIndex = null
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CrimsonHot,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("exclude_switch_${player.id}")
                    )
                }

                // Section 2: Team Locking (Disabled if excluded)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = if (isExcluded) Color.DarkGray else NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LOCK TO TEAM",
                            color = if (isExcluded) Color.DarkGray else NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Team options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Unlocked Option
                        val isNone = !isLocked || selectedTeamIndex == null
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isNone) NeonGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    1.dp,
                                    if (isNone) NeonGreen else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !isExcluded) {
                                    isLocked = false
                                    selectedTeamIndex = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Lock",
                                color = if (isNone) NeonGreen else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Teams (Team A, Team B, ...)
                        for (i in 0 until numTeams) {
                            val isSelected = isLocked && selectedTeamIndex == i
                            val teamColor = when (i % 4) {
                                0 -> NeonGreen
                                1 -> NeonBlue
                                2 -> IndigoAccent
                                else -> GoldStar
                            }
                            val teamLabel = "Team " + (('A' + i).toString())

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) teamColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.03f))
                                    .border(
                                        1.dp,
                                        if (isSelected) teamColor else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = !isExcluded) {
                                        isLocked = true
                                        selectedTeamIndex = i
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("lock_team_${i}_btn_${player.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = teamLabel,
                                    color = if (isSelected) teamColor else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Section 3: Keep Together (Fixed Group)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Groups",
                            tint = if (isExcluded) Color.DarkGray else FuchsiaAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "KEEP TOGETHER (FIXED GROUP)",
                            color = if (isExcluded) Color.DarkGray else FuchsiaAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (existingGroups.isEmpty()) {
                        Text(
                            text = "No fixed groups created yet. Use 'Manage Fixed Groups' on the match screen to form a pair or squad.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // "None" option
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedGroupId == null) FuchsiaAccent.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable(enabled = !isExcluded) { selectedGroupId = null }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = selectedGroupId == null,
                                    onClick = { if (!isExcluded) selectedGroupId = null },
                                    colors = RadioButtonDefaults.colors(selectedColor = FuchsiaAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("No Fixed Group", color = Color.White, fontSize = 12.sp)
                            }

                            // Group list
                            existingGroups.forEach { group ->
                                val isSelected = selectedGroupId == group.id
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) FuchsiaAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f))
                                        .clickable(enabled = !isExcluded) { selectedGroupId = group.id }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (!isExcluded) selectedGroupId = group.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = FuchsiaAccent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(group.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${group.playerIds.size} members", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                onClick = {
                    val constraint = PlayerMatchConstraint(
                        playerId = player.id,
                        isLocked = isLocked && selectedTeamIndex != null && !isExcluded,
                        lockedTeamIndex = if (isLocked && !isExcluded) selectedTeamIndex else null,
                        isExcluded = isExcluded
                    )
                    onSaveConstraint(constraint)
                    onAssignToGroup(if (!isExcluded) selectedGroupId else null)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_constraint_btn_${player.id}")
            ) {
                Text("Save", color = DeepBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
