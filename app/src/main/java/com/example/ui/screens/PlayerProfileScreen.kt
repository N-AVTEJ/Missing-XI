package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.FuchsiaAccent
import com.example.ui.theme.GoldStar
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.MatchStatus
import com.example.util.PlayerMatchTimelineEntry
import com.example.util.PlayerProfileAnalytics
import com.example.util.RelationshipFrequency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    viewModel: AppViewModel,
    playerId: String,
    onNavigateBack: () -> Unit,
    onOpenMatchDetails: (String) -> Unit = {}
) {
    val uiState by viewModel.playerProfileUiState.collectAsState()
    var showArchiveDialog by remember { mutableStateOf(false) }

    val createdDateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val matchDateFormatter = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }

    LaunchedEffect(playerId) {
        viewModel.selectPlayerProfile(playerId)
    }

    BackHandler {
        onNavigateBack()
    }

    val analytics = uiState.analytics
    val player = analytics?.player

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
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("player_profile_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        player?.let { p ->
                            IconButton(
                                onClick = { viewModel.togglePlayerFavorite(p) },
                                modifier = Modifier.testTag("player_profile_favorite_btn")
                            ) {
                                Icon(
                                    imageVector = if (p.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (p.isFavorite) "Remove favorite" else "Mark favorite",
                                    tint = if (p.isFavorite) GoldStar else Color.Gray
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                }

                analytics == null || player == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "Player not found",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                TextButton(onClick = onNavigateBack) {
                                    Text("Return to Directory", color = NeonBlue)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Status Feedback Banner
                        item {
                            AnimatedVisibility(visible = !uiState.statusBannerMessage.isNullOrBlank()) {
                                Surface(
                                    color = NeonGreen.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = uiState.statusBannerMessage.orEmpty(),
                                            color = NeonGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { viewModel.clearPlayerProfileStatusBanner() }) {
                                            Text("OK", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Player Information Header
                        item {
                            PlayerInformationCard(
                                analytics = analytics,
                                createdDateFormatter = createdDateFormatter
                            )
                        }

                        // 2. Performance Statistics
                        item {
                            PerformanceStatisticsCard(analytics = analytics)
                        }

                        // 3. Skill Rating Management (1-10)
                        item {
                            SkillRatingManagementCard(
                                currentRating = player.skillRating,
                                onSelectRating = { newRating ->
                                    viewModel.updatePlayerSkillRatingById(player.id, newRating)
                                }
                            )
                        }

                        // 4. Teammate Chemistry (Top 5)
                        item {
                            RelationshipAnalyticsCard(
                                sectionTitle = "TEAMMATE CHEMISTRY (TOP 5)",
                                accentColor = NeonGreen,
                                emptyMessage = "Not enough data",
                                emptySubtitle = "Play confirmed matches with teammates to build chemistry rankings.",
                                relationships = analytics.topTeammates,
                                testTagPrefix = "teammate_chemistry"
                            )
                        }

                        // 5. Opponent History (Top 5)
                        item {
                            RelationshipAnalyticsCard(
                                sectionTitle = "OPPONENT HISTORY (TOP 5)",
                                accentColor = FuchsiaAccent,
                                emptyMessage = "Not enough data",
                                emptySubtitle = "Face opponents across confirmed matches to track head-to-head matchups.",
                                relationships = analytics.topOpponents,
                                testTagPrefix = "opponent_history"
                            )
                        }

                        // 6. Player Match Timeline Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PLAYER MATCH TIMELINE",
                                        color = NeonBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )
                                }
                                Text(
                                    text = "${analytics.matchTimeline.size} ${if (analytics.matchTimeline.size == 1) "Match" else "Matches"}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (analytics.matchTimeline.isEmpty()) {
                            item {
                                GlassyCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "No confirmed matches recorded yet.",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Confirmed matches featuring ${player.displayName} will appear here in read-only order.",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(analytics.matchTimeline, key = { it.matchId }) { entry ->
                                PlayerTimelineMatchCard(
                                    entry = entry,
                                    dateFormatter = matchDateFormatter,
                                    onOpenMatchDetails = { onOpenMatchDetails(entry.matchId) }
                                )
                            }
                        }

                        // 7. Player Archiving / Restore Controls
                        item {
                            PlayerArchivingControlCard(
                                isArchived = player.isArchived,
                                playerName = player.displayName,
                                onRequestArchive = { showArchiveDialog = true },
                                onRestore = { viewModel.restorePlayerById(player.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showArchiveDialog && player != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            containerColor = Color(0xFF141C2E),
            title = {
                Text(
                    text = "Archive ${player.displayName}?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Archived players remain visible in historical match records and keep all statistics and relationships intact, but are excluded from new team-generation selections by default. You can restore them at any time.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveDialog = false
                        viewModel.archivePlayerById(player.id)
                    },
                    modifier = Modifier.testTag("confirm_archive_player_btn")
                ) {
                    Text("Archive Player", color = GoldStar, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
private fun PlayerInformationCard(
    analytics: PlayerProfileAnalytics,
    createdDateFormatter: SimpleDateFormat
) {
    val player = analytics.player
    val initials = player.displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { player.displayName.take(2).uppercase() }

    val createdDateLabel = if (player.createdAt > 0L) {
        createdDateFormatter.format(Date(player.createdAt))
    } else {
        "Not recorded"
    }

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_info_card"),
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        if (player.isArchived) {
                            Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                        } else {
                            Brush.linearGradient(listOf(NeonBlue, IndigoAccent, FuchsiaAccent))
                        }
                    )
                    .border(
                        2.dp,
                        if (player.isArchived) GoldStar.copy(alpha = 0.6f) else NeonBlue.copy(alpha = 0.8f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = player.displayName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (!player.nickname.isNullOrBlank()) {
                Text(
                    text = "\"${player.nickname}\"",
                    color = NeonBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status, Skill, and Creation Date Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (player.isArchived) GoldStar.copy(alpha = 0.16f) else NeonGreen.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (player.isArchived) GoldStar.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (player.isArchived) Icons.Default.Archive else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (player.isArchived) GoldStar else NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (player.isArchived) "Archived" else "Active",
                            color = if (player.isArchived) GoldStar else NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = GoldStar.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldStar.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "Skill ${player.skillRating}/10",
                        color = GoldStar,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Added $createdDateLabel",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PerformanceStatisticsCard(analytics: PlayerProfileAnalytics) {
    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_performance_stats_card"),
        cornerRadius = 18
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PERFORMANCE STATISTICS",
                color = NeonBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            // 2x2 Primary Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = "Total Matches",
                    value = "${analytics.totalMatchesPlayed}",
                    subtitle = "Confirmed matches",
                    accent = Color.White,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Matches as Joker",
                    value = "${analytics.matchesAsJoker}",
                    subtitle = if (analytics.totalMatchesPlayed > 0) {
                        val pct = (analytics.matchesAsJoker * 100) / analytics.totalMatchesPlayed
                        "$pct% of matches"
                    } else {
                        "Not enough data"
                    },
                    accent = FuchsiaAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = "Teammate Appearances",
                    value = "${analytics.totalTeammateAppearances}",
                    subtitle = "${analytics.uniqueTeammatesCount} unique teammates",
                    accent = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Opponent Appearances",
                    value = "${analytics.totalOpponentAppearances}",
                    subtitle = "${analytics.uniqueOpponentsCount} unique opponents",
                    accent = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Most Frequent Teammate & Opponent
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most Frequent Teammate",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = analytics.mostFrequentTeammateSummary(),
                        color = if (analytics.hasTeammateChemistryData) NeonGreen else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most Frequent Opponent",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = analytics.mostFrequentOpponentSummary(),
                        color = if (analytics.hasOpponentHistoryData) FuchsiaAccent else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Color.Gray.copy(alpha = 0.85f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SkillRatingManagementCard(
    currentRating: Int,
    onSelectRating: (Int) -> Unit
) {
    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skill_rating_management_card"),
        cornerRadius = 18
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SKILL RATING",
                    color = GoldStar,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "$currentRating / 10",
                    color = GoldStar,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Two clean rows of 5 buttons (1..5 and 6..10) with >=48dp touch height
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1..5, 6..10).forEach { range ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        range.forEach { ratingValue ->
                            val isSelected = ratingValue == currentRating
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) GoldStar.copy(alpha = 0.26f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) GoldStar else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectRating(ratingValue) }
                                    .testTag("skill_rating_btn_$ratingValue"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$ratingValue",
                                    color = if (isSelected) GoldStar else Color.White,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Explicit Future-Only Notice
            Surface(
                color = NeonBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Affects future team generation only. Fairness scores and team strengths stored in historical matches remain unchanged.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RelationshipAnalyticsCard(
    sectionTitle: String,
    accentColor: Color,
    emptyMessage: String,
    emptySubtitle: String,
    relationships: List<RelationshipFrequency>,
    testTagPrefix: String
) {
    val maxShared = remember(relationships) {
        (relationships.maxOfOrNull { it.sharedMatches } ?: 1).coerceAtLeast(1)
    }

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        cornerRadius = 18
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = sectionTitle,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            if (relationships.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = emptyMessage,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = emptySubtitle,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                relationships.forEach { rel ->
                    val fraction = (rel.sharedMatches.toFloat() / maxShared.toFloat()).coerceIn(0.08f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_row_${rel.playerId}"),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#${rel.rank}",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = rel.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (rel.isArchived) {
                                    Surface(
                                        color = GoldStar.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = "Archived",
                                            color = GoldStar,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${rel.sharedMatches} ${if (rel.sharedMatches == 1) "match" else "matches"}",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Compact frequency bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTimelineMatchCard(
    entry: PlayerMatchTimelineEntry,
    dateFormatter: SimpleDateFormat,
    onOpenMatchDetails: () -> Unit
) {
    val statusColor = when (entry.status) {
        MatchStatus.COMPLETED -> NeonGreen
        MatchStatus.TOSS_COMPLETED -> GoldStar
        MatchStatus.TEAMS_CONFIRMED -> NeonBlue
        else -> Color.Gray
    }

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenMatchDetails)
            .testTag("player_timeline_match_${entry.matchId}"),
        cornerRadius = 16
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Match identifier + date + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.matchDisplayTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = dateFormatter.format(Date(entry.matchDate)),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = entry.status.displayLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Role / Team badge row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.isJoker) {
                    Surface(
                        color = FuchsiaAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FuchsiaAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Joker",
                            color = FuchsiaAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = IndigoAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = entry.teamAssigned ?: "Team not recorded",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                entry.fairnessScore?.let { score ->
                    Surface(
                        color = NeonGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Fairness $score%",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Teammates & Opponents summaries
            if (!entry.isJoker) {
                Text(
                    text = "Teammates: ${if (entry.teammates.isNotEmpty()) entry.teammates.joinToString(", ") else "Not recorded"}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${if (entry.isJoker) "Match Players" else "Opponents"}: ${if (entry.opponents.isNotEmpty()) entry.opponents.joinToString(", ") else "Not recorded"}",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Open Match Details link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Match Details",
                    color = NeonBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open match details",
                    tint = NeonBlue,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerArchivingControlCard(
    isArchived: Boolean,
    playerName: String,
    onRequestArchive: () -> Unit,
    onRestore: () -> Unit
) {
    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_archiving_card"),
        cornerRadius = 18
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ROSTER STATUS",
                color = if (isArchived) GoldStar else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = if (isArchived) {
                    "$playerName is currently archived. Historical matches and analytics remain intact, and the player is excluded from new team selections until restored."
                } else {
                    "Archive $playerName to hide them from new team-generation selections without losing any historical match records or chemistry stats."
                },
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (isArchived) {
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("restore_player_btn")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Player", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onRequestArchive,
                    border = BorderStroke(1.dp, GoldStar.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("archive_player_btn")
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = GoldStar)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Archive Player", color = GoldStar, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
