import re

path1 = r'c:\Users\dhanu\AndroidStudioProjects\draftit\app\src\main\java\com\flash\draft\ui\canvas\DraftCanvas.kt'
with open(path1, 'r', encoding='utf-8') as f:
    content1 = f.read()

content1 = re.sub(
    r'RenderCanvasElements\(\s*elements = elements,\s*scale = scale,\s*offset = offset,',
    r'RenderCanvasElements(\n                            elements = elements,\n                            scale = 1f,\n                            offset = androidx.compose.ui.geometry.Offset.Zero,',
    content1
)

with open(path1, 'w', encoding='utf-8') as f:
    f.write(content1)

path2 = r'c:\Users\dhanu\AndroidStudioProjects\draftit\app\src\main\java\com\flash\draft\ui\canvas\item\RenderCanvasElements.kt'
with open(path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

# Remove the viewport culling completely
cull_regex = re.compile(
    r'        // --- VIEWPORT CULLING ---.*?        // If element is completely outside the cull bounds, skip rendering it\s*'
    r'        if \(!\(elRight < cullLeft \|\| elLeft > cullRight \|\| elBottom < cullTop \|\| elTop > cullBottom\)\) \{\s*'
    r'            // Render element\s*'
    r'        \} else \{\s*'
    r'            return@forEach\s*'
    r'        \}\s*'
    r'            \}\s*'
    r'        \}',
    re.DOTALL
)

new_content2 = cull_regex.sub('', content2)

with open(path2, 'w', encoding='utf-8') as f:
    f.write(new_content2)

print("Done")
