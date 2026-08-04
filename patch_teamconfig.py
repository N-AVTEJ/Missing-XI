import re

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    content = f.read()

# Add states at the top
state_imports_old = """    val configState by viewModel.configState.collectAsState()
    val currentShuffleNumber by viewModel.currentShuffleNumber.collectAsState()"""
state_imports_new = """    val configState by viewModel.configState.collectAsState()
    val currentShuffleNumber by viewModel.currentShuffleNumber.collectAsState()
    val isGenerating by viewModel.isGeneratingCandidates.collectAsState()
    val generationProgress by viewModel.candidateGenerationProgress.collectAsState()
    val generationTarget by viewModel.candidateGenerationTarget.collectAsState()
    val generationDiagnostics by viewModel.generationDiagnostics.collectAsState()"""

content = content.replace(state_imports_old, state_imports_new)

# Replace the Shuffle Button Item
shuffle_button_old = """            // Shuffle Button Item
            item {
                AnimatedVisibility(
                    visible = configState.error == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NeonButton(
                        text = if (generatedTeams.isEmpty()) "Shuffle & Generate Teams" else "Shuffle Again",
                        onClick = { viewModel.shuffleTeams() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shuffle_again_button"),
                        glowingColor = NeonGreen
                    )
                }
            }"""
shuffle_button_new = """            // Shuffle Button Item
            item {
                AnimatedVisibility(
                    visible = configState.error == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (isGenerating) {
                        GlassyCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = NeonGreen)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Generating Teams...", color = NeonGreen, fontWeight = FontWeight.Bold)
                                Text("$generationProgress / $generationTarget Candidates", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    } else {
                        NeonButton(
                            text = if (generatedTeams.isEmpty()) "Shuffle & Generate Teams" else "Shuffle Again",
                            onClick = { viewModel.shuffleTeams() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shuffle_again_button"),
                            glowingColor = NeonGreen
                        )
                    }
                }
            }"""

content = content.replace(shuffle_button_old, shuffle_button_new)

debug_stats_old = """                                // Duplicates Prevented
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = duplicatesPrevented.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Duplicates Prevented",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }"""

debug_stats_new = debug_stats_old + """
            // Candidate Generation Debug
            if (generationDiagnostics != null) {
                item {
                    val diag = generationDiagnostics!!
                    GlassyCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        cornerRadius = 16
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = "Diagnostics", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Multi-Candidate Generation Debug", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Candidates Generated:", color = Color.Gray, fontSize = 12.sp)
                                Text("${diag.candidatesGenerated}", color = Color.White, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Candidates Rejected (Duplicates):", color = Color.Gray, fontSize = 12.sp)
                                Text("${diag.candidatesRejected}", color = Color.White, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Retry Count:", color = Color.Gray, fontSize = 12.sp)
                                Text("${diag.retryCount}", color = Color.White, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Generation Time:", color = Color.Gray, fontSize = 12.sp)
                                Text("${diag.generationTimeMs} ms", color = Color.White, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Average Candidate Penalty:", color = Color.Gray, fontSize = 12.sp)
                                Text(String.format("%.2f", diag.averageCandidatePenalty), color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }"""

content = content.replace(debug_stats_old, debug_stats_new)

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'w') as f:
    f.write(content)
