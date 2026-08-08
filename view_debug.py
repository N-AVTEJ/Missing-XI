with open('app/src/main/java/com/example/ui/screens/TeamConfigScreen.kt', 'r') as f:
    lines = f.readlines()

found = False
for i, line in enumerate(lines):
    if "Developer Debug Section (Collapsible)" in line:
        found = True
    if found:
        print(f"{i+1}: {line}", end="")
        if "Teammate Pair Tracking Debug Display" in line:
            break
