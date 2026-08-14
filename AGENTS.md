# AGENTS.md — new BV 开发指南

> 本文档是 new BV 项目的 AI 协作开发规范，所有贡献者（含 AI Agent）必须遵守。
> 配套文档：[PRD](docs/PRD.md) | [开发计划](docs/开发计划.md)

---

## 1. 项目认知

### 1.1 项目定位

new BV 是基于 [BV](https://github.com/aaa1115910/bv)（源码在 `bv/` 目录）重构的哔哩哔哩第三方 Android TV 客户端。**不是 1:1 复刻**，而是架构重构 + 功能增强。

### 1.2 必读文档

开工前**必须**阅读以下文档：

| 文档 | 位置 | 用途 |
|---|---|---|
| PRD | `docs/PRD.md` | 完整需求规格（功能/架构/设置项/接口清单） |
| 开发计划 | `docs/开发计划.md` | 阶段任务分解与验收标准 |
| BV 源码 | `bv/` | 参考实现（接口写法、数据模型、播放器逻辑） |
| B 站 API 文档 | `docs/bilibili-API-collect-master/docs/` | 接口字段定义与参数说明 |
| 本文件 | `AGENTS.md` | 开发规范、测试策略、代码风格 |

### 1.3 关键架构决策（不可违背）

| 决策 | 说明 |
|---|---|
| 单 Activity + Navigation-Compose | 禁止新增 Activity（除系统必需） |
| Hilt DI | 禁止使用 Koin 或手动 `object` 单例做 DI |
| 仅 Media3 播放器 | 不接入 VLC，播放器抽象支持 VOD + Live |
| 代理功能完全删除 | 不引入任何代理/ProxyArea/Ali CDN 逻辑 |
| 无 Firebase | 崩溃监控用本地 + 可选自建上报 |
| minSdk 21 | 不允许使用仅 API 22+ 的 API 而不做兼容 |
| Kotlin 2.4.10 / KSP 2.3.10 / Java 17 | 版本锁定，不擅自升级 |

### 1.4 模块结构

```
newBV/
├── app/                    # 主应用（UI、ViewModel、Navigation）
├── core/                   # [待建] 通用工具、主题、交互模式抽象、日志基础设施
├── data/                   # [待建] Room、DataStore、Repository 接口
├── bili-api/               # [待迁移] B 站 HTTP + gRPC 接口封装（从 bv/ 迁移）
├── bili-api-grpc/          # [待迁移] Protobuf 定义（从 bv/ 迁移）
├── bili-subtitle/          # [待迁移] 字幕解析（从 bv/ 迁移）
├── player/                 # [待建] 播放器引擎抽象 + Media3 实现（从 bv/bv-player 迁移）
├── danmaku/                # [待建] 弹幕渲染封装（从 bv/app 散落代码抽取）
├── docs/
│   ├── PRD.md
│   ├── 开发计划.md
│   └── bilibili-API-collect-master/   # B 站 API 参考文档
└── bv/                     # 原版 BV 源码（只读参考，不修改）
```

---

## 2. 构建与命令

### 2.1 环境要求

- JDK 17（需设置 `JAVA_HOME` 环境变量指向 JDK 17 安装路径）
- Android SDK（compileSdk 36）
- Kotlin 2.4.10（由 Gradle 自动下载）

#### JDK 配置

项目需要 JDK 17，确保 `JAVA_HOME` 已正确设置：

```bash
# 验证
java -version  # 应显示 17.x

# 若系统无默认 Java，手动指定（示例）
export JAVA_HOME="/path/to/jdk-17"
```

#### 网络代理（可选）

若开发环境需要代理（如访问 Maven 仓库慢），可通过以下方式配置（**不要**将代理写入项目的 `gradle.properties`，因它是环境特定的）：

```bash
# 方式 1：环境变量（推荐，不影响提交的配置文件）
export https_proxy=http://127.0.0.1:7890
export http_proxy=http://127.0.0.1:7890
./gradlew assembleDebug

# 方式 2：用户级 Gradle 配置（~/.gradle/gradle.properties，不提交到项目）
# systemProp.https.proxyHost=127.0.0.1
# systemProp.https.proxyPort=7890
# systemProp.http.proxyHost=127.0.0.1
# systemProp.http.proxyPort=7890
```

### 2.2 常用命令

```bash
# 构建
./gradlew assembleDebug

# 运行所有单元测试
./gradlew test

# 运行指定模块测试
./gradlew :bili-api:test
./gradlew :app:testDebugUnitTest

# 运行插桩测试（需连接设备/模拟器）
./gradlew connectedAndroidTest
./gradlew :app:connectedDebugAndroidTest

# 生成测试覆盖率报告（JaCoCo）
./gradlew test jacocoAggregatedReport                    # 聚合所有模块
./gradlew :bili-api:jacocoTestReport                     # JVM 模块
./gradlew :app:createDebugJacocoReport                  # Android 模块

# 代码检查
./gradlew ktlintCheck
./gradlew detekt

# 依赖版本检查
./gradlew dependencyUpdates
```

### 2.3 测试凭证配置

接口集成测试需要 B 站账号凭证，配置在 `local.properties`（**不要提交到 git**）：

```properties
sdk.dir=/path/to/Android/Sdk
# 测试用 B 站账号凭证（从浏览器登录后获取）
test.sessdata=你的SESSDATA
test.bili_jct=你的bili_jct
test.uid=你的uid
test.access_token=你的access_token
test.buvid=你的buvid
# 测试用视频（避免使用热门视频，防止数据变更）
test.video.aid=993403941
test.video.cid=1051761130
test.live.roomid=21452505
```

`local.properties` 模板见 `local.properties.template`（应提交到 git）。

---

## 3. 代码规范

### 3.1 规范注释（强制）

**所有公开 API 必须有 KDoc 注释**。这是强制要求，PR 无注释将拒绝合并。

#### 3.1.1 KDoc 格式

```kotlin
/**
 * 视频播放器 ViewModel。
 *
 * 负责管理播放状态、弹幕、字幕、画质切换等播放器核心逻辑。
 * 不负责 UI 渲染，通过 [StateFlow] 暴露状态给 Compose。
 *
 * @param videoPlayRepository 播放数据仓库
 * @param savedStateHandle Navigation 传递的参数
 *
 * @see PlayerUiState
 * @see VideoPlayerController
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val videoPlayRepository: VideoPlayRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * 当前播放位置（毫秒）。
     * 高频更新（每 100ms），UI 通过 [collectAsStateWithLifecycle] 观察。
     */
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    /**
     * 跳转到指定位置。
     *
     * @param positionMs 目标位置（毫秒），会自动 clamp 到 [0, duration]
     * @throws IllegalStateException 如果播放器未初始化
     */
    fun seekTo(positionMs: Long) {
        // 实现细节注释：解释为什么需要 clamp
        val clamped = positionMs.coerceIn(0L, duration.value)
        ...
    }
}
```

#### 3.1.2 注释规则

| 元素 | 注释要求 |
|---|---|
| 公开类/接口 | **必须** KDoc，说明职责 |
| 公开函数/属性 | **必须** KDoc，说明用途、参数、返回值、异常 |
| 私有函数 | 复杂逻辑**必须**注释，简单 getter/setter 可省略 |
| 复杂逻辑块 | **必须**行内注释解释 why（不是 what） |
| TODO | 用 `// TODO(原因)` 格式，关联 issue |
| FIXME | 用 `// FIXME(原因)` 格式，表示有 bug 待修 |

#### 3.1.3 禁止的注释

```kotlin
// ❌ 禁止：无意义的注释
val count = items.size // 获取大小

// ❌ 禁止：注释说谎
val speed = 1.0 // 2倍速

// ❌ 禁止：注释掉的代码（用 git 管理历史）
// val oldSpeed = 0.5

// ✅ 正确：解释 why
// DASH 流视频与音频分离，需要 MergingMediaSource 合并
val mediaSource = MergingMediaSource(videoSource, audioSource)
```

### 3.2 命名规范

| 类型 | 规范 | 示例 |
|---|---|---|
| 类/接口 | PascalCase | `VideoPlayerViewModel` |
| 函数/变量 | camelCase | `seekToPosition` |
| 常量 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 包名 | 全小写 | `dev.frost819.newbv.player` |
| Composable | PascalCase | `VideoPlayerScreen` |
| 枚举值 | PascalCase | `R1080P`, `ApiType.Web` |
| 资源 ID | snake_case | `video_player_menu` |

### 3.3 包结构

```
dev.frost819.newbv/
├── app/               # Application 类
├── data/              # 数据层
│   ├── db/            # Room 实体与 DAO
│   ├── datastore/     # DataStore 偏好
│   └── repository/    # Repository 实现
├── di/                # Hilt 模块
├── network/           # 网络相关（HTTP 服务器、日志上报）
├── player/            # 播放器 UI 与 ViewModel
├── ui/
│   ├── theme/         # 主题、颜色、字体
│   ├── component/     # 通用 Composable
│   ├── navigation/    # Navigation 路由定义
│   └── screen/        # 各功能页面
│       ├── home/
│       ├── player/
│       ├── detail/
│       ├── search/
│       ├── live/
│       └── settings/
└── util/              # 工具类
```

### 3.4 Kotlin 风格

- 使用 `data class` 表示纯数据模型
- 使用 `sealed class` / `sealed interface` 表示有限状态机
- 优先 `val` 不可变，必要时 `var` 但需说明原因
- 协程：`viewModelScope` / `Lifecycle.repeatOnLifecycle`
- Compose：`StateFlow.collectAsStateWithLifecycle()`
- 避免 `!!`，用 `?.` + elvis 或 `requireNotNull`
- 避免 `lateinit var`，用 `by lazy` 或构造注入

---

## 4. 接口开发规范

### 4.1 接口参考优先级

开发 B 站接口时，按以下优先级参考：

```
1. BV 原版源码（bv/bili-api/）  ← 首要参考，已验证可用的实现
       ↓ 若字段/参数不明确
2. bilibili-API-collect 文档    ← 字段定义与参数说明
       ↓ 若文档与实际不一致或文档缺失
3. Playwright 抓取实际请求      ← 验证真实接口行为（见 4.3）
       ↓ App gRPC 接口
4. App 接口基本不用担心不可用    ← 原 BV 已验证，直接迁移
```

### 4.2 参考 BV 原版写法

**原版 `bili-api` 模块是经过验证的可用实现，迁移时优先参考其写法**：

| 参考内容 | 原版位置 |
|---|---|
| HTTP 接口定义 | `bv/bili-api/src/main/kotlin/dev/aaa1115910/biliapi/http/BiliHttpApi.kt` |
| 登录接口 | `bv/bili-api/.../http/BiliPassportHttpApi.kt` |
| 直播接口 | `bv/bili-api/.../http/BiliLiveHttpApi.kt` |
| gRPC 接口 | `bv/bili-api/.../grpc/` + `bv/bili-api-grpc/proto/` |
| 数据模型 | `bv/bili-api/.../entity/` |
| 签名机制 | `bv/bili-api/.../http/util/ApiSign.kt`（WBI + App 签名） |
| buvid 生成 | `bv/bili-api/.../http/util/Buvid.kt` |
| Repository | `bv/bili-api/.../repositories/` |

**迁移要点**：
- 保留原版的接口签名逻辑（WBI `w_rid`、App `sign` MD5）——这是接口可用的关键
- 保留原版的 UA 设置（Web UA / App UA）——风控相关
- 保留原版的 buvid 生成算法
- **删除**所有代理相关代码（`BiliHttpProxyApi`、`ProxyArea`、`proxyChannel`、Ali CDN 替换）
- **删除** Firebase 相关
- 包名从 `dev.aaa1115910.biliapi` 改为 `dev.frost819.newbv.biliapi`

### 4.3 参考 bilibili-API-collect 文档

`docs/bilibili-API-collect-master/docs/` 是社区维护的 B 站 API 文档，包含字段定义、参数说明、错误码。

**常用文档**：

| 功能 | 文档路径 |
|---|---|
| 视频流地址 | `video/videostream_url.md` |
| 视频信息 | `video/info.md` |
| 视频操作（点赞/投币/收藏） | `video/action.md` |
| 推荐 | `video/recommend.md` |
| 弹幕 | `danmaku/` |
| 评论 | `comment/list.md`, `comment/action.md` |
| 直播 | `live/info.md`, `live/live_stream.md`, `live/danmaku.md` |
| 登录 | `login/login_info.md`, `login/login_action/` |
| 用户 | `user/info.md`, `user/relation.md` |
| 历史 | `historytoview/` |
| 收藏 | `fav/` |
| 番剧 | `bangumi/` |

**使用方式**：
- 接口字段不明确时查文档确认含义
- 新增接口（如评论、直播扩展）先查文档了解参数
- 错误码含义查文档对应表

### 4.4 Playwright 抓取 Web 接口（Fallback）

**当 BV 原版无对应实现 + bilibili-API-collect 文档不明确或过时时**，使用 Playwright 抓取 B 站 Web 端实际请求验证接口用法。

#### 4.4.1 适用场景

- 原版未实现的新增接口（如评论详情、直播扩展功能）
- 文档记录的接口返回 403/-352 等错误（可能签名/参数变更）
- 字段含义不明确，需看实际响应

#### 4.4.2 操作步骤

```bash
# 1. 检查playwright是否可用，若不可用则安装 Playwright（首次）
npx playwright install chromium

# 2. 启动抓取脚本（需登录态）
#    在浏览器开发者工具 Network 面板观察请求
#    或用 Playwright 录制脚本自动捕获
```

**抓取要点**：
- 关注：请求 URL、请求方法、请求头（尤其 `Cookie`、`Referer`、`User-Agent`）、请求参数、响应体
- WBI 签名参数：`wts`、`w_rid` 的计算方式（参考原版 `ApiSign.kt`）
- 反爬参数：`buvid3` Cookie、风控参数

#### 4.4.3 注意事项

- **App gRPC 接口基本不用担心不可用**——原版已验证，直接迁移 proto 与调用
- Playwright 仅用于 Web HTTP 接口的验证与补全
- 抓取的凭证（Cookie/Token）**禁止写入代码或提交**，只用于临时验证
- 抓取结果记录在接口文档注释中（URL、参数、响应示例）

### 4.5 接口实现规范

每个新增/迁移的接口必须：

1. **KDoc 注释**：说明接口用途、鉴权方式、对应文档链接
2. **错误处理**：处理 `-101`（未登录）、`-352`（风控）、`-400`（请求错误）等
3. **单元测试**：见第 5 节
4. **数据模型**：用 `@Serializable` data class，字段名与 B 站 API 一致

```kotlin
/**
 * 获取视频流地址（Web API）。
 *
 * 对应文档：docs/bilibili-API-collect-master/docs/video/videostream_url.md
 * 鉴权：SESSDATA Cookie + WBI 签名
 *
 * @param aid 视频 AV 号
 * @param cid 视频 CID
 * @param qn 画质标识（默认 127 = 8K）
 * @param fnval 格式标识（默认 4048 = DASH + 所有高级格式）
 */
suspend fun getVideoPlayUrl(
    aid: Long,
    cid: Long,
    qn: Int = 127,
    fnval: Int = 4048
): PlayUrlData
```

---

## 5. 测试规范（强制）

### 5.1 测试要求

**所有新增代码必须有对应测试，覆盖率目标 ≥ 80%**。原版 BV 测试覆盖率很差，new BV 必须补全。

| 测试类型 | 工具 | 运行环境 | 覆盖目标 |
|---|---|---|---|
| 单元测试 | JUnit 5 + MockK + Turbine | JVM | Repository、ViewModel、工具类、数据模型 |
| 插桩测试 | AndroidJUnit4 + Compose Test | 设备/模拟器 | DAO、DataStore、Composable UI、播放器 |
| 接口集成测试 | JUnit 5 + 真实凭证 | JVM（需网络） | B 站 API 调用验证（可选，CI 跳过） |

### 5.2 测试分层

```
┌─────────────────────────────────────┐
│         插桩测试 (androidTest)        │  ← Composable UI、DAO、播放器
├─────────────────────────────────────┤
│         ViewModel 测试 (test)        │  ← StateFlow 断言（Turbine）
├─────────────────────────────────────┤
│         Repository 测试 (test)       │  ← Mock API，验证业务逻辑
├─────────────────────────────────────┤
│         API/工具测试 (test)          │  ← 签名、解析、序列化
└─────────────────────────────────────┘
         接口集成测试 (test, 可选)       ← 真实 API 调用
```

### 5.3 单元测试规范

#### 5.3.1 测试依赖

```kotlin
// app/build.gradle.kts
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("com.google.truth:truth:1.4.2")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.10")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

#### 5.3.2 命名与结构

```kotlin
/**
 * [VideoPlayRepository] 的单元测试。
 *
 * 验证播放数据获取、画质选择、编码 fallback 等业务逻辑，
 * 不依赖真实网络（Mock BiliHttpApi 与 gRPC Channel）。
 */
class VideoPlayRepositoryTest {

    private lateinit var repository: VideoPlayRepository
    private lateinit var mockHttpApi: BiliHttpApi
    private lateinit var mockAuthRepo: AuthRepository

    @BeforeEach
    fun setUp() {
        mockHttpApi = mockk()
        mockAuthRepo = mockk()
        repository = VideoPlayRepository(mockHttpApi, mockAuthRepo)
    }

    @Test
    fun `getPlayData with Web API returns merged codec data`() = runTest {
        // Given
        coEvery { mockHttpApi.getVideoPlayUrl(any(), any(), any(), any()) } 
            returns fakePlayUrlResponse()

        // When
        val result = repository.getPlayData(aid = 1L, cid = 2L, ApiType.Web)

        // Then
        assertThat(result.dashVideos).isNotEmpty()
        assertThat(result.needPay).isFalse()
    }

    @Test
    fun `getPlayData falls back to App gRPC when Web returns risk control`() = runTest {
        // Given
        coEvery { mockHttpApi.getVideoPlayUrl(any(), any(), any(), any()) } 
            throws RiskControlException(-352)

        // When
        val result = repository.getPlayData(aid = 1L, cid = 2L, ApiType.Web)

        // Then: 自动降级到 App 接口
        assertThat(result).isNotNull()  // gRPC 返回的数据
    }
}
```

#### 5.3.3 测试规则

- **命名**：用反引号描述行为（`` `getPlayData with Web API returns merged codec data` ``）
- **结构**：Given / When / Then（注释分隔）
- **隔离**：每个测试独立，`@BeforeEach` 初始化，不依赖测试顺序
- **断言**：用 Truth (`assertThat`) 或 kotlin-test，不用 JUnit `assertEquals`（可读性差）
- **协程**：用 `runTest` + `kotlinx-coroutines-test`
- **Flow**：用 Turbine 测试 `StateFlow`/`SharedFlow`
- **Mock**：用 MockK（`coEvery` / `coVerify` / `mockk()`）
- **覆盖率**：每个公开函数至少 1 个正常 + 1 个异常 case

### 5.4 插桩测试规范

#### 5.4.1 DAO 测试

```kotlin
/**
 * [SearchHistoryDao] 的插桩测试。
 * 在真实 SQLite 上验证 SQL 正确性。
 */
@RunWith(AndroidJUnit4::class)
class SearchHistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SearchHistoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.searchHistoryDao()
    }

    @After
    fun teardown() = database.close()

    @Test
    fun insert_and_query_by_dateDesc() = runTest {
        dao.insert(SearchHistoryDB(keyword = "test1"))
        delay(100)
        dao.insert(SearchHistoryDB(keyword = "test2"))

        val result = dao.getHistories(10)

        assertThat(result).hasSize(2)
        assertThat(result[0].keyword).isEqualTo("test2")  // 最新的在前
    }
}
```

#### 5.4.2 Compose UI 测试

```kotlin
/**
 * [SmallVideoCard] 的 UI 测试。
 * 验证卡片显示内容与点击/长按交互。
 */
@RunWith(AndroidJUnit4::class)
class SmallVideoCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displays_title_and_playCount() {
        composeRule.setContent {
            SmallVideoCard(data = fakeVideoCardData())
        }

        composeRule.onNodeWithText("测试视频标题").assertIsDisplayed()
        composeRule.onNodeWithText("1.2万播放").assertIsDisplayed()
    }

    @Test
    fun longPress_showsActionButtons() {
        composeRule.setContent {
            SmallVideoCard(data = fakeVideoCardData(), onLongPress = {})
        }

        composeRule.onNodeWithContentDescription("视频卡片").performTouchInput {
            down(center)
            advanceEventTime(2000)  // 长按 2 秒
            up()
        }

        composeRule.onNodeWithText("稍后再看").assertIsDisplayed()
    }
}
```

### 5.5 测试覆盖率

#### 5.5.1 JaCoCo 配置

每个模块配置 JaCoCo，生成覆盖率报告：

```bash
./gradlew test jacocoAggregatedReport
# 聚合报告位置：build/reports/jacoco/jacocoAggregatedReport/html/index.html

# Per-module 报告
./gradlew :bili-api:jacocoTestReport          # JVM 模块
./gradlew :app:createDebugJacocoReport        # Android 模块
# Android 模块报告位置：build/reports/jacoco/createDebugJacocoReport/html/index.html
```

#### 5.5.2 覆盖率目标

| 模块 | 目标覆盖率 | 重点 |
|---|---|---|
| `:bili-api` | ≥ 90% | 签名算法、数据解析、Repository 业务逻辑 |
| `:player` | ≥ 80% | 播放器状态机、URL 解析、编解码选择 |
| `:danmaku` | ≥ 80% | 弹幕解析、蒙版处理 |
| `:bili-subtitle` | ≥ 90% | BCC/SRT 解析编码（已有基础） |
| `:data` | ≥ 85% | DAO、DataStore |
| `:app` ViewModel | ≥ 80% | 每个 ViewModel 的状态流转 |
| `:app` UI | ≥ 60% | 关键交互（点击/长按/导航） |

#### 5.5.3 CI 门禁

- PR 合并前必须通过 `./gradlew test` + `ktlintCheck`
- 覆盖率低于目标时 PR 标记警告（不强制阻断，但需说明原因）
- 插桩测试在 CI 上用模拟器运行（可选，本地开发必须跑）

---

## 6. 架构规范

### 6.1 单向数据流（UDF）

```
UI Event → ViewModel → Repository → Data Source
                ↓
           StateFlow ←←←←←←←←←←←←
                ↓
              Composable
```

- **UI 只发事件，不直接调 Repository**
- **ViewModel 只管状态，不直接操作 UI**
- **Repository 是唯一数据出口**（聚合 Room + DataStore + API）
- **状态用 StateFlow**，事件用 SharedFlow（一次性）

### 6.2 Navigation 路由

用类型安全路由（`@Serializable`），禁止字符串拼接 URL：

```kotlin
// ✅ 正确
@Serializable
data class VideoDetailRoute(val aid: Long, val epid: Long? = null)

navController.navigate(VideoDetailRoute(aid = 12345))

// ❌ 错误
navController.navigate("videoDetail/12345")
```

### 6.3 Hilt DI

```kotlin
// Module
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideBiliHttpApi(): BiliHttpApi = BiliHttpApi
}

// ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recommendRepository: RecommendRepository
) : ViewModel() { ... }

// Composable 中获取
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) { ... }
```

### 6.4 错误处理

#### 6.4.1 ViewModel 层：超时 + 错误统一处理

所有涉及网络资源加载的 ViewModel **必须**遵循以下模式：

1. **超时保护**：用 `withTimeout(10_000L)` 包裹网络请求，防止无限 loading
2. **错误状态**：UiState 中为每个加载源添加 `xxxError: Boolean` 字段
3. **CancellationException 陷阱**：`withTimeout` 超时抛出 `TimeoutCancellationException`（是 `CancellationException` 子类），但普通协程取消（如 ViewModel cleared）也会抛 `CancellationException`，**必须 re-throw** 非 timeout 的取消异常，否则会破坏协程取消机制
4. **刷新清除错误**：`refreshXxx()` 必须清除对应 error 字段

```kotlin
companion object {
    private const val LOAD_TIMEOUT_MS = 10_000L
}

fun loadRecommend() {
    viewModelScope.launch {
        if (_uiState.value.recommendLoading) return@launch

        _uiState.update { it.copy(recommendLoading = true, recommendError = false) }

        runCatching {
            withTimeout(LOAD_TIMEOUT_MS) {
                // 网络请求逻辑
            }
        }.onFailure { error ->
            // ⚠️ 必须 re-throw 非 timeout 的 CancellationException
            if (error is CancellationException && error !is TimeoutCancellationException) {
                throw error
            }
            logger.error(error) { "Failed to load" }
            _uiState.update { it.copy(recommendError = true) }
        }

        _uiState.update { it.copy(recommendLoading = false) }
    }
}

fun refreshRecommend() {
    recommendNextPage = RecommendPage()
    _uiState.update {
        it.copy(recommendItems = emptyList(), recommendHasMore = true, recommendError = false)
    }
    loadRecommend()
}
```

#### 6.4.2 UI 层：ListFooterTip 统一提示组件

所有分页列表页底部**必须**使用 `ListFooterTip` 组件，统一 loading/error/no-more 三态显示：

```kotlin
item(span = { GridItemSpan(maxLineSpan) }) {
    ListFooterTip(
        isLoading = state.recommendLoading,
        isError = state.recommendError,
        hasMore = state.recommendHasMore,
        itemsIsEmpty = state.recommendItems.isEmpty(),
    )
}
```

状态优先级：`loading > error > noMore`（空列表不显示"没有更多"）。

### 6.5 ViewModel 拆分原则

- 单一职责，每个 ViewModel ≤ 300 行
- 原版 `VideoPlayerV3ViewModel`（1417 行）拆分为：
  - `PlayerViewModel`（播放控制、状态）
  - `PlayerMenuViewModel`（设置菜单）
  - `DanmakuViewModel`（弹幕）
  - `SubtitleViewModel`（字幕）
  - `VideoListViewModel`（分集列表）
- ViewModel 间通过 `SharedFlow` 通信

---

## 7. Git 规范

### 7.1 分支策略

| 分支 | 用途 |
|---|---|
| `main` | 稳定发布分支 |
| `dev` | 开发主干 |
| `feature/{描述}` | 功能分支（如 `feature/live-player`） |
| `fix/{描述}` | 修复分支 |
| `test/{描述}` | 测试分支 |

### 7.2 提交信息

格式：`<type>(<scope>): <description>`

| type | 含义 |
|---|---|
| feat | 新功能 |
| fix | 修复 bug |
| test | 新增/修改测试 |
| refactor | 重构（不改功能） |
| docs | 文档 |
| chore | 构建/工具 |
| style | 格式（不改逻辑） |

示例：
```
feat(player): 直播流播放支持
fix(danmaku): 修复 seek 后弹幕不同步
test(bili-api): 补全 VideoPlayRepository 单测
```

### 7.3 PR 规范

- 一个 PR 一个功能点，不混合多个无关改动
- PR 描述说明：做了什么、为什么、如何测试
- 必须通过 `./gradlew test` + `ktlintCheck`
- 必须包含对应测试（新增功能无测试拒绝合并）

---

## 8. 常见模式参考

### 8.1 网络请求 + Repository 模式

参考原版 `bv/bili-api/.../repositories/VideoPlayRepository.kt`，但用 Hilt 注入 + Result 封装。

### 8.2 Compose TV 焦点管理

- 使用 Compose 官方 `focusRestorer` 保持列表焦点
- `core` 模块提供 `focusedBorder` / `focusedScale` 扩展（`FocusExt.kt`）
- `focusedBorder` 根据 `InteractionTracker` 的 `InputMethod` 动态显示：触屏时隐藏，遥控器时显示
- `KeyEventExt` 提供 D-Pad 方向键判断扩展（`isDpadUp` / `isDpadDown` / `isConfirm` 等）

#### 8.2.1 路由跳转后的焦点恢复（强制）

**所有涉及路由跳转的可聚焦元素（按钮、卡片、Chip 等）必须接入 `FocusSaver` / `ScreenFocusSaver`**，否则用户从目标页返回后焦点会落在错误的元素上。

项目提供两个焦点恢复器（`app/ui/component/FocusSaver.kt`）：

| 类 | 适用场景 | key 类型 | 示例 |
|---|---|---|---|
| `FocusSaver` | 列表 / 网格（同类型 item） | `Int` index | 首页推荐网格、分区视频网格 |
| `ScreenFocusSaver` | 混合布局（不同类型可聚焦元素） | `String` key | 详情页（封面、UP 按钮、点赞、Tag 等） |

**使用步骤（3 步）**：

```kotlin
// 1. 创建 FocusSaver
val focusSaver = rememberScreenFocusSaver()

// 2. 在屏幕顶层调用 RestoreFocus()，用于返回时恢复焦点
focusSaver.RestoreFocus()

// 3. 每个可聚焦元素接入：focusRequester + onFocusChanged
Card(
    modifier = Modifier
        .focusRequester(focusSaver.focusRequesterFor("cover"))
        .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey("cover") },
    onClick = { navController.navigate(...) },
) { ... }

// LazyRow 中的 item 同样需要接入（每个 item 用唯一 key）
items(detail.tags) { tag ->
    val tagKey = "tag_${tag.id}"
    SuggestionChip(
        onClick = { navController.navigate(...) },
        modifier = Modifier
            .focusRequester(focusSaver.focusRequesterFor(tagKey))
            .onFocusChanged { if (it.hasFocus) focusSaver.saveFocusedKey(tagKey) },
    ) { Text(tag.name) }
}
```

**关键注意事项**：

1. **LazyRow / LazyColumn 中的 item**：`RestoreFocus()` 内置 50ms 延迟，确保 LazyRow 子项完成组合后再请求焦点。如果跳过这一步，`focusRequesterFor(key)` 返回的 `FocusRequester` 尚未与实际节点绑定，`requestFocus()` 会静默失败
2. **首次进入页面**：`savedKeyValue()` 为空时，手动 `requestFocus()` 到默认元素（如封面卡片）
3. **key 唯一性**：列表 item 的 key 必须包含唯一标识（如 `"tag_${tag.id}"`），不能用纯索引（滚动后索引变化导致恢复到错误 item）
4. **`ScreenFocusSaver` 用 `rememberSaveable`**：跨配置变更（如旋转）和进程恢复保持焦点 key

### 8.3 DataStore Prefs

参考原版 `bv/app/.../util/Prefs.kt` 的 `PrefDelegate` 模式，但迁移到 `data/datastore/` 模块。

### 8.4 播放器状态机

参考原版 `bv/app/.../ui/state/PlayerUiState.kt` 的 sealed class 设计。

---

## 9. 禁止事项

| 禁止 | 原因 |
|---|---|
| 引入 Firebase / Google Services | 国内不可用 |
| 引入代理相关代码 | PRD 决策完全删除 |
| 引入 VLC | PRD 决策仅 Media3 |
| 使用 Koin | PRD 决策用 Hilt |
| 新增 Activity | PRD 决策单 Activity |
| 提交 `local.properties` | 含敏感凭证 |
| 提交 `google-services.json` | 不再使用 |
| 使用 `!!` 强制非空 | 易 NPE |
| 使用 `lateinit var`（除 Hilt 注入） | 易未初始化崩溃 |
| 注释掉的代码提交 | 用 git 管理历史 |
| 无注释的公开 API | 强制要求 KDoc |
| 无测试的新功能 | 强制要求测试 |

---

## 10. 参考资源

- [BV 原版源码](bv/) — 接口实现、数据模型、播放器逻辑参考
- [B 站 API 文档](docs/bilibili-API-collect-master/docs/) — 接口字段与参数
- [PRD](docs/PRD.md) — 完整需求规格
- [开发计划](docs/开发计划.md) — 阶段任务分解
- [Jetpack Compose for TV](https://developer.android.com/training/tv/compose)
- [Navigation-Compose](https://developer.android.com/guide/navigation)
- [Hilt](https://dagger.dev/hilt/)
- [Media3](https://developer.android.com/media/media3)
- [MockK](https://mockk.io/)
- [Turbine](https://github.com/cashapp/turbine)

---

## 11. 踩坑经验总结

> 以下是 Phase 1 开发中实际遇到的问题，记录避免重复踩坑。

### 11.1 Android TV 适配

#### 11.1.1 Manifest Launcher Category

Android TV 的首页只显示 `LEANBACK_LAUNCHER`，不认 `LAUNCHER`（手机用）。不设置则 App 不出现在 TV 桌面应用列表中。

```xml
<!-- ✅ 正确：TV -->
<category android:name="android.intent.category.LEANBACK_LAUNCHER" />

<!-- ❌ 错误：手机 -->
<category android:name="android.intent.category.LAUNCHER" />
```

#### 11.1.2 density 适配

1080p TV 模拟器系统 density 为 320（即 320/160 = 2.0f）。在 `MainActivity.setContent` 中需设置 `LocalDensity.current.density = 2.0f`，否则 UI 元素尺寸异常。

#### 11.1.3 自定义 Typography

TV 观看距离远（3m+），Compose 默认字号偏小。需在 `core/theme/Typography.kt` 中自定义字号，比 Material3 默认大 1.2-1.5 倍。

#### 11.1.4 HTTP 明文流量

B 站部分 CDN 返回 HTTP（非 HTTPS），Android 9 默认禁止明文流量。需配置 `network_security_config.xml` 白名单 B 站域名：

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">bilivideo.com</domain>
    <domain includeSubdomains="true">bilivideo.cn</domain>
    <domain includeSubdomains="true">hdslb.com</domain>
    <!-- 其他 B 站 CDN 域名 -->
</domain-config>
```

### 11.2 Compose TV 焦点管理

#### 11.2.1 弹窗必须用 Dialog 而非 AnimatedVisibility

**问题**：用 `AnimatedVisibility` + 全屏 `Box` + `.focusable()` 实现弹窗，D-Pad 方向键会穿透到下层内容（如左侧导航栏），`.focusable()` 无法阻止。

**原因**：`AnimatedVisibility` 只是叠层在内容上，Compose 焦点系统仍可在所有可见的 focusable 节点间导航。

**解决方案**：使用 `Dialog`，它创建独立窗口，焦点被正确限制在弹窗内。

```kotlin
// ✅ 正确：Dialog 创建独立窗口，焦点不外逃
Dialog(
    onDismissRequest = { showPanel = false },
    properties = DialogProperties(usePlatformDefaultWidth = false),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
    ) {
        PanelContent(
            modifier = Modifier
                .align(Alignment.Center)
                .width(400.dp),
        )
    }
}

// ❌ 错误：AnimatedVisibility 焦点会穿透
AnimatedVisibility(visible = showPanel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .focusable(),  // 无效，D-Pad 仍可逃逸
    ) { ... }
}
```

#### 11.2.2 AnimatedVisibility 需手动请求焦点

`AnimatedVisibility` 内容出现时不会自动聚焦。需要：

```kotlin
val focusRequester = remember { FocusRequester() }
LaunchedEffect(Unit) {
    runCatching { focusRequester.requestFocus() }
}
BackHandler { visible = false }  // 同时处理返回键
```

#### 11.2.3 TV Material3 Card 选中框

TV Material3 的 `Card` 自带 `border` 参数。**不要**在 modifier 上再叠加 `focusedBorder()` 扩展，否则选中时出现双层边框。

```kotlin
// ✅ 正确：用 CardDefaults.border
Card(
    border = CardDefaults.border(focusedBorder = Border(width = 2.dp, color = ...)),
) { ... }

// ❌ 错误：叠加 focusedBorder modifier 导致双层
Card(
    modifier = Modifier.focusedBorder(),
) { ... }
```

#### 11.2.4 弹窗宽度限制

弹窗组件内部不要用 `fillMaxWidth()`（撑满屏幕宽度太大），应由调用方通过 `Modifier.width(400.dp)` 控制宽度。

#### 11.2.5 路由跳转返回后焦点丢失

**问题**：详情页有多个可聚焦元素（封面、UP 按钮、点赞、投币、收藏、Tag Chip 等）。用户点击某个元素跳转到目标页，返回后焦点落在错误位置（如最后一个可聚焦元素）或丢失。

**原因**：Compose Navigation 路由跳转时，源页面被移出 composition；返回时重新组合，焦点系统不知道之前哪个元素有焦点，默认聚焦到第一个或最后一个可聚焦节点。

**解决方案**：所有涉及路由跳转的可聚焦元素必须接入 `FocusSaver` / `ScreenFocusSaver`（见 §8.2.1）。核心原理：
- 元素获得焦点时，通过 `onFocusChanged` 保存 key 到 `rememberSaveable` 状态
- 页面重新组合后，`RestoreFocus()` 读取保存的 key，通过对应的 `FocusRequester` 重新请求焦点
- LazyRow 中的 item 需加 50ms 延迟等待子项组合完成（`RestoreFocus()` 已内置）

**常见遗漏**：LazyRow / LazyColumn 中的可点击 item（如 Tag Chip、相关视频卡片）容易忘记接入焦点追踪，导致从这些 item 跳转返回后焦点恢复到其他元素。

### 11.3 StateFlow 与 Compose

#### 11.3.1 必须用 collectAsState()

在 Composable 中读取 `StateFlow` 必须用 `collectAsState()`，**不能**用 `.value`。`.value` 只读取当前值，不会在数据更新时重组 UI。

```kotlin
// ✅ 正确：UI 随数据更新
val uiState by viewModel.uiState.collectAsState()

// ❌ 错误：UI 不响应更新
val uiState = viewModel.uiState.value
```

### 11.4 第三方库迁移注意事项

#### 11.4.1 Coil 3 vs Coil 2

Coil 3 是 breaking change：网络组件不再自动包含。必须手动配置 `SingletonImageLoader` + `OkHttpNetworkFetcherFactory`，否则无法加载远程图片（Coil 2 自动包含网络组件）。

```kotlin
// BVApplication.onCreate()
val imageLoader = ImageLoader.Builder(this)
    .components {
        add(OkHttpNetworkFetcherFactory(callFactory = ::buildOkHttpClient))
    }
    .build()
SingletonImageLoader.setSafe(imageLoader)
```

#### 11.4.2 BiliHttpApi 是 object 单例

`BiliHttpApi` 是 Kotlin `object` 单例，不是通过构造注入的。Hilt 的 `@Provides` 返回 `BiliHttpApi` 时不会触发其 `init()`。必须在 `BVApplication.onCreate()` 中手动调用 `BiliHttpApi.init(buvid3)`，否则 Repository 调用 API 时 buvid3 为空，请求失败。

#### 11.4.3 两个 ApiType 枚举

项目中存在两个 `ApiType` 枚举：
- `data.datastore.ApiType` — Prefs 存储 用户偏好用
- `biliapi.entity.ApiType` — Repository 调用接口时用

两者枚举值不同，需要手动映射转换。

### 11.5 测试踩坑

#### 11.5.1 ViewModel 测试中的 Dispatcher

ViewModel 中**不要**用 `Dispatchers.IO`，用默认 `viewModelScope.launch {}`。测试时用 `Dispatchers.setMain(testDispatcher)` 拦截，否则测试无法控制协程执行。

```kotlin
@BeforeEach
fun setUp() {
    Dispatchers.setMain(testDispatcher)
}

@AfterEach
fun tearDown() {
    Dispatchers.resetMain()
}

@Test
fun `test something`() = runTest(testDispatcher) {
    // ...
    advanceUntilIdle()  // 等待协程完成
}
```

#### 11.5.2 Prefs 单例测试

Prefs 是全局单例，测试时需注意：
- 用 `@TestInstance(PER_CLASS)` + `@BeforeAll` 初始化 DataStore
- `@BeforeEach` 中 `runBlocking { Prefs.clear() }` 清空状态
- **不要**在 `@AfterAll` 中调 `resetForTesting()`，会导致后续测试 NPE

#### 11.5.3 Compose UI 插桩测试

- 用 `@HiltAndroidTest` + `HiltAndroidRule(order=0)` + `createAndroidComposeRule<MainActivity>(order=1)`
- `SmallVideoCard` 等组件测试需包裹 `TvMaterialTheme` + `Box(Modifier.width(300.dp))` 提供尺寸约束
- TV Material3 的 alpha 限制导致点击/长按交互在插桩测试中不稳定，暂不纳入断言

### 11.6 构建与部署

#### 11.6.1 ADB 安装降级

模拟器上已安装高版本 APK 时，安装低版本会报 `INSTALL_FAILED_VERSION_DOWNGRADE`。需加 `-d` 标志：

```bash
adb install -r -d app-debug.apk
```

#### 11.6.2 包名与 R 类

- Debug 包名带 suffix：`dev.frost819.newbv.debug`（非 `dev.frost819.newbv`）
- R 类包名：`dev.frost819.newbv.R`（非 `dev.frost819.newbv.app.R`）
- Activity 全路径：`dev.frost819.newbv.app.MainActivity`

#### 11.6.3 模拟器 UI 自动化测试

可用 `adb shell uiautomator dump` + `adb shell cat` 导出 UI 层级 XML，配合 Python 解析验证焦点位置：

```bash
# 导出 UI 层级
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml

# 解析焦点节点
adb shell cat /sdcard/ui.xml | python3 -c "
import sys, xml.dom.minidom
dom = xml.dom.minidom.parseString(sys.stdin.read())
for node in dom.getElementsByTagName('node'):
    if node.getAttribute('focused') == 'true':
        print(f'focused bounds={node.getAttribute(\"bounds\")}')
"
```

#### 11.6.4 D-Pad 方向键测试

```bash
adb shell input keyevent KEYCODE_DPAD_UP
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_LEFT
adb shell input keyevent KEYCODE_DPAD_RIGHT
adb shell input keyevent KEYCODE_DPAD_CENTER  # 确认键
adb shell input keyevent KEYCODE_BACK
```

### 11.7 Prefs 与 DataStore

#### 11.7.1 不要用 `dataStore.data.collect` 监听变化

**问题**：原版 BV 和早期 new BV 都在 `Prefs.init()` 中启动 `dataStore.data.collect { updateMemoryCache(it) }`，意图是让磁盘变化同步到内存。但这会引入竞态：

`PrefDelegate.setValue()` 逐字段异步写 DataStore。例如 `AuthData.saveToPrefs()` 连续写 9 个字段，`uid` 写完后 `collect` 触发，此时磁盘快照包含新 `uid` 但旧 `sessData`，`updateMemoryCache` 用这个不完整快照覆盖全部内存缓存，导致 `Prefs.sessData` 被旧值覆盖。

**解决方案**：去掉 `collect`。`PrefDelegate.setValue()` 已经先更新内存再异步写磁盘，内存永远比磁盘先更新。`init()` 只需 `runBlocking { dataStore.data.first() }` 做一次初始加载即可。

#### 11.7.2 Room @Update 必须有有效主键

**问题**：`UserEntity` 用自增 `id` 作主键。`addUser()` 构造 `UserEntity(id = null, ...)` 后调 `upsertUser()`，若用户已存在走 `userDao.update(user)` 分支，但 `user.id` 为 null，Room `@Update` 按主键匹配，静默失败。

**解决方案**：`upsertUser()` 从 DB 查出 existing entity（有有效 id），更新其字段后 `update(existing)`：

```kotlin
override suspend fun upsertUser(user: UserEntity) {
    val existing = userDao.findUserByUid(user.uid)
    if (existing != null) {
        existing.auth = user.auth
        userDao.update(existing)
    } else {
        userDao.insert(user)
    }
}
```

#### 11.7.3 B 站经验进度条计算

B 站 API `next_exp` 是"下一等级所需的经验值**门槛**"（不是剩余），`current_min` 是当前等级的起点。正确公式：

```kotlin
val progress = if (nextExp > currentMin) {
    ((exp - currentMin).toFloat() / (nextExp - currentMin).toFloat()).coerceIn(0f, 1f)
} else {
    1f  // Lv6 满级
}
```

`setCurrentUser()` 切换账号时必须先重置 level/exp/currentMin/nextExp 为 0，再调 `refreshUserInfo()` 从网络拉取新数据，否则 UI 会短暂显示旧用户等级。

### 11.8 B 站接口与风控

#### 11.8.1 playurl 端点选择与 WBI 签名

**问题**：使用 `/x/player/wbi/playurl`（WBI 签名端点）比 `/x/player/playurl`（旧端点）更容易触发 B 站风控（返回 `v_voucher` 验证挑战），连续播放 2-3 次即被拦截。

**原因**：WBI 端点风控策略更严格。原版 BV 一直使用旧的 `/x/player/playurl` 端点（不带 WBI 签名），风控阈值更高。

**解决方案**：与原版 BV 保持一致：
- 使用 `/x/player/playurl`（非 WBI 端点）
- `encApiSign()` 中的 WBI 签名触发条件不含此路径，不会添加 `w_rid`/`wts`
- 不需要 WBI 签名也能正常返回视频流

#### 11.8.2 playurl Cookie 排除 buvid3

**问题**：`injectCookies()` 拦截器为所有非 App 请求注入完整 Cookie（SESSDATA + DedeUserID + buvid3 + b_nut）。但 playurl 请求带 buvid3 会增加风控触发概率。

**原因**：原版 BV 的 `injectBuvid3Cookie()` 显式排除了 playurl 请求（检测路径含 `/x/player/playurl` 或 `/x/player/wbi/playurl`），只注入 `SESSDATA` + `DedeUserID`。

**解决方案**：在 `injectCookies()` 中添加 playurl 排除逻辑，与原版 BV 对齐：

```kotlin
val isPlayUrlRequest =
    request.url.encodedPath.contains("/x/player/playurl") ||
        request.url.encodedPath.contains("/x/player/wbi/playurl")

if (!request.isAppRequest && !isPlayUrlRequest) {
    // 注入 SESSDATA + buvid3 + b_nut 等
}
```

#### 11.8.3 v_voucher 风控响应处理

**问题**：B 站风控触发时返回 `{"code":0,"data":{"v_voucher":"xxx"}}`，`data` 中只有 `v_voucher` 字段，缺少 `PlayUrlData` 的必填字段（`from`/`result`/`quality` 等），导致 kotlinx.serialization 反序列化失败抛出 `SerializationException`，UI 显示原始序列化错误信息。

**解决方案**：在 `getVideoPlayUrl()` 中先解析原始 JSON，检测到 `data` 含 `v_voucher` 时直接抛出 `RiskControlException`，避免反序列化失败：

```kotlin
val rawText = response.bodyAsText()
val parsed = json.decodeFromString<JsonObject>(rawText)
val data = parsed["data"]
if (data is JsonObject && "v_voucher" in data) {
    throw RiskControlException("触发风控，请稍后再试或更换接口类型")
}
return json.decodeFromString(rawText)
```

#### 11.8.4 未登录 try_look 参数

**问题**：未登录时缺少 `try_look=1` 等参数会导致请求被拒。

**原因**：这些参数向 B 站表明"匿名预览"模式，允许未登录用户获取预览流。原版 BV 在 `sessData` 为空时添加这些参数。

**解决方案**：保留原版的条件逻辑：

```kotlin
if (sessData.isEmpty()) {
    parameter("web_location", "1315873")
    parameter("gaia_source", "pre-load")
    parameter("isGaiaAvoided", "true")
    parameter("try_look", "1")
}
```

### 11.9 bili-api 集成测试与单元测试分离

#### 11.9.1 @Tag("integration") 机制

**问题**：bili-api 模块有需要真实网络和 B 站凭证的集成测试，也有纯单元测试。最初用 Gradle `filter { excludeTestsMatching("*RepositoryTest") }` 分离，但 Gradle filter 匹配方法全限定名，与 JUnit 5 反引号测试名（含空格）不兼容，导致集成测试无法被发现。

**解决方案**：改用 JUnit 5 `@Tag` 机制：

```kotlin
// bili-api/build.gradle.kts
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}
```

所有集成测试类必须标注 `@org.junit.jupiter.api.Tag("integration")`：

```kotlin
@org.junit.jupiter.api.Tag("integration")
class VideoPlayRepositoryTest {
    // ...
}
```

**运行命令**：
- `./gradlew :bili-api:test` — 只跑单元测试（DanmakuMaskTest 等）
- `./gradlew :bili-api:integrationTest` — 只跑集成测试（需要凭证 + 网络）

#### 11.9.2 integrationTest task 配置要点

**问题**：`tasks.withType<Test>` 会匹配所有 Test 类型 task（包括自定义的 `integrationTest`），导致 `excludeTags` 和 `includeTags` 同时生效。

**解决方案**：用 `tasks.named<Test>("test")` 只配置默认 `test` task，不用 `tasks.withType<Test>`：

```kotlin
// ✅ 正确：只配置 test task
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("integration") }
}

// ❌ 错误：withType<Test> 会匹配 integrationTest，导致 tag 同时被 include 和 exclude
tasks.withType<Test> {
    useJUnitPlatform { excludeTags("integration") }
}
```

#### 11.9.3 BiliHttpApiTest 参数迁移

**问题**：`BiliHttpApi` 的接口函数（如 `getVideoPlayUrl`）迁移后移除了 `sessData`/`dedeUserID` 参数（改用 `injectCookies` 拦截器统一注入），但测试代码仍在传这些参数，导致编译错误。

**解决方案**：在 `@BeforeAll` 中设置 `BiliHttpApi` 的字段，移除调用处的参数：

```kotlin
companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
        BiliHttpApi.init(BUVID)
        BiliHttpApi.sessData = SESSDATA
        BiliHttpApi.mid = UID
    }
}
```

### 11.10 播放器多 ViewModel 协调与切集时序

#### 11.10.1 切集时弹幕/字幕不重载

**问题**：`playNewVideo` 只更新了 `PlayerViewModel` 的 uiState 和播放地址，但 `DanmakuViewModel` 和 `SubtitleViewModel` 完全不知道视频切换了，旧弹幕/字幕残留屏幕。

**原因**：5-way ViewModel 拆分后，各 ViewModel 独立，`PlayerViewModel` 无法直接调用 `DanmakuViewModel.loadDanmaku()`。UI 层（`VideoPlayerScreen`）需要充当协调者。

**解决方案**：`PlayerViewModel` 暴露 `videoSwitchEvent: SharedFlow<VideoSwitchEvent>`，UI 层收集后协调重载：

```kotlin
// PlayerViewModel
private val _videoSwitchEvent = MutableSharedFlow<VideoSwitchEvent>(extraBufferCapacity = 1)
val videoSwitchEvent = _videoSwitchEvent.asSharedFlow()

data class VideoSwitchEvent(val aid: Long, val cid: Long)

fun playNewVideo(newVideo: VideoListItem) {
    // ... 更新状态 ...
    viewModelScope.launch { _videoSwitchEvent.emit(VideoSwitchEvent(newVideo.aid, newVideo.cid)) }
}

// VideoPlayerScreen
LaunchedEffect(Unit) {
    playerViewModel.videoSwitchEvent.collect { event ->
        danmakuViewModel.clearDanmaku()
        danmakuViewModel.loadDanmaku(event.cid)
        danmakuViewModel.loadDanmakuMask(event.aid, event.cid)
        subtitleViewModel.clearSubtitle()
        subtitleViewModel.loadSubtitleList(event.aid, event.cid)
    }
}
```

#### 11.10.2 lastPlayedCid 不匹配导致断点续播错误

**问题**：多 P 视频中，B 站返回的 `history.lastPlayedCid` 是上次观看的 CID（可能是 P2），`history.progress` 是 P2 的进度。如果当前播放的是 P1，直接应用 `progress` 会把 P2 的进度 seek 到 P1 上。

**解决方案**：仅在 `historyCid == currentCid` 时才应用断点续播：

```kotlin
val historyCid = videoInfoRepository.lastPlayedCid.value
val historyTime = videoInfoRepository.lastPlayedTime.value
if (historyCid == _uiState.value.cid && historyTime > 0) {
    _uiState.update { it.copy(lastPlayed = historyTime) }
}
```

#### 11.10.3 切视频时旧 playData 泄漏

**问题**：`playNewVideo` 未清空 `playData`，新视频加载失败时 `resolveMediaUrls` 会使用旧 `playData` 返回错误 URL，导致播放旧视频流。

**解决方案**：`playNewVideo` 开头清空 `playData = null` 并重置全部 UI 状态（画质/编码/音频列表等）。

#### 11.10.4 isBuffering 错误时未清除

**问题**：`loadVideoWithResources` 的 catch 块和 `onError` 回调都未清除 `isBuffering`，加载失败后 UI 永远显示 loading 转圈。

**解决方案**：所有设置 `PlayerState.Error` 的地方同时 `isBuffering = false`。

#### 11.10.5 SubtitleViewModel 内联 HttpClient

**问题**：`SubtitleViewModel` 直接 `HttpClient(OkHttp)` 创建新实例，绕过了 Hilt DI，每次选择字幕都创建新 client，连接池无法复用。

**解决方案**：在 `NetworkModule` 中 `@Provides` 共享 `HttpClient` 单例，构造注入到 `SubtitleViewModel`。

#### 11.10.6 弹幕 play/pause/seek 未与播放器同步

**问题**：5-way ViewModel 拆分后，`DanmakuViewModel` 的 `play()`/`pause()`/`seekTo()` 从未被调用，弹幕不随播放器播放/暂停/seek 而动。

**原因**：`PlayerViewModel` 无法直接调用 `DanmakuViewModel` 方法，UI 层（`VideoPlayerScreen`）需要充当协调者。

**解决方案**：在 `VideoPlayerScreen` 中添加 `LaunchedEffect` 监听播放器状态，同步调用 DanmakuViewModel：

```kotlin
// 播放/暂停同步
LaunchedEffect(uiState.playerState) {
    when (uiState.playerState) {
        PlayerState.Playing -> danmakuViewModel.play()
        PlayerState.Paused, is PlayerState.Error, PlayerState.Ended -> danmakuViewModel.pause()
        else -> {}
    }
}

// 缓冲同步
LaunchedEffect(uiState.isBuffering) {
    if (uiState.isBuffering) danmakuViewModel.pause()
    else if (uiState.playerState == PlayerState.Playing) danmakuViewModel.play()
}

// seek 同步
onGoTime = { time ->
    playerViewModel.seekToTime(time)
    danmakuViewModel.seekTo(time)
}

// 倍速同步
onPlaySpeedChange = { speed ->
    playerViewModel.updatePlaySpeed(speed)
    danmakuViewModel.updateSpeed(speed)
}
```

#### 11.10.7 直接进播放器时 cid=0 导致 playurl 请求错误

**问题**：`showVideoInfo=false` 时点击视频卡片直接进播放器，但推荐流返回的数据不含 `cid`（或 `cid=0`），播放器用 `cid=0` 调 playurl API 返回"请求错误"。

**原因**：推荐流 API (`/x/web-interface/index/top/feed/rcmd`) 不返回 `cid` 字段，`cid` 需要从视频详情 API 获取。但 `PlayerScreens.kt` 中弹幕/字幕/playurl 在 `loadVideoDetail()` **之前**就用 `route.cid=0` 调用了。

**解决方案**：调整 `PlayerScreens.kt` 中的加载顺序 — 先 `loadVideoDetail()` 获取正确 cid，再用 `actualCid` 加载弹幕/字幕/playurl：

```kotlin
// PlayerScreens.kt LaunchedEffect 内
playerViewModel.init(aid = route.aid, cid = route.cid, ...)
playerViewModel.initVideoPlayer(context)
danmakuViewModel.init()
// 先加载详情，获取正确 cid
playerViewModel.loadVideoDetail(route.aid, route.bvid)
// 再用正确 cid 加载弹幕/字幕
val actualCid = playerViewModel.uiState.value.cid
danmakuViewModel.loadDanmaku(actualCid)
danmakuViewModel.loadDanmakuMask(route.aid, actualCid)
subtitleViewModel.loadSubtitleList(route.aid, actualCid)
// 最后加载播放流
playerViewModel.loadVideoWithResources()
```

同时 `PlayerViewModel.loadVideoDetail()` 必须更新 `_uiState.cid`：

```kotlin
suspend fun loadVideoDetail(aid: Long, bvid: String = "") {
    videoInfoRepository.loadVideoDetail(aid, getApiType(), bvid)
    videoInfoRepository.videoDetail.value?.let { detail ->
        _uiState.update {
            it.copy(
                cid = detail.cid,  // 更新为正确 cid
                authorMid = detail.author.mid,
                authorName = detail.author.name,
            )
        }
    }
    // ... 断点续播逻辑 ...
}
```

#### 11.10.8 播放器→详情页路由应 popBackStack 而非 navigate

**问题**：播放器中点击"详情"按钮会 `navigate(VideoDetailRoute)` 新建一个详情页路由，导致路由堆栈中出现：详情页 → 播放器 → 详情页。

**原因**：`showVideoInfo=false` 时用户从详情页进入播放器，播放器就在详情页路由之上。再 `navigate` 会创建第二层详情页。

**解决方案**：`onGoToVideoDetail` 改为 `popBackStack()`，返回已有的详情页而非新建：

```kotlin
onGoToVideoDetail = {
    navController.popBackStack()
}
```

### 11.11 TV Material3 触屏适配

#### 11.11.1 tvClickable/tvSelectable 不处理触屏事件

**问题**：TV Material3 的 `tvClickable()` / `tvSelectable()` 仅处理 D-Pad Enter 键事件，不含 `pointerInput`，触屏点击完全无效。

**原因**：TV Material3 设计目标是 D-Pad 遥控器，触屏手势需要额外补充。

**解决方案**：`core/focus/TouchClickable.kt` 提供 `Modifier.touchClickable()` 扩展：

```kotlin
fun Modifier.touchClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val hasLongClick = onLongClick != null
    val focusRequester = remember { FocusRequester() }

    this
        .focusRequester(focusRequester)
        .pointerInput(hasLongClick) {
            detectTapGestures(
                onTap = {
                    runCatching { focusRequester.requestFocus() }
                    currentOnClick()
                },
                onLongPress = if (hasLongClick) { { ... } } else { null },
            )
        }
}
```

**关键要点**：

1. **不能用同一 modifier 链叠加多个 `pointerInput`**：`clickable` + `pointerInput(detectTapGestures)` 会导致内层消费 DOWN 事件，外层跳过。只能用单个 `pointerInput`
2. **`composed` + `rememberUpdatedState`**：确保回调始终为最新值
3. **触屏点击请求焦点**：通过 `FocusRequester.requestFocus()` 使焦点驱动逻辑（如 Tab 切换）在触屏模式下正常工作
4. **使用方式**：在 TV Material3 组件的 `onClick` 参数外，同时添加 `Modifier.touchClickable(onClick = ...)` 补充触屏支持

#### 11.11.2 Vector Drawable tint 不解析

**问题**：原版 BV 的 vector drawable XML 带有 `android:tint="?attr/colorControlNormal"`，在 Compose 中不解析为正确颜色，图标不可见。

**解决方案**：在 `Icon` 组件上显式指定 `tint = Color.White`：

```kotlin
Icon(
    painter = painterResource(id = iconRes),
    contentDescription = ...,
    tint = Color.White,  // 显式指定，不依赖 XML 中的 tint
)
```

#### 11.11.3 播放器设置菜单三级展开需手动更新焦点状态

**问题**：设置菜单（画质/弹幕/字幕）的二级菜单点击展开三级菜单时，三级菜单不可见或焦点不正确。

**原因**：二级菜单的 `onClick` 只展开了三级菜单的可见性，未通知父组件焦点状态变化。

**解决方案**：在二级菜单的 `onClick` 中增加 `onFocusStateChange(MenuFocusState.Items)`：

```kotlin
// PictureMenu.kt / DanmakuMenu.kt / ClosedCaptionMenu.kt
MenuItem(
    title = ...,
    onClick = {
        // 展开三级菜单
        onExpandChange(...)
        // 通知父组件焦点进入 Items 状态
        onFocusStateChange(MenuFocusState.Items)
    },
)
```

#### 11.11.4 视频铺满溢出

**问题**：播放器使用 `RESIZE_MODE_FILL` + `.fillMaxSize().aspectRatio()`，视频被拉伸溢出屏幕。

**原因**：原版 BV 用 `RESIZE_MODE_FILL` + `.fillMaxHeight().aspectRatio()` 约束尺寸。newBV 误改为 `.fillMaxSize().aspectRatio()` 导致尺寸约束失效。

**解决方案**：改用 `RESIZE_MODE_FIT` + `.fillMaxSize()`，让 `PlayerView` 自己处理 letterbox：

```kotlin
// BvVideoPlayer.kt
resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

// VideoPlayerScreen.kt
BvVideoPlayer(
    modifier = Modifier.fillMaxSize(),  // 不再手动 aspectRatio
    videoPlayer = videoPlayer,
)
```

### 11.12 直播播放器与弹幕 WebSocket

#### 11.12.1 HLS 直播流 #EXT-X-START 导致 BEHIND_LIVE_WINDOW

**问题**：直播播放几秒后报 `ERROR_CODE_BEHIND_LIVE_WINDOW` 错误。

**原因**：B 站直播 HLS playlist 包含 `#EXT-X-START` 标签，ExoPlayer 解析后会 seek 到该标签指定的偏移位置（通常是直播流开头），导致播放位置落后于直播窗口。

**解决方案**：
1. 自定义 `HlsPlaylistParserFactory`，在解析前剥离 `#EXT-X-START` 行
2. 在 `ExoMediaPlayer.onPlayerError()` 中检测 `ERROR_CODE_BEHIND_LIVE_WINDOW`，自动调用 `seekToDefaultPosition()` + `prepare()` 恢复（内部处理，不暴露到抽象层）

```kotlin
// ExoMediaPlayer.kt
override fun onPlayerError(error: PlaybackException) {
    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
        mPlayer?.seekToDefaultPosition()
        mPlayer?.prepare()
        return
    }
    mPlayerEventListener?.onError(error)
}
```

#### 11.12.2 getDanmuInfo 需要 WBI 签名

**问题**：`getLiveDanmuInfo` 返回 `code=-352`（风控），弹幕 WebSocket 无法获取 token。

**原因**：B 站对 `/xlive/web-room/v1/index/getDanmuInfo` 接口要求 WBI 签名（`w_rid` + `wts`），但该路径不含 `wbi` 关键字，`encApiSign()` 拦截器不会自动签名。

**解决方案**：
1. 在 `encApiSign()` 的 `isWbiRequest` 判断中显式添加 `getDanmuInfo` 路径
2. 请求参数添加 `type=0` 和 `web_location=444.8`

```kotlin
// ApiSign.kt
val isWbiRequest =
    request.url.encodedPath.contains("wbi") ||
        request.url.encodedPath.contains("/xlive/web-room/v1/index/getDanmuInfo")

// BiliLiveHttpApi.kt
client.get("/xlive/web-room/v1/index/getDanmuInfo") {
    parameter("id", roomId)
    parameter("type", 0)
    parameter("web_location", "444.8")
}
```

#### 11.12.3 Ktor WebSocket 发送 auth 后立即 EOF

**问题**：WebSocket 连接成功，发送 auth 包后服务器立即断开（`java.io.EOFException`）。

**原因**：Ktor 的 WebSocket 插件对 HTTP 升级请求的头注入支持不完善，`BiliUserAgent` 插件的 `onRequest` 回调可能不作用于 WebSocket 握手请求，导致缺少 `Referer` / `Origin` 头，B 站服务器拒绝连接。

**解决方案**：放弃 Ktor WebSocket，改用 OkHttp `WebSocket` API 直接构建请求，显式设置 `Referer: https://live.bilibili.com/` 和 `Origin: https://www.bilibili.com`（注意 Origin 是 `www.bilibili.com` 不是 `live.bilibili.com`）：

```kotlin
val request = Request.Builder()
    .url(url)
    .header("User-Agent", webUserAgent)
    .header("Referer", "https://live.bilibili.com/")
    .header("Origin", "https://www.bilibili.com")
    .build()
wsClient.newWebSocket(request, listener)
```

#### 11.12.4 弹幕 WebSocket auth 响应未校验

**问题**：auth 失败时不报错，心跳照样发送，但收不到弹幕。

**解决方案**：解析 `OP_AUTH_REPLY`（type=8）响应体 JSON，校验 `code == 0` 后才启动心跳。认证失败则不发心跳，等待重连。

#### 11.12.5 WebSocket 帧包含多个协议包

**问题**：B 站弹幕服务器会将多个协议包拼接在同一个 WebSocket 帧中发送，只读第一个包会丢失后续弹幕。

**解决方案**：在 `handlePacketBytes` 中循环读取，按 `packetLength` 偏移，直到帧数据耗尽。压缩包（protover 2/3）解压后递归处理内嵌包。
