package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.components.StadiumPitch
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.PlayerSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(viewModel: AppViewModel) {
    val teamName by viewModel.teamName.collectAsState()
    val selectedFormation by viewModel.selectedFormation.collectAsState()
    val selectedSport by viewModel.selectedSport.collectAsState()
    val playerSlots by viewModel.playerSlots.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val pitchThemeColor by viewModel.pitchThemeColor.collectAsState()

    var editingSlot by remember { mutableStateOf<PlayerSlot?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    var draftPlayerName by remember { mutableStateOf("") }

    FrostedMeshBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "LINEUP BUILDER",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            // Squad Meta Settings Card
            item {
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Team Name Input
                        OutlinedTextField(
                            value = teamName,
                            onValueChange = { viewModel.updateTeamName(it) },
                            label = { Text("Team Name", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IndigoAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedLabelColor = IndigoAccent,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("team_name_input")
                        )

                        // Sport Select
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateSportType("Football") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSport == "Football") IndigoAccent else Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SportsSoccer,
                                        contentDescription = "Football",
                                        tint = if (selectedSport == "Football") Color.White else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Football", color = Color.White)
                                }
                            }

                            Button(
                                onClick = { viewModel.updateSportType("Cricket") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSport == "Cricket") IndigoAccent else Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SportsCricket,
                                        contentDescription = "Cricket",
                                        tint = if (selectedSport == "Cricket") Color.White else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cricket", color = Color.White)
                                }
                            }
                        }

                        // Formation Select (Only for Football)
                        if (selectedSport == "Football") {
                            Column {
                                Text("Select Tactical Formation", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("4-3-3", "4-4-2", "3-5-2").forEach { formation ->
                                        val isActive = selectedFormation == formation
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isActive) IndigoAccent else Color.White.copy(alpha = 0.05f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .background(if (isActive) IndigoAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.02f))
                                                .clickable { viewModel.updateFormation(formation) }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = formation,
                                                color = if (isActive) Color.White else Color.LightGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Beautiful Stadium View overlay with slots
            item {
                Text(
                    text = "PITCH PREVIEW (Tap player nodes to edit name)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                ) {
                    StadiumPitch(
                        modifier = Modifier.fillMaxSize(),
                        themeColor = pitchThemeColor
                    )

                    // Overlay Player slots
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val pitchWidth = maxWidth
                        val pitchHeight = maxHeight

                        playerSlots.forEach { slot ->
                            val xOffset = pitchWidth * slot.xPercent - 36.dp
                            val yOffset = pitchHeight * slot.yPercent - 36.dp

                            Box(
                                modifier = Modifier
                                    .offset(x = xOffset, y = yOffset)
                                    .size(72.dp)
                                    .clickable {
                                        editingSlot = slot
                                        draftPlayerName = slot.name
                                        showEditDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Player dot (glowing Indigo & Fuchsia hybrid)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(IndigoAccent, FuchsiaAccent.copy(alpha = 0.8f))
                                                )
                                            )
                                            .border(1.5.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = slot.positionLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Player Name Tag (Semi-transparent frosted chip)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = slot.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Saving Panel
            item {
                NeonButton(
                    text = "Save Lineup to SQLite",
                    onClick = { viewModel.saveActiveLineup() },
                    modifier = Modifier.fillMaxWidth().testTag("save_lineup_button"),
                    glowingColor = IndigoAccent
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Notification popup for saved status
        AnimatedVisibility(
            visible = saveMessage.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = IndigoAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = saveMessage, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Popup Dialog for renaming Player
        if (showEditDialog && editingSlot != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = SurfaceDark,
                title = { Text("Edit Player #${editingSlot!!.index + 1} (${editingSlot!!.positionLabel})", color = Color.White) },
                text = {
                    Column {
                        Text("Enter name for this lineup position:", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = draftPlayerName,
                            onValueChange = { draftPlayerName = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IndigoAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedLabelColor = IndigoAccent,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("player_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                        onClick = {
                            viewModel.updatePlayerName(editingSlot!!.index, draftPlayerName)
                            showEditDialog = false
                        }
                    ) {
                        Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

