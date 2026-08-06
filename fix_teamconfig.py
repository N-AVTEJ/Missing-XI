import re

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    content = f.read()

old_states = """    val candidatePairAnalysis by viewModel.candidatePairAnalysis.collectAsState()"""
new_states = """    val candidatePairAnalysis by viewModel.candidatePairAnalysis.collectAsState()
    val fairnessScore by viewModel.currentFairnessScore.collectAsState()
    val fairnessRating by viewModel.currentFairnessRating.collectAsState()"""
content = content.replace(old_states, new_states)

# Fix unresolved imports: clickable, Build
imports_add = """import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Build
"""
content = content.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\n" + imports_add)

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'w') as f:
    f.write(content)
