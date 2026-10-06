import os

file_path = 'app/src/test/java/com/flash/draft/ui/canvas/CanvasViewModelTest.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('fun tearDown() {\n        Dispatchers.resetMain()', 'fun tearDown() {\n        testDispatcher.scheduler.advanceUntilIdle()\n        Dispatchers.resetMain()')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
