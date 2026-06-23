import re
import subprocess

path = r'c:\Users\dhanu\AndroidStudioProjects\draftit\app\src\main\java\com\flash\draft\ui\canvas\item\RenderCanvasElements.kt'

subprocess.run(["git", "checkout", "--", path], cwd=r"c:\Users\dhanu\AndroidStudioProjects\draftit")

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# ADD MISSING IMPORT
content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.runtime.remember")

# 1. onConnectRequest
content = re.sub(r'onConnectRequest = \{ (.*?) \},', r'onConnectRequest = remember(element.id) { { \1 } },', content)

# 2. onPositionDelta
content = re.sub(r'onPositionDelta = \{ delta -> (.*?) \},', r'onPositionDelta = remember(element.id) { { delta: androidx.compose.ui.geometry.Offset -> \1 } },', content)

# 3. onSaveRequest
content = re.sub(r'onSaveRequest = \{ (.*?) \},', r'onSaveRequest = remember(element.id) { { \1 } },', content)

# 4. onDelete
content = re.sub(
    r'onDelete = \{\s*elements\.removeAll \{ it\.id == element\.id \}\s*elements\.removeAll \{ it is CanvasElement\.Connector && \(it\.startId == element\.id \|\| it\.endId == element\.id\) \}\s*onResetSelectedNoteId\(\)\s*onUpdateElements\(elements\.toList\(\)\)\s*\},',
    r'''onDelete = remember(element.id) { {
                        elements.removeAll { it.id == element.id }
                        elements.removeAll { it is CanvasElement.Connector && (it.startId == element.id || it.endId == element.id) }
                        onResetSelectedNoteId()
                        onUpdateElements(elements.toList())
                    } },''',
    content
)

# 5. onSelect
content = re.sub(
    r'onSelect = \{\s*if \(isConnectingMode\) \{\s*if \(connectingStartNodeId == null\) onUpdateConnectingStartNodeId\(element\.id\)\s*else if \(connectingStartNodeId != element\.id\) onFinishConnection\(element\.id\)\s*\} else if \(!isDrawingMode && !isLassoMode\) \{\s*if \(!selectedGroupIds\.contains\(element\.id\)\) \{\s*onUpdateSelectedGroupIds\(emptySet\(\)\)\s*onUpdateSelectedNoteId\(element\.id\)\s*onBringToFront\(element\.id\)\s*\}\s*\}\s*\}',
    r'''onSelect = remember(element.id, isConnectingMode, connectingStartNodeId, isDrawingMode, isLassoMode, selectedGroupIds) { {
                        if (isConnectingMode) {
                            if (connectingStartNodeId == null) onUpdateConnectingStartNodeId(element.id)
                            else if (connectingStartNodeId != element.id) onFinishConnection(element.id)
                        } else if (!isDrawingMode && !isLassoMode) {
                            if (!selectedGroupIds.contains(element.id)) {
                                onUpdateSelectedGroupIds(emptySet())
                                onUpdateSelectedNoteId(element.id)
                                onBringToFront(element.id)
                            }
                        }
                    } }''',
    content
)

# 6. onSizeDelta
def repl_onsizedelta(m):
    body = m.group(1)
    return f'onSizeDelta = remember(element.id) {{ {{ delta: androidx.compose.ui.geometry.Offset ->{body} }} }},'

content = re.sub(r'onSizeDelta = \{ delta ->(.*?)^\s*\},', repl_onsizedelta, content, flags=re.DOTALL | re.MULTILINE)

# 7. onUpdate
# We need to map the variable name to the type
var_to_type = {
    "updatedFrame": "CanvasElement.FrameNode",
    "updatedNote": "CanvasElement.StickyNote",
    "updatedBlock": "CanvasElement.CodeBlock",
    "updatedCard": "CanvasElement.ImageCard",
    "updatedPdf": "CanvasElement.PdfCard",
    "updatedAudio": "CanvasElement.AudioNote",
    "updatedShape": "CanvasElement.ShapeNode",
    "updatedNode": "CanvasElement.LabelNode",
    "updatedList": "CanvasElement.TaskListNode"
}
# wait, updatedCard is used for both ImageCard and PdfCard? Let's check original.
# It's better to just regex based on the `if (element is ...)` but we can also just use the fact that the cast happens inside `onSizeDelta`.
# Actually, I can just use a generic regex for onUpdate and rely on the variable name.
# Wait, let's just make sure updatedCard type is correct for both. ImageCard Item provides ImageCard, PdfCard item provides PdfCard.
# Let's map by the var name that is captured!

def repl_onupdate(m):
    var_name = m.group(1)
    body = m.group(2)
    # determine type
    typ = "CanvasElement"
    if var_name == "updatedFrame": typ = "CanvasElement.FrameNode"
    elif var_name == "updatedNote": typ = "CanvasElement.StickyNote"
    elif var_name == "updatedBlock": typ = "CanvasElement.CodeBlock"
    elif var_name == "updatedImage": typ = "CanvasElement.ImageCard" # Wait, I don't know if it's updatedImage or updatedCard!
    elif var_name == "updatedCard": typ = "CanvasElement.ImageCard" # Will check below
    elif var_name == "updatedAudio": typ = "CanvasElement.AudioNote"
    elif var_name == "updatedShape": typ = "CanvasElement.ShapeNode"
    # To be perfectly safe, I will just parse the file to find the component type for each block!
    return f'onUpdate = remember(element.id, element.hyperlinkTarget) {{ {{ {var_name} ->{body} }} }},'

# Wait, if I don't know the exact var names, I can do a manual pass with the types.
# Or, even better, I can just not touch `onUpdate`! `onUpdate` only creates 1 lambda per element!
# But wait, 1 lambda per element = 500 new lambdas per frame = recomposing 500 items!
# I MUST fix `onUpdate`.
# I'll just use a generic script that finds the class from `if (element is CanvasElement.XXX)`

content_blocks = content.split('if (element is CanvasElement.')
for i in range(1, len(content_blocks)):
    block = content_blocks[i]
    class_name = block.split(')')[0].strip()
    
    # replace onUpdate
    def repl_onupdate_typed(m):
        var_name = m.group(1)
        body = m.group(2)
        return f'onUpdate = remember(element.id, element.hyperlinkTarget) {{ {{ {var_name}: CanvasElement.{class_name} ->{body} }} }},'
    
    block = re.sub(r'onUpdate = \{ ([a-zA-Z0-9_]+) ->(.*?)^\s*\},', repl_onupdate_typed, block, flags=re.DOTALL | re.MULTILINE)
    content_blocks[i] = block

content = 'if (element is CanvasElement.'.join(content_blocks)


with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Script updated!")
