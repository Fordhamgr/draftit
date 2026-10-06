import os

file_path = 'app/src/test/java/com/flash/draft/ui/canvas/CanvasViewModelTest.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if 'logRender' in line or 'logTool' in line or 'logState' in line or '.e(' in line:
        continue
    new_lines.append(line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
