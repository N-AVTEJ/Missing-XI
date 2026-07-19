package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FrostedMeshBackground
import com.example.ui.components.GlassyCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.FuchsiaAccent
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.GoldStar
import com.example.ui.theme.DeepBg
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToBuild: () -> Unit,
    onNavigateToToss: () -> Unit
) {
    val savedLineups by viewModel.savedLineups.collectAsState()
    val savedTosses by viewModel.savedTosses.collectAsState()
    val userEmail = "madipadiganavtej@gmail.com" // Preserving user identification

    FrostedMeshBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PREMIUM ACCESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                color = IndigoAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "MISSING XI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }

                    // JD Gradient Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(IndigoAccent, FuchsiaAccent)
                                )
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(DeepBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Hero Glass Card (Start Building shortcut)
            item {
                GlassyCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(40.dp))
                        .clickable { onNavigateToBuild() },
                    cornerRadius = 40,
                    borderBrush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
                    ),
                    backgroundBrush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Background transparent trophy/pitch decoration icon (simulating HTML svg outline)
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.04f),
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = 24.dp, y = (-12).dp)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Draft Your\nMissing XI",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 32.sp
                                )
                            )
                            Text(
                                text = "Build elite squads with ease",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.LightGray.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                            )
                            NeonButton(
                                text = "Start Building",
                                onClick = onNavigateToBuild,
                                glowingColor = IndigoAccent,
                                modifier = Modifier.testTag("home_create_squad_btn")
                            )
                        }
                    }
                }
            }

            // Quick Stats Grid Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Toss Widget
                    GlassyCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { onNavigateToToss() },
                        cornerRadius = 24
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GoldStar.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🪙",
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "Toss",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.Gray,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "Quick Flip",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // History Widget
                    GlassyCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        cornerRadius = 24
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📊",
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "History",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.Gray,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "${savedLineups.size} Saved",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Recent Activity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Squads",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )
                    )
                    Text(
                        text = "SEE ALL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IndigoAccent,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.clickable { onNavigateToBuild() }
                    )
                }
            }

            if (savedLineups.isEmpty()) {
                item {
                    GlassyCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No squads drafted yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Show up to 3 recent items
                val recents = savedLineups.take(3)
                items(recents) { lineup ->
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBuild() },
                        cornerRadius = 20
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "XI",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = lineup.teamName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "${lineup.formation} • ${lineup.sportType}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Gray
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Edit",
                                tint = Color.Gray
                            )
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

