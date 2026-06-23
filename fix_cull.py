import re

path1 = r'c:\Users\dhanu\AndroidStudioProjects\draftit\app\src\main\java\com\flash\draft\ui\canvas\DraftCanvas.kt'
with open(path1, 'r', encoding='utf-8') as f:
    content1 = f.read()

content1 = re.sub(
    r'RenderCanvasElements\(\s*elements = elements,\s*scale = 1f,\s*offset = Offset\.Zero,',
    r'RenderCanvasElements(\n                            elements = elements,\n                            scale = scale,\n                            offset = offset,',
    content1
)

with open(path1, 'w', encoding='utf-8') as f:
    f.write(content1)


path2 = r'c:\Users\dhanu\AndroidStudioProjects\draftit\app\src\main\java\com\flash\draft\ui\canvas\item\RenderCanvasElements.kt'
with open(path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

# Replace scale = scale, offset = offset, with 1f and Zero
content2 = re.sub(
    r'scale = scale,\s*offset = offset,',
    r'scale = 1f,\n                    offset = androidx.compose.ui.geometry.Offset.Zero,',
    content2
)

with open(path2, 'w', encoding='utf-8') as f:
    f.write(content2)

print("Done")
