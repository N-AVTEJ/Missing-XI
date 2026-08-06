import re

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    content = f.read()

debug_block = """            // Developer Debug Section (Collapsible)
            if (generationDiagnostics != null) {
                item {
                    val diag = generationDiagnostics!!
                    var isDebugExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                    GlassyCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        cornerRadius = 16
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDebugExpanded = !isDebugExpanded }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, contentDescription = "Diagnostics", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Developer Debug Mode", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Icon(
                                    imageVector = if (isDebugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand/Collapse",
                                    tint = NeonGreen
                                )
                            }
                            
                            AnimatedVisibility(visible = isDebugExpanded) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Candidates Generated:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.candidatesGenerated}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Best Candidate Rank:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.bestCandidateRank}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Lowest Penalty Found:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.lowestPenaltyFound}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Highest Penalty Found:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.highestPenaltyFound}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Average Candidate Penalty:", color = Color.Gray, fontSize = 12.sp)
                                        Text(String.format("%.2f", diag.averageCandidatePenalty), color = Color.White, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Winning Candidate Penalty:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.winningCandidatePenalty}", color = Color.White, fontSize = 12.sp)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Winning Candidate Fairness Score:", color = Color.Gray, fontSize = 12.sp)
                                        Text("${diag.winningCandidateFairnessScore}", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }"""

# Insert it before `// Teammate Pair Tracking Debug Display`
end_marker = "            // Teammate Pair Tracking Debug Display"
content = content.replace(end_marker, debug_block + "\n\n" + end_marker)

# Also need to add Icons.Default.ExpandLess / ExpandMore if they aren't imported.
imports = """import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess"""
content = content.replace("import androidx.compose.material.icons.filled.Group", "import androidx.compose.material.icons.filled.Group\n" + imports)
# also remember and mutableStateOf
content = content.replace("import androidx.compose.runtime.collectAsState\n", "import androidx.compose.runtime.collectAsState\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\n")

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'w') as f:
    f.write(content)
