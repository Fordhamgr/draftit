import os

file_path = 'app/src/main/java/com/flash/draft/ui/canvas/DraftCanvas.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace viewModels usage
content = content.replace('viewModel.elements.value', 'uiState.elements')
content = content.replace('viewModel.scenes.value', 'uiState.scenes')
content = content.replace('val scenes by viewModel.scenes.collectAsStateWithLifecycle()', 'val scenes = uiState.scenes')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("DraftCanvas.kt replaced successfully.")
