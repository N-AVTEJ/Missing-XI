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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.util.MatchFairnessProfile
import kotlin.math.roundToInt

@Composable
fun MatchFairnessProfileSection(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val matchConfig by viewModel.matchFairnessConfig.collectAsState()
    val globalSettings by viewModel.fairnessSettings.collectAsState()
    val isGenerating by viewModel.isGeneratingCandidates.collectAsState()

    val selectedProfile = matchConfig.profile
    val customSettings = matchConfig.customSettings

    GlassyCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Fairness Profile",
                        tint = when (selectedProfile) {
                            MatchFairnessProfile.STRICT -> CrimsonHot
                            MatchFairnessProfile.RELAXED -> NeonBlue
                            MatchFairnessProfile.CUSTOM -> GoldStar
                            MatchFairnessProfile.BALANCED -> NeonGreen
                            MatchFairnessProfile.GLOBAL_DEFAULT -> IndigoAccent
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Fairness Profile",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Select fairness criteria for this match",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Selected Profile Pill
                val badgeColor = when (selectedProfile) {
                    MatchFairnessProfile.STRICT -> CrimsonHot
                    MatchFairnessProfile.RELAXED -> NeonBlue
                    MatchFairnessProfile.CUSTOM -> GoldStar
                    MatchFairnessProfile.BALANCED -> NeonGreen
                    MatchFairnessProfile.GLOBAL_DEFAULT -> IndigoAccent
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedProfile.displayName,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Profile Buttons Grid
            // Row 1: [ Global Default ] [ Relaxed ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileOptionButton(
                    profile = MatchFairnessProfile.GLOBAL_DEFAULT,
                    isSelected = selectedProfile == MatchFairnessProfile.GLOBAL_DEFAULT,
                    color = IndigoAccent,
                    enabled = !isGenerating,
                    onClick = { viewModel.setMatchFairnessProfile(MatchFairnessProfile.GLOBAL_DEFAULT) },
                    modifier = Modifier.weight(1f)
                )
                ProfileOptionButton(
                    profile = MatchFairnessProfile.RELAXED,
                    isSelected = selectedProfile == MatchFairnessProfile.RELAXED,
                    color = NeonBlue,
                    enabled = !isGenerating,
                    onClick = { viewModel.setMatchFairnessProfile(MatchFairnessProfile.RELAXED) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: [ Balanced ] [ Strict ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileOptionButton(
                    profile = MatchFairnessProfile.BALANCED,
                    isSelected = selectedProfile == MatchFairnessProfile.BALANCED,
                    color = NeonGreen,
                    enabled = !isGenerating,
                    onClick = { viewModel.setMatchFairnessProfile(MatchFairnessProfile.BALANCED) },
                    modifier = Modifier.weight(1f)
                )
                ProfileOptionButton(
                    profile = MatchFairnessProfile.STRICT,
                    isSelected = selectedProfile == MatchFairnessProfile.STRICT,
                    color = CrimsonHot,
                    enabled = !isGenerating,
                    onClick = { viewModel.setMatchFairnessProfile(MatchFairnessProfile.STRICT) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: [ Custom ]
            ProfileOptionButton(
                profile = MatchFairnessProfile.CUSTOM,
                isSelected = selectedProfile == MatchFairnessProfile.CUSTOM,
                color = GoldStar,
                enabled = !isGenerating,
                onClick = { viewModel.setMatchFairnessProfile(MatchFairnessProfile.CUSTOM) },
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Description & Threshold summary
            val resolved = matchConfig.resolveSettings(globalSettings)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (selectedProfile) {
                            MatchFairnessProfile.GLOBAL_DEFAULT -> "Global Default: Uses your app settings (${globalSettings.fairnessMode.name})."
                            MatchFairnessProfile.RELAXED -> "Relaxed: Casual & fast team generation with wider balance tolerance."
                            MatchFairnessProfile.BALANCED -> "Balanced: Standard algorithm balancing skill parity and variety."
                            MatchFairnessProfile.STRICT -> "Strict: Competitive mode with maximum fairness enforcement and up to 10 retry batches."
                            MatchFairnessProfile.CUSTOM -> "Custom: Match-specific thresholds. Editing these does not alter global settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "Thresholds: Score ≥ ${resolved.minimumOverallFairnessScore.toInt()} | Diff ≤ ${resolved.maximumTeamStrengthDifference} | TM Pen ≤ ${resolved.maximumTeammatePenalty} | Opp Pen ≤ ${resolved.maximumOpponentPenalty} | Retries: ${resolved.maxAdditionalGenerationAttempts}",
                        color = when (selectedProfile) {
                            MatchFairnessProfile.STRICT -> CrimsonHot
                            MatchFairnessProfile.RELAXED -> NeonBlue
                            MatchFairnessProfile.CUSTOM -> GoldStar
                            MatchFairnessProfile.BALANCED -> NeonGreen
                            MatchFairnessProfile.GLOBAL_DEFAULT -> IndigoAccent
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Custom Controls Section (Visible when CUSTOM profile is selected)
            AnimatedVisibility(
                visible = selectedProfile == MatchFairnessProfile.CUSTOM,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GoldStar, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Match-Specific Custom Thresholds (Isolated from Global Settings)",
                            color = GoldStar,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. Minimum Overall Fairness Score
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Minimum Fairness Score", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${customSettings.minimumOverallFairnessScore.toInt()} / 100", color = GoldStar, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.updateMatchCustomMinScore(customSettings.minimumOverallFairnessScore - 5) },
                                enabled = !isGenerating,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("-5", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = customSettings.minimumOverallFairnessScore.toFloat(),
                                onValueChange = { viewModel.updateMatchCustomMinScore(it.roundToInt().toDouble()) },
                                valueRange = 0f..100f,
                                steps = 19,
                                enabled = !isGenerating,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldStar,
                                    activeTrackColor = GoldStar,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.weight(1f).testTag("match_custom_min_score_slider")
                            )
                            IconButton(
                                onClick = { viewModel.updateMatchCustomMinScore(customSettings.minimumOverallFairnessScore + 5) },
                                enabled = !isGenerating,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("+5", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 2. Max Strength Difference
                    MatchStepperRow(
                        title = "Max Strength Difference",
                        valueText = "±${customSettings.maximumTeamStrengthDifference} pts",
                        currentValue = customSettings.maximumTeamStrengthDifference,
                        minValue = 0,
                        maxValue = 10,
                        enabled = !isGenerating,
                        onDecrement = { viewModel.updateMatchCustomMaxStrengthDiff(customSettings.maximumTeamStrengthDifference - 1) },
                        onIncrement = { viewModel.updateMatchCustomMaxStrengthDiff(customSettings.maximumTeamStrengthDifference + 1) },
                        accentColor = NeonBlue,
                        testTag = "match_custom_strength_diff_stepper"
                    )

                    // 3. Max Teammate Penalty
                    MatchStepperRow(
                        title = "Max Teammate Penalty",
                        valueText = "${customSettings.maximumTeammatePenalty} pts",
                        currentValue = customSettings.maximumTeammatePenalty,
                        minValue = 0,
                        maxValue = 20,
                        enabled = !isGenerating,
                        onDecrement = { viewModel.updateMatchCustomMaxTeammatePenalty(customSettings.maximumTeammatePenalty - 1) },
                        onIncrement = { viewModel.updateMatchCustomMaxTeammatePenalty(customSettings.maximumTeammatePenalty + 1) },
                        accentColor = IndigoAccent,
                        testTag = "match_custom_teammate_penalty_stepper"
                    )

                    // 4. Max Opponent Penalty
                    MatchStepperRow(
                        title = "Max Opponent Penalty",
                        valueText = "${customSettings.maximumOpponentPenalty} pts",
                        currentValue = customSettings.maximumOpponentPenalty,
                        minValue = 0,
                        maxValue = 20,
                        enabled = !isGenerating,
                        onDecrement = { viewModel.updateMatchCustomMaxOpponentPenalty(customSettings.maximumOpponentPenalty - 1) },
                        onIncrement = { viewModel.updateMatchCustomMaxOpponentPenalty(customSettings.maximumOpponentPenalty + 1) },
                        accentColor = FuchsiaAccent,
                        testTag = "match_custom_opponent_penalty_stepper"
                    )

                    // 5. Max Retry Attempts
                    MatchStepperRow(
                        title = "Max Retry Attempts",
                        valueText = "${customSettings.maxAdditionalGenerationAttempts} extra batches",
                        currentValue = customSettings.maxAdditionalGenerationAttempts,
                        minValue = 0,
                        maxValue = 15,
                        enabled = !isGenerating,
                        onDecrement = { viewModel.updateMatchCustomMaxRetries(customSettings.maxAdditionalGenerationAttempts - 1) },
                        onIncrement = { viewModel.updateMatchCustomMaxRetries(customSettings.maxAdditionalGenerationAttempts + 1) },
                        accentColor = GoldStar,
                        testTag = "match_custom_max_retries_stepper"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionButton(
    profile: MatchFairnessProfile,
    isSelected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.03f))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
            .testTag("fairness_profile_${profile.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = profile.displayName,
                color = if (isSelected) color else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
            Text(
                text = when (profile) {
                    MatchFairnessProfile.GLOBAL_DEFAULT -> "Global"
                    MatchFairnessProfile.RELAXED -> "Fast"
                    MatchFairnessProfile.BALANCED -> "Standard"
                    MatchFairnessProfile.STRICT -> "Strict"
                    MatchFairnessProfile.CUSTOM -> "Custom"
                },
                color = if (isSelected) color.copy(alpha = 0.85f) else Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MatchStepperRow(
    title: String,
    valueText: String,
    currentValue: Int,
    minValue: Int,
    maxValue: Int,
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accentColor: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 12.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (enabled && currentValue > minValue) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                    .clickable(enabled = enabled && currentValue > minValue, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = if (enabled && currentValue > minValue) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Text(
                text = valueText,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (enabled && currentValue < maxValue) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                    .clickable(enabled = enabled && currentValue < maxValue, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = if (enabled && currentValue < maxValue) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
