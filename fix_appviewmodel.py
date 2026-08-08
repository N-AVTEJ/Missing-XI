with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False

dup_block_start = "    val opponentPairCounts = MutableStateFlow<Map<String, Int>>(emptyMap())"

count = 0
for i, line in enumerate(lines):
    if line.rstrip() == dup_block_start:
        count += 1
        if count > 1:
            skip = True
            
    if skip:
        # Skip until we pass the candidateOpponentAnalysis line
        if "val candidateOpponentAnalysis =" in line:
            skip = False
        continue
    
    # We also have duplication in updateOpponentHistory and clearSessionHistory
    if "opponentPairCounts.value = com.example.util.OpponentPairTracker.updateOpponentHistory(" in line:
        # Check if previous line is the same
        if i > 0 and "opponentPairCounts.value = com.example.util.OpponentPairTracker.updateOpponentHistory(" in lines[i-1]:
            continue
            
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'w') as f:
    f.writelines(new_lines)
