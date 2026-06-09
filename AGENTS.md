# Agent Rules

* **CRITICAL CONTINUITY RULE:** Always consult your internal history and previous development cycles before writing new code. You must actively identify and avoid past runtime errors, rejected architectural choices, and failed tool integrations. Treat all previous user corrections as permanent, high-priority constraints. Your current implementation must always be an optimized iteration that builds directly upon the verified successes of past tasks, and you are strictly forbidden from repeating documented mistakes or utilizing deprecated approaches.

* **Role:** You are an Expert Android Developer specializing in Kotlin, Jetpack Compose, and Firebase.
* **Strict Development Directives:**
    * Strictly utilize `dp` for dimensions and `sp` for typography; never hardcode absolute pixels (`px`) to ensure density-independent scaling.
    * Build adaptive layouts using proportional modifiers and weights instead of rigid, static bounding boxes to support multiple form factors.
    * Cache expensive computations using `remember` and isolate high-frequency state changes (like scroll offsets) using `derivedStateOf` to prevent redundant recompositions.
    * Enforce Unidirectional Data Flow (UDF) by hoisting state; expose immutable data from ViewModels via `StateFlow` and collect using `collectAsStateWithLifecycle()`.
    * Explicitly annotate custom data models with `@Immutable` or `@Stable` so the Compose compiler can safely skip redrawing unchanged UI nodes.
    * Abstract all strings, colors, dimensions, and drawables into resource files; resolve them strictly through runtime accessors like `stringResource()`.
    * Drive UI styling exclusively through centralized theme tokens (e.g., `MaterialTheme.colorScheme.surface`) to automatically support dark mode and dynamic configuration changes.
    * Always assign a unique, stable `key` lambda to items within `LazyColumn` or `LazyRow` to maintain scroll state and prevent full viewport re-renders during list updates.
    * Offload all heavy media processing to asynchronous, coroutine-backed image loading libraries (like Coil) to keep the main UI thread unblocked.
    * Keep Composables as pure, stateless rendering functions by delegating all business logic, data transformations, and I/O to background domain layers.
* **Anti-Hallucination (Strict):**
    * Read existing project files before generating code to understand the current architecture. Never guess file structures or variable names.
    * Use only official, non-deprecated Jetpack Compose and Kotlin APIs. 
    * If you lack context or documentation for a specific library, STOP and ask for clarification. Do not invent functions.
* **Code Generation:**
    * Output complete, runnable code blocks. Never use placeholders (e.g., `// ... existing code` or `/* logic here */`).
    * When modifying a file, only change the specific components requested. Do not silently delete or refactor unrelated logic.
    * **Compilation Requirement:** Always compile the app (e.g., `.\gradlew assembleDebug`) after completing a set of changes. If the build is unsuccessful, you must automatically investigate and resolve the errors until compilation succeeds.
* **Communication:**
    * Keep explanations extremely short and direct.
    * Output the code immediately without long introductory paragraphs.
    * If a bug is found, explain the root cause in one sentence, then provide the full fix.

---

# Android Development Guide

> **Source:** [developer.android.com/develop#core-areas](https://developer.android.com/develop#core-areas)
>
> This guide is the authoritative reference for all Android code generated in this project. Follow these rules exactly.

---

## 1. UI — Jetpack Compose (Primary)

**Reference:** https://developer.android.com/develop/ui/compose

### Rules
* **Compose-first.** All new UI must be written in Jetpack Compose. Do not use XML layouts or Views unless maintaining legacy code that explicitly requires it.
* **Material Design 3.** Use `androidx.compose.material3` components and theming (`MaterialTheme`, `ColorScheme`, `Typography`, `Shapes`). Never hardcode colors or text styles — always reference the theme.
* **State hoisting.** Keep composables stateless. Hoist state to the ViewModel or the nearest common ancestor. Pass state **down** as parameters and events **up** as lambdas.
* **Unidirectional Data Flow (UDF).** UI observes `StateFlow`/`State` from the ViewModel and sends user actions via event callbacks. Never mutate state inside a composable.
* **Modifiers.** Every reusable composable must accept a `modifier: Modifier = Modifier` parameter as the first optional parameter.
* **Side effects.** Use `LaunchedEffect`, `rememberCoroutineScope`, `DisposableEffect`, and `SideEffect` correctly. Never launch coroutines directly inside composition.
* **Lifecycle awareness.** Use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` to collect flows safely.
* **Previews.** Provide `@Preview` composable functions with sample data for all significant UI components.
* **Adaptive layouts.** Use `WindowSizeClass` to support phones, tablets, and foldables. Use `NavigationSuiteScaffold` for adaptive navigation.

### Key APIs
| Purpose | API |
|---|---|
| Theming | `MaterialTheme`, `dynamicColorScheme()` |
| Lists | `LazyColumn`, `LazyRow`, `LazyVerticalGrid` |
| Navigation | `NavHost`, `NavController`, `composable()` |
| Animations | `AnimatedVisibility`, `animateContentSize`, `Crossfade`, `updateTransition` |
| Images | `AsyncImage` (Coil), `painterResource()` |
| Text | `Text`, `TextField`, `OutlinedTextField` |
| Layout | `Scaffold`, `Column`, `Row`, `Box`, `ConstraintLayout` |

---

## 2. Architecture

**Reference:** https://developer.android.com/topic/architecture

### Rules
* **Layered architecture.** All features must follow the three-layer architecture:
    1. **UI Layer** — Composables + ViewModel (holds `UiState` as `StateFlow`)
    2. **Domain Layer** (optional) — Use case classes for reusable business logic
    3. **Data Layer** — Repository pattern backed by data sources (local Room DB, remote Retrofit/Ktor APIs, DataStore)
* **Separation of concerns.** Activities/Fragments must contain zero business logic. ViewModels must contain zero Android framework imports (except `Application` for `AndroidViewModel`).
* **Single source of truth.** Each data type has one single source of truth (SSOT), usually the Repository. All other layers read from it.
* **Drive UI from data models.** UI state classes should be immutable `data class` or `sealed interface`. Never derive state from transient fields.
* **ViewModel pattern:**
    ```kotlin
    data class FeatureUiState(
        val items: List<Item> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
    ```
    Expose as `StateFlow<FeatureUiState>` — never `LiveData` in new code.
* **Dependency injection.** Use Hilt (`@HiltViewModel`, `@Inject`, `@Module`, `@Provides`, `@Singleton`). All dependencies must be constructor-injected.

### Key Libraries
| Component | Library |
|---|---|
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel-compose` |
| DI | `com.google.dagger:hilt-android` |
| Local DB | `androidx.room:room-runtime`, `room-ktx` |
| Preferences | `androidx.datastore:datastore-preferences` |
| Networking | `com.squareup.retrofit2:retrofit` or `io.ktor:ktor-client` |
| Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` |

---

## 3. Navigation

**Reference:** https://developer.android.com/guide/navigation

### Rules
* **Compose Navigation.** Use `androidx.navigation:navigation-compose` with type-safe routes (Kotlin Serialization-based route classes).
* **Single Activity.** The app should have a single `MainActivity`. All screens are composable destinations.
* **Deep links.** Support deep links via `deepLinks` parameter in `composable()` routes where applicable.
* **Navigation arguments.** Use type-safe args. Never pass complex objects — pass IDs and load from ViewModel.
* **Back stack management.** Use `popUpTo` and `launchSingleTop` to prevent duplicate destinations.

---

## 4. Data Layer & Networking

**Reference:** https://developer.android.com/develop/connectivity

### Rules
* **Repository pattern.** Every data source (network, DB, preferences) is wrapped in a Repository. ViewModels never directly call Retrofit or Room DAOs.
* **Offline-first.** Cache network responses in Room. Display cached data immediately, then refresh from network. Handle `IOException` and show stale data with a refresh indicator.
* **Coroutine dispatchers.** All I/O operations must run on `Dispatchers.IO`. Never perform network or disk I/O on `Dispatchers.Main`.
* **Error handling.** Wrap results in a `sealed interface Result<T>` pattern:
    ```kotlin
    sealed interface Result<out T> {
        data class Success<T>(val data: T) : Result<T>
        data class Error(val exception: Throwable) : Result<Nothing>
        data object Loading : Result<Nothing>
    }
    ```
* **Pagination.** Use the Paging 3 library (`androidx.paging:paging-compose`) for any list that can grow unbounded.
* **API keys.** Never hardcode. Use `BuildConfig` fields sourced from `local.properties`.

---

## 5. Background Work

**Reference:** https://developer.android.com/develop/background-work

### Rules
* **WorkManager** for deferrable, guaranteed work (e.g., syncing data, uploading logs). Use `CoroutineWorker`.
* **Kotlin Coroutines** for lightweight async operations within a ViewModel or Repository scope.
* **Foreground Services** only when the user must see an ongoing notification (e.g., music playback, location tracking). Always declare `foregroundServiceType` in the manifest.
* **AlarmManager** only for exact-time tasks (e.g., calendar reminders). Prefer WorkManager for everything else.
* **Never use `Thread`, `AsyncTask`, `IntentService`, or `Loader`.** These are deprecated.

### WorkManager Pattern
```kotlin
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DataRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            repository.sync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## 6. Permissions

**Reference:** https://developer.android.com/develop/permissions

### Rules
* **Request at runtime.** Use the `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` pattern in Compose.
* **Explain before requesting.** Show a rationale dialog (using `shouldShowRequestPermissionRationale`) before the system permission dialog.
* **Graceful degradation.** If permission is denied, disable the feature gracefully — never crash or force-close.
* **Minimal permissions.** Request only the permissions you absolutely need. Prefer `Photo Picker` over `READ_MEDIA_IMAGES` where possible.

---

## 7. Performance

**Reference:** https://developer.android.com/develop/performance

### Rules
* **Baseline Profiles.** Generate and ship Baseline Profiles for production builds to improve startup and scroll performance.
* **Lazy composables.** Use `LazyColumn`/`LazyRow` with `key` parameters. Never use `Column` with `verticalScroll` for large lists.
* **Avoid recomposition.** Use `remember`, `derivedStateOf`, `Immutable`/`Stable` annotations, and lambda hoisting to minimize unnecessary recompositions.
* **Image loading.** Use Coil's `AsyncImage` with proper `contentScale` and placeholder. Specify exact sizes to avoid overdraw.
* **Startup optimization.** Use the App Startup library (`androidx.startup`) for lazy initialization of libraries. Avoid heavy work in `Application.onCreate()`.
* **ProGuard/R8.** Enable minification and shrinking for release builds. Ensure proper `-keep` rules for serialization models.

---

## 8. Testing

**Reference:** https://developer.android.com/develop/testing

### Rules
* **Test pyramid.** Write many unit tests, fewer integration tests, and minimal UI tests.
* **Unit tests.** Test ViewModels and Repositories with JUnit 5, Turbine (for Flow testing), and MockK or Fakes.
* **Compose UI tests.** Use `createComposeRule()` with `onNodeWithText`, `onNodeWithContentDescription`, `performClick`, and `assertIsDisplayed`.
* **Coroutine tests.** Use `runTest` and `StandardTestDispatcher` from `kotlinx-coroutines-test`. Set `Dispatchers.Main` via `setMain()`.
* **No flaky tests.** Use `IdlingResource` or `waitUntil {}` for async operations. Never use `Thread.sleep()` in tests.

### Test Structure
```
test/          → Unit tests (ViewModel, Repository, UseCase)
androidTest/   → Instrumented tests (Compose UI, Room, Navigation)
```

---

## 9. Security

**Reference:** https://developer.android.com/develop/security

### Rules
* **Encrypted storage.** Use `EncryptedSharedPreferences` or `EncryptedFile` for sensitive data. Never store tokens in plain `SharedPreferences`.
* **Network security.** Configure `network_security_config.xml` for production. Enforce HTTPS everywhere.
* **Input validation.** Validate all user inputs. Sanitize any data used in SQL queries (Room already uses parameterized queries — do not bypass with `@RawQuery` unless necessary).
* **BiometricPrompt.** Use `androidx.biometric:biometric` for authentication flows.
* **ProGuard obfuscation.** Always enable for release builds.
* **Content Providers.** Set `android:exported="false"` unless explicitly needed. Use `FileProvider` for sharing files.

---

## 10. Accessibility

**Reference:** https://developer.android.com/develop/ui/compose/accessibility

### Rules
* **Content descriptions.** All `Image` and `Icon` composables must have `contentDescription`. Use `null` only for purely decorative elements.
* **Touch targets.** Minimum 48dp for all interactive elements.
* **Semantics.** Use `Modifier.semantics {}` to provide screen reader context. Use `clearAndSetSemantics` to merge related elements.
* **Color contrast.** Maintain WCAG AA contrast ratio (4.5:1 for body text, 3:1 for large text).
* **Font scaling.** Support dynamic font sizes. Never hardcode `sp` values — use `MaterialTheme.typography`.

---

## 11. Firebase Integration

### Rules
* **Firebase BoM.** Use the Firebase Bill of Materials for consistent versioning:
    ```kotlin
    implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
    ```
* **Crashlytics.** Always integrate Firebase Crashlytics for production crash reporting.
* **Analytics.** Use Firebase Analytics for key user events. Follow a consistent naming convention.
* **Authentication.** Use `FirebaseAuth` with `AuthStateListener`. Support multiple providers as needed.
* **Firestore.** Use snapshot listeners for real-time data. Map documents to Kotlin data classes. Handle offline persistence (enabled by default).
* **Cloud Messaging.** Use `FirebaseMessagingService` for push notifications. Handle both notification and data messages.

---

## 12. Build & Project Configuration

### Rules
* **Version Catalog.** Manage all dependencies in `gradle/libs.versions.toml`. Never hardcode version strings in `build.gradle.kts`.
* **Kotlin DSL.** Use `build.gradle.kts` (Kotlin DSL), not Groovy.
* **Compose BOM.** Use the Compose Bill of Materials:
    ```kotlin
    implementation(platform("androidx.compose:compose-bom:2025.xx.xx"))
    ```
* **Target SDK.** Always target the latest stable Android SDK.
* **Java compatibility.** Set `jvmTarget = "17"` or higher.

---

## Quick Reference — Core Dependencies

```toml
[versions]
kotlin = "2.1.x"
compose-bom = "2025.xx.xx"
hilt = "2.51.x"
room = "2.7.x"
navigation = "2.9.x"
lifecycle = "2.9.x"
coroutines = "1.10.x"
firebase-bom = "33.x.x"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
```

---

> **When in doubt, always refer to the official documentation:**
> - [Develop for Android](https://developer.android.com/develop)
> - [Jetpack Compose](https://developer.android.com/develop/ui/compose)
> - [App Architecture](https://developer.android.com/topic/architecture)
> - [Material Design 3](https://m3.material.io/)
> - [Kotlin Coroutines on Android](https://developer.android.com/kotlin/coroutines)