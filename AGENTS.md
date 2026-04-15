# AGENTS.md - WebImageViewer (Web Crawler Android App)

## Project Overview
Android app for crawling web pages, extracting images/links, downloading ZIP archives, and browsing media content. Built with Kotlin, targeting API 26-35.

## Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.eddy.webcrawler.MyTestClass"

# Run a single test method
./gradlew test --tests "com.eddy.webcrawler.MyTestClass.myTestMethod"

# Run instrumented tests on device/emulator
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Clean build
./gradlew clean

# Install on connected device
./gradlew installDebug
```

## Architecture
- **Pattern**: MVVM with Repository pattern
- **DI**: Hilt/Dagger2 (`@HiltViewModel`, `@AndroidEntryPoint`, `@Module`/`@InstallIn`)
- **Database**: Room with Flow-based queries (`LinkIndexDao`, `AppDatabase`)
- **Network**: OkHttp + Jsoup for HTML parsing
- **Image Loading**: Coil (`imageView.load(url) { ... }`)
- **Video**: ExoPlayer (Media3)
- **Background Work**: WorkManager with `CoroutineWorker` + Hilt (`@HiltWorker`)
- **Navigation**: Jetpack Navigation Component (nav_graph.xml)
- **UI Binding**: ViewBinding (`FragmentXxxBinding`)
- **Async**: Kotlin Coroutines + Flow (`StateFlow`, `lifecycleScope.launch`, `repeatOnLifecycle`)

## Code Style & Conventions

### Package Structure
```
com.eddy.webcrawler
├── data/
│   ├── crawler/      # WebCrawler (OkHttp + Jsoup)
│   ├── db/           # Room DAO + Database
│   ├── download/     # DownloadWorker (WorkManager)
│   ├── model/        # Data classes, entities, enums
│   └── repository/   # CrawlerRepository
├── di/               # Hilt modules (AppModule)
└── ui/
    ├── adapter/      # RecyclerView adapters
    ├── fragment/     # Fragments
    └── viewmodel/    # ViewModels + sealed UI states
```

### Imports
- Group imports: android.* / androidx.* / third-party / stdlib / javax.inject
- Use wildcard imports for Android view groups: `import android.view.*`
- Explicit imports for all other packages
- No unused imports

### Naming Conventions
| Type | Convention | Example |
|------|-----------|---------|
| Classes | PascalCase | `MainFragment`, `ContentAdapter` |
| Functions/Properties | camelCase | `submitContent`, `loadPage` |
| Constants (companion) | UPPER_SNAKE_CASE | `TYPE_IMAGE`, `KEY_URL` |
| XML IDs | snake_case | `btnDownload`, `recyclerView` |
| Layout files | snake_case with prefix | `fragment_main.xml`, `item_image.xml` |
| ViewModel states | PascalCase sealed classes | `CrawlState`, `PageState` |

### ViewModel State Pattern
Use sealed classes for UI states with `Idle`, `Loading`, `Success(data)`, `Error(message)` variants:
```kotlin
sealed class PageState {
    object Idle : PageState()
    object Loading : PageState()
    data class Success(val content: PageContent) : PageState()
    data class Error(val message: String) : PageState()
}
```

### Fragment Binding Pattern
Always use nullable binding with cleanup in `onDestroyView`:
```kotlin
private var _binding: FragmentXxxBinding? = null
private val binding get() = _binding!!
override fun onDestroyView() { super.onDestroyView(); _binding = null }
```

### Coroutines & Flow
- Use `viewModelScope.launch` for ViewModel work
- Use `viewLifecycleOwner.lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }` in Fragments
- Expose `StateFlow` from ViewModels, collect with `repeatOnLifecycle`
- Use `Result<T>` for repository error handling with `fold(onSuccess, onFailure)`

### Error Handling
- Repository layer: return `Result<T>` using `runCatching { }` and `mapCatching { }`
- ViewModel: convert to sealed state via `Result.fold()`
- Worker: return `Result.failure(workDataOf(KEY_ERROR to message))`
- UI: show error text + retry button on `Error` state

### Dependency Injection
- `@Singleton` for app-wide providers in `AppModule`
- `@HiltViewModel` + `@Inject constructor` for ViewModels
- `@AndroidEntryPoint` on Activities/Fragments
- `@HiltWorker` + `@AssistedInject` for WorkManager workers

### XML Layout Conventions
- Use Material Components (`MaterialToolbar`, `MaterialButton`, `TextInputLayout`)
- Reference theme attributes: `?attr/colorOnSurface`, `?attr/colorError`
- Use `app:layout_behavior` for CoordinatorLayout children
- Set `android:visibility="gone"` by default for conditional views

### RecyclerView Adapters
- Use sealed `ContentItem` classes for multiple view types
- Define `TYPE_*` constants in `companion object`
- Inner `ViewHolder` classes with `bind()` methods
- Use `DiffUtil` or `ListAdapter` when possible

### Safety
- Prevent zip-slip in extraction: validate `canonicalPath.startsWith(destDir.canonicalPath)`
- Clean up temp files after extraction: `zipFile.delete()`
- Use `use { }` blocks for all stream/IO operations
- Set User-Agent header on all HTTP requests

## Key Dependencies
| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin Coroutines | 1.8.1 | Async |
| OkHttp | 4.12.0 | HTTP client |
| Jsoup | 1.18.1 | HTML parsing |
| Coil | 2.7.0 | Image loading |
| Media3 ExoPlayer | 1.4.1 | Video playback |
| Room | 2.6.1 | Local database |
| Hilt | 2.51.1 | Dependency injection |
| WorkManager | 2.9.1 | Background tasks |
| Navigation | 2.8.3 | Fragment navigation |
| ViewPager2 | 1.1.0 | Horizontal paging |
| DataStore | 1.1.1 | Preferences storage |
