import os

file_path = 'app/src/main/java/com/flash/draft/ui/canvas/CanvasViewModel.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if '_uiState.value.elements = savedData' in line:
        continue
    if '_uiState.value.scenes = savedScenes' in line:
        continue
    new_lines.append(line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
