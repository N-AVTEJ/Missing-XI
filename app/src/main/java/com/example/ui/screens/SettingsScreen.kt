package com.example.ui.screens

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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val firebaseService = viewModel.firebaseService
    val currentUser by firebaseService.currentUser.collectAsState()
    val syncStatus by firebaseService.syncStatus.collectAsState()
    val pitchThemeColor by viewModel.pitchThemeColor.collectAsState()

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
                    text = "UTILITY SETTINGS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = IndigoAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            // Firebase Auth & Cloud Sync Card
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

            // Pitch Customizable Styles Card
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

            // Developer Metadata
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

