import re

path2 = r'c:\Users\dhanu\AndroidStudioProjects\draftit\app\src\main\java\com\flash\draft\ui\canvas\item\RenderCanvasElements.kt'
with open(path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

# Just remove the culling logic block
content_start = content2.find('// --- VIEWPORT CULLING ---')
content_end = content2.find('if (element is CanvasElement.StickyNote)')

if content_start != -1 and content_end != -1:
    new_content2 = content2[:content_start] + content2[content_end:]
    with open(path2, 'w', encoding='utf-8') as f:
        f.write(new_content2)
    print("Replaced Culling")
else:
    print("Could not find culling bounds")

