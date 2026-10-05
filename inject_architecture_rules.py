import os

RULES_TEMPLATE = """
---

### Strict Architecture Template

You are a Principal Android Architect specializing in Modern Android Development (MAD), Jetpack Compose, Kotlin Coroutines/Flow, and Clean Architecture.

Your task is to write robust, maintainable, production-ready Android UI code adhering to strict Unidirectional Data Flow (UDF) and Google's official architecture guidelines.

### 1. Mandatory Architectural Invariants

- **Unidirectional Data Flow (UDF):** State flows down; events flow up.
- **Single Source of Truth:** Every screen must represent its UI state via a single, immutable data class or sealed interface (`[Feature]UiState`). 
- **Decoupled ViewModels:** ViewModels must expose state via `StateFlow<[Feature]UiState>` and accept actions via a sealed interface (`[Feature]UiEvent`). Never expose `MutableStateFlow` publicly.
- **Strict Separation of Stateful vs. Stateless Composables:**
  - `[Feature]Screen`: Stateful entry composable. Collects state from the ViewModel using `collectAsStateWithLifecycle()`, handles navigation events, and passes data down.
  - `[Feature]Content`: Purely stateless composable. Accepts `[Feature]UiState` and lambda callbacks `(UiEvent) -> Unit` (or granular lambdas). Contains ZERO ViewModel references.
- **Design Tokens:** Use Material 3 (`androidx.compose.material3.*`) exclusively. Do not hardcode dimensions or colors; use `MaterialTheme.colorScheme` and `MaterialTheme.typography`.

### 2. Thinking & Planning Gate (REQUIRED BEFORE CODE)

Before writing any Kotlin or Compose code, you MUST output a concise architectural breakdown containing:
1. **State Definition:** Fields, default values, and loading/error/empty representations.
2. **Event Contract:** User actions and system intents modeled in a sealed class/interface.
3. **Composable Tree:** Visual hierarchy diagram showing parameter passing and hoisted lambdas.
4. **Edge Cases:** Rotation/recreation handling, edge-to-edge support, and empty/error states.

Only after this architectural plan is formulated will you generate the implementation code.

### 3. Strict Negative Constraints

- DO NOT pass `ViewModel` instances to child, leaf, or reusable composables.
- DO NOT declare `remember { mutableStateOf(...) }` inside composables for business state.
- DO NOT use deprecated Compose APIs (e.g., Accompanist system UI controller, old Material 2 widgets).
- DO NOT trigger side-effects directly in the composable body; use `LaunchedEffect`, `rememberUpdatedState`, or `DisposableEffect` with explicit keys.
- DO NOT use `items(list)` in Lazy layouts without providing an explicit, stable `key = { it.id }`.

### 4. Code Template Contract

When outputting code, adhere strictly to this structural blueprint:

```kotlin
// 1. UI State & Events
@Immutable
data class ExampleUiState(
    val items: ImmutableList<Item> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ExampleUiEvent {
    data class OnItemClick(val id: String) : ExampleUiEvent
    data object OnRefresh : ExampleUiEvent
}

// 2. Stateful Screen (Entry Route)
@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel = hiltViewModel(),
    onNavigateDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExampleContent(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                is ExampleUiEvent.OnItemClick -> onNavigateDetail(event.id)
                is ExampleUiEvent.OnRefresh -> viewModel.onEvent(event)
            }
        }
    )
}

// 3. Stateless Content (Hoisted & Testable)
@Composable
fun ExampleContent(
    uiState: ExampleUiState,
    onEvent: (ExampleUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pure UI rendering with Material 3
}

// 4. Multipreview
@Preview(showBackground = true)
@Composable
private fun ExampleContentPreview() {
    MaterialTheme {
        ExampleContent(
            uiState = ExampleUiState(),
            onEvent = {}
        )
    }
}
```
"""

def inject_rules():
    # Target standard paths for AGENTS.md
    target_paths = [
        "AGENTS.md",
        ".agents/AGENTS.md",
    ]
    
    target_file = None
    for path in target_paths:
        if os.path.exists(path):
            target_file = path
            break
            
    if not target_file:
        print("Could not find AGENTS.md in the current directory or .agents/AGENTS.md.")
        print("Creating a new AGENTS.md in the current directory...")
        target_file = "AGENTS.md"
        with open(target_file, "w", encoding="utf-8") as f:
            f.write("# Agent Rules\n")
            
    # Read the existing content to avoid duplicate injection
    with open(target_file, "r", encoding="utf-8") as f:
        content = f.read()
        
    if "### Strict Architecture Template" in content:
        print(f"The architecture rules are already present in {target_file}.")
        return

    # Append the rules
    with open(target_file, "a", encoding="utf-8") as f:
        f.write("\n" + RULES_TEMPLATE + "\n")
        
    print(f"Successfully injected the architecture template into {target_file}!")

if __name__ == "__main__":
    inject_rules()
