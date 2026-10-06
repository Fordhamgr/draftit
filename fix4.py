import os
file_path = 'app/src/test/java/com/flash/draft/ui/canvas/CanvasViewModelTest.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

if '@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)' not in content:
    content = content.replace('class CanvasViewModelTest {', '@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)\nclass CanvasViewModelTest {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
