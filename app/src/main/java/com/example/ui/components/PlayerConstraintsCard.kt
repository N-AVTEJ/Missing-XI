package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.util.PlayerMatchConstraint

@Composable
fun PlayerConstraintsCard(
    constraints: Map<String, PlayerMatchConstraint>,
    groups: List<LockedPlayerGroup>,
    validationError: String?,
    onManageGroupsClick: () -> Unit,
    onClearConstraintsClick: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lockedCount = constraints.values.count { it.isLocked && it.lockedTeamIndex != null }
    val excludedCount = constraints.values.count { it.isExcluded }
    val groupCount = groups.size
    val totalConstraints = lockedCount + excludedCount + groupCount

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Validation Error Banner
        if (validationError != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonHot.copy(alpha = 0.15f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CrimsonHot)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("constraint_validation_error_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Constraint Error",
                        tint = CrimsonHot,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Invalid Match Constraints",
                            color = CrimsonHot,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = validationError,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(24.dp).testTag("dismiss_constraint_error_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.7f)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (totalConstraints > 0) NeonGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("match_constraints_summary_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Match Constraints",
                            tint = if (totalConstraints > 0) NeonGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MATCH CONSTRAINTS",
                            color = if (totalConstraints > 0) NeonGreen else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    if (totalConstraints > 0) {
                        TextButton(
                            onClick = onClearConstraintsClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp).testTag("clear_match_constraints_btn")
                        ) {
                            Text(
                                text = "Clear All",
                                color = CrimsonHot,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Badges row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Locked count chip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (lockedCount > 0) NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(
                                1.dp,
                                if (lockedCount > 0) NeonGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (lockedCount > 0) NeonGreen else Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$lockedCount Locked",
                                color = if (lockedCount > 0) NeonGreen else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Groups count chip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (groupCount > 0) FuchsiaAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(
                                1.dp,
                                if (groupCount > 0) FuchsiaAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = if (groupCount > 0) FuchsiaAccent else Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$groupCount Groups",
                                color = if (groupCount > 0) FuchsiaAccent else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Excluded count chip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (excludedCount > 0) CrimsonHot.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(
                                1.dp,
                                if (excludedCount > 0) CrimsonHot.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = if (excludedCount > 0) CrimsonHot else Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$excludedCount Excluded",
                                color = if (excludedCount > 0) CrimsonHot else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Manage Groups Button
                OutlinedButton(
                    onClick = onManageGroupsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("manage_fixed_groups_btn"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FuchsiaAccent),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(FuchsiaAccent.copy(alpha = 0.6f))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (groupCount > 0) "Manage Fixed Groups ($groupCount)" else "Create Fixed Groups (Keep Together)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
