import re

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'r') as f:
    content = f.read()

old_state = """    val candidatePairAnalysis = MutableStateFlow<CandidatePairAnalysis?>(null)"""
new_state = """    val candidatePairAnalysis = MutableStateFlow<CandidatePairAnalysis?>(null)
    val currentFairnessScore = MutableStateFlow(0)
    val currentFairnessRating = MutableStateFlow("")"""
content = content.replace(old_state, new_state)

with open('app/src/main/java/com/example/ui/viewmodel/AppViewModel.kt', 'w') as f:
    f.write(content)
