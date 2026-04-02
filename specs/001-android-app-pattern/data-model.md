# Data Model: Web Crawler Android App

## Entities

### LinkIndex

Represents a crawled web page's filtered link collection. Persisted in Room DB.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | Unique identifier |
| `sourceUrl` | `String` | `NOT NULL`, indexed | The URL that was crawled |
| `filterPattern` | `String` | `NOT NULL` | The regex pattern used for filtering |
| `title` | `String` | `NOT NULL` | Page title extracted from `<title>` tag |
| `crawlTimestamp` | `Long` | `NOT NULL` | Epoch millis when crawl completed |
| `status` | `String` | `NOT NULL` | One of: `CRAWLING`, `SUCCESS`, `ERROR`, `EMPTY` |
| `errorMessage` | `String?` | `NULLABLE` | Error message if status is `ERROR` |

**Relationships**: One-to-many with `LinkEntry` (via `linkIndexId` foreign key)

**Validation rules**:
- `sourceUrl` must be a valid URL format
- `filterPattern` must be a valid regex (validated before crawl)
- `crawlTimestamp` must be positive

**State transitions**:
```
Idle → CRAWLING → SUCCESS (with LinkEntries)
                → ERROR (with errorMessage)
                → EMPTY (no matching links)
```

---

### LinkEntry

A single link within a link index. Persisted in Room DB as child of `LinkIndex`.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | `@PrimaryKey(autoGenerate = true)` | Unique identifier |
| `linkIndexId` | `Long` | `NOT NULL`, `@ForeignKey` → `LinkIndex.id` (CASCADE DELETE) | Parent link index |
| `displayName` | `String` | `NOT NULL` | Link text or derived filename |
| `url` | `String` | `NOT NULL` | Absolute URL |
| `type` | `String` | `NOT NULL` | One of: `IMAGE`, `DOWNLOAD`, `LINK` |
| `fileExtension` | `String?` | `NULLABLE` | File extension (e.g., `.zip`, `.jpg`) for downloads |
| `downloadStatus` | `String?` | `NULLABLE` | One of: `NOT_DOWNLOADED`, `DOWNLOADING`, `DOWNLOADED`, `EXTRACTED`, `FAILED` |
| `localPath` | `String?` | `NULLABLE` | Local file path after download/extraction |

**Relationships**: Many-to-one with `LinkIndex`

**Validation rules**:
- `url` must be a valid absolute URL
- `type` must be one of the defined enum values
- `fileExtension` required when `type == DOWNLOAD`
- `downloadStatus` required when `type == DOWNLOAD`
- `localPath` populated only after successful download

---

### ContentItem (UI Model)

Sealed class for RecyclerView items within a link index page. Not persisted — derived from `LinkEntry`.

```kotlin
sealed class ContentItem {
    abstract val stableId: String

    data class ImageItem(
        override val stableId: String,  // URL or unique hash
        val url: String,
        val displayName: String
    ) : ContentItem()

    data class LinkItem(
        override val stableId: String,
        val url: String,
        val displayName: String
    ) : ContentItem()

    data class DownloadItem(
        override val stableId: String,
        val url: String,
        val displayName: String,
        val fileExtension: String,
        val downloadStatus: DownloadStatus,
        val localPath: String?
    ) : ContentItem()
}

enum class DownloadStatus {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, EXTRACTED, FAILED
}
```

---

### ZipMediaItem (UI Model)

Represents a media file within an extracted ZIP archive. Not persisted — discovered at runtime.

| Field | Type | Description |
|-------|------|-------------|
| `name` | `String` | Filename within the ZIP |
| `localPath` | `String` | Absolute path to extracted file |
| `mediaType` | `MediaType` | `IMAGE` or `VIDEO` |

```kotlin
enum class MediaType { IMAGE, VIDEO }
```

**Supported extensions**:
- Images: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`, `.svg`
- Videos: `.mp4`, `.webm`, `.mkv`, `.avi`

---

## Room Database Schema

```kotlin
@Database(
    entities = [LinkIndex::class, LinkEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkIndexDao(): LinkIndexDao
    abstract fun linkEntryDao(): LinkEntryDao
}
```

### LinkIndexDao

```kotlin
@Dao
interface LinkIndexDao {
    @Query("SELECT * FROM linkindex ORDER BY crawlTimestamp DESC")
    fun getAllIndices(): Flow<List<LinkIndex>>

    @Query("SELECT * FROM linkindex WHERE id = :id")
    suspend fun getIndexById(id: Long): LinkIndex?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndex(index: LinkIndex): Long

    @Update
    suspend fun updateIndex(index: LinkIndex)

    @Delete
    suspend fun deleteIndex(index: LinkIndex)
}
```

### LinkEntryDao

```kotlin
@Dao
interface LinkEntryDao {
    @Query("SELECT * FROM linkentry WHERE linkIndexId = :indexId ORDER BY id ASC")
    fun getEntriesByIndexId(indexId: Long): Flow<List<LinkEntry>>

    @Query("SELECT * FROM linkentry WHERE linkIndexId = :indexId AND type = 'IMAGE'")
    fun getImagesByIndexId(indexId: Long): Flow<List<LinkEntry>>

    @Query("SELECT * FROM linkentry WHERE linkIndexId = :indexId AND type = 'DOWNLOAD'")
    fun getDownloadsByIndexId(indexId: Long): Flow<List<LinkEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LinkEntry>)

    @Update
    suspend fun updateEntry(entry: LinkEntry)

    @Query("DELETE FROM linkentry WHERE linkIndexId = :indexId")
    suspend fun deleteEntriesByIndexId(indexId: Long)
}
```

---

## Type Converters

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
```

---

## Data Flow

```
User enters URL
    → MainViewModel.crawl(url, pattern)
        → CrawlerRepository.crawl(url, pattern)
            → WebCrawler.fetchAndParse(url, pattern)
                → OkHttp GET → Jsoup parse → extract links/images
        → Create LinkIndex entity + LinkEntry list
        → Room insert (cascade)
        → Return Result<CrawlResult>
    → UI: StateFlow emits Success → ViewPager2 updates

User taps "Download ZIP"
    → BrowseViewModel.startDownload(entryId)
        → Build WorkManager OneTimeWorkRequest
        → Enqueue DownloadWorker
    → DownloadWorker.doWork()
        → OkHttp download ZIP to filesDir/downloads/
        → Extract with zip-slip prevention to filesDir/extracted/{indexId}/
        → Update LinkEntry: downloadStatus = EXTRACTED, localPath = ...
        → Room update
    → UI: WorkInfo Flow updates → button changes to "View ZIP"

User taps "View ZIP"
    → Navigate to ZipViewerFragment
    → ZipViewerViewModel.scanMedia(localPath)
        → Walk extracted directory, find image/video files
        → Return List<ZipMediaItem>
    → UI: Display media in RecyclerView with Coil (images) / ExoPlayer (videos)
```
