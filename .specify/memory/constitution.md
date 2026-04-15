<!--
SYNC IMPACT REPORT
==================
Version change: N/A (template) → 1.0.0
Modified principles: N/A (initial constitution)
Added sections:
  - Core Principles (5 principles)
  - 開發流程規範 (Development Workflow)
  - 品質把關機制 (Quality Gates)
  - Governance
Removed sections: N/A
Templates requiring updates:
  - .specify/templates/plan-template.md — ✅ Constitution Check section references constitution; no changes needed
  - .specify/templates/spec-template.md — ✅ No constitution-specific constraints; no changes needed
  - .specify/templates/tasks-template.md — ✅ Task categories align with testing/code-quality principles; no changes needed
  - .specify/templates/checklist-template.md — ✅ Template-agnostic; no changes needed
  - .specify/templates/agent-file-template.md — ✅ No constitution references; no changes needed
Follow-up TODOs: None
-->

# WebImageViewer Constitution

## Core Principles

### I. 程式品質至上

所有程式碼必須遵守以下非妥協性規範：

- **架構紀律**：嚴格遵循 MVVM + Repository 模式，各層職責清晰，禁止跨層呼叫。ViewModel 不得直接操作 View，Repository 不得持有 Context。
- **命名規範**：類別使用 PascalCase、函式與屬性使用 camelCase、常數使用 UPPER_SNAKE_CASE、XML ID 使用 snake_case。所有名稱必須具備語意，禁止無意義縮寫。
- **空值安全**：全面使用 Kotlin 可空型別系統，禁止使用 `!!` 強制解包（除非有明確註解說明不可為空的原因）。優先使用 `?.`、`?:`、`let`、`run` 等安全操作符。
- **資源管理**：所有 I/O 操作必須使用 `use { }` 區塊；Fragment 的 ViewBinding 必須使用 nullable binding 並於 `onDestroyView()` 釋放；Zip 解壓縮必須驗證 `canonicalPath.startsWith(destDir.canonicalPath)` 防止 zip-slip 攻擊。
- **錯誤處理**：Repository 層一律回傳 `Result<T>`，ViewModel 透過 `fold()` 轉換為 sealed state，UI 層顯示錯誤訊息與重試按鈕。禁止吞掉異常或印出 stack trace 後繼續執行。
- **依賴注入**：全面使用 Hilt，ViewModel 使用 `@HiltViewModel`、Fragment 使用 `@AndroidEntryPoint`、Worker 使用 `@HiltWorker`。禁止在類別內部手動建立依賴實例。

**理由**：一致的程式品質是維護性與可擴展性的基礎，任何妥協都會在專案成長時產生指數級的技術債。

### II. 測試標準強制

測試是交付的必備條件，非選配項目：

- **單元測試**：所有 ViewModel、Repository、Use Case 必須具備單元測試。測試檔名格式為 `{ClassName}Test.kt`，放置於 `src/test/java/` 目錄。使用 `gradlew test` 執行，必須全數通過。
- **儀器測試**：涉及 Android Framework 的元件（Fragment、Room DAO、DataStore）必須編寫儀器測試，放置於 `src/androidTest/java/` 目錄。使用 `gradlew connectedAndroidTest` 執行。
- **單一測試方法**：使用 `gradlew test --tests "com.eddy.webcrawler.MyTestClass.myTestMethod"` 可執行單一測試，便於開發階段快速驗證。
- **測試覆蓋目標**：核心業務邏輯（爬蟲解析、下載管理、資料庫操作）覆蓋率不得低於 80%。UI 適配器與 View 層以儀器測試驗證關鍵互動即可。
- **測試命名**：測試方法名稱必須清楚描述情境與預期結果，格式為 `given{情境}_when{操作}_then{預期結果}`。例如：`givenInvalidUrl_whenCrawl_thenReturnsError()`。
- **測試獨立性**：每個測試必須可獨立執行，不得依賴其他測試的執行順序或副作用。使用 `@Before` 設定初始狀態、`@After` 清理資源。

**理由**：沒有測試的程式碼等於沒有驗證的假設。強制測試標準確保每次重構與功能新增都不會破壞既有行為。

### III. 使用者體驗一致性

所有使用者介面必須維持統一的外觀與互動模式：

- **Material Design 元件**：全面使用 Material Components（`MaterialToolbar`、`MaterialButton`、`TextInputLayout`），禁止混用原生 Android 元件與 Material 元件。
- **色彩系統**：使用主題屬性（`?attr/colorOnSurface`、`?attr/colorError` 等），禁止硬編碼色值。確保深色模式與淺色模式皆能正確顯示。
- **狀態管理模式**：所有 Fragment 的 UI 狀態必須使用 sealed class（`Idle`、`Loading`、`Success(data)`、`Error(message)`），透過 `StateFlow` 暴露，使用 `repeatOnLifecycle(Lifecycle.State.STARTED)` 收集。
- **載入與錯誤狀態**：所有非同步操作必須顯示載入指示器；錯誤狀態必須顯示錯誤文字與重試按鈕；空狀態必須顯示提示訊息。禁止出現無回應的 UI。
- **圖片載入**：統一使用 Coil 載入圖片，必須設定 placeholder 與 error drawable。長列表必須使用 RecyclerView 或 ViewPager2 並實作 View Recycling，禁止一次性載入所有圖片至記憶體。
- **導航一致性**：使用 Jetpack Navigation Component（nav_graph.xml）管理 Fragment 導航，禁止使用 `FragmentManager` 手動替換 Fragment。

**理由**：使用者體驗的一致性直接影響產品的專業感與可信度。任何介面的不一致都會造成使用者的認知負擔與困惑。

### IV. 效能需求規範

所有功能必須符合以下效能基準：

- **記憶體管理**：圖片載入必須限制快取大小，避免 OOM。RecyclerView 必須正確回收 ViewHolder。大型檔案下載必須使用 WorkManager 在背景執行，禁止在主執行緒進行 I/O 操作。
- **網路效能**：所有 HTTP 請求必須設定 User-Agent 標頭。OkHttp 必須設定合理的連線逾時（連線 10 秒、讀取 30 秒）。圖片解析必須在背景執行緒進行，禁止阻塞主執行緒。
- **啟動效能**：冷啟動時間不得超過 2 秒。禁止在 `Application.onCreate()` 或 `Activity.onCreate()` 中執行阻塞操作。所有初始化必須延遲或使用 WorkManager 排程。
- **滾動流暢度**：RecyclerView 滾動幀率不得低於 50 FPS。圖片預覽列表必須使用 Coil 的記憶體快取與磁碟快取，避免重複解碼。
- **資料庫效能**：Room 查詢必須使用 Flow 進行非同步操作，禁止在主執行緒執行查詢。大量資料操作必須使用分頁或批次處理。
- **電池優化**：背景爬蟲任務必須使用 WorkManager 的 `Constraints` 設定網路條件（`setRequiredNetworkType(NetworkType.CONNECTED)`），避免在無網路時浪費電量。

**理由**：效能問題是使用者流失的主要原因之一。明確的效能基準確保產品在不同裝置與網路環境下皆能提供可接受的體驗。

### V. 正體中文優先

所有使用者可見內容與開發協作文件一律使用正體中文：

- **使用者介面**：所有字串資源（`strings.xml`）必須使用正體中文。禁止在 UI 中出現簡體中文、英文混合（專有名詞除外，如 OKHttp、Coil、Hilt 等技術名稱）。
- **錯誤訊息**：所有錯誤提示必須使用正體中文，語氣友善且具備可操作性。例如：「無法連線，請檢查網路設定後重試」而非「Connection failed」。
- **程式碼註解**：所有程式碼註解必須使用正體中文。技術專有名詞可保留英文原文，但說明文字必須為正體中文。
- **文件規範**：所有 Markdown 文件（README、開發指南、功能規格書）必須使用正體中文。Git commit message 可使用英文（遵循 conventional commits），但 PR 描述與 issue 內容必須使用正體中文。
- **例外情況**：程式碼中的識別字（變數名、函式名、類別名）遵循 Kotlin 命名規範（英文 camelCase/PascalCase），不受此原則限制。第三方函式庫名稱、API 端點、協議名稱保留原文。

**理由**：正體中文是目標使用者的主要語言。一致的語言使用提升產品的在地化品質，並確保開發團隊的溝通效率。

## 開發流程規範

- **功能開發流程**：每個功能必須從建立 spec 開始（`/speckit.plan`），經過實作計畫（`plan.md`）、任務拆解（`tasks.md`），最後實作與測試。禁止跳過規劃直接寫程式碼。
- **程式碼審查**：所有 PR 必須通過至少一位審查者。審查者必須確認符合本憲章的所有原則，並檢查測試覆蓋率。
- **分支策略**：功能分支命名格式為 `{issue-number}-{feature-name}`。主分支（main/master）必須隨時保持可建置狀態。
- **CI/CD 閘道**：PR 合併前必須通過 `gradlew test`、`gradlew lint`、`gradlew assembleDebug` 三項檢查。任一失敗即禁止合併。

## 品質把關機制

- **Lint 檢查**：執行 `gradlew lint`，禁止有 Error 級別的警告。Warning 級別應在合理時間內修正。
- **編譯檢查**：執行 `gradlew assembleDebug`，必須成功產生 APK 且無編譯錯誤。
- **測試閘道**：所有單元測試與儀器測試必須全數通過。新增功能若未包含對應測試，PR 不得合併。
- **憲章合規審查**：每次程式碼審查必須核對本憲章五項原則，任何違反必須有明確理由記錄於 PR 描述中。

## Governance

本憲章為專案最高指導原則，優先於所有其他開發慣例與個人偏好。

- **修訂程序**：任何原則的新增、修改或移除必須透過 PR 提出，說明修訂理由與影響範圍，經團隊共識後合併。修訂後必須更新版本號與修正日期。
- **版本政策**：遵循語義化版本（MAJOR.MINOR.PATCH）。MAJOR： backward incompatible 的原則移除或重新定義；MINOR：新增原則或實質擴展指導內容；PATCH：文字潤飾、釐清說明、錯字修正。
- **合規審查**：所有 PR 與程式碼審查必須驗證憲章合規性。若因特殊需求必須違反某項原則，必須在 PR 描述中說明理由與替代方案，並經審查者同意。
- **執行時期指引**：開發過程中的技術決策與程式風格細節，請參考 `AGENTS.md` 文件。本憲章定義「為什麼」與「什麼」，AGENTS.md 定義「怎麼做」。

**Version**: 1.0.0 | **Ratified**: 2026-04-01 | **Last Amended**: 2026-04-01
