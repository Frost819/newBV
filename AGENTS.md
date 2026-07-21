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
./gradlew jacocoTestReport

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
./gradlew jacocoTestReport
# 报告位置：build/reports/jacoco/jacocoTestReport/html/index.html
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

```kotlin
// Repository 返回 sealed Result
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Throwable>
}

// ViewModel 处理
fun loadRecommend() {
    viewModelScope.launch {
        when (val result = repository.getRecommend()) {
            is Result.Success -> _uiState.update { it.copy(items = result.data) }
            is Result.Error -> _uiState.update { it.copy(error = result.exception) }
        }
    }
}
```

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
