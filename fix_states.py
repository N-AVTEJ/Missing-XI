import re

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    content = f.read()

state_new = """    val currentShuffleNumber by viewModel.currentShuffleNumber.collectAsState()
    val isGenerating by viewModel.isGeneratingCandidates.collectAsState()
    val generationProgress by viewModel.candidateGenerationProgress.collectAsState()
    val generationTarget by viewModel.candidateGenerationTarget.collectAsState()
    val generationDiagnostics by viewModel.generationDiagnostics.collectAsState()"""

content = content.replace("    val currentShuffleNumber by viewModel.currentShuffleNumber.collectAsState()", state_new)

with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'w') as f:
    f.write(content)
