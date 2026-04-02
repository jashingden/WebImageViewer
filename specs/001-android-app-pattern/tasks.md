# Tasks: Web Crawler Android App

**Input**: Design documents from `/specs/001-android-app-pattern/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: Included per constitution Principle II (測試標準強制) — unit tests for core logic, instrumented tests for DAOs and Fragments.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Source**: `app/src/main/java/com/example/webcrawler/`
- **Unit Tests**: `app/src/test/java/com/example/webcrawler/`
- **Instrumented Tests**: `app/src/androidTest/java/com/example/webcrawler/`
- **Resources**: `app/src/main/res/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, build configuration, and resource scaffolding

- [X] T001 Create Android project structure per plan.md (app/src/main, app/src/test, app/src/androidTest)
- [X] T002 Configure build.gradle.kts with all dependencies (OkHttp 4.12.0, Jsoup 1.18.1, Coil 2.7.0, Media3 ExoPlayer 1.4.1, Room 2.6.1, Hilt 2.51.1, WorkManager 2.9.1, Navigation 2.8.3, ViewPager2 1.1.0, DataStore 1.1.1, Kotlin Coroutines 1.8.1)
- [X] T003 [P] Configure AndroidManifest.xml with INTERNET permission, disable default WorkManager initializer, register WebCrawlerApp application class
- [X] T004 [P] Create resource files: res/values/strings.xml (Traditional Chinese), res/values/colors.xml, res/values/themes.xml with Material Components theme
- [X] T005 [P] Create placeholder drawable resources (ic_placeholder, ic_error) for Coil loading states
- [X] T006 Create WebCrawlerApp.kt as Application class with @HiltAndroidApp and Configuration.Provider for HiltWorkerFactory

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 [P] Create Room entities: LinkIndex.kt and LinkEntry.kt with @Entity annotations, field definitions, and foreign key relationship per data-model.md
- [X] T008 [P] Create data models: ContentItem.kt (sealed class with ImageItem, LinkItem, DownloadItem) and ZipMediaItem.kt with MediaType enum
- [X] T009 Create Room DAOs: LinkIndexDao.kt and LinkEntryDao.kt with all Flow-based and suspend queries per data-model.md
- [X] T010 Create AppDatabase.kt with @Database annotation, TypeConverters, and DAO abstract methods
- [X] T011 Create Hilt AppModule.kt with @Singleton providers for OkHttpClient (with User-Agent interceptor, timeouts), Jsoup, AppDatabase, and DataStore
- [X] T012 Create CrawlerRepository.kt with Result<T>-based API wrapping WebCrawler and Room operations
- [X] T013 Create navigation graph: res/navigation/nav_graph.xml with destinations for MainFragment, BrowseFragment, ZipViewerFragment
- [X] T014 Create MainActivity.kt with NavHostFragment and @AndroidEntryPoint

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 - Crawl Web Page and Build Link Index (Priority: P1) 🎯 MVP

**Goal**: User enters a URL, app crawls the page, extracts and filters links, persists a link index in Room DB

**Independent Test**: Enter a URL, trigger crawl, verify link index is generated with filtered links matching configured patterns. Delivers a browsable index of web links.

### Tests for User Story 1

- [X] T015 [P] [US1] Unit test for WebCrawler in app/src/test/java/com/example/webcrawler/data/crawler/WebCrawlerTest.kt (given{情境}_when{操作}_then{預期結果} naming, test valid URL, invalid URL, empty page, pattern filtering)
- [X] T016 [P] [US1] Unit test for CrawlerRepository in app/src/test/java/com/example/webcrawler/data/repository/CrawlerRepositoryTest.kt (test Result<T> success/error paths, mock WebCrawler and DAO)
- [X] T017 [P] [US1] Unit test for MainViewModel in app/src/test/java/com/example/webcrawler/ui/viewmodel/MainViewModelTest.kt (test CrawlState transitions: Idle → Loading → Success/Error)
- [ ] T018 [P] [US1] Instrumented test for LinkIndexDao in app/src/androidTest/java/com/example/webcrawler/data/db/LinkIndexDaoTest.kt (test insert, query, getAllIndices Flow emission)
- [ ] T019 [US1] Instrumented test for MainFragment in app/src/androidTest/java/com/example/webcrawler/ui/fragment/MainFragmentTest.kt (test URL input, crawl trigger, loading state, success/error display)
- [X] T020 [P] [US1] Create WebCrawler.kt in app/src/main/java/com/example/webcrawler/data/crawler/ with OkHttp GET, Jsoup HTML parsing, link extraction (attr("abs:href"), attr("abs:src")), regex pattern filtering, all on Dispatchers.IO
- [X] T021 [P] [US1] Create layout: res/layout/fragment_main.xml with URL TextInputEditText, filter pattern TextInputEditText, Crawl MaterialButton, loading ProgressBar, error TextView with retry button
- [X] T022 [P] [US1] Create MainViewModel.kt in app/src/main/java/com/example/webcrawler/ui/viewmodel/ with @HiltViewModel, StateFlow<CrawlState> (Idle/Loading/Success/Error), crawl(url, pattern) function calling CrawlerRepository
- [X] T023 [P] [US1] Create MainFragment.kt in app/src/main/java/com/example/webcrawler/ui/fragment/ with @AndroidEntryPoint, nullable binding pattern, lifecycleScope + repeatOnLifecycle collecting MainViewModel state, URL validation, crawl trigger
- [X] T024 [US1] Implement CrawlerRepository.crawl(url, pattern) in app/src/main/java/com/example/webcrawler/data/repository/CrawlerRepository.kt — call WebCrawler, create LinkIndex + LinkEntry list, insert into Room, return Result<CrawlResult>
- [ ] T025 [US1] Add DataStore preference management for filter pattern (read/write default pattern) in app/src/main/java/com/example/webcrawler/data/

**Checkpoint**: User Story 1 is fully functional — user can crawl a page and generate a persisted link index

---

## Phase 4: User Story 2 - Browse Images in a Link Index Page (Priority: P2)

**Goal**: User selects a link index and browses extracted images in a vertically scrollable view with auto-scaled images

**Independent Test**: Crawl a page with images, select the resulting link index, verify images load, scale to screen width, and maintain aspect ratio in a vertically scrollable list.

### Tests for User Story 2

- [X] T026 [P] [US2] Unit test for BrowseViewModel in app/src/test/java/com/example/webcrawler/ui/viewmodel/BrowseViewModelTest.kt (test PageState transitions, link index selection, image list emission)
- [ ] T027 [P] [US2] Instrumented test for BrowseFragment in app/src/androidTest/java/com/example/webcrawler/ui/fragment/BrowseFragmentTest.kt (test ViewPager2 page navigation, RecyclerView image display, scroll behavior)
- [X] T028 [P] [US2] Create LinkIndexPagerAdapter.kt in app/src/main/java/com/example/webcrawler/ui/adapter/ extending FragmentStateAdapter, dynamic page count from Room Flow, offscreenPageLimit = 1
- [X] T029 [P] [US2] Create ContentAdapter.kt in app/src/main/java/com/example/webcrawler/ui/adapter/ as ListAdapter<ContentItem> with DiffUtil.ItemCallback, multiple view types (TYPE_IMAGE, TYPE_LINK, TYPE_DOWNLOAD), inner ViewHolder classes with bind() methods
- [X] T030 [P] [US2] Create layouts: res/layout/fragment_browse.xml (ViewPager2), res/layout/item_image.xml (ImageView with match_parent width, adjustViewBounds=true), res/layout/item_link.xml (link display TextView)
- [X] T031 [P] [US2] Create BrowseViewModel.kt in app/src/main/java/com/example/webcrawler/ui/viewmodel/ with @HiltViewModel, StateFlow<PageState>, loadLinkIndices() and selectIndex(indexId) functions, Flow-based Room queries
- [X] T032 [P] [US2] Create BrowseFragment.kt in app/src/main/java/com/example/webcrawler/ui/fragment/ with @AndroidEntryPoint, nullable binding pattern, ViewPager2 setup with LinkIndexPagerAdapter, shared RecyclerView.RecycledViewPool
- [X] T033 [US2] Create IndexPageFragment.kt in app/src/main/java/com/example/webcrawler/ui/fragment/ — individual ViewPager2 page containing vertical RecyclerView with ContentAdapter, Coil image loading with crossfade/placeholder/error
- [X] T034 [US2] Implement LinkEntry to ContentItem mapping in CrawlerRepository — convert crawled LinkEntry list to ContentItem sealed class instances for RecyclerView display
- [X] T035 [US2] Add navigation from MainFragment to BrowseFragment on successful crawl via nav_graph.xml with safe args for link index ID

**Checkpoint**: User Stories 1 AND 2 both work independently — full crawl → browse images flow complete

---

## Phase 5: User Story 3 - Download and Extract ZIP Archives (Priority: P3)

**Goal**: User sees "Download ZIP" button for .zip links, downloads and extracts automatically with zip-slip protection

**Independent Test**: Crawl a page with .zip links, tap download button, verify file downloads, extracts automatically, and "View ZIP" button appears.

### Tests for User Story 3

- [X] T036 [P] [US3] Unit test for DownloadWorker in app/src/test/java/com/example/webcrawler/data/download/DownloadWorkerTest.kt (test download success, zip-slip prevention, extraction failure, cleanup on error)
- [X] T037 [US3] Unit test for zip-slip prevention logic — test malicious ZIP entries with ../ paths are rejected
- [X] T038 [P] [US3] Create DownloadWorker.kt in app/src/main/java/com/example/webcrawler/data/download/ as @HiltWorker CoroutineWorker — download ZIP via OkHttp, extract with ZipInputStream, zip-slip validation (canonicalPath check), update LinkEntry status in Room, progress reporting via setProgress()
- [X] T039 [P] [US3] Update ContentAdapter.kt — add DownloadItem view type with "Download ZIP" / "View ZIP" button states, download progress indicator, retry button on failure
- [X] T040 [P] [US3] Create layout: res/layout/item_zip_download.xml with download link info, status TextView, action button (Download ZIP / View ZIP / Retry)
- [X] T041 [US3] Update BrowseViewModel.kt — add startDownload(entryId, linkIndexId) function to enqueue WorkManager OneTimeWorkRequest, observe WorkInfo Flow for progress/status updates
- [X] T042 [US3] Update IndexPageFragment.kt — wire up Download ZIP button click to BrowseViewModel.startDownload(), observe download status changes, update button state, handle error messages
- [ ] T043 [US3] Add storage permission handling and app-private directory setup (filesDir/downloads, filesDir/extracted) in WebCrawlerApp.kt or DownloadWorker

**Checkpoint**: User Stories 1, 2, AND 3 all work independently — full crawl → browse → download ZIP flow complete

---

## Phase 6: User Story 4 - View Extracted ZIP Contents (Priority: P4)

**Goal**: User taps "View ZIP" and browses extracted images and videos from the ZIP archive

**Independent Test**: Download and extract a ZIP containing images and videos, tap "View ZIP", verify media is displayed correctly.

### Tests for User Story 4

- [X] T044 [P] [US4] Unit test for ZipViewerViewModel in app/src/test/java/com/example/webcrawler/ui/viewmodel/ZipViewerViewModelTest.kt (test media scanning, empty directory, mixed file types)
- [ ] T045 [US4] Instrumented test for ZipViewerFragment in app/src/androidTest/java/com/example/webcrawler/ui/fragment/ZipViewerFragmentTest.kt (test image display with Coil, video playback with ExoPlayer, empty state)
- [X] T046 [P] [US4] Create ZipMediaAdapter.kt in app/src/main/java/com/example/webcrawler/ui/adapter/ as ListAdapter<ZipMediaItem> with DiffUtil, multiple view types (TYPE_IMAGE, TYPE_VIDEO), Coil for images, PlayerView for videos
- [X] T047 [P] [US4] Create layout: res/layout/fragment_zip_viewer.xml (RecyclerView + empty state TextView), res/layout/item_zip_media.xml (shared media item layout with ImageView and PlayerView)
- [X] T048 [P] [US4] Create ZipViewerViewModel.kt in app/src/main/java/com/example/webcrawler/ui/viewmodel/ with @HiltViewModel, StateFlow<ZipViewerState> (Idle/Loading/Success/Error), scanMedia(localPath) function to walk directory and discover image/video files
- [X] T049 [P] [US4] Create ZipViewerFragment.kt in app/src/main/java/com/example/webcrawler/ui/fragment/ with @AndroidEntryPoint, nullable binding pattern, RecyclerView with ZipMediaAdapter, ExoPlayer lifecycle management (init in onViewCreated, release in onDestroyView), safe args for extraction path
- [X] T050 [US4] Update nav_graph.xml — add ZipViewerFragment destination with safe arg for localPath, add navigation action from BrowseFragment/IndexPageFragment
- [X] T051 [US4] Wire "View ZIP" button click to navigate to ZipViewerFragment with extraction path argument
- [X] T052 [US4] Implement media file type detection in ZipViewerViewModel — scan directory recursively, filter by supported extensions (.jpg, .jpeg, .png, .gif, .webp, .bmp, .svg for images; .mp4, .webm, .mkv, .avi for videos), return List<ZipMediaItem>

**Checkpoint**: All user stories are independently functional — complete crawl → browse → download ZIP → view media flow

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T053 [P] Update all strings.xml values to Traditional Chinese per constitution Principle V (error messages, button labels, empty states)
- [ ] T054 [P] Add empty state handling across all fragments (no links found, no images, no media in ZIP) with user-friendly Traditional Chinese messages
- [ ] T055 [P] Implement DataStore-based user preferences for filter pattern persistence and retrieval
- [ ] T056 [P] Add RecyclerView shared RecycledViewPool optimization for ViewPager2 pages in BrowseFragment
- [ ] T057 [P] Implement proper error handling and retry flows across all ViewModels (network errors, storage errors, corrupted ZIP)
- [ ] T058 Run ./gradlew test and fix any failing unit tests
- [ ] T059 Run ./gradlew lint and fix all lint warnings
- [ ] T060 Run ./gradlew connectedAndroidTest and fix any failing instrumented tests
- [ ] T061 Validate quickstart.md flow: build debug APK, install on device, run full user flow
- [ ] T062 Code cleanup: remove unused imports, verify no !! operator usage, confirm all I/O uses use { } blocks

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed) or sequentially in priority order (P1 → P2 → P3 → P4)
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) — Depends on LinkIndex/LinkEntry entities and DAOs from Phase 2; integrates with US1 crawl results
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — Depends on LinkEntry downloadStatus field; integrates with US2's ContentAdapter
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) — Depends on successful ZIP extraction from US3; independent UI layer

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services
- Services before UI components
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] (T003, T004, T005) can run in parallel
- All Foundational tasks marked [P] (T007, T008) can run in parallel
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for WebCrawler in WebCrawlerTest.kt"
Task: "Unit test for CrawlerRepository in CrawlerRepositoryTest.kt"
Task: "Unit test for MainViewModel in MainViewModelTest.kt"
Task: "Instrumented test for LinkIndexDao in LinkIndexDaoTest.kt"

# Launch all parallel implementation tasks together:
Task: "Create WebCrawler.kt in data/crawler/"
Task: "Create fragment_main.xml layout"
Task: "Create MainViewModel.kt in ui/viewmodel/"
Task: "Create MainFragment.kt in ui/fragment/"
```

## Parallel Example: User Story 2

```bash
# Launch all tests for User Story 2 together:
Task: "Unit test for BrowseViewModel in BrowseViewModelTest.kt"
Task: "Instrumented test for BrowseFragment in BrowseFragmentTest.kt"

# Launch all parallel implementation tasks together:
Task: "Create LinkIndexPagerAdapter.kt in ui/adapter/"
Task: "Create ContentAdapter.kt in ui/adapter/"
Task: "Create fragment_browse.xml, item_image.xml, item_link.xml layouts"
Task: "Create BrowseViewModel.kt in ui/viewmodel/"
Task: "Create BrowseFragment.kt in ui/fragment/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test crawl → link index generation independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4 → Test independently → Deploy/Demo
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
   - Developer D: User Story 4
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All strings.xml in Traditional Chinese per constitution Principle V
- No !! operator usage anywhere — use safe calls or elvis operator
- All I/O operations use use { } blocks for resource safety
- Zip-slip prevention is mandatory in DownloadWorker
- OkHttp timeouts: connect 10s, read 30s
