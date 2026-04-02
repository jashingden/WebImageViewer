# Feature Specification: Web Crawler Android App

**Feature Branch**: `001-android-app-pattern`  
**Created**: Thu Apr 02 2026  
**Status**: Draft  
**Input**: User description: "我要開發一個 android app, 可以去抓取 (爬蟲) 指定網頁的內容, 並且提供以下功能: - 分析指定網頁裡的連結, 過濾出指定的 pattern, 建立'網頁連結索引' - 根據單一個'網頁連結索引', 解析出'圖片元素'、'下載連結', 須包含名稱及 url - app UI規劃為: 左右滑動切換'網頁連結索引'頁面, 每一頁為上下滑動 - 每一頁顯示解析出的圖片, 圖片須自動調整成最適螢幕寬度, 維持原本的長寛比例 - 若有下載連結且副檔名為'.zip', 則顯示'下載zip'按鈕, 按下後可以下載zip檔案並且自動解壓縮檔案, 然後新增'檢視zip'按鈕, 按下後可以看圖片及影片"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Crawl Web Page and Build Link Index (Priority: P1)

As a user, I want to enter a web page URL and have the app analyze all links on that page, filtering them by configurable patterns, so that I can build a "web link index" for further exploration.

**Why this priority**: This is the foundational capability — without crawling and indexing, no other features can function. It delivers immediate value by surfacing structured link data from any web page.

**Independent Test**: Can be fully tested by entering a URL, triggering the crawl, and verifying that a link index is generated with filtered links matching the configured patterns. Delivers a browsable index of web links.

**Acceptance Scenarios**:

1. **Given** the user has entered a valid URL, **When** the user triggers the crawl action, **Then** the app fetches the page, extracts all links, filters them by the configured pattern, and displays the resulting link index.
2. **Given** the user has entered an invalid or unreachable URL, **When** the user triggers the crawl action, **Then** the app displays a clear error message and allows the user to retry.
3. **Given** a page has no links matching the configured pattern, **When** the crawl completes, **Then** the app displays an empty state message indicating no matching links were found.

---

### User Story 2 - Browse Images in a Link Index Page (Priority: P2)

As a user, I want to select a link index and browse all extracted images in a vertically scrollable view, with images auto-scaled to fit the screen width while maintaining their original aspect ratio, so that I can quickly preview visual content.

**Why this priority**: Image browsing is a core user-facing feature that provides the primary value proposition — visual content discovery from crawled pages.

**Independent Test**: Can be fully tested by crawling a page with images, selecting the resulting link index, and verifying that images load, scale to screen width, and maintain aspect ratio in a vertically scrollable list.

**Acceptance Scenarios**:

1. **Given** a link index contains image elements, **When** the user opens that index page, **Then** all images are displayed in a vertically scrollable list, each scaled to fit screen width while preserving aspect ratio.
2. **Given** an image fails to load (broken URL, network error), **When** the user scrolls to that image, **Then** a placeholder or error indicator is shown instead of a blank space.
3. **Given** a link index contains many images, **When** the user scrolls through the list, **Then** images load progressively as they come into view without blocking the scroll experience.

---

### User Story 3 - Download and Extract ZIP Archives (Priority: P3)

As a user, I want to see a "Download ZIP" button next to any link with a `.zip` extension, download the file, and have it automatically extracted, so that I can access the contents without manual steps.

**Why this priority**: ZIP handling adds significant utility for users who want to bulk-download and inspect archived content. It is dependent on link indexing but independent of image browsing.

**Independent Test**: Can be fully tested by crawling a page with `.zip` links, tapping the download button, and verifying the file downloads, extracts automatically, and a "View ZIP" button appears.

**Acceptance Scenarios**:

1. **Given** a link index contains a `.zip` download link, **When** the user views that index page, **Then** a "Download ZIP" button is displayed next to the link.
2. **Given** the user taps the "Download ZIP" button, **When** the download and extraction complete successfully, **Then** the button changes to "View ZIP" and the extracted contents are stored locally.
3. **Given** the download fails (network error, invalid file), **When** the user attempts to download, **Then** an error message is shown and the "Download ZIP" button remains available for retry.

---

### User Story 4 - View Extracted ZIP Contents (Images and Videos) (Priority: P4)

As a user, I want to tap a "View ZIP" button after extraction and browse the images and videos contained within the archive, so that I can preview media content without leaving the app.

**Why this priority**: This completes the ZIP workflow by allowing users to consume the extracted media. It depends on successful download and extraction.

**Independent Test**: Can be fully tested by downloading and extracting a ZIP containing images and videos, then tapping "View ZIP" and verifying media is displayed correctly.

**Acceptance Scenarios**:

1. **Given** a ZIP archive has been extracted and contains images, **When** the user taps "View ZIP", **Then** images are displayed in a browsable view.
2. **Given** a ZIP archive has been extracted and contains videos, **When** the user taps "View ZIP", **Then** videos are playable within the app.
3. **Given** a ZIP archive contains no supported media files, **When** the user taps "View ZIP", **Then** an empty state message is shown indicating no media was found.

---

### Edge Cases

- What happens when the crawled page requires JavaScript rendering (dynamic content)? The app handles static HTML only; dynamically loaded content may not be captured.
- How does the system handle extremely large pages with thousands of links? The app should impose a reasonable limit on the number of links processed and display a warning if the limit is reached.
- What happens when the device runs out of storage during ZIP download or extraction? The app should display a storage error and clean up any partially downloaded or extracted files.
- How does the system handle ZIP archives with nested directories or non-media files? Only image and video files are displayed in the ZIP viewer; other file types are ignored.
- What happens when the user navigates away during an active crawl or download? Background operations continue; the user can return to see progress or completion.
- How does the system handle malicious or corrupted ZIP files (e.g., zip-slip attacks)? The app validates extraction paths to prevent directory traversal and rejects corrupted archives with an error message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept a user-provided URL and fetch the HTML content of the specified web page.
- **FR-002**: System MUST parse the fetched HTML to extract all hyperlink references (`<a href>` and `<img src>` elements).
- **FR-003**: System MUST filter extracted links against a configurable URL pattern, retaining only matching links in the link index.
- **FR-004**: System MUST create a "web link index" record containing the source URL, timestamp, and all filtered links with their names and URLs.
- **FR-005**: System MUST parse each link index to extract image elements (name and URL) and download links (name and URL).
- **FR-006**: System MUST display each link index as a separate page in a horizontally swipeable view (ViewPager-style navigation).
- **FR-007**: System MUST display extracted images within each link index page in a vertically scrollable list.
- **FR-008**: System MUST automatically scale images to fit the screen width while maintaining the original aspect ratio.
- **FR-009**: System MUST display a "Download ZIP" button next to any download link with a `.zip` file extension.
- **FR-010**: System MUST download the ZIP file to local storage when the user taps the "Download ZIP" button.
- **FR-011**: System MUST automatically extract the downloaded ZIP file to a local directory upon successful download.
- **FR-012**: System MUST replace the "Download ZIP" button with a "View ZIP" button after successful extraction.
- **FR-013**: System MUST display images and playable videos from extracted ZIP contents when the user taps "View ZIP".
- **FR-014**: System MUST persist link index records locally so they are available across app sessions.
- **FR-015**: System MUST provide user-friendly error messages for network failures, invalid URLs, download errors, and extraction failures.
- **FR-016**: System MUST validate ZIP extraction paths to prevent directory traversal (zip-slip) attacks.

### Key Entities

- **Link Index**: Represents a crawled web page's filtered link collection. Contains: source URL, crawl timestamp, filter pattern used, and a list of extracted links.
- **Link Entry**: A single link within a link index. Contains: display name, URL, and type (image, download, or general link).
- **Image Element**: An extracted image reference. Contains: name (derived from filename or alt text), URL, and parent link index reference.
- **Download Entry**: A downloadable resource reference. Contains: name, URL, file extension, download status, and local file path (after download).
- **ZIP Archive**: A downloaded and extracted ZIP file. Contains: original URL, local extraction path, extraction status, and a list of contained media files.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can crawl a web page and generate a link index within 10 seconds for pages with up to 500 links.
- **SC-002**: 95% of images load and display correctly within 3 seconds of the link index page becoming visible.
- **SC-003**: Users can download and extract a ZIP file (up to 100MB) with a single tap and view contents within 30 seconds on a standard 4G connection.
- **SC-004**: 90% of first-time users successfully complete a full crawl → browse images → download ZIP → view extracted media flow without errors.
- **SC-005**: The app handles pages with up to 1,000 links without crashing or becoming unresponsive.
- **SC-006**: ZIP extraction correctly prevents directory traversal attacks, rejecting 100% of malicious archive attempts.

## Assumptions

- Users have stable internet connectivity when performing web crawls and downloads.
- The app targets Android devices running API 26 (Android 8.0) or higher.
- Crawling is limited to static HTML content; pages requiring JavaScript rendering (single-page apps) may not be fully captured.
- The URL pattern filter uses a simple substring or regex-based matching approach configurable by the user.
- Extracted ZIP contents are stored in the app's private storage directory and are not accessible by other apps.
- Video playback supports common formats (MP4, WebM) available on Android devices.
- No user authentication or account system is required for v1 — the app is a standalone tool.
- Link indices are stored locally on the device; no cloud sync or sharing between devices is required for v1.
- The app has permission to access device storage for downloading and extracting ZIP files.
