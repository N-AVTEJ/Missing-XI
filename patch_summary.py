import re

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    content = f.read()

# We need to replace everything from `// Duplicate Detection & Shuffle Stats Display` to `// Teammate Pair Tracking Debug Display`
start_marker = "            // Duplicate Detection & Shuffle Stats Display"
end_marker = "            // Teammate Pair Tracking Debug Display"

if start_marker in content and end_marker in content:
    before = content.split(start_marker)[0]
    after = end_marker + content.split(end_marker)[1]
    
    new_summary = """            // Shuffle Summary
            val analysis = candidatePairAnalysis
            if (analysis != null) {
                item {
                    GlassyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shuffle_summary_card"),
                        cornerRadius = 16
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header: Fairness Score & Rating
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "Fairness Engine",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "FAIRNESS SCORE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonBlue,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                val ratingColor = when (fairnessRating) {
                                    "Excellent", "Very Good" -> NeonGreen
                                    "Good" -> NeonBlue
                                    "Average" -> GoldStar
                                    else -> FuchsiaAccent
                                }
                                Text(
                                    text = fairnessRating.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ratingColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ratingColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("fairness_rating")
                                )
                            }
                            
                            // Score Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val ratingColor = when (fairnessRating) {
                                    "Excellent", "Very Good" -> NeonGreen
                                    "Good" -> NeonBlue
                                    "Average" -> GoldStar
                                    else -> FuchsiaAccent
                                }
                                Text(
                                    text = "$fairnessScore",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ratingColor,
                                    modifier = Modifier.testTag("fairness_score")
                                )
                                Text(
                                    text = "/100",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.Bottom).padding(bottom = 8.dp)
                                )
                            }
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            
                            // Stats Grid: New Pairs, Repeated Pairs, Total Penalty
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$fairnessScore", style = MaterialTheme.typography.titleMedium, color = Color.Transparent, fontSize = 0.sp) // hack for alignment? No just use fixed height or ignore
                                    Text("${analysis.newPairs}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    Text("New Pairs", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                                HorizontalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${analysis.repeatedPairs}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (analysis.repeatedPairs > 0) GoldStar else Color.LightGray)
                                    Text("Repeated Pairs", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                                HorizontalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val penalty = analysis.penaltyResult.totalPenalty
                                    Text("$penalty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (penalty > 0) FuchsiaAccent else NeonGreen)
                                    Text("Total Penalty", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            // Additional Info Grid: Shuffle Number, Unique Teams, Duplicates, Joker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (currentShuffleNumber > 0) "#$currentShuffleNumber" else "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Shuffle No.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                                HorizontalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$uniqueTeamsGenerated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonBlue)
                                    Text("Unique Teams", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                                HorizontalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$duplicatesPrevented", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (duplicatesPrevented > 0) GoldStar else Color.LightGray)
                                    Text("Duplicates", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                }
                                if (jokerPlayer != null) {
                                    HorizontalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$jokerPlayer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FuchsiaAccent, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max=60.dp))
                                        Text("Current Joker", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
"""
    
    with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'w') as f:
        f.write(before + new_summary + "\n" + after)
    print("Replaced stats display with Shuffle Summary")
else:
    print("Could not find markers")
