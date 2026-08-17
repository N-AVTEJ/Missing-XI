package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerEntity
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.util.PlayerChemistryEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    viewModel: AppViewModel,
    playerId: String,
    onNavigateBack: () -> Unit
) {
    val allActivePlayers by viewModel.allActivePlayers.collectAsState()
    val teammatePairCounts by viewModel.teammatePairCounts.collectAsState()
    val opponentPairCounts by viewModel.opponentPairCounts.collectAsState()

    val player = remember(allActivePlayers, playerId) {
        allActivePlayers.find { it.id == playerId }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    FrostedMeshBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Player Profile",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        player?.let { p ->
                            IconButton(onClick = { viewModel.togglePlayerFavorite(p) }) {
                                Icon(
                                    imageVector = if (p.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (p.isFavorite) GoldStar else Color.Gray
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (player == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Player not found", color = Color.Gray)
                }
            } else {
                val matchesCount = player.matchesPlayed.coerceAtLeast(player.totalMatches)
                val jokerCount = player.matchesAsJoker.coerceAtLeast(player.totalTimesJoker)
                val lastPlayed = player.lastPlayedAt.coerceAtLeast(player.lastUsedAt)
                val lastPlayedStr = if (lastPlayed > 0) dateFormat.format(Date(lastPlayed)) else "Never"

                val favTeammateInfo = remember(player, teammatePairCounts) {
                    PlayerChemistryEngine.getFavoriteTeammate(player.displayName, teammatePairCounts)
                }
                val favOpponentInfo = remember(player, opponentPairCounts) {
                    PlayerChemistryEngine.getFavoriteOpponent(player.displayName, opponentPairCounts)
                }

                var skillRating by remember(player.skillRating) { mutableFloatStateOf(player.skillRating.toFloat()) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card with Avatar
                    item {
                        GlassyCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Avatar Circle with Initials
                                val initials = player.displayName.take(2).uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(NeonBlue, FuchsiaAccent)
                                            )
                                        )
                                        .border(2.dp, GoldStar.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = player.displayName,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (!player.nickname.isNullOrBlank()) {
                                    Text(
                                        text = "\"${player.nickname}\"",
                                        color = NeonBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Favorite & Active Status Badges
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (player.isFavorite) {
                                        Surface(
                                            color = GoldStar.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = GoldStar,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Favorite Player", color = GoldStar, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Surface(
                                        color = NeonGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = NeonGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Active", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Key Stats Grid Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Matches Played
                            GlassyCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 16
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🏏", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$matchesCount",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Matches Played",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Times Joker
                            GlassyCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 16
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🃏", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$jokerCount",
                                        color = FuchsiaAccent,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Times Joker",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Current Streak
                            GlassyCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 16
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🔥", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${player.currentPlayStreak}",
                                        color = GoldStar,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Play Streak",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Skill Rating Control Card
                    item {
                        GlassyCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⭐", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Skill Rating",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Text(
                                        text = "⭐ ${skillRating.toInt()} / 10",
                                        color = GoldStar,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Slider(
                                    value = skillRating,
                                    onValueChange = {
                                        skillRating = it
                                        viewModel.updatePlayerSkill(player, it.toInt())
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GoldStar,
                                        activeTrackColor = GoldStar,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("1 (Beginner)", color = Color.Gray, fontSize = 11.sp)
                                    Text("5 (Intermediate)", color = Color.Gray, fontSize = 11.sp)
                                    Text("10 (Elite)", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Player Chemistry Section (Favorite Teammate & Favorite Opponent)
                    item {
                        GlassyCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "PLAYER CHEMISTRY",
                                    color = NeonBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Favorite Teammate
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🤝", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Favorite Teammate",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = favTeammateInfo?.idOrName ?: player.favoriteTeammateId ?: "None Recorded",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    if (favTeammateInfo != null) {
                                        Text(
                                            text = "${favTeammateInfo.matchCount} Matches",
                                            color = NeonGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NeonGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                // Favorite Opponent
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚔️", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Favorite Opponent",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = favOpponentInfo?.idOrName ?: player.favoriteOpponentId ?: "None Recorded",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    if (favOpponentInfo != null) {
                                        Text(
                                            text = "${favOpponentInfo.matchCount} Matches",
                                            color = FuchsiaAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(FuchsiaAccent.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Activity Details Card
                    item {
                        GlassyCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "ACTIVITY HISTORY",
                                    color = NeonBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Last Played At", color = Color.Gray, fontSize = 13.sp)
                                    Text(lastPlayedStr, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Unique Teammates", color = Color.Gray, fontSize = 13.sp)
                                    Text("${player.totalTeammates}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Unique Opponents", color = Color.Gray, fontSize = 13.sp)
                                    Text("${player.totalOpponents}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
