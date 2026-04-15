# Quickstart: WebImageViewer (Web Crawler Android App)

## Prerequisites

- **Android Studio**: Iguana (2023.2.1) or newer, or command-line tools
- **JDK**: 17 or newer (Android Gradle Plugin 8.x requires JDK 17)
- **Android SDK**: API 35 (Android 15) platform tools
- **Emulator or Device**: API 26+ (Android 8.0+)

## Setup

### 1. Clone and Open

```bash
git clone <repository-url>
cd project
```

Open in Android Studio: **File → Open →** select the `project` directory.

### 2. Sync Gradle

Android Studio will prompt to sync Gradle. Click **Sync Now**.

Or via command line:

```bash
./gradlew --version
```

### 3. Build Debug APK

```bash
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Install on Device/Emulator

```bash
./gradlew installDebug
```

Or drag the APK onto a running emulator.

### 5. Run Tests

```bash
# All unit tests
./gradlew test

# Single test class
./gradlew test --tests "com.eddy.webcrawler.data.crawler.WebCrawlerTest"

# Single test method
./gradlew test --tests "com.eddy.webcrawler.data.crawler.WebCrawlerTest.givenInvalidUrl_whenCrawl_thenReturnsError"

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### 6. Lint Check

```bash
./gradlew lint
```

## Project Structure

```
app/src/main/java/com/example/webcrawler/
├── data/
│   ├── crawler/WebCrawler.kt          # OkHttp + Jsoup crawl logic
│   ├── db/                            # Room database + DAOs
│   ├── download/DownloadWorker.kt     # WorkManager background download
│   ├── model/                         # Data classes, entities, enums
│   └── repository/CrawlerRepository.kt # Result<T>-based API
├── di/AppModule.kt                    # Hilt dependency injection
└── ui/
    ├── adapter/                       # RecyclerView + ViewPager2 adapters
    ├── fragment/                      # Main, Browse, ZipViewer fragments
    └── viewmodel/                     # ViewModels with StateFlow states
```

## Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| OkHttp | 4.12.0 | HTTP client with User-Agent interceptor |
| Jsoup | 1.18.1 | HTML parsing with CSS selectors |
| Coil | 2.7.0 | Image loading with caching |
| Media3 ExoPlayer | 1.4.1 | Video playback |
| Room | 2.6.1 | Local SQLite database |
| Hilt | 2.51.1 | Dependency injection |
| WorkManager | 2.9.1 | Background download tasks |
| Navigation | 2.8.3 | Fragment navigation graph |
| ViewPager2 | 1.1.0 | Horizontal page navigation |
| DataStore | 1.1.1 | User preferences storage |

## Architecture Overview

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│  Fragment   │────▶│  ViewModel   │────▶│  Repository   │
│  (UI)       │◀────│  (StateFlow) │◀────│  (Result<T>)  │
└─────────────┘     └──────────────┘     └───────┬───────┘
                                                  │
                                    ┌─────────────┼─────────────┐
                                    ▼             ▼             ▼
                              ┌──────────┐ ┌──────────┐ ┌──────────┐
                              │WebCrawler│ │   Room   │ │WorkManager│
                              │(OkHttp+  │ │   DB     │ │(Download) │
                              │ Jsoup)   │ │          │ │           │
                              └──────────┘ └──────────┘ └──────────┘
```

## User Flow

1. **Crawl**: Enter URL + filter pattern → tap "Crawl" → app fetches HTML, extracts links/images, saves to Room DB
2. **Browse**: Horizontal swipe between link indices → vertical scroll within each page → images auto-scale to screen width
3. **Download ZIP**: Tap "Download ZIP" button → WorkManager downloads in background → auto-extracts with zip-slip protection
4. **View ZIP**: Tap "View ZIP" → browse extracted images (Coil) and videos (ExoPlayer)

## Configuration

### User-Agent

Defined in `AppModule.kt`. Customize for your target sites:

```kotlin
private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ..."
```

### Filter Pattern

Default pattern stored in DataStore preferences. Users can customize via settings UI. Example patterns:

- Same domain: `^https?://([a-zA-Z0-9-]+\.)*example\.com/.*`
- Image files only: `.*\.(jpg|jpeg|png|gif|webp)(\?.*)?$`

### Timeouts

Configured in `AppModule.kt`:
- Connect: 10 seconds
- Read: 30 seconds

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Build fails with JDK version error | Ensure JDK 17+ is set in **File → Project Structure → SDK Location** |
| Crawl returns empty results | Verify the filter pattern matches URLs on the target page; check that the page is static HTML |
| Images not loading | Check network permissions in `AndroidManifest.xml`; verify URLs are absolute |
| ZIP download fails | Check storage permissions; ensure sufficient disk space |
| App crashes on startup | Run `./gradlew clean assembleDebug` to clear stale build artifacts |
