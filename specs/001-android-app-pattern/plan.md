# Implementation Plan: Web Crawler Android App

**Branch**: `001-android-app-pattern` | **Date**: 2026-04-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-android-app-pattern/spec.md`

## Summary

Build an Android app that crawls web pages using OkHttp + Jsoup, extracts and filters links by configurable regex patterns, persists link indices in Room DB, and presents them in a ViewPager2-based horizontal swipe interface. Each page displays images (Coil, auto-scaled to screen width) in a vertical RecyclerView. ZIP download links trigger WorkManager background downloads with zip-slip protection; after extraction, a "View ZIP" button opens a Media3 ExoPlayer-based media viewer for images and videos. All dependencies are injected via Hilt.

## Technical Context

**Language/Version**: Kotlin 1.9.23 (aligned with Android Gradle Plugin 8.x)
**Primary Dependencies**: OkHttp 4.12.0, Jsoup 1.18.1, Coil 2.7.0, Media3 ExoPlayer 1.4.1, Room 2.6.1, Hilt 2.51.1, WorkManager 2.9.1, Navigation Component 2.8.3, ViewPager2 1.1.0
**Storage**: Room DB (link index persistence), app-private file storage (ZIP downloads/extracted content), DataStore 1.1.1 (user preferences like filter pattern)
**Testing**: JUnit 4 + Mockito/Kotest for unit tests (`src/test/java/`), AndroidX Test + Espresso for instrumented tests (`src/androidTest/java/`)
**Target Platform**: Android API 26 (minSdk) – API 35 (targetSdk)
**Project Type**: Mobile app (Android, single-module)
**Performance Goals**: Crawl pages with ≤500 links within 10s; 95% images load within 3s; ZIP download + extract ≤100MB within 30s on 4G; RecyclerView scroll ≥50 FPS
**Constraints**: No main-thread I/O; OkHttp timeouts (connect 10s, read 30s); zip-slip prevention mandatory; no `!!` operator; all UI states via sealed classes + StateFlow
**Scale/Scope**: Single-user local app; up to 1,000 links per crawl; no cloud sync or authentication in v1

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. 程式品質至上** | PASS | MVVM + Repository enforced; Hilt DI throughout; nullable binding pattern; `use {}` for I/O; zip-slip guard; `Result<T>` error handling; naming conventions defined in AGENTS.md |
| **II. 測試標準強制** | PASS | Unit tests for ViewModel, Repository, WebCrawler, DownloadWorker; instrumented tests for Fragments, Room DAO; test naming `given{情境}_when{操作}_then{預期結果}`; 80% coverage target for core logic |
| **III. 使用者體驗一致性** | PASS | Material Components throughout; theme-based colors; sealed UI states (Idle/Loading/Success/Error) via StateFlow + repeatOnLifecycle; Coil with placeholder/error; Navigation Component (nav_graph.xml) |
| **IV. 效能需求規範** | PASS | Coil caching; WorkManager for background downloads; OkHttp timeouts set; Room queries via Flow; RecyclerView recycling; no blocking on main thread |
| **V. 正體中文優先** | PASS | All strings.xml in Traditional Chinese; code comments in Traditional Chinese; error messages user-facing in Traditional Chinese; identifiers follow Kotlin naming (English) |

**Gate Result**: ALL PASS — no violations. Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-android-app-pattern/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (N/A for internal-only app)
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/
├── src/main/
│   ├── java/com/example/webcrawler/
│   │   ├── WebCrawlerApp.kt                 # Application class, Hilt entry
│   │   ├── data/
│   │   │   ├── crawler/
│   │   │   │   └── WebCrawler.kt            # OkHttp + Jsoup crawl logic
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt           # Room database
│   │   │   │   ├── LinkIndexDao.kt          # DAO for link indices
│   │   │   │   └── LinkEntryDao.kt          # DAO for link entries
│   │   │   ├── download/
│   │   │   │   └── DownloadWorker.kt        # WorkManager CoroutineWorker
│   │   │   ├── model/
│   │   │   │   ├── LinkIndex.kt             # Room entity
│   │   │   │   ├── LinkEntry.kt             # Room entity
│   │   │   │   ├── ContentItem.kt           # Sealed class for RecyclerView
│   │   │   │   └── ZipMediaItem.kt          # Data class for ZIP media
│   │   │   └── repository/
│   │   │       └── CrawlerRepository.kt     # Result<T>-based API
│   │   ├── di/
│   │   │   └── AppModule.kt                 # Hilt modules (OkHttp, Jsoup, DB)
│   │   └── ui/
│   │       ├── adapter/
│   │       │   ├── LinkIndexPagerAdapter.kt # ViewPager2 adapter
│   │       │   ├── ContentAdapter.kt        # RecyclerView adapter (images/links)
│   │       │   └── ZipMediaAdapter.kt       # RecyclerView adapter (ZIP contents)
│   │       ├── fragment/
│   │       │   ├── MainFragment.kt          # URL input + crawl trigger
│   │       │   ├── BrowseFragment.kt        # ViewPager2 container for link indices
│   │       │   └── ZipViewerFragment.kt     # Media viewer for extracted ZIP
│   │       └── viewmodel/
│   │           ├── MainViewModel.kt         # Crawl state management
│   │           ├── BrowseViewModel.kt       # Link index browsing state
│   │           └── ZipViewerViewModel.kt    # ZIP media state
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── fragment_main.xml
│   │   │   ├── fragment_browse.xml
│   │   │   ├── fragment_zip_viewer.xml
│   │   │   ├── item_image.xml
│   │   │   ├── item_link.xml
│   │   │   └── item_zip_media.xml
│   │   ├── navigation/
│   │   │   └── nav_graph.xml
│   │   └── values/
│   │       ├── strings.xml                  # Traditional Chinese
│   │       ├── colors.xml
│   │       └── themes.xml
│   └── AndroidManifest.xml
├── src/test/java/com/example/webcrawler/
│   ├── data/crawler/WebCrawlerTest.kt
│   ├── data/repository/CrawlerRepositoryTest.kt
│   └── ui/viewmodel/MainViewModelTest.kt
└── src/androidTest/java/com/example/webcrawler/
    ├── data/db/LinkIndexDaoTest.kt
    └── ui/fragment/MainFragmentTest.kt
```

**Structure Decision**: Single-module Android app following the package structure defined in AGENTS.md. No separate backend or frontend modules — all logic runs on-device. The `contracts/` directory is omitted since this is a self-contained app with no external API contracts.

## Constitution Check (Post-Design Re-Evaluation)

*Re-checked after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. 程式品質至上** | PASS | Design enforces MVVM + Repository; Hilt DI for all layers including `@HiltWorker`; nullable binding pattern in all fragments; `use {}` for all I/O streams; zip-slip guard in `DownloadWorker`; `Result<T>` in repository layer; naming conventions match AGENTS.md |
| **II. 測試標準強制** | PASS | Unit tests defined for `WebCrawler`, `CrawlerRepository`, `MainViewModel`; instrumented tests for `LinkIndexDao`, `MainFragment`; test naming follows `given{情境}_when{操作}_then{預期結果}` pattern; 80% coverage target for core logic (crawler, repository, worker) |
| **III. 使用者體驗一致性** | PASS | Material Components in all layouts; theme-based color attributes; sealed UI states (`Idle`/`Loading`/`Success`/`Error`) via `StateFlow` + `repeatOnLifecycle`; Coil with placeholder/error drawables; Navigation Component via `nav_graph.xml`; ViewPager2 for horizontal navigation |
| **IV. 效能需求規範** | PASS | Coil memory/disk caching; WorkManager for background downloads with network constraints; OkHttp timeouts (connect 10s, read 30s); Room queries via `Flow`; RecyclerView with `DiffUtil` and shared `RecycledViewPool`; no main-thread I/O |
| **V. 正體中文優先** | PASS | All `strings.xml` values in Traditional Chinese; code comments in Traditional Chinese; error messages user-facing in Traditional Chinese with actionable guidance; identifiers follow Kotlin naming conventions (English) |

**Gate Result**: ALL PASS — design is fully compliant with constitution. No violations to justify.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | No constitution violations | N/A |
