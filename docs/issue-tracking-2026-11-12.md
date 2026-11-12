# Issue / PR 跟踪清单（2026-11-12）

> 调查基线：`main` @ `db182d45`；对比已发布版本 `v1.2.0` @ `a660c6f9`。
> 范围：全部未回复/待处理的 issue 与 PR，**不含 #303**（隧道播放黑屏，另行跟进）。
> 本文档只做调查结论与修复计划，不代表已完成修复。所有 `file:line` 均基于上述基线。

---

## 0. 总览

| # | 类型 | 标题 | 结论 | 优先级 | 状态 |
|---|---|---|---|---|---|
| [#333](https://github.com/Frost819/newBV/issues/333) | Bug | 屏显参数音频码率错 1000 倍、无视频码率 | 已定位：单位未换算 + 未渲染视频码率 | P1 | ☐ 待修 |
| [#332](https://github.com/Frost819/newBV/issues/332) | Feature | 自定义键打开评论后无法再按关闭 | 已定位：动作只 open + Dialog 窗口截获按键 | P0 | ☐ 待修 |
| [#330](https://github.com/Frost819/newBV/issues/330) | Bug | 索尼 4K 画面破碎 | 代码侧可解释；根因需真机取证，可先加规避开关 | P1 | ☐ 待取证 |
| [#328](https://github.com/Frost819/newBV/issues/328) | Bug | 索尼 OOM 崩溃 | 主因：v1.2.0 未含 `83f05240` 缓冲上限修复，且低堆设备仍会选 4K | P0 | ☐ 待发版/修 |
| [#324](https://github.com/Frost819/newBV/issues/324) | Bug | 特殊电视打开「编解码信息」闪退 | 已定位：`getCapabilitiesForType` 裸调用 + 无逐项容错 | P0 | ☐ 待修 |
| [#321](https://github.com/Frost819/newBV/issues/321) | Bug | 字幕无法分辨 AI 生成 | 已在 `a8194338` 修复，但**未发布** | P2 | ☐ 待回复/发版 |
| [#285](https://github.com/Frost819/newBV/issues/285) | Bug | 加载慢/加载失败（IPv6 线索） | 已定位机制：无 IPv4 偏好 + 每地址 10s 串行连接 + 上层 10s 超时 | P0/P1 | ☐ 待修 |
| [#325](https://github.com/Frost819/newBV/pull/325) | PR | 媒体首包补 Range + 放宽缓冲 | 方向对，但 seek 会发两条 Range、直播未验证、缓冲改动与 #328 相反 | — | ☐ 需修改 |
| [#331](https://github.com/Frost819/newBV/pull/331) | PR | 底部控制栏清晰度快切 | 方向对，5 项必须修改（含必红测试、弹窗期控制栏被隐藏） | — | ☐ 需修改 |

### 最高杠杆动作

**先发 `v1.2.1`。** `main` 上有 25 个提交未发布，其中直接对应本次 issue 的修复：

| 提交 | 内容 | 对应 issue |
|---|---|---|
| `83f05240` | 缓冲策略按设备堆上限自适应（20s/32MiB） | #328 |
| `a8194338` | 字幕菜单补充 AI 字幕标注 | #321 |
| `656a06a2` | 移除 jsoup，修复 Android 7.x PGC 轮播崩溃 | #320（已回复） |
| `2066a309` | 直播间 UP 主页入口与关注按钮 | #317（已回复） |
| `38db0796` | 仅网络类错误才切换 CDN | 播放稳定性 |
| `364f1636` 等 | 弹幕屏蔽体系 | — |

`git tag --contains 83f05240` 为空，`git show v1.2.0:...ExoMediaPlayer.kt | grep LoadControl` 无命中——**#328 用户当前版本完全没有缓冲上限保护**，这是唯一能从日志曲线（14MB → 155MB → 191MB/192MB）解释其 OOM 的改动。发版即可覆盖 #321/#328 的一半。

---

## 1. #333 屏显参数：音频码率错 1000 倍 + 无视频码率

### 现象
开启「设置 → 播放 → 播放器调试信息」后，浮层显示 `audio: 192000 kbps`（实际 192 kbps），且没有视频码率行。

### 根因（已核实）
- `player/src/main/kotlin/dev/frost819/newbv/player/impl/exo/ExoMediaPlayer.kt:287`
  ```kotlin
  audio: ${mPlayer?.audioFormat?.bitrate ?: 0} kbps
  ```
  Media3 `Format.bitrate` 单位是 **bps**，直接拼 `kbps` 导致数值大 1000 倍。
- `ExoMediaPlayer.kt:288` 只有 `video codec:`，**没有 `video:` 码率行**；`DashVideo.bandwidth` 在 `app` 模块从未被读取（全模块 grep 无命中）。
- 该写法继承自原版 BV（`bv/bv-player/.../ExoMediaPlayer.kt:190`），不是迁移引入。
- 直播浮层是另一套字符串（`LivePlayerViewModel.kt:298-327`），**不含码率**，因此不会出现 1000 倍错误，但同样缺码率。

### 修复方案
1. `player/.../Extends.kt` 新增纯函数（与既有 `formatMinSec` 同风格）：
   ```kotlin
   fun Int.formatBitrateKbps(): String =
       if (this <= 0) "未知" else "${this / 1000} kbps"
   ```
2. `ExoMediaPlayer.debugInfo`：音频改用该函数，并补 `video:` 行。
   - **保留** `video codec:` / `audio codec:` 两行——`player/src/androidTest/.../ExoMediaPlayerTest.kt:142-143`、`ExoMediaPlayerStreamTest.kt:244` 断言了这两个字符串。
3. 可选（P2）：直播浮层补码率（允许「未知」，FLV/HLS 常无 `AVERAGE-BANDWIDTH`）。

### 测试
- `player/src/test/.../ExtendsTest.kt`：`192_000 → "192 kbps"`、`0 / -1 → "未知"`、`999 → "0 kbps"`。
- 插桩：`ExoMediaPlayerStreamTest` 断言 `debugInfo` 含 `video: \d+ kbps`，且不含 6 位以上数字 + `kbps`。

### 风险
- `Format.bitrate` 是 **peak 优先**，可能与 B 站 `bandwidth`（平均）不同；如需与 B 站显示一致，需另做 P2 方案（从 `DashVideo/DashAudio.bandwidth` 传值）。
- 试看/durl 流 `bandwidth = 0`，必须走「未知」分支。

---

## 2. #332 自定义键「打开评论」无法再按关闭

### 根因（两层，缺一不可）
1. **状态层**：`app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/player/VideoPlayerScreen.kt:302-308` 的 `onShowComments` 是幂等 open（无条件 `showCommentsDialog = true`），没有 toggle。
2. **事件路由层**：评论面板是 `Dialog`（`CommentsDialog.kt:205-208`），Android 上是独立 Window。自定义快捷键只挂在播放器根节点（`VideoPlayerController.kt:395-403` 的 `onPreviewKeyEvent`），Dialog 打开后按键只派发给 Dialog 窗口，**永远到不了播放器分发层**。面板内只有 `BackHandler`（`CommentsDialog.kt:209`）。

排除项：
- 不是 `showClickableControllers` 提前放行（该集合 `VideoPlayerController.kt:115-122` 不含评论弹窗）。
- 不能改回 `AnimatedVisibility`（AGENTS §11.2.1 要求弹窗必须用 Dialog，否则焦点穿透）。

### 修复方案
1. `VideoPlayerScreen.kt` 抽 `openComments()` / `dismissComments()`，`onShowComments` 改为按 `showCommentsDialog` toggle；`onDismiss` 复用 `dismissComments()`（避免 `resumeAfterComments` 两套恢复逻辑）。
2. `CommentsDialog.kt` 增加可选参数 `onShortcutAction: ((PlayerCustomShortcutAction) -> Unit)?`，在根 `Box` 挂 `Modifier.playerCustomShortcutKeyInterceptor(...)`（新增文件，放 `ui/component/player/`）：
   - 命中绑定键且 `repeatCount == 0` 的 KeyDown → 回调；
   - KeyUp 与长按重复 → 消费但不触发；
   - 未绑定键 → 返回 false（不影响 BackHandler 与 D-pad）。
3. 调用侧：`onShortcutAction = { if (it == OpenComments) dismissComments() }`。
4. 同一根因的连带项（P2）：`OpenInteraction`（`VideoPlayerScreen.kt:295-301` + `:376-392`）存在同样问题，建议一并修。

### 注意
- **不要**把 `showCommentsDialog` 加进 `showClickableControllers`，否则所有自定义快捷键失效。
- 长按必须按 `repeatCount != 0` 忽略，否则反复开关闪烁。
- 评论图片查看层是第二个嵌套 Dialog（`CommentsDialog.kt:761`），若要求完整覆盖需再挂一次拦截器。

### 测试
- 单测（P1）：把 `VideoPlayerController.kt:289-299` 的判定抽成纯函数 `resolveShortcutKey(...)`，覆盖 KeyUp / repeat / 未绑定 / 空表。
- 插桩：`Box(Modifier.playerCustomShortcutKeyInterceptor { ... }) { Button() }` + `performKeyInput`，验证「焦点在子节点、拦截器在祖先」这一关键路由假设。

---

## 3. #330 索尼 4K 画面破碎（两个现象）

> 结论分「代码可确认」与「需真机验证」，不要直接把推测当根因回复用户。

### 可确认的代码事实
- 视频层：`player/.../BvVideoPlayer.kt:27-43`，`PlayerView` 默认 **SurfaceView**，`resizeMode = RESIZE_MODE_FILL`，未开隧道播放、无 TextureView 路径、无 `setZOrder*` / `PixelCopy`。
- 尺寸来自**接口声明**的宽高：`VideoPlayerScreen.kt:343-357` 用 `fillMaxHeight().aspectRatio(uiState.videoWidth / videoHeight)`；直播则用播放器实际 `videoWidth/Height`（`LivePlayerScreen.kt:113-121`）。
- 竖屏视频必然经历一次「16:9 → 9:16」的 Surface 尺寸重排（`PlayerViewModel.kt:1309` 解析完 URL 才写入宽高，而 `BvVideoPlayer` 已以默认比例创建）。
- 能力判定只查 mime/profile/level/宽高×帧率，**不查码率、不查色彩**：`app/.../util/VideoCapability.kt:75-93` 构造的 `Format` 无 `bitrate` / `colorInfo`；Media3 `MediaCodecInfo` 本身也不校验 `bitrateRange`。
- 自动降档**只由错误码触发**：`player/.../PlaybackErrors.kt:39-47` 把解码类错误码视为不支持，`PlayerViewModel.kt:1039-1081` 才回退。**花屏不产生异常**，所以既不降档也不提示。
- 全仓库无竖屏相关逻辑（`app`/`player` 内 grep 无 `isVerticalVideo` / portrait 处理）；上游历史提交 `5fe4e1a0` 有 `PortraitVideoFixMode`（Limit 1080P / TextureView），main 未移植；未合并分支 `4fef59cf` 有隧道播放开关实验。
- 「调出进度条」本身不重建视频 Surface、不截屏、不改尺寸（`VideoPlayerController.kt:395-458`、`ControllerVideoInfo.kt:130-177`，`PlayerView.setPlayer` 同实例早退）。

### 优先向用户索取（决定后续方向）
1. 复现视频 BV 号 +「播放器调试信息」截图（分辨率/编码）。
2. 设置 → 编解码信息里 `video/hevc` / `video/avc` 硬解解码器的码率范围与 `3840x2160` 帧率截图。
3. 关键二分：只看进度条不 seek 是否破碎；4K **横屏**是否也坏；关弹幕后是否改善。
4. logcat（含 `MediaCodecVideoRenderer` / `CCodec` / `SurfaceFlinger` / `[PLAYBACK]`）。

### 建议动作
- **P0（取证）**：给用户 1080P 规避 + 按上面清单收集信息；同时加诊断日志：
  - `ExoMediaPlayer` 加 `EventLogger`（一次性拿到解码器选择/Format/首帧/丢帧）；
  - `debugInfo` 补 `videoFormat.bitrate/codecs/colorInfo`、`videoSize + rotation`、`videoRendererName`；
  - `BvVideoPlayer` 记录 Surface 尺寸变化；
  - `PlayerViewModel.resolveMediaUrls` 打印选中 track（qn/codec/width/height/frameRate/bandwidth）。
- **P1（规避开关）**：新增「竖屏视频最高 1080P」开关（参考 `5fe4e1a0`），默认关；在 `PlayerViewModel.loadPlaybackConfig` 里对竖屏且 qn ≥ 4K 封顶 1080P60。
- **P1（消除重排）**：`VideoAspectRatio.Default` 改 `fillMaxSize` + `RESIZE_MODE_FIT`（与 AGENTS §11.11.4 记录一致，当前代码是回归点），需回归 4:3 留白与比例设置。
- **P2**：能力判定纳入码率/竖屏；`VideoCodec` 补 `dvhe` 前缀；`setEnableDecoderFallback(true)`；弹幕关闭时真正移除弹幕 View（当前 `alpha=0` 仍参与合成，`VideoPlayerScreen.kt:360-372`）。

---

## 4. #328 索尼电视 OOM

### 日志事实
- 三份崩溃均为 `OutOfMemoryError: Failed to allocate a 8208 byte allocation ... max allowed footprint 201326592`，发生在不同线程（OkHttp / Compose / TaskRunner）→ live set 顶满，而非单点大对象泄漏。
- 第 2 份曲线：进入播放器时 14MB/29MB → 50 秒内涨到 155MB/179MB → 191MB/192MB，之后每轮 GC 只回收 0~318KB；`Skipped 497 frames`。
- 实际播放的是 **4K AVC**（默认「杜比视界 + H265」回退而来）：`MtkACodecPlugin: 4k contents`、`nativeWindow 3840x2160`。
- 弹幕：整片仅 4807 条、当前切片 8 条、无 `OOM Pool` 日志 → 不是本次主因（但仍是低端机风险项）。

### 逐条核实用户/AI 可疑点

| 可疑点 | 判定 | 依据 |
|---|---|---|
| a. 弹幕缓存池 50MB 占 26% 堆 | **代码事实成立，本 issue 主因不成立** | `danmaku-engine/.../DanmakuConfig.kt:204-211` `(maxMemory/6).coerceAtLeast(50MB)`；但它是「已释放缓存上限」而非常驻，且 Android 8+ Bitmap 像素在 native 堆 |
| b. `updateConfig` Float `!=` 触发 4 代失效且每帧 | **部分成立** | `DanmakuSystem.kt:51-53` 每帧调用，但 `:55-56` 有 `newConfig` 空判断；`:70` 确实精确比较；日志中 `1.0 → 1.0000007` 只出现一次（菜单 0.01 累减 75 次产生误差） |
| c. `PlayerScreens` 异步详情后同步读 cid/lastPlayed 竞态 | **不成立** | `loadVideoDetail` 是 suspend 且顺序等待（`PlayerScreens.kt:69-84`）；日志中续播 1492s 与弹幕第 5 段一致，证明取值正确 |
| d. `replayMedia` 不 stop/clear 会叠资源 | **前半成立、后半不成立** | `PlayerViewModel.kt:921-932` 确实未 stop；但 Media3 `setMediaSource` 会先移除并释放旧 source，不累积 |
| e. 无 `onTrimMemory` / `largeHeap` | **成立** | 全仓 grep 为空；Coil 内部自带 trim，但自有内存（弹幕池/VideoShot）无释放钩子 |

### 真正的主因（本次最大发现）
**v1.2.0 没有 LoadControl 配置，走 Media3 默认「最多缓冲 50 秒」策略；该设备 Java 堆仅 192MB。** 修复提交 `83f05240` 不在 v1.2.0 tag 中（已验证），日志内存曲线与默认策略完全吻合。

### 修复清单
| 优先级 | 动作 | 位置 | 说明 |
|---|---|---|---|
| P0 | **发 v1.2.1**（含 `83f05240`） | 发布流程 | 192MB → 预算 32MiB / 20s / 回看 0；预计回收 60–100MB |
| P0 | 低堆设备画质上限 | `app/.../util/PlaybackSelection.kt:83-116`、`PlayerViewModel.kt:1195-1240` | `maxMemory < 256MB` 或 `isLowRamDevice` 时封顶 1080P60；用户手动选 4K 给提示 |
| P1 | 弹幕缓存池按堆比例 | `DanmakuConfig.kt:204-211`、`CacheManager.kt:65` | 改 `(maxMemory/16).coerceIn(4MB, 24MB)`；避免多播放器各 50MB |
| P1 | 加 `onTrimMemory` 释放自有内存 | `BVApplication.kt`、`DanmakuPlayer`/`CacheManager` | 新增 `trimCache()`，只清缓存池，不要走 `WORKER_MSG_RELEASE`（不可复用） |
| P1 | `updateConfig` epsilon / 合并失效 | `DanmakuSystem.kt:60-96`、`StepLessMenuItem.kt:66-75`、`DanmakuViewModel.kt:761-764` | `abs(a-b) > 1e-3f`；菜单侧 scale 吸附到 0.01 |
| P1 | 换源前显式 `stop()` | `PlayerViewModel.kt:921-932, 1314-1322`、`ExoMediaPlayer.kt:200-217` | 确定性释放；另修 `ExoMediaPlayer.kt:371-376` 复用 `mMediaSource` 的隐患 |
| P2 | Coil 显式内存上限 | `BVApplication.kt:154-163` | 默认约 28–38MB，可压到堆的 8% |
| P2 | 长视频弹幕分段淘汰 / 关闭弹幕时停引擎 / release 收敛引擎日志 | `DataSystem.kt`、`DanmakuViewModel.kt:594-601` | 长尾优化 |

> `largeHeap` **不建议**作为主方案：堆变大但 GC 停顿与 LMK 风险同时上升。

### 验证
- 让用户升级后播同一条 `BV1Ntxr6xEjG` 4K60 30 分钟，抓 `dumpsys meminfo`（Java/Native/Graphics）与 `MB/192MB` 日志；`dumpsys media.codec` 确认 codec client 不增长。

---

## 5. #324 特殊电视打开「编解码信息」闪退

### 根因（已核实）
`app/src/main/kotlin/dev/frost819/newbv/app/util/CodecUtil.kt`：
- `:55` `codecInfo.getCapabilitiesForType(codecInfo.supportedTypes.first())` —— AOSP 在内部表缺失时明确抛 `IllegalArgumentException`；厂商 ROM 声明了 type 但 native 无 caps 时必抛。`supportedTypes` 为空还会 `NoSuchElementException`。
- `:58` `capabilities.mimeType`（可为 null → NPE）、`:68` `capabilities.colorFormats.toList()`（个别厂商返回 null → NPE）。
- `:20` 整表 `.map { fromCodecInfo(it) }` 无逐项容错，**一个坏 codec 让整页失败**。
- `MediaCodecScreen.kt:71-78` 加载无 try-catch，且在 `Dispatchers.IO` 抛异常 → 进程被杀（全局 `CrashHandler` 只记录后转发原 handler，仍闪退）。

### 修复方案（P0，改动面小）
1. `CodecUtil.parseCodecs()` 外层 `runCatching` + 逐 codec `mapNotNull { runCatching { ... } }`，失败打 warn（记录 codec 名，便于真机取证）。
2. `fromCodecInfo` 内部对 `supportedTypes.firstOrNull()`、`getCapabilitiesForType`、`mimeType`、`colorFormats` 逐项兜底（缺失回退 `type` / `emptyList()`）。
3. `MediaCodecScreen` 改用 `withContext(Dispatchers.IO) { runCatching { ... } }` 回主线程赋值，失败显示可读错误 + 重试，而不是空列表。
4. P1：字段缺失显示「未知」而非 `null`；帧率矩阵改为选中项按需计算（当前每个 codec 调 `getCapabilitiesForType` 15 次，弱机首进页数秒无响应）。

### 测试
- 抽纯函数：`classifyCodecMode(name, isAudio, isSoftwareOnly)`、`fromRaw(...)`、`resolutionName(Int)`。
- 注入式容错测试：fake source 的第 2 个 codec 抛异常，断言整表仍返回 n-1 项。
- 插桩：真机跑 `parseCodecs()` 不抛且列表非空。

### 回复要点
请用户复现后走「设置 → 日志」导出 `logs_crash_*.log`（修复后即便仍异常也会记录被跳过的 codec 名）。

---

## 6. #321 字幕无法分辨是否 AI 生成

### 结论：已修复，但**未发布**
- 修复提交 `a8194338`（2026-11-03）不在 v1.2.0 中（已验证）。
- 判定字段：Web `/x/player/v2` 的 `subtitles[].type` 与 App gRPC `DmViewReply` 的 `SubtitleItem.type`，均映射到 `Subtitle.type`（`bili-api/.../entity/video/Subtitle.kt:32-37, 59-64`）。
- 渲染唯一入口：`app/.../ui/component/player/menu/ClosedCaptionMenu.kt:107-117`，`type == SubtitleType.AI` 时追加 `(AI)`；已有单测 `SubtitleTrackLabelTest`（4 例）。
- 多语言、直播、番剧（播放器仍是占位页）均无遗漏入口。

### 可选加固（P2，若用户升级后仍有问题再做）
1. 兜底判定：`type == AI || aiType == Translate || lang.startsWith("ai-")`。
2. `VideoMoreInfo.kt:200-206` 的 `type/ai_type/ai_status` 加默认值 `= 0`，避免缺字段导致**整份字幕列表为空**。
3. 后缀剥离改正则，兼容繁体/半角括号；`(AI)` 建议前置为 `[AI] 中文`，避免 200dp 宽 + 单行省略把标记截掉。

### 回复要点
「已在 `a8194338` 修复，请升级到下一个版本验证；若仍无法区分，请附 BV 号与日志，便于核对接口返回的 `type/ai_type/lang`。」

---

## 7. #285 加载慢 / 加载不完整 / 加载失败（IPv6）

### 用户最新线索
路由器分配了 IPv6 地址但 IPv6 出口异常，B 站优先 IPv6 → 一直超时；关闭路由器 IPv6 后恢复正常；建议增加「仅用 IPv4」开关。

### 机制链（代码已确认）
1. **没有任何 DNS / IPv4 偏好**：全仓无自定义 `okhttp3.Dns`，8 个独立 client 各自默认 `Dns.SYSTEM`（主 HTTP、直播、Passport、字幕、播放器媒体、直播 WS、Coil、GitHub 更新）。
2. **连接超时按「每地址 10s」串行**：`bili-api/.../http/BiliHttpApi.kt:154` `CONNECT_TIMEOUT_MS = 10_000L`；OkHttp 按 DNS 返回顺序逐地址建连，IPv6 黑洞吃满 10s 才轮到 IPv4。
3. **App 层 `withTimeout` 恰好也是 10s**：`HomeViewModel.kt:30`、`UgcViewModel.kt:22`、`LiveHomeViewModel.kt:34`、`DanmakuViewModel.kt:775` 等 → 不是「慢 10s」，而是**在 10s 处直接取消并显示加载失败**。
4. **gRPC 更差**：`bili-api/.../grpc/utils/Channel.kt:32-45` 无 socket 连接超时，`:23` 统一 20s deadline → IPv6 黑洞吃满 20s，`InternalSubchannel` 的换地址逻辑来不及执行。
5. 次生放大器：WBI 冷启动会串行发两次 nav（`ApiSign.kt:137-138` + `BiliHttpApi.kt:2005-2026`，无 Mutex/单飞）；App 推荐过滤后翻页超时被静默吞掉（`HomeViewModel.kt:166-168`）→「只有几条、没有报错」。

### 修复计划
| 优先级 | 动作 | 位置 |
|---|---|---|
| P0 | 统一连接超时 10s → 3s（读/写保持 15–20s） | `BiliHttpApi.kt:154`、`NetworkModule.kt:357`、`OkHttpUtil.kt:73-90`、`BVApplication.kt:154-164`、`LiveDataWebSocket.kt:69-74` |
| P0 | 全部 OkHttp client 挂「IPv4 优先」Dns（v4 优先、v6 兜底，避免 IPv6-only 网络变砖） | 新增 `bili-api/.../util/Ipv4FirstDns.kt`；挂载点上表 7 处 |
| P0 | gRPC 连接超时 3s（用 `OkHttpChannelBuilder` + 自定义 socketFactory），3s 失败后可立刻切 IPv4 | `Channel.kt:32` |
| P1 | 「仅使用 IPv4」设置项（默认关，重启生效） | `PrefKeys.kt:38-41`、`Prefs.kt:232-250`、`OtherSetting.kt:47-88`、`BVApplication.kt:78-100` |
| P1 | 修 WBI 冷启动双往返（Mutex + 共享 Deferred） | `ApiSign.kt:137-138`、`BiliHttpApi.kt:2005-2026` |
| P2 | 收敛 8 个 client 为 1–2 个；网络变化监听；诊断入口（展示解析结果/地址族/请求耗时） | 架构级 |

### 注意
- 不要单纯把 `LOAD_TIMEOUT_MS` 调大：只是把「失败」变「慢」，且 gRPC 20s 仍会撞墙。
- 不要开 `retryOnTimeout`：IPv6 黑洞会变成 3×20s 放大。
- 不建议为 `fastFallback` 升级 OkHttp 5：与 ktor 3.1.3 / media3 1.8 / coil 3.1 传递版本绑定冲突。
- `bili-api` 是纯 JVM 模块，不能反向依赖 `data.Prefs`，开关需通过 `BiliNetworkConfig` 全局下发。

### 验证
`ping6` 丢包 + `ping` 正常复现；加 OkHttp `EventListener` 打印实际地址族与耗时；验收：IPv6 黑洞下首页首屏 < 2s、详情 < 3s、起播 < 5s。

---

## 8. PR #325 评审结论（需修改）

### 核心问题：seek 时会发出**两条** `Range`
PR 假设「media3 会用自身计算的 Range 覆盖同名默认头」，在 media3 1.8.0 上不成立（已核对依赖源码）：

`OkHttpDataSource.java`：
- `:397` 默认头走 `builder.header(...)`（替换语义）；
- `:402` media3 自己的 Range 走 `builder.addHeader(HttpHeaders.RANGE, ...)`（**追加**语义）。

因此 `position != 0` 的 seek 请求会同时带 `Range: bytes=0-` 与 `Range: bytes=<pos>-`。服务端可能返回 `multipart/byteranges`、只取第一条（数据错位）、或 400/416。作者只验证了 EdgeOne + 点播，国内镜像/Akamai 的 seek 未验证；一旦 4xx 还会被 `38db0796` 的「网络类错误才切 CDN」误判为线路故障。

**改法**：不要依赖默认头覆盖，改为包装 `HttpDataSource`，仅在 `position == 0 && length == LENGTH_UNSET` 时 `setRequestProperty("Range", "bytes=0-")`，否则 `removeRequestProperty`。

### 其他必须项
2. **直播路径也带上了 Range**：`LivePlayerViewModel.initVideoPlayer()` 与点播共用 `ExoMediaPlayer`，而 `LiveRepository` 默认优先 `http_stream`（FLV 无限流）。需给 `VideoPlayerOptions` 加开关，直播关闭；或补直播真机验证 + 用例。
3. **缓冲改动与 #328 方向相反**：192MB 堆下 `heap/6 = 32MiB`，所以 16→32MiB 的 floor **无效**，真正生效的是 20s→60s；字节预算又是硬闸门，1080P 常驻样本会从 ~15MB 涨到 32MiB（+15~17MB）。建议拆分独立 PR，并附 192MB 设备实测。
4. **`MAX_BUFFER_TIERS` 三档同值后是死代码**，改单常量；`PlayerLoadControl.kt:14-16, 26, 80` 的 KDoc 已过期（AGENTS §3.1.3 禁止注释说谎）。
5. **测试没覆盖契约**：`range value starts from zero and keeps open end` 是自证式断言；需用 MockWebServer / fake `Call.Factory` 断言 `headers.values("Range")` 只有一条（`position>0` 用例当前必红）。

### 结论
方向正确、根因可信，但**不能直接合并**；修掉 Range 注入方式、直播策略、拆分缓冲改动、补请求头级测试后可合。

---

## 9. PR #331 评审结论（需修改）

### 必须修改
| 编号 | 问题 | 位置 |
|---|---|---|
| M1 | 文字按钮没有 `contentDescription`，`onNodeWithContentDescription("清晰度")` **必然失败** | `ControllerVideoInfo.kt:495-503`（Text 分支）；测试 `ControllerVideoInfoBottomTest.kt:95-101` |
| M2 | 隐藏用例是空断言（本来就没有该语义） | `ControllerVideoInfoBottomTest.kt:103-109` 改断言 `onNodeWithText("1080P").assertDoesNotExist()` |
| M3 | 弹窗打开期间 5s 自动隐藏照跑；关闭弹窗后整个控制栏消失，`onInteraction()` 续期无效 | `rememberControllerAutoHide(visible = showInfoSeekController && !showQualityDialog)` |
| M4 | KDoc 声称「点击遮罩关闭」实际不可用（`usePlatformDefaultWidth=false` + `fillMaxSize`，outside 永远判不到），触屏无返回键时没有取消路径 | `QualitySelectDialog.kt:47, 70-76`：给遮罩加 `pointerInput { detectTapGestures { onDismiss() } }` |
| M5 | 插桩测试从未执行，且仓库无 CI 跑 `connectedAndroidTest` | 合并前必须设备/模拟器跑一次 `./gradlew :app:connectedDebugAndroidTest` |

### 建议项
- `ControllerVideoInfo.kt:442` 用 `icon = 0` 作「无图标」哨兵，重构易崩；改 `icon: Int? = null`。
- `QualitySelectDialog` 的 `Column` 不可滚动，档位 6–8 个时小屏会溢出；改 `LazyColumn` 或 `verticalScroll`。
- `closeAllControllers()` 未重置 `showQualityDialog`。
- 测试缺：初始聚焦、`onSelect(qn)` 回调、空列表；`delay(50)` 聚焦需 `mainClock.advanceTimeBy(100)`。
- `MenuListItem.selected` 无语义，读屏与断言均不可用。
- 跨编码档位切换后 `mediaProfileState.videoCodec` 可能与实际流不一致（既有问题，建议另开 issue）。

### 做对了的地方（回复时肯定）
- `Dialog` 独立窗口符合 §11.2.1；状态提升到 `VideoPlayerController`、放在信息栏兄弟位置，避免被 `AnimatedVisibility` 连带销毁。
- `availableQuality.size > 1` 用 Map key 数正确（不会被同档多编码放大）。
- `displayName` 收敛到 `EnumDisplayNames.kt` 逐项比对无行为变化。

---

## 10. 建议执行批次

### 批次 A：发版（0 代码改动，收益最大）
- [ ] 打 `v1.2.1`，包含 main 上 25 个未发布提交
- [ ] 回帖 #321 / #320 / #317 让用户升级验证
- [ ] 回帖 #328 说明缓冲策略已修，请复测并回传 `dumpsys meminfo`

### 批次 B：小改动高收益（1 个 PR 可合并）
- [ ] #333 `formatBitrateKbps` + 视频码率行（+单测）
- [ ] #324 CodecUtil 逐项容错 + 页面错误态（+纯函数单测）
- [ ] #332 评论 toggle + Dialog 内快捷键拦截（+单测/插桩）
- [ ] （顺手）#321 兜底判定与字段默认值、字幕开关文案反转

### 批次 C：稳定性
- [ ] #285 短连接超时 + IPv4 优先 Dns（先 P0，无设置项）
- [ ] #328 低堆设备画质上限 + 弹幕池按堆比例 + `onTrimMemory`
- [ ] #285 P1「仅使用 IPv4」设置项

### 批次 D：PR 处理
- [ ] #325 要求修改：Range 注入方式、直播策略、拆缓冲改动、补请求头测试
- [ ] #331 要求修改：M1–M5，建议项可后续

### 批次 E：需真机取证
- [ ] #330 诊断日志 → 收集 Sony 日志/解码器能力/二分结果 → 决定竖屏 1080P 开关与 FIT 模式

---

## 11. 待用户/真机提供的信息

| issue | 需要的信息 | 用途 |
|---|---|---|
| #330 | BV 号 + 调试信息截图 + 编解码信息页截图 + logcat（含 SurfaceFlinger/CCodec） + 4K 横屏是否复现 | 区分解码器能力 vs 合成层 |
| #328 | 升级后 `dumpsys meminfo` / `logcat | grep "MB/192MB"` / `dumpsys media.codec` | 验证 83f05240 效果、确认无 codec 累积 |
| #324 | 复现后导出的 `logs_crash_*.log` | 确认具体 codec 名与异常类型 |
| #285 | `adb shell ping6`（B 站 AAAA）与 `ping`（A）对比、首屏请求耗时日志 | 确认地址族顺序与耗时 |
| #332 | 遥控器自定义键键码/型号 | 排除按键被系统占用 |
