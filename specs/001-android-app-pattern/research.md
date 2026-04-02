# Research: Web Crawler Android App

## Decision: OkHttp + Jsoup for Web Crawling

**Rationale**: OkHttp provides robust HTTP client capabilities with configurable timeouts, interceptors, and connection pooling. Jsoup offers tolerant HTML parsing with CSS selector support. Together they handle static HTML crawling efficiently on Android.

**Key patterns**:
- Single `OkHttpClient` instance via Hilt `@Singleton` with `User-Agent` interceptor
- `connectTimeout(10s)`, `readTimeout(30s)`, `retryOnConnectionFailure(true)`
- `Jsoup.parse(html, baseUrl)` with `baseUrl` to resolve relative URLs
- `attr("abs:href")` and `attr("abs:src")` for absolute URL resolution
- Pre-compiled `Regex` objects for link filtering (never in loops)
- `response.use { }` to prevent connection leaks
- All crawling on `Dispatchers.IO` via `suspend` functions

**Alternatives considered**:
- **Retrofit**: Overkill for HTML crawling; designed for typed APIs
- **HtmlUnit**: Heavy, not Android-optimized
- **WebView-based scraping**: Requires JS rendering but much heavier; static HTML only for v1

---

## Decision: WorkManager + HiltWorker for Background Downloads

**Rationale**: WorkManager guarantees execution even if app is killed or device restarts. `@HiltWorker` with `@AssistedInject` cleanly injects dependencies into workers.

**Key patterns**:
- Application implements `Configuration.Provider` with injected `HiltWorkerFactory`
- Disable default WorkManager initializer in `AndroidManifest.xml`
- `@HiltWorker` class with `@AssistedInject constructor(@Assisted ctx, @Assisted params, injected deps)`
- `CoroutineWorker` base class for suspend-based `doWork()`
- `setProgress(workDataOf(...))` for progress reporting to UI
- `BackoffPolicy.EXPONENTIAL` with 10s initial delay for retries
- `Result.retry()` for transient failures, `Result.failure()` for permanent errors
- App-private storage via `context.filesDir.resolve("downloads")`

**Alternatives considered**:
- **Coroutine + ViewModel scope**: Lost on app death; not suitable for downloads
- **Foreground Service**: More complex; WorkManager handles lifecycle automatically
- **DownloadManager**: System-level but less control over extraction flow

---

## Decision: Zip-Slip Prevention Pattern

**Rationale**: Malicious ZIP files can contain entries with `../` paths that escape the target directory. Validation against `canonicalPath` is the industry-standard defense.

**Key pattern**:
```kotlin
val canonicalDest = destDir.canonicalPath
val canonicalOut = outFile.canonicalPath
if (!canonicalOut.startsWith("$canonicalDest${File.separator}")) {
    throw SecurityException("Bad zip entry: path traversal attempt")
}
```

**Additional safeguards**:
- Clean up temp ZIP file after extraction: `zipFile.delete()`
- Clean up partial extraction on failure: `destDir.deleteRecursively()`
- Use `ZipInputStream` with `use { }` blocks for stream safety

---

## Decision: Coil for Image Loading

**Rationale**: Coil is Kotlin-first, coroutine-native, and handles lifecycle-aware image loading automatically. Integrates seamlessly with RecyclerView recycling.

**Key patterns**:
- `imageView.load(url) { crossfade(true); placeholder(R.drawable.ic_placeholder); error(R.drawable.ic_error) }`
- Auto-cancels in-flight requests on view recycling
- Memory and disk caching built-in
- `layout_width="match_parent"` + `adjustViewBounds="true"` for full-width aspect-ratio-preserving images

**Alternatives considered**:
- **Glide**: Mature but Java-heavy; Coil has better coroutine integration
- **Picasso**: Simpler but less active development

---

## Decision: Media3 ExoPlayer for Video Playback

**Rationale**: Media3 is the modern AndroidX media library (successor to standalone ExoPlayer). Supports MP4, WebM, and streaming formats out of the box.

**Key patterns**:
- `AndroidMedia3` player with `PlayerView` in XML
- Lifecycle-aware: initialize in `onViewCreated`, release in `onDestroyView`
- Supports local file URIs from extracted ZIP contents

**Alternatives considered**:
- **VideoView**: Limited format support, less control
- **MediaPlayer**: Deprecated for complex use cases

---

## Decision: Room DB for Link Index Persistence

**Rationale**: Room provides type-safe SQLite access with Flow-based reactive queries. Fits the constitution's requirement for non-blocking database operations.

**Key patterns**:
- `@Entity` data classes for `LinkIndex` and `LinkEntry`
- `@Dao` interface with `suspend` and `Flow` return types
- `@Database` singleton via Hilt `@Singleton` provider
- Migration strategy via `fallbackToDestructiveMigration()` for v1

---

## Decision: ViewPager2 + FragmentStateAdapter for Horizontal Navigation

**Rationale**: ViewPager2 is the modern replacement for ViewPager, built on RecyclerView. `FragmentStateAdapter` handles fragment lifecycle automatically.

**Key patterns**:
- `FragmentStateAdapter` with dynamic page count
- `offscreenPageLimit = 1` for smooth adjacent-page preloading
- Each page fragment contains a vertical `RecyclerView`
- Shared `RecyclerView.RecycledViewPool` across page fragments for memory efficiency
- `DiffUtil.ItemCallback` for efficient list updates in `ListAdapter`

**Alternatives considered**:
- **Custom horizontal ScrollView**: No lifecycle management, no recycling
- **TabLayout + FragmentManager**: More manual management; ViewPager2 is simpler

---

## Decision: Hilt for Dependency Injection

**Rationale**: Hilt is the Android-recommended DI framework. Provides compile-time validation, lifecycle-aware scopes, and seamless WorkManager integration.

**Key patterns**:
- `@HiltAndroidApp` on Application class
- `@AndroidEntryPoint` on Activities and Fragments
- `@HiltViewModel` on ViewModels
- `@Module` + `@InstallIn(SingletonComponent::class)` for app-wide providers
- `@HiltWorker` + `@AssistedInject` for WorkManager workers

---

## Decision: Sealed Class UI States + StateFlow

**Rationale**: Sealed classes provide exhaustive compile-time checking of all possible UI states. Combined with `StateFlow` and `repeatOnLifecycle`, they create a safe, leak-free reactive UI pattern.

**Key pattern**:
```kotlin
sealed class CrawlState {
    object Idle : CrawlState()
    object Loading : CrawlState()
    data class Success(val content: CrawlResult) : CrawlState()
    data class Error(val message: String) : CrawlState()
}
```

---

## Decision: Traditional Chinese for All User-Facing Content

**Rationale**: Per constitution Principle V, all strings.xml, error messages, and user-facing text must be in Traditional Chinese.

**Key patterns**:
- `strings.xml` with Traditional Chinese values
- Error messages: actionable, friendly tone (e.g., 「無法連線，請檢查網路設定後重試」)
- Technical terms retained in English (OkHttp, Coil, Hilt)
