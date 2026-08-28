# danmaku-engine — AkDanmaku 弹幕引擎（收编）

本模块是 [AkDanmaku](https://github.com/KwaiAppTeam/AkDanmaku)（快手开源，MIT License）的**源码收编版本**，
经 [Frost819 fork](https://github.com/Frost819/AkDanmaku) `1.0.4` tag 引入，替代 jitpack 依赖
`com.github.Frost819:AkDanmaku:1.0.4`。

## 为什么收编

- 弹幕分段加载需要引擎提供**增量 append + 真正的清空**能力：
  - `updateData()` 底层是 `DataSystem.addItems()`（增量追加），并非全量替换；
  - 上游没有清空 API，切视频时旧弹幕会残留在时间轴上（`updateData(emptyList())` 是 no-op）。
- 收编后可直接维护引擎（如 `clearData()`），不必维护独立 fork 版本。

## 与上游的差异

| 修改 | 位置 | 说明 |
|---|---|---|
| 新增 `clearData()` | `ui/DanmakuPlayer.kt`、`ecs/system/DataSystem.kt` | 清空数据列表、待处理队列、id 去重集合、当前切片窗口并移除全部实体；切视频时必须调用它清数据 |
| `BuildConfig` 包名 | `ext/Trace.kt` | 构建配置类由模块 namespace `dev.frost819.newbv.danmaku.engine` 生成 |
| 代码风格统一 | 全模块 | 已按项目 ktlint 规则格式化（4 空格缩进等），与上游 2 空格风格存在纯格式差异；逻辑一致 |
| detekt 适配 | `RenderSystem.kt`、`OrderedRangeList.kt`、`CacheManager.kt`、`DrawingCache.kt` 等 | 复杂条件拆分、printStackTrace 改 Log、外部 API vararg 透传的 `@Suppress`（带注释） |
| 构建脚本 | `build.gradle.kts` | 适配本项目：compileSdk 36 / minSdk 21 / Java 17，依赖 gdx 1.12.0 + ashley 1.7.4（版本登记在 `gradle/libs.versions.toml`） |

其余源码与上游 1.0.4 逻辑一致（含 `jniLibs/` 各 ABI 的 `libgdx.so`，与原 AAR 打包等价）。

## 使用约定

- 源码**包名保持 `com.kuaishou.akdanmaku`**，便于后续对照上游更新；不重构为 `dev.frost819.*`。
- 该模块**正常参与 detekt 与 ktlint 检查**（无豁免），修改时需同时通过两项检查。
- 升级上游版本时，重新拷贝 `library/src/main/java` + `jniLibs`，重新应用上表差异，最后执行 `ktlintFormat` 统一风格。
