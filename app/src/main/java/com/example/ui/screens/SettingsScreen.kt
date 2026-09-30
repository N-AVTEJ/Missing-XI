package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import com.example.util.FairnessConfig
import com.example.util.FairnessMode
import com.example.util.FairnessSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val firebaseService = viewModel.firebaseService
    val currentUser by firebaseService.currentUser.collectAsState()
    val syncStatus by firebaseService.syncStatus.collectAsState()
    val pitchThemeColor by viewModel.pitchThemeColor.collectAsState()
    val fairnessSettings by viewModel.fairnessSettings.collectAsState()

    var showSignInDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("madipadiganavtej@gmail.com") }

    val coroutineScope = rememberCoroutineScope()

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
                    text = "FAIRNESS & CONTROLS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            // 1. Dedicated Fairness Settings & Control Panel Card
            item {
                FairnessControlPanelCard(
                    viewModel = viewModel,
                    fairnessSettings = fairnessSettings
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "APP & UTILITY SETTINGS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            // 2. Firebase Auth & Cloud Sync Card
            item {
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Cloud Status",
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Firebase Cloud Service",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            // Status badge
                            val isSignedIn = currentUser != null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSignedIn) NeonGreen.copy(alpha = 0.15f) else CrimsonHot.copy(alpha = 0.15f))
                                    .border(1.dp, if (isSignedIn) NeonGreen.copy(alpha = 0.3f) else CrimsonHot.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isSignedIn) "Connected" else "Offline Sandbox",
                                    color = if (isSignedIn) NeonGreen else CrimsonHot,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        if (currentUser != null) {
                            Text(
                                text = "Authenticated user: ${currentUser!!.email}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                            Text(
                                text = "Firestore Persistence: Active Sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            NeonButton(
                                text = "Log Out from Firebase",
                                onClick = { firebaseService.signOut() },
                                modifier = Modifier.fillMaxWidth().testTag("firebase_logout_btn"),
                                glowingColor = CrimsonHot
                            )
                        } else {
                            Text(
                                text = "Sign in with your Google account to automatically sync your team lineups and coin toss results directly to Firestore cloud database.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                            Text(
                                text = "Sync status: $syncStatus",
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            NeonButton(
                                text = "Firebase Auth Google Sign-In",
                                onClick = { showSignInDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("firebase_login_btn"),
                                glowingColor = IndigoAccent
                            )
                        }
                    }
                }
            }

            // 3. Pitch Customizable Styles Card
            item {
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme Color",
                                tint = IndigoAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stadium Pitch Accent Theme",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Text(
                            text = "Customize the neon glowing boundaries of the stadium pitch:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Neon Green", "Neon Blue", "Crimson Hot").forEach { colorName ->
                                val isSelected = pitchThemeColor == colorName
                                val accentColor = when (colorName) {
                                    "Neon Blue" -> NeonBlue
                                    "Crimson Hot" -> CrimsonHot
                                    else -> NeonGreen
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f))
                                        .clickable { viewModel.pitchThemeColor.value = colorName }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colorName,
                                        color = if (isSelected) accentColor else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Developer Metadata Card
            item {
                GlassyCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "APP IDENTITY INFO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Version: 1.0.0 (Production Release)", color = Color.White, fontSize = 13.sp)
                        Text("Database: Android Room (SQLite) Local + Firestore Sync", color = Color.White, fontSize = 13.sp)
                        Text("Developer Session ID: madipadiganavtej@gmail.com", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Sign In Email Dialog
    if (showSignInDialog) {
        AlertDialog(
            onDismissRequest = { showSignInDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Mock/Verify Google Sign-In", color = Color.White) },
            text = {
                Column {
                    Text("The AI Studio sandbox environment simulates authenticated Firebase sessions. Please confirm the Google credential target email:", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = IndigoAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedLabelColor = IndigoAccent,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("signin_email_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    onClick = {
                        coroutineScope.launch {
                            firebaseService.signInWithEmailAndPasswordStub(emailInput)
                            showSignInDialog = false
                        }
                    }
                ) {
                    Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignInDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun FairnessControlPanelCard(
    viewModel: AppViewModel,
    fairnessSettings: FairnessSettings
) {
    val activeMode = fairnessSettings.fairnessMode

    GlassyCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Fairness Controls",
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Fairness Quality Gate",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Evaluation rules & quality thresholds",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Active Mode Badge
                val badgeColor = when (activeMode) {
                    FairnessMode.STRICT -> CrimsonHot
                    FairnessMode.RELAXED -> NeonBlue
                    FairnessMode.CUSTOM -> GoldStar
                    FairnessMode.BALANCED -> NeonGreen
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeMode.name,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Presets Selection Tabs
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "FAIRNESS MODE PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(FairnessMode.RELAXED, FairnessMode.BALANCED, FairnessMode.STRICT).forEach { mode ->
                        val isSelected = activeMode == mode
                        val accentColor = when (mode) {
                            FairnessMode.RELAXED -> NeonBlue
                            FairnessMode.BALANCED -> NeonGreen
                            FairnessMode.STRICT -> CrimsonHot
                            else -> IndigoAccent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(if (isSelected) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.03f))
                                .clickable { viewModel.selectFairnessMode(mode) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("fairness_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (mode) {
                                        FairnessMode.RELAXED -> "Relaxed"
                                        FairnessMode.BALANCED -> "Balanced"
                                        FairnessMode.STRICT -> "Strict"
                                        else -> mode.name
                                    },
                                    color = if (isSelected) accentColor else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = when (mode) {
                                        FairnessMode.RELAXED -> "Min 50%"
                                        FairnessMode.BALANCED -> "Min 70%"
                                        FairnessMode.STRICT -> "Min 85%"
                                        else -> ""
                                    },
                                    color = if (isSelected) accentColor.copy(alpha = 0.9f) else Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // If CUSTOM mode is active, display Custom mode banner
                if (activeMode == FairnessMode.CUSTOM) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldStar.copy(alpha = 0.12f))
                            .border(1.dp, GoldStar.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = GoldStar, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Custom Configuration Active — Individual thresholds have been modified.",
                                color = GoldStar,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Mode Explanation Banner
                val explanation = when (activeMode) {
                    FairnessMode.RELAXED -> "Relaxed Mode: Faster team generation with flexible fairness tolerance. Allows slight variations in team ratings and minor pair repetition."
                    FairnessMode.BALANCED -> "Balanced Mode: Missing XI's recommended standard. Strongly optimizes skill parity (Diff ≤ 2) and teammate novelty with up to 5 extra retry batches."
                    FairnessMode.STRICT -> "Strict Mode: Maximum parity and zero tolerance for imbalance. Demands tight skill parity (Diff ≤ 1), minimal pair penalties, and up to 10 additional candidate generation attempts."
                    FairnessMode.CUSTOM -> "Custom Mode: Generates and evaluates lineups based strictly on your customized score and penalty thresholds."
                }
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Threshold Controls Header
            Text(
                text = "QUALITY GATE THRESHOLDS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            // 1. Minimum Overall Fairness Score (0-100)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minimum Fairness Score",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${fairnessSettings.minimumOverallFairnessScore.toInt()} / 100",
                        color = if (fairnessSettings.minimumOverallFairnessScore >= 80) NeonGreen else if (fairnessSettings.minimumOverallFairnessScore >= 65) NeonBlue else GoldStar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.updateMinimumFairnessScore((fairnessSettings.minimumOverallFairnessScore - 5).coerceAtLeast(0.0)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-5", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = fairnessSettings.minimumOverallFairnessScore.toFloat(),
                        onValueChange = { viewModel.updateMinimumFairnessScore(it.roundToInt().toDouble()) },
                        valueRange = 0f..100f,
                        steps = 19, // step of 5
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fairness_min_score_slider")
                    )

                    IconButton(
                        onClick = { viewModel.updateMinimumFairnessScore((fairnessSettings.minimumOverallFairnessScore + 5).coerceAtMost(100.0)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+5", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Arrangements scoring below this target fail the gate and trigger additional generation attempts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            // 2. Maximum Team Strength Difference (0-10)
            ThresholdStepperRow(
                title = "Max Team Strength Difference",
                valueText = "±${fairnessSettings.maximumTeamStrengthDifference} pts",
                description = "Maximum gap allowed between the highest and lowest team total skill ratings.",
                currentValue = fairnessSettings.maximumTeamStrengthDifference,
                minValue = 0,
                maxValue = 10,
                onDecrement = { viewModel.updateMaximumTeamStrengthDifference(fairnessSettings.maximumTeamStrengthDifference - 1) },
                onIncrement = { viewModel.updateMaximumTeamStrengthDifference(fairnessSettings.maximumTeamStrengthDifference + 1) },
                accentColor = NeonBlue,
                testTag = "fairness_strength_diff_stepper"
            )

            // 3. Maximum Teammate Penalty (0-20)
            ThresholdStepperRow(
                title = "Max Teammate Penalty",
                valueText = "${fairnessSettings.maximumTeammatePenalty} pts",
                description = "Tolerance for repeating past teammate pairings based on match history.",
                currentValue = fairnessSettings.maximumTeammatePenalty,
                minValue = 0,
                maxValue = 20,
                onDecrement = { viewModel.updateMaximumTeammatePenalty(fairnessSettings.maximumTeammatePenalty - 1) },
                onIncrement = { viewModel.updateMaximumTeammatePenalty(fairnessSettings.maximumTeammatePenalty + 1) },
                accentColor = IndigoAccent,
                testTag = "fairness_teammate_penalty_stepper"
            )

            // 4. Maximum Opponent Penalty (0-20)
            ThresholdStepperRow(
                title = "Max Opponent Penalty",
                valueText = "${fairnessSettings.maximumOpponentPenalty} pts",
                description = "Tolerance for facing familiar opponents from recent matches.",
                currentValue = fairnessSettings.maximumOpponentPenalty,
                minValue = 0,
                maxValue = 20,
                onDecrement = { viewModel.updateMaximumOpponentPenalty(fairnessSettings.maximumOpponentPenalty - 1) },
                onIncrement = { viewModel.updateMaximumOpponentPenalty(fairnessSettings.maximumOpponentPenalty + 1) },
                accentColor = GoldStar,
                testTag = "fairness_opponent_penalty_stepper"
            )

            // 5. Max Additional Generation Attempts (0-15)
            ThresholdStepperRow(
                title = "Max Retry Generation Attempts",
                valueText = "${fairnessSettings.maxAdditionalGenerationAttempts} extra batches",
                description = "Additional candidate attempts executed before settling on the Best Available candidate.",
                currentValue = fairnessSettings.maxAdditionalGenerationAttempts,
                minValue = 0,
                maxValue = 15,
                onDecrement = { viewModel.updateMaxAdditionalGenerationAttempts(fairnessSettings.maxAdditionalGenerationAttempts - 1) },
                onIncrement = { viewModel.updateMaxAdditionalGenerationAttempts(fairnessSettings.maxAdditionalGenerationAttempts + 1) },
                accentColor = FuchsiaAccent,
                testTag = "fairness_max_retries_stepper"
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Live Thresholds Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE QUALITY GATE RULES",
                        color = IndigoAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Overall Fairness Score ≥ ${fairnessSettings.minimumOverallFairnessScore.toInt()} / 100",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Team Strength Difference ≤ ${fairnessSettings.maximumTeamStrengthDifference} pts",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Teammate Repetition Penalty ≤ ${fairnessSettings.maximumTeammatePenalty}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Opponent Repetition Penalty ≤ ${fairnessSettings.maximumOpponentPenalty}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Additional Attempt Batches: up to ${fairnessSettings.maxAdditionalGenerationAttempts}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Reset to Defaults Button
            NeonButton(
                text = "Reset to Defaults (Balanced)",
                onClick = { viewModel.resetFairnessSettingsToDefault() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_fairness_defaults_btn"),
                glowingColor = NeonGreen
            )
        }
    }
}

@Composable
fun ThresholdStepperRow(
    title: String,
    valueText: String,
    description: String,
    currentValue: Int,
    minValue: Int,
    maxValue: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accentColor: Color,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Decrement Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (currentValue > minValue) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                        .clickable(enabled = currentValue > minValue, onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-",
                        color = if (currentValue > minValue) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = valueText,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                // Increment Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (currentValue < maxValue) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                        .clickable(enabled = currentValue < maxValue, onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = if (currentValue < maxValue) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
