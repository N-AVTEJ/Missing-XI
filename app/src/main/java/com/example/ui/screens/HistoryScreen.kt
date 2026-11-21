package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.CrimsonHot
import com.example.ui.theme.GoldStar
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.AppViewModel
import com.example.util.MatchDateRangeFilter
import com.example.util.MatchHistoryItem
import com.example.util.MatchSortOrder
import com.example.util.MatchStatus
import com.example.util.MatchStatusFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: AppViewModel,
    onOpenMatchDetails: (String) -> Unit = {},
    onStartNewMatch: () -> Unit = {}
) {
    val savedLineups by viewModel.savedLineups.collectAsState()
    val savedTosses by viewModel.savedTosses.collectAsState()
    val historyState by viewModel.matchHistoryUiState.collectAsState()

    var activeTab by remember { mutableStateOf("Matches") } // "Matches", "Lineups", "Tosses"
    var matchPendingDelete by remember { mutableStateOf<MatchHistoryItem?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val dayHeaderFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.loadMatchHistory()
    }

    FrostedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH HISTORY",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                )
                if (activeTab == "Matches" && historyState.totalCount > 0) {
                    Text(
                        text = "${historyState.totalCount} Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.testTag("match_history_total_count")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Top Tab Switcher (Matches, Lineups, Tosses)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(99.dp))
                    .padding(4.dp)
            ) {
                listOf("Matches", "Lineups", "Tosses").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (isSelected) IndigoAccent else Color.Transparent)
                            .clickable { activeTab = tab }
                            .testTag("history_tab_${tab.lowercase()}")
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (tab) {
                                    "Matches" -> Icons.Default.History
                                    "Lineups" -> Icons.Default.Groups
                                    else -> Icons.Default.Casino
                                },
                                contentDescription = tab,
                                tint = if (isSelected) Color.White else Color.Gray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeTab) {
                "Matches" -> {
                    MatchHistoryDashboardContent(
                        state = historyState,
                        dateFormatter = dateFormatter,
                        dayHeaderFormatter = dayHeaderFormatter,
                        onSearchChanged = { viewModel.searchMatchHistory(it) },
                        onStatusFilterSelected = { status ->
                            viewModel.applyMatchHistoryFilters(statusFilter = status)
                        },
                        onProfileFilterSelected = { profile ->
                            viewModel.applyMatchHistoryFilters(fairnessProfile = profile)
                        },
                        onDateRangeSelected = { range ->
                            viewModel.applyMatchHistoryFilters(dateRange = range)
                        },
                        onSortOrderSelected = { order ->
                            viewModel.sortMatchHistory(order)
                        },
                        onClearFilters = { viewModel.clearMatchHistoryFilters() },
                        onLoadMore = { viewModel.loadNextMatchHistoryPage() },
                        onOpenMatchDetails = onOpenMatchDetails,
                        onRequestDeleteMatch = { matchPendingDelete = it },
                        onStartNewMatch = {
                            viewModel.startNewMatch()
                            onStartNewMatch()
                        }
                    )
                }

                "Lineups" -> {
                    SavedLineupsTabContent(
                        savedLineups = savedLineups,
                        dateFormatter = dateFormatter,
                        onDeleteLineup = { viewModel.deleteLineup(it) }
                    )
                }

                else -> {
                    SavedTossesTabContent(
                        savedTosses = savedTosses,
                        dateFormatter = dateFormatter,
                        onClearAllTosses = { viewModel.clearAllTosses() }
                    )
                }
            }
        }
    }

    // Safe Match Deletion Confirmation Dialog
    val pending = matchPendingDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { matchPendingDelete = null },
            title = {
                Text(
                    text = "Delete ${pending.displayTitle}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Deleting this match also removes its associated toss history. Player profiles, global fairness settings, and unrelated matches will not be affected."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idToDelete = pending.id
                        matchPendingDelete = null
                        viewModel.deleteMatchRecord(idToDelete)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CrimsonHot),
                    modifier = Modifier.testTag("confirm_delete_history_match_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MatchHistoryDashboardContent(
    state: com.example.util.MatchHistoryUiState,
    dateFormatter: SimpleDateFormat,
    dayHeaderFormatter: SimpleDateFormat,
    onSearchChanged: (String) -> Unit,
    onStatusFilterSelected: (MatchStatusFilter) -> Unit,
    onProfileFilterSelected: (String?) -> Unit,
    onDateRangeSelected: (MatchDateRangeFilter) -> Unit,
    onSortOrderSelected: (MatchSortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenMatchDetails: (String) -> Unit,
    onRequestDeleteMatch: (MatchHistoryItem) -> Unit,
    onStartNewMatch: () -> Unit
) {
    val filter = state.filter
    var showProfileMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar (case-insensitive search by player name, match number, or match ID)
        OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = {
                Text(
                    text = "Search player, #match, or ID...",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search matches",
                    tint = NeonBlue,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (filter.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChanged("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("match_history_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MatchStatusFilter.entries.forEach { statusOption ->
                val isSelected = filter.statusFilter == statusOption
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) NeonGreen.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) NeonGreen.copy(alpha = 0.6f)
                            else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onStatusFilterSelected(statusOption) }
                        .testTag("status_filter_${statusOption.name.lowercase()}")
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusOption.displayLabel,
                        color = if (isSelected) NeonGreen else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Filters (Profile, Date Range) + Sort Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Dropdown
            Box {
                CompactFilterDropdownButton(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    label = filter.sortOrder.displayLabel,
                    isHighlighted = filter.sortOrder != MatchSortOrder.NEWEST_FIRST,
                    accent = IndigoAccent,
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("sort_dropdown_button")
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    MatchSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.displayLabel) },
                            onClick = {
                                showSortMenu = false
                                onSortOrderSelected(order)
                            }
                        )
                    }
                }
            }

            // Fairness Profile Dropdown (shown only when profiles exist in stored records)
            if (state.availableProfiles.isNotEmpty()) {
                Box {
                    CompactFilterDropdownButton(
                        icon = Icons.Default.FilterList,
                        label = filter.fairnessProfile ?: "All Profiles",
                        isHighlighted = !filter.fairnessProfile.isNullOrBlank(),
                        accent = NeonBlue,
                        onClick = { showProfileMenu = true },
                        modifier = Modifier.testTag("profile_filter_dropdown")
                    )
                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Profiles") },
                            onClick = {
                                showProfileMenu = false
                                onProfileFilterSelected(null)
                            }
                        )
                        state.availableProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile) },
                                onClick = {
                                    showProfileMenu = false
                                    onProfileFilterSelected(profile)
                                }
                            )
                        }
                    }
                }
            }

            // Date Range Dropdown
            Box {
                CompactFilterDropdownButton(
                    icon = Icons.Default.History,
                    label = filter.dateRange.displayLabel,
                    isHighlighted = filter.dateRange != MatchDateRangeFilter.ALL_TIME,
                    accent = GoldStar,
                    onClick = { showDateMenu = true },
                    modifier = Modifier.testTag("date_range_filter_dropdown")
                )
                DropdownMenu(
                    expanded = showDateMenu,
                    onDismissRequest = { showDateMenu = false }
                ) {
                    MatchDateRangeFilter.entries.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.displayLabel) },
                            onClick = {
                                showDateMenu = false
                                onDateRangeSelected(range)
                            }
                        )
                    }
                }
            }

            if (filter.hasActiveFilters) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrimsonHot.copy(alpha = 0.14f))
                        .border(1.dp, CrimsonHot.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onClearFilters() }
                        .testTag("clear_filters_button")
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clear",
                        color = CrimsonHot,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Content: Loading / Empty State / Paginated Match Cards
        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }

            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_history_empty_state"),
                        cornerRadius = 24
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Empty Match History",
                                tint = Color.Gray,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = if (filter.hasActiveFilters) "No matching matches found." else "No matches yet.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (filter.hasActiveFilters) {
                                    "Try adjusting your search or clearing active filters."
                                } else {
                                    "Your completed matches will appear here."
                                },
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (filter.hasActiveFilters) {
                                OutlinedButton(
                                    onClick = onClearFilters,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                        brush = SolidColor(NeonBlue.copy(alpha = 0.6f))
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Clear Filters", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                NeonButton(
                                    text = "Start New Match",
                                    onClick = onStartNewMatch,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("history_start_new_match_button"),
                                    glowingColor = NeonGreen
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                val groupByDate = filter.sortOrder == MatchSortOrder.NEWEST_FIRST ||
                    filter.sortOrder == MatchSortOrder.OLDEST_FIRST

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("match_history_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (groupByDate) {
                        val grouped = state.items.groupBy { item ->
                            dayHeaderFormatter.format(Date(item.createdAt))
                        }
                        grouped.forEach { (dateLabel, dayMatches) ->
                            item(key = "header_$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }
                            items(dayMatches, key = { it.id }) { item ->
                                MatchHistoryCard(
                                    item = item,
                                    dateFormatter = dateFormatter,
                                    onClick = { onOpenMatchDetails(item.id) },
                                    onDeleteClick = { onRequestDeleteMatch(item) }
                                )
                            }
                        }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            MatchHistoryCard(
                                item = item,
                                dateFormatter = dateFormatter,
                                onClick = { onOpenMatchDetails(item.id) },
                                onDeleteClick = { onRequestDeleteMatch(item) }
                            )
                        }
                    }

                    // Pagination Footer
                    item(key = "pagination_footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Showing ${state.items.size} of ${state.totalCount} matches",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            if (state.hasMorePages) {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    enabled = !state.isLoadingMore,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = !state.isLoadingMore).copy(
                                        brush = SolidColor(NeonBlue.copy(alpha = 0.55f))
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("load_more_matches_button")
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = NeonBlue,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Load More Matches", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFilterDropdownButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isHighlighted: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHighlighted) accent.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                1.dp,
                if (isHighlighted) accent.copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isHighlighted) accent else Color.LightGray,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (isHighlighted) Color.White else Color.LightGray,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun MatchHistoryCard(
    item: MatchHistoryItem,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isCompleted = item.status == MatchStatus.COMPLETED
    val statusColor = when (item.status) {
        MatchStatus.COMPLETED -> NeonGreen
        MatchStatus.TOSS_COMPLETED -> GoldStar
        MatchStatus.TEAMS_CONFIRMED -> NeonBlue
        else -> IndigoAccent
    }

    GlassyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("match_history_card_${item.id}"),
        cornerRadius = 20
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top Row: Match Number + Status + Quick Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.displayTitle.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.status.displayLabel,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    if (item.wasManuallyEdited) {
                        Text(
                            text = "EDITED",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_history_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${item.displayTitle}",
                        tint = CrimsonHot.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = dateFormatter.format(Date(item.createdAt)),
                color = Color.Gray,
                fontSize = 11.sp
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Team sizes & player count
            val teamSummaryText = if (item.teamSizesSummary.isNotEmpty()) {
                item.teamSizesSummary.joinToString("  •  ") { (name, count) -> "$name: $count" }
            } else {
                "${item.teamCount} Teams  •  ${item.totalEligiblePlayers} Players"
            }
            Text(
                text = teamSummaryText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Fairness score + Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.fairnessScore?.let { "Fairness: $it/100" } ?: "Fairness data unavailable",
                    color = if (item.fairnessScore != null) NeonGreen else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (!item.fairnessProfile.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndigoAccent.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.fairnessProfile,
                            color = IndigoAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Latest Toss + View Details Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tossText = if (item.tossResult != null && item.tossWon != null) {
                    "Toss: ${item.tossResult.displayName} — ${if (item.tossWon) "Won" else "Lost"}"
                } else {
                    "No toss recorded"
                }
                Text(
                    text = tossText,
                    color = when {
                        item.tossWon == true -> GoldStar
                        item.tossWon == false -> Color.LightGray
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )

                Text(
                    text = "View Details →",
                    color = if (isCompleted) NeonBlue else Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SavedLineupsTabContent(
    savedLineups: List<com.example.data.model.LineupEntity>,
    dateFormatter: SimpleDateFormat,
    onDeleteLineup: (Int) -> Unit
) {
    if (savedLineups.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "No Squads",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("No lineups built yet", color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("Create squad configurations in the Build tab!", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("lineups_history_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(savedLineups, key = { it.id }) { lineup ->
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (lineup.sportType == "Cricket") GoldStar.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = lineup.sportType.uppercase(),
                                        color = if (lineup.sportType == "Cricket") GoldStar else NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Formation: ${lineup.formation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lineup.teamName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = dateFormatter.format(Date(lineup.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            val formattedPlayers = lineup.playersJson.split(", ")
                                .map { it.substringAfter(":") }
                                .take(11)
                                .joinToString(", ")

                            Text(
                                text = "Squad: $formattedPlayers",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onDeleteLineup(lineup.id) },
                            modifier = Modifier.testTag("delete_lineup_${lineup.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Lineup",
                                tint = CrimsonHot.copy(alpha = 0.85f)
                            )
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
private fun SavedTossesTabContent(
    savedTosses: List<com.example.data.model.TossEntity>,
    dateFormatter: SimpleDateFormat,
    onClearAllTosses: () -> Unit
) {
    if (savedTosses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "No Tosses",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("No match tosses saved yet", color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("Flip a coin in the Toss tab!", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = onClearAllTosses,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonHot.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CrimsonHot.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .testTag("clear_toss_history_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear All Toss Histories", color = CrimsonHot, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tosses_history_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedTosses, key = { it.id }) { toss ->
                    GlassyCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dateFormatter.format(Date(toss.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Choice: ${toss.choice}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "|  Result: ${toss.result}",
                                        color = Color.LightGray,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (toss.hasWon) NeonGreen.copy(alpha = 0.15f) else CrimsonHot.copy(alpha = 0.15f))
                                    .border(1.dp, if (toss.hasWon) NeonGreen.copy(alpha = 0.3f) else CrimsonHot.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (toss.hasWon) "WIN" else "LOSS",
                                    color = if (toss.hasWon) NeonGreen else CrimsonHot,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
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
}
