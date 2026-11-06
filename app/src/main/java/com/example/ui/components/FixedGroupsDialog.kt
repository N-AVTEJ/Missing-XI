package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun FixedGroupsDialog(
    activePlayers: List<Player>,
    constraints: Map<String, PlayerMatchConstraint>,
    groups: List<LockedPlayerGroup>,
    onCreateGroup: (name: String, playerIds: List<String>) -> Unit,
    onDeleteGroup: (groupId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val selectedPlayerIds = remember { mutableStateListOf<String>() }
    var creationError by remember { mutableStateOf<String?>(null) }

    val eligiblePlayers = activePlayers.filter { constraints[it.id]?.isExcluded != true }
    val playerMap = activePlayers.associateBy { it.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Fixed Groups",
                        tint = FuchsiaAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCreatingNew) "New Fixed Group" else "Fixed Player Groups",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                if (!isCreatingNew) {
                    IconButton(
                        onClick = {
                            isCreatingNew = true
                            newGroupName = "Group ${groups.size + 1}"
                            selectedPlayerIds.clear()
                            creationError = null
                        },
                        modifier = Modifier.testTag("add_fixed_group_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Group",
                            tint = FuchsiaAccent
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isCreatingNew) {
                    // Group Name Input
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FuchsiaAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )

                    Text(
                        text = "Select at least 2 players to stay on the same team:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    if (creationError != null) {
                        Text(
                            text = creationError!!,
                            color = CrimsonHot,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Player Selection List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(eligiblePlayers) { player ->
                            val isAssignedToOtherGroup = groups.any { g -> g.playerIds.contains(player.id) }
                            val isSelected = selectedPlayerIds.contains(player.id)
                            val isLockedToTeam = constraints[player.id]?.isLocked == true

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> FuchsiaAccent.copy(alpha = 0.2f)
                                            isAssignedToOtherGroup -> Color.White.copy(alpha = 0.02f)
                                            else -> Color.White.copy(alpha = 0.05f)
                                        }
                                    )
                                    .clickable(enabled = !isAssignedToOtherGroup) {
                                        if (isSelected) {
                                            selectedPlayerIds.remove(player.id)
                                        } else {
                                            selectedPlayerIds.add(player.id)
                                        }
                                        creationError = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("group_player_item_${player.id}")
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (!isAssignedToOtherGroup) {
                                            if (checked) selectedPlayerIds.add(player.id)
                                            else selectedPlayerIds.remove(player.id)
                                            creationError = null
                                        }
                                    },
                                    enabled = !isAssignedToOtherGroup,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FuchsiaAccent,
                                        uncheckedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        color = if (isAssignedToOtherGroup) Color.DarkGray else Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    if (isAssignedToOtherGroup) {
                                        Text(
                                            text = "Already in another group",
                                            color = Color.DarkGray,
                                            fontSize = 10.sp
                                        )
                                    } else if (isLockedToTeam) {
                                        val label = constraints[player.id]?.getLockedTeamLabel() ?: "Team"
                                        Text(
                                            text = "Locked to $label",
                                            color = NeonGreen,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // View Existing Groups
                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No fixed groups active",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Tap the + button above to create a fixed pair or squad.",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(groups) { group ->
                                val members = group.playerIds.mapNotNull { playerMap[it]?.name }
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(FuchsiaAccent.copy(alpha = 0.3f))
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("fixed_group_card_${group.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(FuchsiaAccent)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = group.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${group.playerIds.size} players)",
                                                    color = FuchsiaAccent,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = members.joinToString(", "),
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteGroup(group.id) },
                                            modifier = Modifier.testTag("delete_group_btn_${group.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Group",
                                                tint = CrimsonHot
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = FuchsiaAccent),
                    onClick = {
                        val trimmedName = newGroupName.trim()
                        if (trimmedName.isEmpty()) {
                            creationError = "Please enter a group name."
                            return@Button
                        }
                        if (selectedPlayerIds.size < 2) {
                            creationError = "Select at least 2 players for this group."
                            return@Button
                        }
                        onCreateGroup(trimmedName, selectedPlayerIds.toList())
                        isCreatingNew = false
                    },
                    modifier = Modifier.testTag("save_new_group_btn")
                ) {
                    Text("Save Group", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_groups_dialog_btn")
                ) {
                    Text("Done", color = DeepBg, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isCreatingNew) {
                        isCreatingNew = false
                        creationError = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (isCreatingNew) "Back" else "Close", color = Color.Gray)
            }
        }
    )
}
