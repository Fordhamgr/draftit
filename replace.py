import os
import re

file_path = 'app/src/main/java/com/flash/draft/ui/canvas/CanvasViewModel.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add import for update
if 'import kotlinx.coroutines.flow.update' not in content:
    content = content.replace('import kotlinx.coroutines.flow.MutableStateFlow', 'import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.update')

# Remove _elements, elements, _scenes, scenes
content = re.sub(r'    private val _elements = MutableStateFlow<List<CanvasElement>>\(emptyList\(\)\)\n    val elements = _elements.asStateFlow\(\)\n', '', content)
content = re.sub(r'    private val _scenes = MutableStateFlow<List<Scene>>\(emptyList\(\)\)\n    val scenes = _scenes.asStateFlow\(\)\n', '', content)

# Replace occurrences
content = content.replace('_elements.value', '_uiState.value.elements')
content = content.replace('_scenes.value', '_uiState.value.scenes')

content = re.sub(r'_uiState\.value\.elements = newElements', r'_uiState.update { it.copy(elements = newElements) }', content)

# updateElements
content = re.sub(
    r'_uiState\.value\.elements = newElements',
    r'_uiState.update { it.copy(elements = newElements) }',
    content
)

# Undo
content = re.sub(
    r'_uiState\.value\.elements = prevState\.elements\s+_uiState\.value\.scenes = prevState\.scenes',
    r'_uiState.update { it.copy(elements = prevState.elements, scenes = prevState.scenes) }',
    content
)

# Redo
content = re.sub(
    r'_uiState\.value\.elements = nextState\.elements\s+_uiState\.value\.scenes = nextState\.scenes',
    r'_uiState.update { it.copy(elements = nextState.elements, scenes = nextState.scenes) }',
    content
)

# addScene
content = re.sub(
    r'_uiState\.value\.scenes = _uiState\.value\.scenes \+ newScene',
    r'_uiState.update { it.copy(scenes = it.scenes + newScene) }',
    content
)

# removeScene
content = re.sub(
    r'_uiState\.value\.scenes = _uiState\.value\.scenes\.filterNot { it\.id == id }',
    r'_uiState.update { it.copy(scenes = it.scenes.filterNot { s -> s.id == id }) }',
    content
)

# renameScene
content = re.sub(
    r'_uiState\.value\.scenes = _uiState\.value\.scenes\.map { if \(it\.id == id\) it\.copy\(name = newName\) else it }',
    r'_uiState.update { s -> s.copy(scenes = s.scenes.map { if (it.id == id) it.copy(name = newName) else it }) }',
    content
)

# clearCanvas
content = re.sub(
    r'_uiState\.value\.elements = emptyList\(\)',
    r'_uiState.update { it.copy(elements = emptyList()) }',
    content
)

# init block
content = re.sub(
    r'_uiState\.value\.elements = savedData\s+_uiState\.value\.scenes = savedScenes',
    r'',
    content
)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("CanvasViewModel replaced successfully.")
