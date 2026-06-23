# The Ultimate Jetpack Compose Canvas Optimization Guide

This document is a culmination of advanced optimization strategies applied to infinite canvases and highly interactive Compose UI. By following these rules, your canvas will run at a buttery-smooth 60-120 FPS without micro-stutters or Garbage Collection (GC) pauses.

## 1. Eliminate Autoboxing with Unboxed State Holders
When tracking highly volatile state (like `scale`, `offsetX`, `offsetY` changing 60+ times a second), **never** use standard generic state holders for primitive values.

**❌ Bad:**
```kotlin
var scale by remember { mutableStateOf(1f) } // Allocates a new java.lang.Float object every frame!
```

**✅ Good:**
```kotlin
var scale by remember { mutableFloatStateOf(1f) } // Stores the primitive float directly. Zero allocations.
var lastTime by remember { mutableLongStateOf(0L) }
```
*Why?* Primitives passed into `mutableStateOf<T>` are boxed into objects. Rapidly changing them creates thousands of discarded objects on the heap, forcing the Garbage Collector to run and cause micro-stutters (jank).

## 2. Defer State Reads to the Draw Phase
If you read an observable state (`scale`, `offset`) directly inside a `@Composable` block, the *entire block* will recompose every time the state changes. 

**❌ Bad (Causes thousands of recompositions during drag):**
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Reading scale here causes this entire Box's lambda to recompose!
    val perfMode = scale < 0.5f 
    DrawCanvas(scale = scale) 
}
```

**✅ Good (Defers read to graphicsLayer and DrawScope):**
```kotlin
Box(modifier = Modifier
    .fillMaxSize()
    .graphicsLayer {
        // Read scale INSIDE the graphicsLayer block.
        // It skips the composition and layout phases completely!
        scaleX = scale 
        scaleY = scale
    }
) {
    // Pass a lambda provider instead of the raw value if a child needs it during draw time.
    DrawCanvas(scaleProvider = { scale }) 
}
```

## 3. Lambda Caching (`remember`) in Heavy Lists
When rendering hundreds of items (like Canvas notes, frames, or shapes), passing an inline lambda will break the stability of the component, causing every single element to recompose even if it hasn't changed.

**❌ Bad:**
```kotlin
RenderItem(
    onDelete = { elements.remove(it) } // Creates a new lambda instance every frame
)
```

**✅ Good:**
```kotlin
RenderItem(
    // The lambda is cached and only recreated if element.id changes.
    onDelete = remember(element.id) { { elements.remove(it) } } 
)
```

## 4. Let the GPU Handle Culling (`graphicsLayer`)
Software-based Viewport Culling (looping over every element on the CPU to check if it's within the screen bounds) can paradoxically cause more lag than it saves when dealing with complex UIs.

**The Fix:**
1. Do not calculate intersections on the CPU in Kotlin.
2. Render everything with `scale = 1f` and `offset = Offset.Zero` inside the actual child composables.
3. Wrap the parent container in a `Modifier.graphicsLayer` and apply the `translationX/Y` and `scaleX/Y` there.
4. The GPU will natively hardware-clip everything outside the screen almost instantly.

## 5. Use Primitive Arrays for Heavy Data (Sketches)
When storing thousands of points for a freehand drawing or sketch, a `List<Float>` or `List<Offset>` requires thousands of object allocations. 

**✅ Good:**
Use `FloatArray` and `LongArray` for X coords, Y coords, pressures, and timestamps.
```kotlin
data class Sketch(
    val xCoords: FloatArray,
    val yCoords: FloatArray,
    val times: LongArray
)
```
*Why?* A `FloatArray` is a single contiguous block of memory. A `List<Float>` is an array of pointers to thousands of individual `java.lang.Float` objects scattered across the heap. Using arrays drastically reduces memory footprint and iteration speed.

## 6. Zero-Allocation Connector & Arrow Rendering
When drawing complex background elements like connectors/arrows 120 times a second, even minor allocations or redundant math can spike the `MaxFrameTime` (jank).

### A. Avoid `filterIsInstance` on hot paths
**❌ Bad:**
```kotlin
// Allocates a new ArrayList on EVERY frame!
val connectors = elements.filterIsInstance<CanvasElement.Connector>() 
connectors.forEach { ... }
```
**✅ Good:**
```kotlin
// Zero allocations.
elements.forEach { element ->
    if (element is CanvasElement.Connector) { ... }
}
```

### B. Use Inline Value Classes (`Offset`, `Size`) over `Pair`
When returning dimensions or coordinates from helper functions in a hot draw loop, never use `Pair<Float, Float>`.
**❌ Bad:**
```kotlin
fun getDimensions(): Pair<Float, Float> // Boxes 2 Floats AND creates a Pair object!
fun getCenter(): Offset? // Nullable Offset forces boxing!
```
**✅ Good:**
```kotlin
fun getDimensions(): Size // Size is an inline value class (primitive long under the hood). Zero allocations!
fun getCenter(): Offset // Non-nullable Offset is primitive. Use Offset.Unspecified instead of null.
```

### C. Collapse Shadow Passes (GPU Draw Calls)
Fake blurs created by drawing multiple layered paths are extremely expensive for the GPU.
**❌ Bad:** Drawing a thick line, then a medium line, then a thin line for a shadow, then repeating for the arrow head (13+ draw calls per arrow).
**✅ Good:** Merge the shadow into a single `drawPath` call with a thick stroke width and a very low alpha (e.g., `alpha = 0.08f`). Reduces GPU draw calls by >60% with identical visuals.

### D. Trigonometric Math Caching
**❌ Bad:**
```kotlin
val x1 = center.x + radius * cos(angle)
val y1 = center.y + radius * sin(angle)
val x2 = center.x - radius * cos(angle)
val y2 = center.y - radius * sin(angle)
```
**✅ Good:**
```kotlin
val c = cos(angle)
val s = sin(angle)
val x1 = center.x + radius * c
// ...
```
Calling `Math.cos` and `Math.sin` is CPU intensive. Cache the results in local variables if used multiple times per vertex calculation.

## 7. Zero-Allocation Gesture Math
During high-frequency touch events like pinch-to-zoom or multi-touch pan, allocating any objects inside the `pointerInput` lambda or camera transform function will trigger the Garbage Collector and cause frame drops.

**❌ Bad:**
```kotlin
fun calculateTransform(): Pair<Float, Offset> { ... } // Allocates a Pair on every frame!
```

**✅ Good:**
Inline the camera transform math directly into the `pointerInput` block to keep all calculations strictly on the primitive stack without allocating return objects.

Furthermore, do not use defensive list copies like `elements.toList().forEach` inside the recomposition scope of the gesture handler, as this creates a full `ArrayList` allocation on every zoomed frame. Compose's `SnapshotStateList` can be iterated over directly (`elements.forEach`) safely.
