import re

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    content = f.read()

# I need to add state for opponent pair analysis and stats
old_states = """    val candidatePairAnalysis by viewModel.candidatePairAnalysis.collectAsState()
    val fairnessScore by viewModel.currentFairnessScore.collectAsState()
    val fairnessRating by viewModel.currentFairnessRating.collectAsState()"""
new_states = """    val candidatePairAnalysis by viewModel.candidatePairAnalysis.collectAsState()
    val candidateOpponentAnalysis by viewModel.candidateOpponentAnalysis.collectAsState()
    val fairnessScore by viewModel.currentFairnessScore.collectAsState()
    val fairnessRating by viewModel.currentFairnessRating.collectAsState()
    val opponentStats by viewModel.opponentStatistics.collectAsState()"""
content = content.replace(old_states, new_states)

# Let's add Opponent Analysis to Developer Debug Mode
old_debug = """                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Winning Candidate Fairness Score:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.winningCandidateFairnessScore}", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }"""
new_debug = """                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Winning Candidate Fairness Score:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.winningCandidateFairnessScore}", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Opponent Analysis Developer Debug Display
            if (currentShuffleNumber > 0 || opponentStats.uniquePairsCount > 0) {
                item {
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("opponent_pair_tracking_card"),
                        cornerRadius = 16
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.filled.Group,
                                        contentDescription = "Opponent Pair Tracking",
                                        tint = FuchsiaAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Opponent Pair Tracking",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FuchsiaAccent,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "DEV TRACKER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unique Opponent Pairs
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${opponentStats.uniquePairsCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FuchsiaAccent,
                                        modifier = Modifier.testTag("unique_opponent_pairs")
                                    )
                                    Text(
                                        text = "Unique Opponents",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .width(1.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )
                                // Total Occurrences
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${opponentStats.totalPairOccurrences}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FuchsiaAccent,
                                        modifier = Modifier.testTag("total_opponent_occurrences")
                                    )
                                    Text(
                                        text = "Total Encounters",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Repeated Opponents
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${opponentStats.repeatedPairOccurrences}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (opponentStats.repeatedPairOccurrences > 0) FuchsiaAccent else Color.LightGray,
                                        modifier = Modifier.testTag("repeated_opponent_occurrences")
                                    )
                                    Text(
                                        text = "Repeated Opponents",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .width(1.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )
                                // Max Repeated Encounters
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${opponentStats.maxRepeatedPairCount}×",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (opponentStats.maxRepeatedPairCount > 1) FuchsiaAccent else Color.LightGray,
                                        modifier = Modifier.testTag("max_repeated_opponent")
                                    )
                                    Text(
                                        text = "Most Repeated",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }"""
content = content.replace(old_debug, new_debug)

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'w') as f:
    f.write(content)
