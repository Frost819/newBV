# new BV 产品需求文档（PRD）

> **项目名称**：new BV —— 哔哩哔哩第三方客户端重构版
> **文档版本**：v1.0
> **创建日期**：2026-07-20
> **基准项目**：BV（源码位于 `bv/` 目录，作者 aaa1115910 / Frost819）
> **目标平台**：Android TV 为主，兼容触屏设备

---

## 0. 文档信息与修订记录

| 版本 | 日期 | 修订人 | 修订内容 |
|---|---|---|---|
| v1.0 | 2026-07-20 | new BV 团队 | 初始版本，基于 BV 源码分析产出 |

### 文档说明

本 PRD 基于对 BV 源码（`bv/` 目录）的深度分析，结合 new BV 的重构改进定位编写。文档涵盖：

- **第 1-2 章**：项目概述与技术架构
- **第 3 章**：从 BV 继承并重构的现有功能（含完整行为规格）
- **第 4 章**：new BV 新增功能需求
- **第 5-6 章**：非功能性需求与交互设计规范
- **第 7-8 章**：设置项全量清单与数据接口清单
- **第 9-10 章**：里程碑与风险对策

### 关键决策摘要

| 决策项 | 结论 |
|---|---|
| 项目定位 | 重构改进版（非 1:1 复刻，非精简） |
| 目标平台 | Android TV 为主，同时支持触屏交互 |
| 架构 | 单 Activity + Navigation-Compose（类型安全路由） |
| 依赖注入 | Hilt（替换原版 Koin） |
| 播放器引擎 | 仅 Media3/ExoPlayer，兼容 VOD + Live 双模式（不接入 VLC） |
| 代理功能 | **完全删除**所有代理相关功能 |
| 崩溃监控 | 本地为主 + 可选自建上报（移除 Firebase Crashlytics） |
| 交互日志 | 本地文件 + Ktor 网页端查看 |
| minSdk | 21（Android 5.0+，与原版一致） |
| Kotlin / KSP / Java | 2.4.10 / 2.3.10 / 17 |
| AGP / Gradle | 9.1.1 / 9.3.1 |
| compileSdk / targetSdk | 36 / 36 |

---

## 1. 项目概述

### 1.1 项目背景与目标

BV 是一款基于 Jetpack Compose 开发的哔哩哔哩第三方 Android TV 应用，支持 Android 5.0+，在功能完整性、播放器体验、弹幕渲染等方面表现优秀。但其存在以下核心痛点：

1. **稳定性问题**：焦点相关闪退频发、播放器弹幕蒙版 OOM、分 P 加载崩溃
2. **架构可维护性差**：多 Activity 架构导致状态重建与焦点丢失；巨型 ViewModel（如播放器 ViewModel 达 1417 行）；模块边界模糊
3. **风控/接口脆弱**：B 站风控导致 Web 接口易被封，需频繁手动切换接口与 UA
4. **性能瓶颈**：列表滚动卡顿、缩略图加载不流畅、弹幕蒙版内存占用过高
5. **功能缺失**：直播功能有底层 API 无 UI；评论、触屏适配等缺失
6. **云端依赖**：依赖 Firebase Crashlytics，国内不可用

**new BV 的目标**：以 BV 为基础进行重构，保留其核心功能与优秀体验，同时：

- 采用单 Activity + Navigation 架构，根治焦点与状态问题
- 拆分巨型 ViewModel，建立清晰的模块分层
- 新增直播观看、评论浏览、触屏适配等核心缺失功能
- 建立本地为主的崩溃监控与交互日志体系，不依赖国内不可用的云端服务
- 移除所有代理相关功能，简化产品形态
- 支持黑夜/白天主题切换，提升用户体验

### 1.2 目标用户与使用场景

| 用户画像 | 使用场景 | 核心诉求 |
|---|---|---|
| 电视盒子用户 | 客厅电视上观看 B 站视频/直播 | D-pad 遥控器流畅操作、播放稳定 |
| 智能电视用户 | 电视原生应用 | 大屏适配、遥控器交互 |
| 平板/触屏设备用户 | 平板上观看 | 触屏操作、响应迅速 |
| B 站重度用户 | 番剧追番、UP 主关注、稍后再看 | 功能完整、交互高效 |

**不支持声明**：new BV 不支持在中国大陆地区内使用，如有相关使用需求请使用官方客户端。

### 1.3 与原版 BV 的差异总览

| 维度 | 原版 BV | new BV |
|---|---|---|
| 架构 | 多 Activity（每个功能独立 Activity） | 单 Activity + Navigation-Compose |
| 依赖注入 | Koin 4.0 + annotations + KSP | Hilt |
| 导航 | Intent 跳转 + companion 函数 | Navigation-Compose 类型安全路由 |
| 播放器 | Media3（VLC 代码休眠未接入） | Media3，兼容 VOD + Live |
| 代理 | HTTP 代理 + gRPC 代理 + ProxyArea + Ali CDN | **完全删除** |
| 崩溃监控 | Firebase Crashlytics（可选） | 本地为主 + 可选自建上报 |
| 主题 | 固定深色 | 黑夜/白天可切换 |
| 触屏 | 仅 D-pad | D-pad + 触屏双模式 |
| 直播 | 有底层 API 无 UI | 完整直播功能 |
| 评论 | 无 | 详情页 + 播放器内 |
| ViewModel | 巨型（播放器 1417 行） | 按职责拆分 |
| 交互日志 | 仅错误日志 | 全交互 log + 网页端 |
| CDN 策略 | 用户可配置官方 CDN 优先 | 内部自动策略（不暴露） |
| 图片库 | Coil 2.7 | Coil 3.x |
| Firebase | 依赖（可选） | 完全移除 |

### 1.4 名词解释

| 术语 | 含义 |
|---|---|
| UGC | User Generated Content，用户投稿视频 |
| PGC | Professional Generated Content，专业内容（番剧、电影、纪录片等） |
| DASH | Dynamic Adaptive Streaming over HTTP，B 站视频采用 DASH 格式，视频流与音频流分离 |
| 弹幕 | 视频上滚动的用户评论文字 |
| WBI 签名 | B 站 Web 接口的反爬虫签名机制，基于 `w_rid` 参数 |
| buvid | B 站设备标识，用于接口请求 |
| 分 P | 同一投稿视频的多个分段（Part） |
| 合集 | UP 主创建的视频合集（UGC Season） |
| 稍后再看 | Watch Later，用户标记待看视频列表 |
| 一键三连 | 点赞 + 投币 + 收藏一次性完成 |
| 防遮挡蒙版 | Danmaku Mask，智能弹幕碰撞避让的蒙版数据 |
| Media3 | AndroidX 的媒体播放库（含 ExoPlayer） |
| akdanmaku | 弹幕渲染库（基于 ECS 架构） |
| Hilt | Google 推出的 Android 依赖注入框架（基于 Dagger） |
| Navigation-Compose | Jetpack Compose 的导航组件 |
| DataStore | AndroidX 的数据持久化方案（替代 SharedPreferences） |
| Room | AndroidX 的 SQLite ORM |

---

## 2. 技术架构

### 2.1 技术栈选型

#### 2.1.1 版本配置

| 项 | 版本 | 说明 |
|---|---|---|
| AGP | 9.1.1 | Android Gradle Plugin |
| Gradle | 9.3.1 | 构建工具 |
| Kotlin | 2.4.10 | 主语言（2026-07-14 发布的最新稳定版） |
| KSP | 2.3.10 | Kotlin Symbol Processing（适配 Kotlin 2.4 + AGP 9） |
| Java (JDK) | 17 | AGP 9 最低要求 |
| compileSdk | 36 | Android 16 Baklava |
| minSdk | 21 | Android 5.0+，与原版一致，兼容老电视盒子 |
| targetSdk | 36 | 最新 |

#### 2.1.2 核心依赖

| 库 | 版本 | 用途 | 与原版差异 |
|---|---|---|---|
| Compose BOM | latest stable | UI 框架，BOM 统一管理版本 | 原版单管版本，改 BOM |
| Compose Material3 | latest stable | Material3 组件 | 同原版 |
| Compose TV Material | latest stable | TV 专用组件 | 同原版 |
| Navigation-Compose | latest stable | 单 Activity 导航 | **新增**（原版无） |
| Hilt | 2.60.1+ | 依赖注入 | **替换** Koin |
| Media3 | 1.8.0 | 播放器引擎 | 同原版 |
| Coil | 3.x | 图片加载 | **升级**（原版 2.7） |
| Ktor | 3.1.3 | HTTP 客户端 + 本地服务器 | 同原版 |
| gRPC-kotlin | 1.4.1 / grpc 1.72.0 | gRPC 通信 | 同原版 |
| Room | 2.7.1 | 本地数据库 | 同原版 |
| DataStore | 1.1.7 | 偏好设置持久化 | 同原版 |
| akdanmaku | 1.0.4 | 弹幕渲染 | 同原版（Frost819 fork） |
| kotlinx.serialization | 1.8.1 | JSON 序列化 | 同原版 |
| kotlinx.coroutines | 1.10.2 | 协程 | 同原版 |
| kotlin-logging | 7.0.7 | 日志门面 | 同原版 |
| qrcode-kotlin | 3.3.0 | 二维码生成（minSDK 23 以下限制） | 同原版 |
| protobuf | 4.31.0 | protobuf 运行时 | 同原版 |
| Lottie | 6.6.6 | 动画 | 同原版 |

#### 2.1.3 移除的依赖

| 库 | 原版用途 | 移除原因 |
|---|---|---|
| Firebase Crashlytics / Analytics | 崩溃监控 | 国内不可用，改本地方案 |
| Koin | 依赖注入 | 替换为 Hilt |
| Geetest Sensebot | 短信登录验证码 | 短信登录降级，暂不接入（保留代码注释） |
| compose-remember-preference | 偏好绑定 | 改用 DataStore + Compose 原生 |
| slf4j-handroid | 日志后端 | 改用 kotlin-logging 自带 |

### 2.2 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                      new BV App                          │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │              UI 层 (Compose)                       │   │
│  │  ┌─────────────┐ ┌──────────┐ ┌───────────────┐  │   │
│  │  │  TV 适配层   │ │ 触屏适配层│ │  主题系统      │  │   │
│  │  │ (D-pad焦点)  │ │(触屏交互) │ │(黑/白切换)    │  │   │
│  │  └─────────────┘ └──────────┘ └───────────────┘  │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │     Navigation-Compose (单Activity)         │  │   │
│  │  │  Main ─ Player ─ Detail ─ Settings ─ Login  │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │  Screens + Components + VideoCards          │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
│                         │ hilt()                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │          ViewModel 层 (按职责拆分)                 │   │
│  │  HomeVM │ SearchVM │ PlayerVM │ DetailVM │ ...    │   │
│  └──────────────────────────────────────────────────┘   │
│                         │                                │
│  ┌──────────────────────────────────────────────────┐   │
│  │          Repository 层 (数据聚合)                  │   │
│  │  VideoRepo │ UserRepo │ SearchRepo │ LiveRepo ... │   │
│  └──────────────────────────────────────────────────┘   │
│                         │                                │
│  ┌─────────────────────┬────────────────────────────┐   │
│  │   Data 层            │   Platform 层               │   │
│  │ ┌─────────────────┐ │ ┌────────────────────────┐ │   │
│  │ │ Room (DB)       │ │ │ bili-api (HTTP/gRPC)   │ │   │
│  │ │ - UserDB        │ │ │ - BiliHttpApi          │ │   │
│  │ │ - SearchHistory │ │ │ - BiliPassportApi      │ │   │
│  │ │ - InteractionLog│ │ │ - BiliLiveApi          │ │   │
│  │ │                 │ │ │ - gRPC Channel         │ │   │
│  │ └─────────────────┘ │ └────────────────────────┘ │   │
│  │ ┌─────────────────┐ │ ┌────────────────────────┐ │   │
│  │ │ DataStore       │ │ │ bili-subtitle          │ │   │
│  │ │ (Preferences)   │ │ │ bili-api-grpc (proto)  │ │   │
│  │ └─────────────────┘ │ └────────────────────────┘ │   │
│  └─────────────────────┴────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │          基础设施层                                │   │
│  │  ┌──────────┐ ┌──────────┐ ┌───────────────────┐  │   │
│  │  │播放器引擎 │ │ 弹幕引擎  │ │ 日志/崩溃/监控     │  │   │
│  │  │ (Media3) │ │(akdanmaku)│ │(本地+Ktor网页端)  │  │   │
│  │  └──────────┘ └──────────┘ └───────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.3 模块划分

相比原版 BV 的模块结构，new BV 进行了更清晰的分层与职责划分：

| 模块 | 类型 | 职责 | 对应原版 |
|---|---|---|---|
| `:app` | Application | 主应用、UI、ViewModel、Repository、导航 | `app` |
| `:bili-api` | Library | B 站 HTTP + gRPC 接口封装、签名、实体 | `bili-api` |
| `:bili-api-grpc` | Library | Protobuf 定义 | `bili-api-grpc` |
| `:bili-subtitle` | Library | 字幕解析与编码 | `bili-subtitle` |
| `:player` | Library | 播放器引擎抽象与 Media3 实现（VOD + Live） | `bv-player` |
| `:danmaku` | Library | 弹幕渲染封装（akdanmaku 集成） | 散落在 app 中 |
| `:core` | Library | 通用工具、主题、交互模式抽象 | 散落在 app 中 |
| `:data` | Library | Room 实体、DAO、DataStore、Repository 接口 | 散落在 app 中 |

**新增模块说明**：
- `:danmaku`：将原版散落在 app 中的弹幕相关代码（`DanmakuPlayerCompose`、`DanmakuMaskUtil`）独立成模块，职责清晰
- `:core`：抽象 `InputMethod` + `InteractionTracker`（运行时输入方式追踪，同时支持 D-pad / Touch）、主题系统、通用 UI 组件、日志基础设施
- `:data`：将 Room DAO、DataStore Prefs、Repository 接口独立，便于测试与复用

### 2.4 导航设计

采用 Navigation-Compose 类型安全路由，单 Activity 架构。相比原版多 Activity 方案的优势：
- 统一的返回栈管理
- 屏幕间状态共享与恢复
- 焦点在导航间自然流转
- 动画过渡统一控制
- 深链接支持

#### NavGraph 结构

```
MainActivity (@HiltAndroidApp)
└─ NavHost
   ├─ mainGraph (主功能区)
   │  ├─ home (首页：动态/推荐/热门)
   │  ├─ ugc (分区)
   │  ├─ pgc (影视)
   │  ├─ personal (个人：稍后再看/历史/收藏/追番)
   │  ├─ search (搜索入口)
   │  ├─ live (直播首页) [新增]
   │  └─ upSpace (UP 主页)
   ├─ detailGraph (详情区)
   │  ├─ videoDetail (视频详情)
   │  ├─ seasonDetail (番剧详情)
   │  ├─ comments (评论列表) [新增]
   ├─ playerGraph (播放器区)
   │  ├─ videoPlayer (视频播放)
   │  └─ livePlayer (直播播放) [新增]
   ├─ settingsGraph (设置区)
   │  ├─ settingsHome
   │  ├─ audioVideoSetting
   │  ├─ uiSetting
   │  ├─ otherSetting
   │  ├─ storageSetting
   │  ├─ networkSetting
   │  └─ aboutSetting
   ├─ loginGraph (登录区)
   │  ├─ qrLogin
   │  └─ userSwitch (多账户管理)
   └─ pgcFeatureGraph (PGC 功能区)
      ├─ pgcIndex (索引筛选)
      ├─ animeTimeline (番剧时间表)
      └─ followingSeason (追番列表)
```

#### 路由参数设计

采用类型安全路由（Serialization），关键路由示例：

```kotlin
@Serializable
data class VideoDetailRoute(
    val aid: Long,
    val epid: Long? = null,
    val fromSeason: Boolean = false
)

@Serializable
data class PlayerRoute(
    val avid: Long,
    val cid: Long,
    val title: String,
    val partTitle: String,
    val played: Long = 0,
    val fromSeason: Boolean = false,
    val subType: Int = 0,
    val epid: Long? = null,
    val seasonId: Long? = null,
    val isLive: Boolean = false  // [新增] 区分直播/点播
)

@Serializable
data class CommentsRoute(
    val aid: Long,
    val fromPlayer: Boolean = false  // [新增] 标记来源
)
```

### 2.5 数据流架构

遵循单向数据流（UDF）原则：

```
用户交互 (D-pad/触屏)
     │
     ▼
Composable (UI) ──事件──▶ ViewModel (State Holder)
     ▲                         │
     │                         ▼
     │                    Repository (数据聚合)
     │                         │
     │                         ▼
     │              ┌──────────┴──────────┐
     │              │                     │
     │         Room/DataStore        bili-api (HTTP/gRPC)
     │         (本地持久化)          (B 站接口)
     │              │                     │
     └──StateFlow───┴─────────────────────┘
```

**ViewModel 拆分原则**（解决原版巨型 ViewModel 问题）：

| 原版 ViewModel | new BV 拆分 |
|---|---|
| `VideoPlayerV3ViewModel` (1417 行) | `PlayerViewModel`（播放控制）<br>`PlayerMenuViewModel`（菜单/设置）<br>`DanmakuViewModel`（弹幕）<br>`SubtitleViewModel`（字幕）<br>`VideoListViewModel`（分集列表）<br>`PlayerInteractionViewModel`（交互日志、快捷键） |
| `VideoInfoViewModel` | `VideoDetailViewModel`（详情数据）<br>`VideoActionViewModel`（点赞/投币/收藏） |
| `MainViewModel` | `HomeViewModel`、`UgcViewModel`、`PgcViewModel`、`PersonalViewModel`（各 Tab 独立） |

每个 ViewModel 职责单一，通过 `SharedFlow` / `StateFlow` 通信，避免上帝对象。

### 2.6 接口层设计

#### 2.6.1 接口分层（移除代理层）

原版 BV 的接口层包含 Web HTTP、App gRPC、代理三层。new BV **完全删除代理层**，保留 Web 与 App 双接口：

```
┌─────────────────────────────────────┐
│           Repository 层              │
│  (VideoRepo, UserRepo, SearchRepo)  │
└──────────────┬──────────────────────┘
               │ ApiType (Web / App)
               ▼
┌──────────────────────────────────────┐
│            接口适配层                  │
│  ┌────────────────┬────────────────┐ │
│  │   Web HTTP     │   App gRPC     │ │
│  │ (Ktor + SESSDATA)│(grpc + access_key)│
│  └────────────────┴────────────────┘ │
│  ┌──────────────────────────────────┐│
│  │      风控降级 / 自动切换           ││
│  │  (失败自动 fallback, UA 轮换)     ││
│  └──────────────────────────────────┘│
└──────────────────────────────────────┘
               │
               ▼
      api.bilibili.com / grpc.biliapi.net
```

**移除的内容**：
- `BiliHttpProxyApi`（代理 HTTP 接口）
- `ProxyArea`（区域代理判断：港/台）
- gRPC `proxyChannel`（代理 gRPC 通道）
- `replaceUrlDomainWithAliCdn`（Ali CDN 域名替换）
- `preferOfficialCdn` 设置项与相关 CDN 过滤逻辑
- 所有 `proxyHttpServer` / `proxyGRPCServer` 配置

**风控降级策略**（新增，替代原版手动代理）：

```
请求失败/风控码触发
     │
     ├─ -352 (风控) ──▶ 自动切换 Web ↔ App 接口
     ├─ -101 (未登录) ──▶ 提示重新登录
     ├─ 网络超时 ──▶ 重试 + UA 轮换
     └─ 403/Request banned ──▶ UA 轮换 + 重试
```

- UA 轮换池：维护多个 Web UA 与 App UA，请求失败时轮换
- 接口降级：Web 接口失败自动尝试 App gRPC，反之亦然
- 降级过程对用户透明，仅在连续失败时提示

#### 2.6.2 接口类型对照

| 功能 | Web HTTP | App gRPC |
|---|---|---|
| 推荐视频 | `/x/web-interface/wbi/index/top/feed/rcmd` | `Popular.Index` |
| 热门视频 | `/x/web-interface/popular` | `Popular.Index` |
| 视频详情 | `/x/web-interface/wbi/view/detail` | `View.View` |
| 播放地址 (UGC) | `/x/player/playurl` (fnval=4048, qn=127) | `Player.PlayViewUnite` |
| 播放地址 (PGC) | `/pgc/player/web/v2/playurl` | `PlayURL.PlayView` |
| 弹幕 | `/x/v1/dm/list.so` (XML) | `DM.DmView` (元数据) |
| 字幕 | `/x/player/wbi/v2` | `DM.DmView` |
| 搜索 | `/x/web-interface/wbi/search/...` | `Search.SearchByType` |
| 历史 | `/x/web-interface/history/cursor` | `History.CursorV2` |
| 动态 | `/x/polymer/web-dynamic/v1/feed/all` | `Dynamic.DynVideo` |
| 稍后再看 | `/x/v2/history/toview` | (同 Web) |
| 收藏 | `/x/v3/fav/...` | (同 Web) |
| 点赞/投币 | `/x/web-interface/archive/...` | (同 Web) |
| 直播 [新增] | `api.live.bilibili.com/...` | (同 Web) |

**鉴权方式**：
- Web API：`SESSDATA` Cookie + WBI 签名 + buvid3 Cookie
- App gRPC：`access_key`（gRPC metadata `authorization`）+ 设备信息 metadata

### 2.7 播放器引擎抽象

#### 2.7.1 统一播放器接口

new BV 需要播放器兼容**视频点播（VOD）**与**直播（Live）**两种场景。设计统一的播放器抽象：

```kotlin
interface BvMediaPlayer {
    // 通用控制
    fun playUrl(url: String, audioUrl: String? = null)
    fun prepare()
    fun start()
    fun pause()
    fun stop()
    fun release()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)

    // 状态
    val isPlaying: StateFlow<Boolean>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val bufferedPercentage: StateFlow<Int>
    val videoSize: StateFlow<VideoSize>
    val playerState: StateFlow<PlaybackState>

    // VOD 专属
    val isLive: Boolean  // 标记是否直播流

    // 事件
    fun setListener(listener: PlayerEventListener)
}

sealed interface PlaybackState {
    object Ready : PlaybackState
    object Playing : PlaybackState
    object Paused : PlaybackState
    object Buffering : PlaybackState
    object Ended : PlaybackState
    data class Error(val message: String) : PlaybackState
}
```

#### 2.7.2 Media3 实现

唯一实现基于 Media3/ExoPlayer：

| 场景 | MediaSource | 说明 |
|---|---|---|
| UGC 视频 (DASH) | `MergingMediaSource(ProgressiveMediaSource(video), ProgressiveMediaSource(audio))` | 视频流与音频流分离合并 |
| PGC 视频 (DASH) | 同上 | 同上 |
| 直播 (HLS/FLV) [新增] | `HlsMediaSource` 或 `ProgressiveMediaSource` (FLV) | 直播流不支持 seek，duration 为 0 |

**直播适配要点**：
- 直播流 `isLive = true`，播放器 UI 隐藏进度条与缩略图
- 直播弹幕通过 WebSocket 实时接收（非 XML 接口）
- 直播无"分集"概念，播放器内视频列表隐藏
- 直播心跳上报逻辑不同（直播心跳接口）

#### 2.7.3 编解码选择策略

继承原版逻辑，无代理干扰：

```
画质选择:
  1. 用户默认画质可用 → 选默认
  2. 否则选 ≤ 默认的最高画质
  3. 否则选最低可用画质

编码选择:
  1. 用户默认编码可用 → 选默认
  2. 否则按优先级：AVC > HEVC > AV1 > DVH1 > HVC1

音轨选择:
  1. Dolby ↔ HiRes 互为 fallback
  2. 192K → 132K → 64K → 首个

CDN 选择 [简化]:
  1. 过滤 P2P MCDN (.mcdn.bilivideo.) 与 .szbdyd.com
  2. 过滤纯 IP URL
  3. 选首个有效 URL
  （注：移除 Ali CDN 替换与代理逻辑）
```

**支持的编码**：

| 编码 | 前缀 | codecId | 说明 |
|---|---|---|---|
| H.264 (AVC) | `avc1` | 7 | 默认，兼容性最好 |
| H.265 (HEVC) | `hev1` | 12 | 高压缩，需硬件支持 |
| AV1 | `av01` | 13 | 新一代编码 |
| Dolby Vision (DVH1) | `dvh1` | 0 | 杜比视界 |
| Dolby Vision (HVC1) | `hvc` | 0 | 杜比视界 |

**支持的画质**：240P / 360P / 480P / 720P / 720P60 / 1080P / 1080P+ / 1080P60 / 4K / HDR / Dolby / 8K

**解码模式**：
- 硬件解码（默认）：使用设备 MediaCodec 硬件解码器
- 软件视频解码（可选）：过滤到 `OMX.google.*` / `c2.android.*` 软件解码器
- FFmpeg 音频解码（可选）：通过 `EXTENSION_RENDERER_MODE_ON` 启用 FFmpeg 音频渲染器（支持 flac/mp3/aac/ac3/eac3）

---

## 3. 功能需求 - 现有功能（从 BV 继承并重构）

本章描述从原版 BV 继承的核心功能，按新架构重写。每个功能模块包含：功能描述、用户故事、交互规格、数据流、与原版的差异。

### 3.1 启动与初始化

#### 3.1.1 启动流程

**功能描述**：应用启动时的初始化流程，包括启动页、用户锁检查、数据初始化。

**用户故事**：
- 作为用户，我希望应用启动快速，能记住我的登录状态与上次位置
- 作为有隐私需求的用户，我希望应用启动时需要密码解锁

**启动流程**：

```
Application.onCreate()
  ├─ 初始化 Hilt
  ├─ Prefs.init() (从 DataStore 同步读取偏好到内存缓存)
  │   └─ 自动生成 buvid / buvid3（若不存在）
  ├─ 初始化 AuthRepository (从 Prefs 恢复登录态)
  ├─ 初始化 bili-api 接口层
  ├─ 初始化 Coil 图片加载
  ├─ 初始化 Ktor 本地日志服务器 [增强]
  ├─ 注册全局未捕获异常处理器 [增强]
  └─ 初始化交互日志记录器 [新增]
       │
       ▼
MainActivity (SplashScreen)
  ├─ 检查默认用户是否有锁
  │   ├─ 有锁 → UnlockUserScreen (D-pad 方向密码输入)
  │   └─ 无锁 → MainScreen
  └─ 加载首页数据（动态/推荐/热门并行）
```

**与原版差异**：
- 移除 `BVApp.initProxy()`（代理初始化）
- 移除 `BVApp.initDeviceInfo()` 中的代理设备信息
- 新增全局未捕获异常处理器（写本地崩溃日志）
- 新增交互日志记录器初始化

#### 3.1.2 用户锁（启动锁）

**功能描述**：启动时若默认用户设置了锁，需输入 D-pad 方向密码解锁。

**交互规格**：
- 密码组成：上/下/左/右四个方向 + 确认键（D-pad Center）
- 输入界面：全屏，显示已输入方向序列（圆点图标）
- 返回键：**禁用**（`BackHandler(true) {}`），必须输完密码
- 输入错误：清空重新输入，无错误次数限制（不锁定）
- 解锁成功：进入 MainScreen

**状态机**：
```
ChooseUser (选择用户) ──选中──▶ InputPassword (输入密码)
                                    │
                          ┌─────────┴─────────┐
                          │                   │
                     密码正确              密码错误
                          │                   │
                          ▼                   ▼
                   MainScreen          清空重输
```

### 3.2 登录与账户

#### 3.2.1 二维码登录

**功能描述**：通过扫描二维码登录 B 站账号，支持 Web QR 与 TV QR 两种方式。

**用户故事**：
- 作为电视用户，我无法直接输入账号密码，需用手机扫码登录
- 作为用户，我希望登录后多设备同步观看历史与收藏

**登录方式**：

| 方式 | 接口 | 返回 | 适用场景 |
|---|---|---|---|
| Web QR | `passport.bilibili.com/x/passport-login/web/qrcode/*` | SESSDATA + bili_jct 等 Cookie | Web 接口鉴权 |
| TV QR | `passport.bilibili.com/x/passport-tv-login/qrcode/*` | accessToken + refreshToken | App gRPC 鉴权 |

**默认方式**：TV QR（同时获得 Cookie 与 Token，支持双接口）

**交互流程**：
```
登录页
  ├─ 请求二维码 → 显示二维码图片
  ├─ 轮询登录状态（定时器）
  │   ├─ WaitingForScan (等待扫描)
  │   ├─ WaitingForConfirm (等待确认)
  │   ├─ Expired (过期) → 提示按确认重新生成
  │   ├─ Error → 提示按确认重试
  │   └─ Success → 保存凭证，finish 登录
  └─ D-pad Center: 过期/错误时重新请求二维码
```

**凭证存储**：
- `AuthData` 序列化为 JSON 存入 Room `UserDB.auth` 字段
- 同时镜像到 DataStore（Prefs）用于快速恢复
- 包含字段：`uid`, `uidCkMd5`, `sid`, `biliJct`, `sessData`, `tokenExpiredData`, `accessToken`, `refreshToken`

#### 3.2.2 SMS 登录（保留代码，暂不启用）

**功能描述**：手机号 + 短信验证码登录，含极验验证码。

**状态**：原版代码存在但已注释关闭。new BV **保留代码但暂不启用**（移除 Geetest SDK 依赖），未来如需启用再接入。

#### 3.2.3 多账户管理

**功能描述**：支持添加多个 B 站账号，快速切换。

**用户故事**：
- 作为多账号用户，我希望快速在不同账号间切换，无需重新登录

**功能规格**：
- 账户列表：横向滚动头像列表，底部"添加用户"按钮
- 切换账号：点击头像即切换，重新初始化接口层与数据
- 管理模式：底部开关进入管理模式，可：
  - 显示 Token（二维码形式，便于迁移）
  - 用户锁设置（跳转 `UserLockSettingsScreen`）
  - 删除账号（二次确认）
- 锁定用户：切换到锁定用户需先输入 D-pad 密码解锁

#### 3.2.4 无痕模式

**功能描述**：开启后不向 B 站上报观看心跳（历史记录）。

**交互**：
- 入口：左侧导航栏头像 → UserPanel → 无痕模式开关
- 状态持久化（DataStore `incognitoMode`）
- 播放器心跳上报前检查：`if (Prefs.incognitoMode) return`

### 3.3 主页导航

#### 3.3.1 左侧导航栏

**功能描述**：主导航采用左侧 NavigationDrawer 形式（非抽屉，常驻显示）。

**导航项**（5 项）：

| 项 | 图标 | 功能 |
|---|---|---|
| 搜索 | `Icons.Default.Search` | 进入搜索页 |
| 个人 | `Icons.Default.Person` | 稍后再看/历史/收藏/追番 |
| 主页 | `Icons.Default.Home` | 动态/推荐/热门 |
| 分区 | `Icons.Default.OndemandVideo` | UGC 分区 |
| 影视 | `Icons.Default.Movie` | PGC 影视 |
| 直播 [新增] | `Icons.Default.LiveTv` | 直播首页 |

> 注：原版为 5 项（无直播），new BV 新增直播项，共 **6 项**。

**导航栏顶部**：用户头像按钮
- 已登录：显示头像，点击弹出 `UserPanel`（用户信息 + 无痕开关 + 关注列表 + 账户管理）
- 未登录：显示默认图标，点击跳转登录页

**导航栏底部**：设置图标，点击进入设置页

**D-pad 交互**：
- 左侧栏按右键 → 焦点移至内容区对应 `FocusRequester`
- 内容区按左键 → 焦点回到导航栏
- 导航项需按确认键才切换（不跟随 focus 切换，减少卡顿）

**启动页设置**：用户可设置启动时默认聚焦的导航项（原版 `homeLeftNaviItem` 设置）。

#### 3.3.2 退出确认

**功能描述**：在 MainScreen 按返回键时，需二次确认退出。

**交互**：
- 首次按返回：Toast 提示"再按一次退出"
- 3 秒内再次按返回：退出应用
- 超过 3 秒：重置计数

### 3.4 首页（动态/推荐/热门）

#### 3.4.1 顶部导航

**功能描述**：首页内容区顶部 3 个 Tab 切换。

| Tab | 说明 |
|---|---|
| 动态 | 关注的 UP 主动态视频（需登录） |
| 推荐 | 个性化推荐视频 |
| 热门 | 热门视频榜单 |

**可配置项**：
- Tab 顺序可调整
- 首个 Tab 可设置（`firstHomeTopNavItem`）

#### 3.4.2 推荐页

**功能描述**：4 列网格展示推荐视频，无限滚动。

**数据流**：
- Web API：`/x/web-interface/wbi/index/top/feed/rcmd`
- App gRPC：`Popular.Index`
- 滚动至末尾 20 项内触发加载更多
- 列表末尾显示"没有更多了"提示

#### 3.4.3 热门页

**功能描述**：4 列网格展示热门视频，无限滚动。

**数据流**：Web API `/x/web-interface/popular`

#### 3.4.4 动态页

**功能描述**：展示关注 UP 主的动态视频。

**前置条件**：需登录，未登录显示"请先登录"提示

**数据流**：
- Web API：`/x/polymer/web-dynamic/v1/feed/all`
- App gRPC：`Dynamic.DynVideo`
- 支持 UGC 与 PGC 动态项

#### 3.4.5 菜单键刷新

**功能描述**：在首页任意 Tab 按菜单键刷新当前列表。

**交互**：
- 按菜单键 → 清空当前列表 → 重新加载 → 焦点回到顶部导航

### 3.5 分区（UGC）

#### 3.5.1 分区导航

**功能描述**：UGC 分区页，16 个分区顶部导航切换。

**分区列表**：

| 分区 | 英文 |
|---|---|
| 动画 | Douga |
| 游戏 | Game |
| 鬼畜 | Kichiku |
| 音乐 | Music |
| 舞蹈 | Dance |
| 影视 | Cinephile |
| 娱乐 | Ent |
| 知识 | Knowledge |
| 科技 | Tech |
| 资讯 | Information |
| 美食 | Food |
| 生活 | Life |
| 汽车 | Car |
| 时尚 | Fashion |
| 运动 | Sports |
| 动物 | Animal |

**数据流**：`app.bilibili.com/x/v2/region/dynamic` + `/x/web-interface/region/feed/rcmd`

**交互**：
- 预加载当前 + 相邻 2 个 Tab，平滑切换
- 菜单键刷新当前分区

### 3.6 影视（PGC）

#### 3.6.1 PGC 分类

**功能描述**：PGC 影视页，6 个分类顶部导航。

| 分类 | 说明 |
|---|---|
| 番剧 | Anime |
| 国创 | GuoChuang |
| 电影 | Movie |
| 纪录片 | Documentary |
| 电视剧 | TV |
| 综艺 | Variety |

#### 3.6.2 PGC 首页布局

**功能描述**：每个分类页包含轮播、功能按钮、推荐行、排行榜行。

**布局结构**：
```
LazyColumn
├─ Carousel (轮播，自动旋转，D-pad 可导航)
├─ FeatureButtons (分类专属功能)
│   ├─ 番剧: 时间表 / 我追的番 / 索引 / 巴哈姆特
│   └─ 其他: 索引 / 关注列表 等
├─ PgcFeedVideoRow (横向滚动季卡片)
└─ PgcFeedRankRow (排行榜，带背景图渐变)
```

**数据流**：`/pgc/page/web/v3/feed` + HTML 抓取 `__INITIAL_STATE__`（轮播数据）

#### 3.6.3 PGC 索引筛选

**功能描述**：全量 PGC 索引浏览器，支持多维度筛选。

**筛选维度**：
排序方式、版本、语言、地区、完结状态、版权、季状态、月份、制作方、风格、年份、上映日期

**交互**：
- 菜单键打开筛选弹窗
- 4 列网格展示筛选结果，无限滚动

**数据流**：`/pgc/season/index/result`

#### 3.6.4 番剧时间表

**功能描述**：番剧放送时间表，按星期展示。

**数据流**：`/pgc/web/timeline` 或 `/pgc/app/timeline`

### 3.7 视频详情页（UGC）

#### 3.7.1 详情页布局

**功能描述**：UGC 视频详情页，垂直滚动展示视频信息与操作。

**布局结构**：
```
LazyColumn
├─ ArgueTips (充电专属/竖屏/争议提示)
├─ VideoInfoData
│   ├─ 封面 (可点击播放)
│   ├─ 标题
│   ├─ 投稿日期
│   ├─ 统计 (播放/弹幕/点赞/投币/收藏)
│   ├─ UP 主信息 (可点击进入 UP 页，关注/取关)
│   └─ 操作按钮行
│       ├─ LikeButton (点赞，长按一键三连)
│       ├─ CoinButton (投币)
│       ├─ FavoriteButton (收藏)
│       └─ TagChips (标签，可点击进入搜索结果页)
├─ VideoDescription (简介，可展开)
├─ VideoPartRow (分 P 列表，>5 显示网格弹窗)
├─ VideoUgcSeasonRow (合集列表)
├─ CommentsPreview [新增] (评论预览，点击进入完整评论页)
└─ VideosRow (相关视频)
```

#### 3.7.2 视频操作

**点赞**：
- 短按：点赞/取消点赞
- 长按 2 秒：一键三连（点赞 + 投币 + 收藏），带进度动画
- 接口：`/x/web-interface/archive/like`（点赞）/ `/x/web-interface/archive/like/triple`（三连）

**投币**：
- 点击弹出投币数量选择（1-2 枚）
- 接口：`/x/web-interface/coin/add`

**收藏**：
- 未收藏：点击添加到默认收藏夹
- 已收藏：点击弹出收藏夹列表（多选 FilterChip）
- 接口：`/x/v3/fav/resource/deal`

#### 3.7.3 分 P 与合集

**分 P**：
- 横向列表展示，每个分 P 显示进度条叠加
- >5 个分 P：显示"全部"按钮，打开网格弹窗（分 Tab，每 Tab 20 个）
- 历史续播按钮：跳转到上次播放的分 P

**合集**：
- UGC Season 展示，类似分 P 布局
- 支持合集内分集切换

#### 3.7.4 评论预览 [新增]

**功能描述**：详情页底部显示评论预览（前几条热门评论），点击进入完整评论页。

**详见**：第 4.2 节（评论浏览）

### 3.8 番剧详情页（PGC）

#### 3.8.1 详情页布局

**功能描述**：PGC 番剧/影视详情页。

**布局结构**：
```
LazyColumn
├─ SeasonInfoPart
│   ├─ 封面 (可点击打开季选择器)
│   ├─ 标题
│   ├─ 最新集描述
│   ├─ 完整简介
│   ├─ 播放按钮 (续播或播放首集)
│   └─ 追番/取消追番
├─ SeasonEpisodeRow (正片列表，横向滚动)
│   └─ 每集显示封面缩略 + 进度条叠加
│   └─ 集数多时打开分 Tab 网格弹窗
├─ SectionEpisodes (PV/SP 等附加分集)
└─ CommentsPreview [新增]
```

#### 3.8.2 季选择器

**功能描述**：多季番剧的季切换，全屏覆盖层。

**交互**：
- 横向滚动季封面，显示背景图与标题
- 自动滚动到当前季

#### 3.8.3 播放逻辑

- 构建 `VideoListItem` 列表传递给播放器
- 续播：从上次观看进度继续
- 历史更新：从播放器返回后刷新进度（200ms 延迟）

### 3.9 播放器（核心）

播放器是 new BV 最核心、最复杂的功能模块。本节详细描述播放器的全部功能与交互。

#### 3.9.1 播放器整体结构

```
VideoPlayerScreen
├─ BvVideoPlayer (Media3 视频画面)
├─ DanmakuPlayerCompose (弹幕渲染层)
├─ PersistentSeeker [可选] (常显迷你进度条)
└─ VideoPlayerController (所有覆盖层)
    ├─ DebugInfo [debug] (调试信息)
    ├─ BottomSubtitle (字幕显示)
    ├─ SkipTips (跳过/续播/试看提示)
    ├─ PlayStateTips (暂停/缓冲/错误图标)
    ├─ RelatedVideosController (相关视频)
    ├─ ControllerVideoInfo (顶部标题+时钟，底部进度条+按钮)
    ├─ VideoListController (分集列表)
    ├─ MenuController (设置菜单侧栏)
    ├─ LiveInfoController [新增] (直播信息：在线人数等)
    └─ CommentsController [新增] (播放器内评论)
```

#### 3.9.2 D-pad 遥控器交互映射

**核心键位映射表**：

| 按键 | 短按 | 长按 |
|---|---|---|
| D-pad Center / Enter / Space | 播放/暂停；若有"从头播放"提示则从头播放 | 打开设置菜单 |
| D-pad Up | 打开视频列表（分集） | — |
| D-pad Down | 打开信息/进度条控制器 | — |
| D-pad Left / MediaRewind | 快退 10s（连续按加速） | — |
| D-pad Right / MediaFastForward | 快进 10s（连续按加速） | — |
| Menu 键 | 切换设置菜单 | — |
| Back | 关闭覆盖层；双击 3s 内退出 | — |
| MediaPlayPause | 播放/暂停 | — |
| MediaPlay | 播放 | — |
| MediaPause | 暂停 | — |

**快进/快退加速机制**：
- 基础步进：10s
- 加速规则：200ms 内连续按 5 次后，步进增加 5s（10→15→20→25...）
- 取消跳集：若正在显示"播放下一集"提示，快退取消之

**自定义快捷键**：见第 3.9.10 节

#### 3.9.3 进度条与缩略图

**进度条**：
- Canvas 绘制：轨道 + 缓冲百分比 + 当前位置
- 常显模式：2dp 细条；非常显：8dp 粗条 + 缓冲指示
- 可聚焦，带焦点边框

**缩略图预览**：
- 快进/快退时显示视频截图缩略图
- Sprite sheet 帧提取（`getSpriteFrame(position, cache)`）
- 低质量过滤绘制（`FilterQuality.Low`），优化内存
- 位置沿进度条展示，边缘夹紧

**时间显示**：
- 格式：`current / total`
- 时长 > 1 小时：`HH:MM:SS` 格式
- 时长 ≤ 1 小时：`MM:SS` 格式

#### 3.9.4 弹幕

**弹幕渲染**：基于 akdanmaku（ECS 架构 + SimpleRenderer）

**弹幕类型**：
| 类型 | 说明 |
|---|---|
| 滚动弹幕 | 从右向左滚动 |
| 顶部弹幕 | 固定顶部居中 |
| 底部弹幕 | 固定底部居中 |

**弹幕设置项**：

| 设置 | 范围 | 默认 |
|---|---|---|
| 类型开关 | 全部/滚动/顶部/底部（多选，含 All 智能联动） | 全部 |
| 字体大小 | 0.5 - 4.0 | 1.75 |
| 透明度 | 0 - 1 | 0.7 |
| 速度倍率 | 0.5 / 0.75 / 1.0 / 1.25 / 1.5 | 1.0 |
| 显示区域 | 0 - 1（屏幕比例） | 0.5 |
| 防遮挡蒙版 | 开/关 | 关 |
| 用户等级屏蔽 [新增] | 0-6 级 | 0（不屏蔽） |

**弹幕防遮挡蒙版**：
- 从接口获取蒙版数据（Web: SVG 帧 / App: 位图帧）
- 按时间段缓存，线性查找当前时间对应帧
- 自适应轮询延迟（帧激活时 20-300ms，无帧 100ms，暂停 200ms）
- 蒙版转换为 `bitmapMask` 应用到弹幕渲染

**弹幕同步**：
- 播放：`danmakuPlayer.start()`
- 暂停/缓冲：`danmakuPlayer.pause()`
- Seek：`danmakuPlayer.seekTo(time)` + `pause()`（防止 akdanmaku seek 后自动播放导致不同步）

**弹幕数据源**：
- 点播：`/x/v1/dm/list.so`（XML 格式解析）
- 直播 [新增]：WebSocket 实时接收（`DANMU_MSG` 事件）

**透明度处理**：
- 通过 Compose `Modifier.alpha()` 应用（而非 DanmakuConfig，避免 akdanmaku 闪全不透明 bug）

**用户等级屏蔽 [新增]**：
- 弹幕数据包含 `level` 字段
- 设置弹幕等级阈值，低于阈值的弹幕不显示
- 接口数据已含等级信息，无需额外请求

#### 3.9.5 字幕

**字幕格式**：BCC JSON（B 站格式），通过 `bili-subtitle` 模块解析

**字幕设置项**：

| 设置 | 范围 | 默认 |
|---|---|---|
| 字幕轨道 | 关闭 + 所有可用轨道 | 关闭 |
| 字号 | 12 - 48 SP | 24 |
| 背景透明度 | 0 - 1 | 0.4 |
| 底部边距 | 0 - 48 DP | 12 |

**AI 字幕**：字幕名后缀"(AI)"标识

**自动启用**：若上次启用了字幕，新视频自动启用第一个可用字幕

**字幕数据流**：
- 元数据：`/x/player/wbi/v2`（Web）或 `DM.DmView`（App gRPC）
- 字幕内容：HTTP GET 字幕 URL → `SubtitleParser.fromBccString()`

#### 3.9.6 画质/编码/音轨/宽高比/倍速

**画质选择**：
- 可用画质列表（降序排列）
- 用户默认画质优先，否则选 ≤ 默认的最高画质

**编码选择**：H.264 / H.265 / AV1 / Dolby Vision (DVH1/HVC1)

**音轨选择**：64K / 132K / 192K / Dolby Atmos / Hi-Res（智能 fallback）

**宽高比**：
| 选项 | 说明 |
|---|---|
| Default | 根据视频尺寸自动 |
| 4:3 | 强制 4:3 |
| 16:9 | 强制 16:9 |

**倍速**：0.5 / 1.0 / 1.25 / 1.5 / 2.0（固定挡位）

**切换画质/编码/音轨**：
- 暂停播放 → 重新解析 URL → seek 到当前位置 → 恢复播放

#### 3.9.7 视频列表（分集）

**功能描述**：播放器内左侧覆盖层，显示播放队列/分集列表。

**功能**：
- 支持合集与分 P 同时显示
- 嵌套 UGC 分 P 可展开/折叠
- 自动滚动并聚焦当前播放项
- 宽度 300dp，黑色 50% 覆盖

#### 3.9.8 相关视频

**功能描述**：播放器内全屏居中覆盖层，展示相关视频。

**布局**：横向 `VideosRow`，渐变背景（透明边缘，黑色 50% 中间）

#### 3.9.9 播放控制与导航

**断点续播**：
- 首次 `onPlay` 时 seek 到 `lastPlayed` 位置
- 显示"从上次播放位置继续，按确认键从头播放"提示（5 秒）
- 按确认键：从头播放

**循环播放**：
- 单视频循环开关
- 开启时播放结束自动重头播放

**播放结束动作**：

| 选项 | 行为 |
|---|---|
| Pause | 暂停 |
| PlayNext | 播放下一集（UGC 分 P 或列表下一项），5 秒倒计时 |
| PlayRelated | 播放首个相关视频 |
| Exit | 退出播放器 |

**查找下一播放目标**：遍历 `availableVideoList`，支持嵌套 UGC 分 P

**心跳上报**：
- 播放中每 15 秒上报一次（首次 5 秒延迟）
- 暂停/分离时上报（分离时 3s 超时）
- 无痕模式禁用上报
- Web: `/x/click-interface/web/heartbeat`；App: `/x/v2/history-report`

**付费视频试看**：
- 检测 `needPay` 标志
- 试看片段显示"视频需付费，当前为试看片段"提示

#### 3.9.10 自定义快捷键

**功能描述**：用户可将遥控器按键绑定到播放器动作。

**可绑定键**：除 BACK / ESCAPE / BUTTON_B / DPAD_CENTER / ENTER / NUMPAD_ENTER 外的所有正数 keyCode

**简单动作**（无参数）：
- ShowInfo（显示信息）
- OpenSettings（打开设置）
- OpenVideoList（打开分集列表）
- OpenRelatedVideos（打开相关视频）
- TogglePlayPause（播放/暂停）
- PlayPrevious（上一集）
- PlayNext（下一集）
- OpenVideoDetail（打开详情页）
- OpenUpPage（打开 UP 主页）
- ToggleLoop（循环开关）
- ToggleDanmaku（弹幕开关）
- ToggleSubtitle（字幕开关）
- TogglePersistentBottomProgress（常显进度条开关）
- LikeVideo [新增]（点赞）
- CoinVideo [新增]（投币）
- FavoriteVideo [新增]（收藏）
- OpenComments [新增]（打开评论）

**值动作**（带参数）：
- SetPlaybackSpeed（0.25 - 4.0）
- SetResolution（12 种画质）
- SetAudio（5 种音轨）
- SetVideoCodec（5 种编码）
- SetAspectRatio（3 种宽高比）
- SetDanmakuScale（0.5 - 4.0）
- SetDanmakuOpacity（0 - 1）
- SetDanmakuSpeedFactor（0.5 - 1.5）
- SetDanmakuArea（0 - 1）
- SetDanmakuMaskEnabled（开/关）
- SetDanmakuLevelFilter [新增]（0-6 级屏蔽）
- SetSubtitleFontSize（12 - 48 SP）
- SetSubtitleBackgroundOpacity（0 - 1）
- SetSubtitleBottomPadding（0 - 48 DP）

**Toggle 语义**：值动作按键再次按下时恢复上一个值（如绑定"2x 倍速"，再按恢复原速）

**快捷键配置弹窗**：
- 阶段：Main → CaptureKey → PickAction → (PickActionValue) → ConfirmClear
- 捕获键阶段监听任意按键，禁用键 Toast 提示
- 动作选择列出所有可绑定动作分组
- 值选择列出该动作的可选值

**快捷键触发提示 [新增]**：
- 自定义快捷键触发时弹出 Toast 提示动作名称（如"已切换到 2x 倍速"）
- 提示时长 1.5 秒
- 可在设置中关闭此提示

**持久化**：JSON 序列化（版本化 `v=1`），紧凑键名，向后兼容别名

#### 3.9.11 播放器内设置菜单

**功能描述**：右侧滑出设置面板，4 个 Tab 导航。

**Tab 结构**：

| Tab | 图标 | 内容 |
|---|---|---|
| 倍速 | `Icons.Outlined.Speed` | 0.5/1/1.25/1.5/2 倍速选择 |
| 画面 | `Icons.Outlined.Image` | 画质/编码/宽高比/音轨 |
| 弹幕 | `Icons.Outlined.ClearAll` | 类型开关/大小/透明度/速度/区域/蒙版/等级屏蔽 [新增] |
| 字幕 | `Icons.Outlined.ClosedCaption` | 轨道/字号/背景/边距 |

**焦点状态**：`MenuNav`（导航）↔ `Menu`（菜单项）↔ `Items`（值列表），D-pad Left 逐级返回

**直播模式适配 [新增]**：
- 直播流时，"倍速"Tab 隐藏（直播不支持变速）
- "画面"Tab 隐藏画质/编码（直播流固定）
- 保留"弹幕"与"字幕"Tab

#### 3.9.12 CDN 选择 [简化]

**移除项**：
- ~~官方 CDN 优先设置~~
- ~~Ali CDN 域名替换~~
- ~~代理 URL 域名替换~~

**保留逻辑**：
- 过滤 P2P MCDN URL（`.mcdn.bilivideo.`）
- 过滤 `.szbdyd.com` URL
- 过滤纯 IP URL
- 选首个有效 URL

**直播流 [新增]**：直接使用直播 API 返回的流地址，无 CDN 选择

#### 3.9.13 播放器内评论 [新增]

**功能描述**：播放器内可查看视频评论。

**详见**：第 4.2 节（评论浏览）

### 3.10 搜索

#### 3.10.1 搜索输入页

**功能描述**：三列横向布局的搜索输入界面。

**布局**：
```
Row
├─ 搜索输入框 + 软键盘 (6×6 字母数字网格)
├─ 热词/建议 (空关键词显示热词，有输入显示建议)
└─ 搜索历史 (可删除单条/全部)
```

**软键盘**：6×6 网格（A-Z + 0-9）+ 清除/删除/搜索按钮

**搜索历史**：
- 存入 Room `search_history` 表
- 按时间倒序，支持单条删除与全部删除（二次确认）

**热词开关**：可在设置中隐藏热词（`showHotword`）

#### 3.10.2 搜索结果页

**功能描述**：四类结果 Tab 切换。

| Tab | 列数 | 卡片类型 |
|---|---|---|
| 视频 | 4 | SmallVideoCard |
| 番剧 | 6 | SeasonCard |
| 影视 | 6 | SeasonCard |
| 用户 | 3 | UpCard |

**筛选**（仅视频 Tab，菜单键触发）：
- 排序：综合/最多播放/最新发布/最多弹幕/最多收藏/最多评论/最多点赞
- 时长：全部/<10min/10-30min/30-60min/>60min
- 分区：全部 + 主分区
- 子分区：选中分区时显示

**无限滚动**：20 项阈值触发加载

### 3.11 个人页

#### 3.11.1 顶部导航

**功能描述**：个人页 4 个 Tab。

| Tab | 说明 |
|---|---|
| 稍后再看 | Watch Later 列表 |
| 历史 | 观看历史 |
| 收藏 | 收藏夹列表 |
| 追番 | Followed PGC seasons |

**可配置**：Tab 顺序可调，首个 Tab 可设置（`firstPersonalTopNavItem`）

#### 3.11.2 稍后再看

**功能描述**：稍后再看列表，分为"未看完"与"已看完"两组。

**布局**：4 列网格，分组显示

**操作**：
- 卡片长按快捷操作：添加/删除稍后再看
- 卡片 `delToView=true`：稍后再看图标变为删除图标

**数据流**：`/x/v2/history/toview`

#### 3.11.3 历史

**功能描述**：观看历史列表，4 列网格无限滚动。

**数据流**：
- Web: `/x/web-interface/history/cursor`
- App gRPC: `History.CursorV2`

**视频卡片已播进度条**：历史页卡片显示已播放进度条（基于历史数据中的 `progress` / `duration`）

#### 3.11.4 收藏

**功能描述**：收藏夹列表，顶部 Tab 切换收藏夹。

**布局**：顶部 `TabRow`（收藏夹切换）+ 4 列网格（视频列表）

**数据流**：`/x/v3/fav/resource/list`

#### 3.11.5 追番

**功能描述**：追番列表，4 列网格。

**布局**：`SeasonCard` 网格，含筛选弹窗（类型/状态）

**数据流**：`/pgc/app/follow/v2/{type}` 或 `/x/space/bangumi/follow/list`

### 3.12 UP 主页

**功能描述**：UP 主空间页，展示 UP 主视频列表。

**布局**：4 列网格 `SmallVideoCard`

**数据流**：
- Web: `/x/space/wbi/arc/search`
- App: `app.bilibili.com/x/v2/space/archive/cursor`

**视频卡片已播进度条**：UP 页卡片显示已播放进度条（接口 `playback_position` 字段）

### 3.13 设置

设置页采用左右分栏布局（左侧导航 + 右侧内容），D-pad Left 从内容返回导航。

#### 3.13.1 设置分类

| 分类 | 内容 |
|---|---|
| 音视频 | 默认画质/编码/音轨/倍速/播放结束动作/自定义快捷键/软解开关 |
| 界面 | 启动页/首页置顶/个人页置顶/显示视频详情页/常显进度条/界面缩放/主题 [新增] |
| 其他 | 接口选择 (Web/App)/日志查看/崩溃上报端点 [新增] |
| 存储 | 图片缓存/其他缓存/崩溃日志/交互日志 [新增] 清理 |
| 信息 | 设备信息/编解码信息 |
| 关于 | 版本信息/检查更新 |

**移除的设置项**：
- ~~Cookies 导入/导出~~
- ~~FPS 显示开关~~
- ~~代理设置（启用代理/HTTP代理服务器/gRPC代理服务器）~~
- ~~官方 CDN 优先（PCDN）~~
- ~~播放器类型（PlayerType，原版仅 Media3 可选）~~

#### 3.13.2 主题设置 [新增]

**功能描述**：黑夜/白天主题切换。

**选项**：
| 选项 | 说明 |
|---|---|
| 跟随系统 | 根据系统暗色模式自动切换 |
| 黑夜模式 | 固定深色 |
| 白天模式 | 固定浅色 |

**默认**：跟随系统

**实现**：Compose `MaterialTheme` 的 `colorScheme` 根据 `uiMode` 切换 `darkColorScheme()` / `lightColorScheme()`

#### 3.13.3 崩溃上报端点 [新增]

**功能描述**：配置崩溃日志上报的自建服务器端点。

**规格**：
- 默认空（关闭上报）
- 用户填入 URL 后启用上报
- 崩溃发生时自动 POST 崩溃日志 + 设备信息 + 最近交互日志
- 上报失败静默忽略（不影响用户体验）
- 明确提示"崩溃日志将发送至您配置的服务器"

#### 3.13.4 缓存自动清理 [新增]

**功能描述**：缓存达到阈值时自动清理。

**规格**：
- 图片缓存阈值：默认 500MB，可在存储设置中调整
- 其他缓存阈值：默认 200MB
- 达到阈值时自动清理最旧缓存（LRU）
- 存储设置页显示当前缓存大小与阈值

### 3.14 其他功能

#### 3.14.1 在线更新

**功能描述**：基于 GitHub Release 检查应用更新。

**数据流**：`api.github.com/repos/{owner}/{repo}/releases`

**交互**：
- 设置 → 关于 → 检查更新
- 发现新版本 → 弹出 `UpdateDialog` 显示版本号与更新日志
- 下载 APK → 安装

#### 3.14.2 日志网页端

**功能描述**：局域网内通过浏览器查看/下载日志。

**实现**：Ktor CIO HTTP 服务器（随机端口）
- `/` — 日志管理 UI（HTML）
- `/api/logs/list` — 日志文件列表 JSON
- `/api/logs/{filename}` — 下载日志文件（白名单：`logs_manual_*` / `logs_crash_*` / `logs_interaction_*` [新增]）
- `/api/logs/create-manual-and-download` — 创建并下载手动日志

#### 3.14.3 CDN 测速

**功能描述**：WebView 方式的 CDN 测速页。

#### 3.14.4 编解码信息

**功能描述**：显示设备 MediaCodec 编解码器能力。

**内容**：每个解码器的类型（硬/软）、最大实例、颜色格式、音频码率范围、视频码率范围、帧率范围、各分辨率下支持/可达帧率

#### 3.14.5 区域封锁页

**功能描述**：检测到区域封锁时显示提示页。

> 注：移除代理后，区域封锁内容无法访问。显示明确提示"此内容受区域限制，当前版本不支持访问"。

---

## 4. 功能需求 - 新增功能（new BV 规划）

本章描述 new BV 相对原版 BV 的新增功能需求。每项标注优先级（P0 核心必做 / P1 重要 / P2 增强）。

### 4.1 直播观看 [P0]

#### 4.1.1 功能描述

原版 BV 已有直播底层 API（`BiliLiveHttpApi` 获取直播间信息、`LiveDataWebSocket` 接收弹幕），但无任何直播 UI。new BV 补全完整的直播观看功能。

#### 4.1.2 用户故事

- 作为用户，我想在电视上看 B 站直播，包括关注的 UP 直播、分区直播
- 作为用户，我想在直播中发送/查看弹幕，了解直播信息

#### 4.1.3 功能规格

**4.1.3.1 直播首页**

布局结构：
```
LazyColumn
├─ LiveCarousel (直播轮播，推荐直播间)
├─ FeatureButtons
│   ├─ 关注直播 (关注的 UP 正在直播)
│   └─ 分区导航
├─ LiveFeedRow (分区直播列表，横向滚动)
└─ LiveRankRow (直播排行榜)
```

**4.1.3.2 直播分区**

直播分区列表（游戏/娱乐/单机/手游/虚拟主播/生活/知识/赛事/电台/户外等），每个分区显示直播流列表。

**4.1.3.3 直播播放器**

直播播放器复用 `VideoPlayerScreen`，但适配直播场景：

| 差异点 | 点播 | 直播 |
|---|---|---|
| 流类型 | DASH (视频+音频分离) | HLS / FLV |
| 进度条 | 显示，可 seek | 隐藏，不可 seek |
| 缩略图 | 显示 | 隐藏 |
| 分集列表 | 显示 | 隐藏 |
| 倍速 | 可调 | 隐藏（直播固定 1x） |
| 画质/编码 | 可选 | 隐藏（直播流固定） |
| 弹幕源 | XML 接口 | WebSocket 实时 |
| 心跳 | 播放心跳 | 直播心跳接口 |
| 在线人数 | 无 | 显示 [见 4.6] |
| 时长 | 视频时长 | 直播时长（从开播计算） |

**直播弹幕 WebSocket**：
- 连接直播弹幕服务器（WSS）
- 发送认证包（房间 ID + 用户 token）
- 每 30 秒发送心跳包
- 解析 `DANMU_MSG` 事件为弹幕
- 解析其他事件（礼物/进场/关注等，可选显示）

**4.1.3.4 直播间信息**

播放器内显示：
- 主播头像与名称
- 直播标题
- 在线人数（实时更新）
- 直播分区标签
- 关注按钮

#### 4.1.4 数据流

| 功能 | 接口 |
|---|---|
| 直播首页 | `api.live.bilibili.com/xlive/web-interface/v1/index/...` |
| 分区直播 | `api.live.bilibili.com/room/v1/area/getListByAreaID` |
| 关注直播 | `api.live.bilibili.com/xlive/app-interface/v1/relation/liveList` |
| 直播间信息 | `api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom` |
| 流地址 | `api.live.bilibili.com/xlive/web-room/v1/playUrl/playUrl` |
| 弹幕 token | `api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo` |
| 弹幕 | WebSocket WSS |

#### 4.1.5 与原版差异

原版 `BiliLiveHttpApi` 与 `LiveDataWebSocket` 代码保留并复用，新增完整 UI 层。

### 4.2 评论浏览 [P1]

#### 4.2.1 功能描述

原版 BV 无评论功能。new BV 新增评论浏览，支持详情页评论预览 + 完整评论页 + 播放器内评论。

#### 4.2.2 用户故事

- 作为用户，我想在看视频前看评论了解内容质量
- 作为用户，我想在播放时查看评论互动

#### 4.2.3 功能规格

**4.2.3.1 详情页评论预览**

布局：详情页相关视频上方插入评论预览区
- 显示前 3 条热门评论（头像 + 用户名 + 内容摘要）
- 底部"查看全部 N 条评论"按钮 → 跳转完整评论页

**4.2.3.2 完整评论页**

布局：
```
LazyColumn
├─ 顶部 Tab (全部/热门)
├─ 评论排序选项
└─ 评论列表
    ├─ CommentItem
    │   ├─ 头像 + 用户名 + 等级
    │   ├─ 内容
    │   ├─ 点赞数 + 回复数
    │   └─ 楼中楼 (展开子评论)
    └─ ...
```

**评论项交互**：
- 点击评论项：展开楼中楼（子评论）
- 点赞按钮：评论点赞
- 无限滚动加载

**4.2.3.3 播放器内评论 [P2]**

布局：播放器内右侧覆盖层（类似分集列表），展示评论
- 简化版评论列表（头像 + 用户名 + 内容）
- 不支持楼中楼（空间有限）
- 播放器内评论为只读浏览，不发送

#### 4.2.4 数据流

| 功能 | 接口 |
|---|---|
| 评论列表 | `/x/v2/reply/main` (Web) 或 `app.bilibili.com/x/v2/reply/main` (App) |
| 楼中楼 | `/x/v2/reply/reply` |
| 点赞评论 | `/x/v2/reply/action` |

#### 4.2.5 实现说明

- 评论数据模型包含：用户信息、内容、点赞数、回复数、时间、等级
- 楼中楼懒加载（展开时请求子评论）
- 评论页独立 NavGraph 路由，支持从详情页或播放器进入

### 4.3 触屏适配 [P0]

#### 4.3.1 功能描述

原版 BV 仅支持 D-pad 遥控器交互。new BV 抽象交互模式层，同时支持 D-pad 与触屏。

#### 4.3.2 交互模式抽象

原版基于设备能力互斥选择 D-pad/Touch，无法处理"触屏设备也想用遥控器"的场景。
new BV 改为**运行时追踪最近输入方式**，始终同时支持两种输入。

```kotlin
enum class InputMethod {
    DPad,   // D-pad / 遥控器 / 键盘方向键
    Touch   // 触屏点击/手势
}

class InteractionTracker(initial: InputMethod = InputMethod.DPad) {
    private val _inputMethod = MutableStateFlow(initial)
    val inputMethod: StateFlow<InputMethod> = _inputMethod.asStateFlow()

    fun onTouch() { _inputMethod.value = InputMethod.Touch }
    fun onDpadKey() { _inputMethod.value = InputMethod.DPad }
}

// Activity 驱动
override fun onTouchEvent(event: MotionEvent) { tracker.onTouch() }
override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (isDpadKey(keyCode)) tracker.onDpadKey()
    return super.onKeyDown(keyCode, event)
}

// Composable 注入
CompositionLocalProvider(LocalInteractionTracker provides tracker) {
    BVTheme { /* tracker 已在子树中传播 */ }
}
```

#### 4.3.3 适配规格

**4.3.3.1 焦点处理**

| 组件 | D-pad 模式 | 触屏模式 |
|---|---|---|
| 视频卡片 | 焦点边框高亮，D-pad 导航 | 点击直接触发，无焦点边框 |
| 列表 | D-pad 滚动跟随焦点 | 手势滑动滚动 |
| 按钮 | 焦点高亮 + 确认触发 | 点击触发 |
| 弹窗 | D-pad 导航选项 | 点击选项 |

**4.3.3.2 播放器手势 [触屏模式]**

| 手势 | 动作 |
|---|---|
| 单击 | 显示/隐藏控制器 |
| 双击 | 播放/暂停 |
| 左右滑动 | 快进/快退 |
| 上下滑动（左半屏） | 亮度调节 |
| 上下滑动（右半屏） | 音量调节 |
| 长按 | 倍速播放（2x） |
| 捏合 | 缩放（宽高比） |

**4.3.3.3 滚动优化**

- D-pad 模式：焦点驱动滚动，`focusRestorer` 保持焦点
- 触屏模式：手势滚动，fling 支持

#### 4.3.4 实现原则

- 所有交互组件基于 `InputMethod`（`InteractionTracker` 运行时追踪）动态适配
- 不硬编码 D-pad 逻辑，`focusedBorder` 自动根据 `InputMethod` 显示/隐藏
- 始终同时支持两种输入：用户拿起遥控器→焦点边框出现，用户触摸屏幕→焦点边框消失

### 4.4 黑夜/白天主题切换 [P1]

#### 4.4.1 功能描述

原版固定深色主题。new BV 支持黑夜/白天/跟随系统三种模式。

#### 4.4.2 功能规格

**主题选项**：
| 选项 | 说明 |
|---|---|
| 跟随系统 | 根据系统 `uiMode` 自动切换 |
| 黑夜模式 | 固定深色 |
| 白天模式 | 固定浅色 |

**实现**：
- `Theme.kt` 中根据 `Prefs.themeMode` + 系统 `uiMode` 决定 `colorScheme`
- 黑夜：`darkColorScheme()`（黑色背景 + 白色文字）
- 白天：`lightColorScheme()`（白色背景 + 黑色文字）
- 播放器始终深色（视频观看场景）

**设置入口**：设置 → 界面 → 主题

### 4.5 缓存满阈值自动清理 [P2]

#### 4.5.1 功能描述

图片/其他缓存达到阈值时自动清理最旧缓存，避免占用过多存储。

#### 4.5.2 功能规格

- 图片缓存阈值：默认 500MB，可调（100MB - 2GB）
- 其他缓存阈值：默认 200MB，可调（50MB - 1GB）
- 清理策略：LRU（最久未访问优先清理）
- 检查时机：App 启动 + 每次写入缓存后
- 存储设置页显示：当前缓存大小 / 阈值 / 手动清理按钮

#### 4.5.3 实现

- Coil 3.x 的 `ImageLoader` 配置 `maxSizeBytes` 与 `maxSize` 自动管理
- 其他缓存（更新包、缩略图等）手动管理，工具类 `CacheManager` 统一处理

### 4.6 视频卡片已播进度条 [P1]

#### 4.6.1 功能描述

在 UP 主页、稍后再看、历史页的视频卡片上显示已播放进度条，让用户直观了解观看进度。

#### 4.6.2 功能规格

**显示位置**：
- UP 主页视频卡片
- 稍后再看视频卡片
- 历史页视频卡片
- 相关视频卡片（可选）

**显示规则**：
- 卡片封面底部叠加进度条（与 B 站官方客户端一致）
- 进度条高度 3dp，白色半透明背景 + 主题色前景
- 进度计算（两种数据形式统一为 0~1 比例）：
  - 历史 / 稍后再看：`progress / duration`（均为秒）
  - UP 主页 / 相关视频：`playback_position / 100`（百分比）
- 无观看记录的卡片不显示进度条

**数据源**：
- 历史页：接口返回数据含 `progress` / `duration`（秒）
- 稍后再看：接口返回数据含 `progress` / `duration`（秒）
- UP 主页：接口返回数据含 `playback_position`（0-100 百分比）
- 相关视频：接口返回数据含 `playback_position`（同 UP 主页接口）

无需本地缓存，所有场景进度均来自各页面接口返回值。

### 4.7 播放器兼容直播源 [P0]

#### 4.7.1 功能描述

播放器需同时兼容点播（DASH）与直播（HLS/FLV）流，播放器内设置根据流类型自适应。

#### 4.7.2 功能规格

详见第 4.1.3.3 节（直播播放器差异表）

**核心适配点**：
- `BvMediaPlayer` 接口新增 `isLive` 标志
- Media3 实现：直播用 `HlsMediaSource` 或 `ProgressiveMediaSource`（FLV），点播用 `MergingMediaSource`
- 播放器 UI 根据 `isLive` 隐藏/显示控件
- 弹幕数据源切换（XML → WebSocket）
- 心跳接口切换（播放心跳 → 直播心跳）

### 4.8 播放器内评论显示 [P2]

详见第 4.2.3.3 节（播放器内评论）

### 4.9 播放器点赞/投币/收藏快捷键 [P1]

#### 4.9.1 功能描述

在播放器内可通过快捷键或控制栏快捷入口执行点赞/投币/收藏操作。

#### 4.9.2 功能规格

**自定义快捷键**：
- `LikeVideo`（点赞/取消点赞）
- `CoinVideo`（投币，默认 1 枚）
- `FavoriteVideo`（收藏/取消收藏，默认收藏夹）
- 已纳入第 3.9.10 节自定义快捷键动作列表

**控制栏快捷入口**：
- `ControllerVideoInfo` 底部按钮行新增点赞/投币/收藏图标按钮
- 点赞按钮：短按点赞/取消，长按一键三连（与详情页一致）
- 投币按钮：弹出数量选择
- 收藏按钮：弹出收藏夹选择（或快速收藏）

**操作反馈**：
- 操作成功：Toast 提示"已点赞"/"已投币"/"已收藏"
- 操作失败：Toast 提示错误信息
- 按钮状态实时更新（已点赞/已投币/已收藏高亮）

### 4.10 弹幕按用户等级屏蔽 [P2]

#### 4.10.1 功能描述

弹幕数据包含用户等级信息，new BV 支持按等级屏蔽低等级弹幕。

#### 4.10.2 功能规格

- 设置项：弹幕等级阈值（0-6 级，默认 0 = 不屏蔽）
- 入口：播放器设置菜单 → 弹幕 Tab → 等级屏蔽
- 弹幕渲染时过滤：`if (danmaku.level < threshold) skip`
- 数据源：弹幕 XML 数据已含 `level` 字段（第 9 列）

#### 4.10.3 自定义快捷键

支持绑定快捷键快速切换等级阈值（`SetDanmakuLevelFilter`，0-6 值）

### 4.11 播放器同时观看人数显示 [P2]

#### 4.11.1 功能描述

播放器内显示视频的实时同时观看人数。

#### 4.11.2 功能规格

**点播视频**：
- 显示"xxx 人正在看"
- 数据来源：视频详情接口的 `stat` 或独立接口
- 更新频率：播放期间定期刷新（如每 60 秒）

**直播**：
- 显示"xxx 人在线"
- 数据来源：直播间信息接口 + WebSocket 实时更新
- 实时刷新

**显示位置**：
- 播放器信息控制器（`ControllerVideoInfo`）顶部栏，与时钟并列
- 直播信息控制器（`LiveInfoController`）[新增]

### 4.12 自定义快捷键触发时 Toast 提示 [P2]

#### 4.12.1 功能描述

自定义快捷键触发时弹出 Toast 提示当前执行的动作。

#### 4.12.2 功能规格

- 触发快捷键时 Toast 显示动作名称（如"已切换到 2x 倍速"、"已点赞"、"弹幕已关闭"）
- Toast 时长：1.5 秒
- Toast 位置：屏幕底部居中
- 设置开关：设置 → 音视频 → 快捷键触发提示（默认开）

### 4.13 全交互 log 记录 [P1]

#### 4.13.1 功能描述

记录所有用户交互（按键/点击/导航/播放操作）到本地日志文件，便于问题定位与行为分析。

#### 4.13.2 功能规格

**记录范围**：
- 遥控器/触屏按键事件（keyCode + 动作描述）
- 页面导航（源页面 → 目标页面）
- 播放器操作（播放/暂停/seek/切换画质等）
- 设置变更（设置项 + 旧值 → 新值）
- 卡片操作（点击/长按/快捷操作）

**日志格式**：
```
[2026-07-20 10:30:45.123] [PlayerScreen] [KEY_EVENT] keyCode=DPAD_CENTER action=PlayPause
[2026-07-20 10:30:46.456] [PlayerScreen] [PLAYBACK] action=SeekTo position=120000
[2026-07-20 10:31:00.789] [MainScreen] [NAVIGATION] from=Home to=VideoDetail aid=12345
[2026-07-20 10:31:05.012] [VideoDetailScreen] [CARD_ACTION] action=Like aid=12345
[2026-07-20 10:31:10.345] [SettingsScreen] [SETTING_CHANGE] key=defaultQuality oldValue=R1080P newValue=R4K
```

**存储**：
- 文件：`logs_interaction_YYYYMMDD.log`（按天分文件）
- 路径：应用私有存储 `files/logs/`
- 滚动策略：单文件最大 10MB，保留最近 7 天
- 写入方式：异步写入（不阻塞 UI）

**查看方式**：
- Ktor 网页端 `/api/logs/list` 列出交互日志文件
- `/api/logs/{filename}` 下载
- 网页端 UI 增加交互日志分类

**崩溃关联**：
- 崩溃发生时，自动将最近 100 条交互日志附入崩溃报告
- 便于从崩溃日志还原用户操作路径

**隐私**：
- 交互日志仅记录操作类型与参数，不记录用户敏感信息（Cookie/Token 等）
- 默认开启，可在设置中关闭
- 日志文件仅本地存储，不上报（除非配置了崩溃上报端点）

---

## 5. 非功能性需求

### 5.1 稳定性

#### 5.1.1 焦点闪退治理

**问题**：原版 BV 频发焦点相关闪退（`focusRestorer` 异常、父子焦点冲突、导航切换焦点丢失）。

**对策**：
- 单 Activity + Navigation 架构天然解决跨 Activity 焦点丢失
- 统一焦点管理工具类 `FocusManager`，封装 `FocusRequester` 与 `focusRestorer`
- 所有列表组件使用标准化焦点恢复策略
- 焦点变更加 try-catch 防护，异常时重置焦点而非崩溃
- 全局未捕获异常处理器记录焦点相关崩溃堆栈

#### 5.1.2 播放器崩溃防护

**问题**：原版播放器弹幕蒙版 OOM、分 P 加载崩溃、播放器抽风报错。

**对策**：
- 弹幕蒙版内存优化：分段加载，单段最大 2MB，LRU 缓存淘汰
- 分 P 加载异常捕获，失败时降级为单 Part 播放
- 播放器错误统一处理：显示错误卡片 + 重试按钮，而非崩溃
- ViewModel 拆分降低单类复杂度，减少状态不一致
- 播放器生命周期严格管理：onPause 暂停、onDestroy 释放，避免内存泄漏

#### 5.1.3 OOM 防护

**对策**：
- 图片加载统一 Coil 3.x，配置 `maxSizeBytes` 限制
- 列表大数据分页加载，避免一次性加载过多
- 弹幕数据分批渲染，长视频弹幕按时间段分段
- 缩略图缓存 LRU 淘汰
- 定期 `System.gc()` 触发点（如退出播放器后）

#### 5.1.4 全局异常处理

- `Thread.setDefaultUncaughtExceptionHandler` 捕获未处理异常
- 记录崩溃日志：堆栈 + 设备信息 + 最近 100 条交互日志
- 崩溃日志写入 `logs_crash_YYYYMMDD_HHMMSS.log`
- 可选上报到自建端点（默认关闭）
- 崩溃后优雅退出（非强制关闭）

### 5.2 性能

#### 5.2.1 列表滚动流畅度

**目标**：列表滚动 60fps，无明显卡顿。

**对策**：
- `TvLazyVerticalGrid` / `LazyColumn` 标准化封装，统一焦点恢复
- 卡片渲染优化：避免嵌套过深，减少 `Modifier` 链
- 图片加载异步，占位图与错误图
- 滚动时暂停非关键加载（如缩略图预加载）
- 动画简化：原版部分动画过重，new BV 精简过渡动画

#### 5.2.2 弹幕蒙版性能

**对策**：
- 蒙版数据分段缓存，按时间段按需加载
- 蒙版帧查找优化：线性查找 + 缓存当前位置
- 轮询延迟自适应：帧激活时 20-300ms，无帧 100ms，暂停 200ms
- 蒙版位图复用，避免重复创建
- 长视频蒙版分段，单段最大 2MB

#### 5.2.3 缩略图缓存

**对策**：
- Sprite sheet 帧缓存 LRU，最大 50MB
- 低质量绘制（`FilterQuality.Low`）
- 帧提取异步，不阻塞 UI
- 缓存命中优先，未命中按需加载

#### 5.2.4 启动速度

**目标**：冷启动 ≤ 2 秒（中端设备）。

**对策**：
- Application.onCreate 异步初始化非关键组件
- 首页数据预加载（启动时并行请求动态/推荐/热门）
- Splash 屏过渡，掩盖初始化耗时
- 避免启动期同步 IO（Prefs.init 已优化为阻塞读取一次后异步同步）

### 5.3 兼容性

#### 5.3.1 Android 版本兼容

- minSdk 21（Android 5.0+），与原版一致
- 使用 `androidx.core` 兼容包处理版本差异
- Android 6.0 以下字体降级（原版逻辑保留）
- 编解码能力检测，不支持 H.265/AV1 的设备自动降级 H.264

#### 5.3.2 分辨率适配

- 4K 电视：默认弹幕比例 175%、透明度 70%、区域 50%（原版逻辑保留）
- 密度可调（`Prefs.density`，基于屏幕宽度 / 960f）
- 卡片网格列数自适应屏幕宽度

#### 5.3.3 遥控器兼容

- 标准按键映射（D-pad/Menu/Back/Media 键）
- 非标准遥控器键码兼容（`keyCodeToString` fallback）
- 游戏手柄兼容（BUTTON_B 作为返回键）

### 5.4 安全性

#### 5.4.1 凭证存储

- `AuthData`（含 SESSDATA / accessToken）存入 Room，序列化为 JSON
- Room 数据库不导出（`android:allowBackup` 谨慎配置）
- 未来考虑加密存储（EncryptedSharedPreferences / SQLCipher）

#### 5.4.2 风控规避

- UA 轮换池（Web UA + App UA 多个）
- buvid 本地生成（MAC 地址 MD5），避免设备指纹固定
- 请求失败自动降级 Web ↔ App 接口
- WBI 签名正确实现（mixin key 置换 + w_rid MD5）
- 不暴露代理配置给用户（移除代理功能简化风控面）

#### 5.4.3 隐私合规

- 无痕模式禁用心跳上报
- 交互日志不记录敏感信息（Cookie/Token/密码）
- 崩溃上报默认关闭，用户主动开启
- 不集成任何第三方统计/广告 SDK

### 5.5 可维护性

#### 5.5.1 模块化

- 清晰的模块边界（`:app` / `:bili-api` / `:player` / `:danmaku` / `:core` / `:data`）
- 依赖方向单向：UI → ViewModel → Repository → Data/API
- 禁止跨层调用（如 UI 直接调 API）

#### 5.5.2 ViewModel 拆分

- 单一职责原则，每个 ViewModel ≤ 300 行
- 原版巨型 ViewModel 拆分（见第 2.5 节）
- ViewModel 间通过 SharedFlow 通信，避免上帝对象

#### 5.5.3 统一错误处理

- Repository 层统一返回 `Result<T>` 或 sealed class
- UI 层统一错误展示（Toast / 错误页 / 重试）
- 网络错误分类：网络不可用 / 鉴权失败 / 风控 / 服务器错误 / 业务错误

#### 5.5.4 代码规范

- Ktlint + Detekt 静态检查
- 命名规范（ViewModel 后缀 / Repository 后缀 / Route 后缀）
- 公共组件抽取（`SmallVideoCard` / `VideosRow` / `TopNav` 等）
- 注释规范（复杂逻辑必注释，KDoc 规范）

### 5.6 包体积

#### 5.6.1 分包策略

- `lite` flavor：排除 VLC `.so`（若未来接入）、ABI 通用
- `default` flavor：全 ABI（arm64-v8a / armeabi-v7a / x86_64 / x86）
- ABI 拆分：可选按 ABI 生成独立 APK

#### 5.6.2 资源优化

- R8/ProGuard 混淆（release）
- 图片资源压缩（WebP）
- 移除未使用依赖（Firebase / Geetest / Koin）
- protobuf 代码裁剪（`ProtobufConfiguration` 仅编译用到的 proto）

---

## 6. 交互设计规范

### 6.1 TV 遥控器交互映射总表

#### 6.1.1 全局键位

| 按键 | 主页 | 详情页 | 搜索 | 设置 | 列表页 |
|---|---|---|---|---|---|
| D-pad Center | 进入/播放 | 播放/操作 | 搜索 | 确认 | 打开详情 |
| D-pad Up/Down | 切换导航 | 滚动 | 切换列 | 切换项 | 滚动 |
| D-pad Left/Right | 导航↔内容 | 切换按钮 | 切换列 | 导航↔内容 | 切换卡片 |
| Menu | 刷新 | — | 筛选 | — | 刷新 |
| Back | 退出确认 | 返回 | 返回 | 返回 | 返回 |

#### 6.1.2 播放器键位

详见第 3.9.2 节

### 6.2 触屏交互映射总表

#### 6.2.1 全局手势

| 手势 | 主页 | 详情页 | 搜索 | 设置 | 列表页 |
|---|---|---|---|---|---|
| 单击 | 打开详情 | 操作 | 搜索 | 选择 | 打开详情 |
| 长按 | 卡片快捷操作 | 一键三连 | 删除历史 | — | 卡片快捷操作 |
| 上下滑动 | 列表滚动 | 滚动 | 列表滚动 | 滚动 | 列表滚动 |
| 左右滑动 | Tab 切换 | — | — | — | — |

#### 6.2.2 播放器手势

详见第 4.3.3.2 节

### 6.3 焦点管理规范

#### 6.3.1 焦点恢复

- 列表滚动后焦点保持（`focusRestorer`）
- 导航返回后焦点恢复到之前位置
- 弹窗关闭后焦点回到触发元素

#### 6.3.2 父子焦点

- 父组件获取焦点时，默认聚焦首个子组件
- 子组件失去焦点时，焦点回到父组件
- 避免焦点丢失（无焦点元素时 fallback 到首个可聚焦元素）

#### 6.3.3 滚动聚焦

- 焦点元素滚动到可视区域（`bringIntoView`）
- 边缘卡片不完全显示时自动滚动
- 避免焦点驱动的过度滚动

### 6.4 视觉规范

#### 6.4.1 主题

- 深色主题（默认）：黑色背景 (`#000000`) + 白色文字 (`#FFFFFF`)
- 浅色主题：白色背景 (`#FFFFFF`) + 黑色文字 (`#000000`)
- 主题色：B 站粉 (`#FB7299`) 作为强调色
- 播放器始终深色

#### 6.4.2 卡片样式

**SmallVideoCard**：
- 封面 16:9，圆角 8dp
- 渐变叠加：播放量 + 弹幕量 + 时长（底部）
- 标题 2 行， ellipsize
- UP 主名 + 图标 + 投稿时间
- 焦点边框：2dp 主题色，圆角 8dp
- 已播进度条 [新增]：底部 3dp，主题色

**SeasonCard**：
- 竖版封面 0.75 比例
- 评分徽章（右下角渐变）
- 标题 + 副标题

#### 6.4.3 密度适配

- `density` 基于屏幕宽度 / 960f 计算
- 用户可在设置中调整（0.5 - 5.0）
- 影响字体大小与间距缩放

#### 6.4.4 动画

- 过渡动画：300ms 标准时长
- 焦点缩放：0.95 → 1.0（150ms spring）
- 弹窗入场：fade + slide（200ms）
- 避免：过度动画、嵌套动画导致的卡顿

---

## 7. 设置项完整清单

### 7.1 音视频设置

| 设置项 | 类型 | 默认 | 范围/选项 | 存储 |
|---|---|---|---|---|
| 默认画质 | 枚举 | 1080P (80) | 240P/360P/480P/720P/720P60/1080P/1080P+/1080P60/4K/HDR/Dolby/8K | `dq` |
| 默认视频编码 | 枚举 | AVC | AVC/HEVC/AV1/DVH1/HVC1 | `dvc` |
| 默认音频编码 | 枚举 | 192K (30280) | 64K/132K/192K/Dolby/HiRes | `da` |
| 默认播放速度 | 枚举 | 1.0x | 0.5/1/1.25/1.5/2 | `dps` |
| 播放结束动作 | 枚举 | PlayNext | Pause/PlayNext/PlayRelated/Exit | `action_after_play` |
| 自定义播放快捷键 | JSON | "" | 可绑定多组 | `player_custom_shortcuts` |
| 快捷键触发提示 [新增] | 开关 | 开 | — | `shortcut_toast` |
| 启用视频软解 | 开关 | 关 | — | `enable_software_video_decoder` |
| 启用音频软解 (FFmpeg) | 开关 | 关 | — | `enable_ffmpeg_audio_renderer` |

### 7.2 界面设置

| 设置项 | 类型 | 默认 | 范围/选项 | 存储 |
|---|---|---|---|---|
| 启动页 | 枚举 | 主页 | 搜索/个人/主页/分区/影视/直播 | `home_left_nav` |
| 首页置顶 Tab | 枚举 | 动态 | 动态/推荐/热门 | `first_home_top_nav` |
| 个人页置顶 Tab | 枚举 | 稍后再看 | 稍后再看/历史/收藏/追番 | `first_personal_top_nav` |
| 显示视频详情页 | 开关 | 开 | 关闭后点击直接播放 | `show_video_info` |
| 显示常显进度条 | 开关 | 关 | — | `show_persistent_seek` |
| 界面缩放 | 浮点 | 屏幕宽度/960f | 0.5 - 5.0 | `density` |
| 主题 [新增] | 枚举 | 跟随系统 | 跟随系统/黑夜/白天 | `theme_mode` |

### 7.3 弹幕设置（播放器内）

| 设置项 | 类型 | 默认 | 范围/选项 | 存储 |
|---|---|---|---|---|
| 弹幕类型 | 多选 | 全部 | 全部/滚动/顶部/底部 | `ddts` |
| 弹幕大小 | 浮点 | 1.75 | 0.5 - 4.0 | `dds2` |
| 弹幕透明度 | 浮点 | 0.7 | 0 - 1 | `ddo` |
| 弹幕速度 | 枚举 | 1.0x | 0.5/0.75/1.0/1.25/1.5 | `ddsf` |
| 弹幕区域 | 浮点 | 0.5 | 0 - 1 | `dda` |
| 防遮挡蒙版 | 开关 | 关 | — | `prefer_enable_webmark` |
| 用户等级屏蔽 [新增] | 整数 | 0 | 0-6 | `danmaku_level_filter` |

### 7.4 字幕设置（播放器内）

| 设置项 | 类型 | 默认 | 范围/选项 | 存储 |
|---|---|---|---|---|
| 字幕轨道 | 枚举 | 关闭 | 关闭 + 可用轨道 | 运行时 |
| 字幕字号 | 整数 SP | 24 | 12 - 48 | `dsfs` |
| 字幕背景透明度 | 浮点 | 0.4 | 0 - 1 | `dsbo` |
| 字幕底部边距 | 整数 DP | 12 | 0 - 48 | `dsbp` |

### 7.5 其他设置

| 设置项 | 类型 | 默认 | 范围/选项 | 存储 |
|---|---|---|---|---|
| 接口选择 | 枚举 | Web | Web/App | `api_type` |
| 查看日志 | 动作 | — | 跳转日志页 | — |
| 交互日志记录 [新增] | 开关 | 开 | — | `interaction_log` |
| 崩溃上报端点 [新增] | 字符串 | "" (关闭) | URL | `crash_report_endpoint` |

### 7.6 存储设置

| 设置项 | 类型 | 默认 | 说明 |
|---|---|---|---|
| 图片缓存大小 | 只读+清理 | — | 显示当前大小，可清理 |
| 其他缓存大小 | 只读+清理 | — | 含更新包等 |
| 崩溃日志 | 只读+清理 | — | 崩溃日志文件 |
| 交互日志 [新增] | 只读+清理 | — | 交互日志文件 |
| 图片缓存阈值 [新增] | 整数 MB | 500 | 100 - 2000 | `image_cache_threshold` |
| 其他缓存阈值 [新增] | 整数 MB | 200 | 50 - 1000 | `other_cache_threshold` |

### 7.7 已删除的设置项（相对原版）

| 原版设置项 | 删除原因 |
|---|---|
| Cookies 导入/导出 | 简化，不再需要手动 Cookie 管理 |
| FPS 显示开关 | 调试用，非用户功能 |
| 启用代理 / HTTP 代理服务器 / gRPC 代理服务器 | 代理功能完全删除 |
| 官方 CDN 优先 (PCDN) | 改为内部自动策略 |
| 播放器类型 (PlayerType) | 仅 Media3，无需选择 |

### 7.8 账户相关设置（Prefs，非设置页）

| 属性 | 类型 | 默认 | 存储 |
|---|---|---|---|
| isLogin | Boolean | false | `il` |
| uid | Long | 0 | `uid` |
| sid | String | "" | `sid` |
| sessData | String | "" | `sd` |
| biliJct | String | "" | `bj` |
| uidCkMd5 | String | "" | `ucm` |
| tokenExpiredData | Date | Date(0) | `ted` |
| accessToken | String | "" | `access_token` |
| refreshToken | String | "" | `refresh_token` |
| buvid | String | 自动生成 | `random_buvid` |
| buvid3 | String | 自动生成 | `random_buvid3` |
| incognitoMode | Boolean | false | `im` |
| showHotword | Boolean | true | `shw` |

---

## 8. 数据接口清单

### 8.1 Web HTTP API 清单

| 功能 | 端点 | 鉴权 | 用途 |
|---|---|---|---|
| 推荐 | `/x/web-interface/wbi/index/top/feed/rcmd` | SESSDATA | 首页推荐 |
| 热门 | `/x/web-interface/popular` | SESSDATA | 热门视频 |
| 视频详情 | `/x/web-interface/wbi/view/detail` | SESSDATA | 视频信息 |
| UGC 播放地址 | `/x/player/playurl` | SESSDATA | fnval=4048, qn=127 |
| PGC 播放地址 v2 | `/pgc/player/web/v2/playurl` | SESSDATA + buvid3 | PGC 流地址 |
| 弹幕 (XML) | `/x/v1/dm/list.so` | SESSDATA | 弹幕数据 |
| 动态 | `/x/polymer/web-dynamic/v1/feed/all` | SESSDATA | 关注动态 |
| 用户信息 | `/x/space/acc/info` | SESSDATA | UP 主信息 |
| 自身信息 | `/x/space/myinfo` | SESSDATA | 登录用户信息 |
| 历史 | `/x/web-interface/history/cursor` | SESSDATA | 观看历史 |
| 稍后再看 | `/x/v2/history/toview` | SESSDATA | 稍后再看列表 |
| 稍后再看添加 | `/x/v2/history/toview/add` | SESSDATA+csrf | 添加 |
| 稍后再看删除 | `/x/v2/history/toview/del` | SESSDATA+csrf | 删除 |
| 相关视频 | `/x/web-interface/archive/related` | 无 | 相关推荐 |
| 收藏夹列表 | `/x/v3/fav/resource/list` | SESSDATA | 收藏列表 |
| 收藏操作 | `/x/v3/fav/resource/deal` | SESSDATA+csrf | 收藏/取消 |
| 心跳 (Web) | `/x/click-interface/web/heartbeat` | SESSDATA+csrf | 历史上报 |
| 视频更多信息 | `/x/player/wbi/v2` | SESSDATA+buvid3 | 字幕/进度/蒙版 |
| 点赞 | `/x/web-interface/archive/like` | SESSDATA+csrf | 点赞 |
| 投币 | `/x/web-interface/coin/add` | SESSDATA+csrf+buvid3 | 投币 |
| 一键三连 | `/x/web-interface/archive/like/triple` | SESSDATA+csrf | 三连 |
| UP 主空间 | `/x/space/wbi/arc/search` | SESSDATA | UP 视频 |
| 番剧详情 | `/pgc/view/web/season` | SESSDATA | PGC 详情 |
| 番剧追番 | `/pgc/web/follow/add` / `/del` | SESSDATA+csrf | 追番 |
| 时间表 | `/pgc/web/timeline` | 无 | 番剧时间表 |
| 关注/取关 | `/x/relation/modify` | SESSDATA+csrf | 关注 UP |
| 搜索 | `/x/web-interface/wbi/search/...` | buvid3 | 搜索 |
| 搜索热词 | `/x/web-interface/wbi/search/square` | buvid3 | 热词 |
| 搜索建议 | `s.search.bilibili.com/main/suggest` | buvid3 | 建议 |
| PGC 索引 | `/pgc/season/index/result` | 无 | 番剧索引 |
| PGC Feed | `/pgc/page/web/v3/feed` | 无 | PGC 推荐 |
| PGC 初始状态 | HTML 抓取 `__INITIAL_STATE__` | 无 | 轮播数据 |
| 追番列表 | `/x/space/bangumi/follow/list` | SESSDATA | 我的追番 |
| Nav/WBI 密钥 | `/x/web-interface/nav` | 无 | WBI 签名 |
| 视频截图 | `/x/player/videoshot` | 无 | 缩略图 |
| 分区动态 | `app.bilibili.com/x/v2/region/dynamic` | access_key | 分区 |
| 评论 [新增] | `/x/v2/reply/main` | SESSDATA | 评论列表 |
| 楼中楼 [新增] | `/x/v2/reply/reply` | SESSDATA | 子评论 |
| 评论点赞 [新增] | `/x/v2/reply/action` | SESSDATA+csrf | 评论点赞 |
| 直播首页 [新增] | `api.live.bilibili.com/xlive/web-interface/v1/index/...` | SESSDATA | 直播推荐 |
| 直播间信息 [新增] | `api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom` | SESSDATA | 直播详情 |
| 直播流地址 [新增] | `api.live.bilibili.com/xlive/web-room/v1/playUrl/playUrl` | SESSDATA | 直播流 |
| 直播弹幕信息 [新增] | `api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo` | SESSDATA | 弹幕 token |

### 8.2 App gRPC API 清单

| 功能 | Service | RPC | 用途 |
|---|---|---|---|
| UGC 播放地址 | `Player` | `PlayViewUnite` | UGC 流地址 |
| PGC 播放地址 | `PlayURL` | `PlayView` | PGC 流地址 |
| 弹幕元数据 | `DM` | `DmView` | 弹幕/字幕/蒙版 |
| 弹幕分段 | `DM` | `DmSegMobile` | 弹幕分段 |
| 视频详情 | `View` | `View` | 视频信息 |
| 热门 | `Popular` | `Index` | 热门视频 |
| 历史 | `History` | `CursorV2` | 观看历史 |
| 搜索建议 | `Search` | `Suggest3` | 搜索建议 |
| 类型搜索 | `Search` | `SearchByType` | 分类搜索 |
| 全量搜索 | `Search` | `SearchAll` | 全量搜索 |
| 动态 | `Dynamic` | `DynVideo` | 动态视频 |

**gRPC Metadata**：
- `authorization`: `identify_v1 {accessKey}`
- `x-bili-metadata-bin`: protobuf（accessKey, mobiApp, device, build, channel, buvid, platform）
- `x-bili-device-bin`: protobuf（appId, mobiApp, device, build, channel, buvid, platform）
- `x-bili-local-bin`: protobuf（timezone）
- `x-bili-network-bin`: protobuf（network type）

### 8.3 本地持久化清单

#### 8.3.1 Room 数据库

**AppDatabase**（version 4，从原版 v3 升级）：

| 表 | 字段 | 用途 |
|---|---|---|
| `user` | id, uid, username, avatar, auth(JSON), lock | 多账户管理 |
| `search_history` | id, keyword, search_date | 搜索历史 |
| `interaction_log` [新增] | id, timestamp, screen, action, params(JSON) | 交互日志（可选缓存） |

#### 8.3.2 DataStore Preferences

详见第 7 章设置项清单（所有 `存储` 列的 key）。

#### 8.3.3 文件存储

| 路径 | 内容 |
|---|---|
| `files/logs/logs_manual_*.log` | 手动日志 |
| `files/logs/logs_crash_*.log` | 崩溃日志 |
| `files/logs/logs_interaction_*.log` [新增] | 交互日志 |
| `cache/image_cache/` | Coil 图片缓存 |
| `cache/other/` | 其他缓存（更新包等） |

---

## 9. 里程碑与优先级

### 9.1 优先级定义

| 优先级 | 含义 | 目标 |
|---|---|---|
| P0 | 核心必做 | 首个可用版本必须完成 |
| P1 | 重要 | 核心体验完整，建议首版包含 |
| P2 | 增强 | 后续迭代补充 |

### 9.2 迭代计划

#### Milestone 1: 基础架构搭建 (P0)

- 单 Activity + Navigation 骨架
- Hilt DI 集成
- 主题系统（黑夜/白天/跟随系统）
- 交互模式抽象（D-pad / Touch）
- Room + DataStore 基础
- bili-api 模块迁移（移除代理、移除 Firebase）
- 播放器引擎抽象（VOD）

#### Milestone 2: 核心功能 (P0)

- 登录（QR + 多账户 + 用户锁）
- 主页导航（6 项含直播入口）
- 首页（动态/推荐/热门）
- 视频详情页
- 番剧详情页
- 播放器（VOD 全功能：弹幕/字幕/画质/快捷键）
- 搜索
- 个人页（稍后再看/历史/收藏/追番）
- 设置（音视频/界面/其他/存储/信息/关于）

#### Milestone 3: 新增核心 (P0-P1)

- 直播观看 (P0)
- 触屏适配 (P0)
- 播放器兼容直播源 (P0)
- 评论浏览 - 详情页 (P1)
- 黑夜/白天主题切换 (P1)
- 视频卡片已播进度条 (P1)
- 播放器点赞/投币/收藏快捷键 (P1)
- 全交互 log 记录 (P1)
- 崩溃监控本地体系 (P1)

#### Milestone 4: 增强 (P2)

- 播放器内评论 (P2)
- 弹幕按用户等级屏蔽 (P2)
- 播放器同时观看人数显示 (P2)
- 自定义快捷键触发 Toast 提示 (P2)
- 缓存满阈值自动清理 (P2)
- 评论 - 播放器内 (P2)

### 9.3 验收标准

每个 Milestone 的验收标准：

| Milestone | 验收标准 |
|---|---|
| M1 | 应用可启动，Navigation 可跳转，主题可切换，Hilt 注入正常 |
| M2 | 完整 VOD 观看流程（登录→浏览→播放→弹幕字幕→历史），功能对标原版 BV |
| M3 | 直播可观看，触屏可操作，评论可浏览，崩溃可记录，主题可切换 |
| M4 | 所有 P2 功能完成，稳定性与性能达标 |

---

## 10. 风险与对策

### 10.1 技术风险

| 风险 | 影响 | 概率 | 对策 |
|---|---|---|---|
| B 站接口风控加强 | 接口不可用 | 高 | UA 轮换 + Web/App 接口自动降级 + buvid 生成优化 |
| gRPC 接口变更 | App 接口不可用 | 中 | proto 文件版本管理，及时跟进更新 |
| Compose TV 组件不稳定 | UI 异常 | 中 | 关注 androidx.tv 版本更新，必要时降级稳定版 |
| akdanmaku 库维护停滞 | 弹幕渲染问题 | 中 | 保留 fork 能力，必要时自行修复 |
| Media3 直播流兼容性 | 直播播放失败 | 中 | HLS/FLV 多格式支持，失败时提示重试 |
| Navigation-Compose 焦点问题 | 焦点丢失 | 中 | 统一焦点管理工具，充分测试 |

### 10.2 产品风险

| 风险 | 影响 | 概率 | 对策 |
|---|---|---|---|
| 区域限制内容无法访问 | 部分内容不可用 | 高 | 明确提示，移除代理后已知限制 |
| B 站账号封禁 | 用户无法使用 | 中 | 无痕模式 + 风控规避 + 提示谨慎使用 |
| 第三方客户端法律风险 | 项目下架 | 低 | 不商业化，开源，明确"不支持中国大陆使用" |
| 用户隐私担忧 | 用户流失 | 低 | 透明日志政策，默认不上报，无第三方 SDK |

### 10.3 工程风险

| 风险 | 影响 | 概率 | 对策 |
|---|---|---|---|
| 架构重构工作量大 | 进度延误 | 高 | 分 Milestone 迭代，M1 先搭骨架 |
| 原版代码迁移复杂 | 迁移错误 | 中 | 逐模块迁移，保留原版作为对照 |
| 播放器 ViewModel 拆分困难 | 状态不一致 | 中 | 充分测试，SharedFlow 通信 |
| 直播功能从零开发 | 进度风险 | 中 | 复用原版底层 API，优先 UI 骨架 |
| 测试覆盖不足 | 质量问题 | 中 | 关键路径单元测试 + 手动测试清单 |

---

## 附录

### A. 原版 BV 源码参考索引

| 模块 | 路径 | 关键文件 |
|---|---|---|
| App 主入口 | `bv/app/src/main/kotlin/dev/aaa1115910/bv/` | `BVApp.kt`, `MainActivity.kt` |
| 活动 | `activities/` | `MainActivity`, `VideoPlayerV3Activity`, `VideoInfoActivity`, `SeasonInfoActivity`, `LoginActivity`, `SettingsActivity` |
| 屏幕 | `screen/` | `MainScreen`, `VideoPlayerV3Screen`, `VideoInfoScreen`, `SeasonInfoScreen`, `SearchInputScreen`, `SearchResultScreen`, `PersonalContent`, `LoginScreen`, `SettingsScreen` |
| 播放器控制 | `component/controllers/` | `VideoPlayerController`, `MenuController`, `ControllerVideoInfo`, `VideoListController`, `RelatedVideosController` |
| 视频卡片 | `component/videocard/` | `SmallVideoCard`, `LargeVideoCard`, `SeasonCard`, `VideosRow` |
| 设置 | `screen/settings/content/` | `AudioVideoSetting`, `UISetting`, `NetworkSetting`, `OtherSetting`, `StorageSetting` |
| ViewModel | `viewmodel/` | `VideoPlayerV3ViewModel`(1417行), `VideoInfoViewModel`, `MainViewModel` |
| Repository | `repository/` | `UserRepository`, `VideoInfoRepository` |
| DAO | `dao/` | `AppDatabase`, `UserDao`, `SearchHistoryDao` |
| bili-api | `bili-api/src/main/kotlin/dev/aaa1115910/biliapi/` | `BiliHttpApi`, `BiliHttpProxyApi`(删除), `BiliPassportHttpApi`, `BiliLiveHttpApi` |
| 播放器引擎 | `bv-player/src/main/kotlin/.../player/` | `AbstractVideoPlayer`, `ExoMediaPlayer`, `BvVideoPlayer` |
| 字幕 | `bili-subtitle/` | `SubtitleParser`, `SubtitleEncoder` |
| 构建配置 | `buildSrc/src/main/kotlin/` | `AppConfiguration`, `ProtobufConfiguration` |

### B. 移除功能清单

| 移除项 | 原版位置 | 原因 |
|---|---|---|
| Firebase Crashlytics / Analytics | `app/build.gradle.kts`, `BVApp.kt` | 国内不可用 |
| 代理 HTTP API | `bili-api/http/BiliHttpProxyApi.kt` | 代理功能完全删除 |
| ProxyArea 区域代理 | `app/entity/proxy/ProxyArea.kt` | 代理功能完全删除 |
| gRPC 代理通道 | `bili-api/repositories/ChannelRepository.kt` | 代理功能完全删除 |
| Ali CDN 替换 | `VideoPlayerV3ViewModel.kt` `replaceUrlDomainWithAliCdn` | 代理功能完全删除 |
| 官方 CDN 优先设置 | `Prefs.kt` `preferOfficialCdn`, `NetworkSetting.kt` | 改内部自动策略 |
| Cookies 导入导出 | `component/settings/CookiesDialog.kt` | 简化 |
| FPS 显示 | `Prefs.kt` `showFps`, `component/FpsMonitor.kt` | 调试功能 |
| PlayerType 选择 | `entity/PlayerType.kt`, `PlayerTypeSetting.kt` | 仅 Media3 |
| Koin DI | 全项目 | 替换为 Hilt |
| Geetest SDK | `libs.versions.toml`, `SmsLoginContent.kt` | 短信登录暂不启用 |
| VLC 播放器 | `libs/libVLC/`, `LibVLCDownloaderDialog.kt` | 仅 Media3 |

### C. 新增功能清单

| 新增项 | 优先级 | 章节 |
|---|---|---|
| 直播观看 | P0 | 4.1 |
| 触屏适配 | P0 | 4.3 |
| 播放器兼容直播源 | P0 | 4.7 |
| 评论浏览（详情页） | P1 | 4.2 |
| 黑夜/白天主题 | P1 | 4.4 |
| 视频卡片已播进度条 | P1 | 4.6 |
| 播放器点赞/投币/收藏快捷键 | P1 | 4.9 |
| 全交互 log 记录 | P1 | 4.13 |
| 崩溃监控本地体系 | P1 | 5.1.4 |
| 播放器内评论 | P2 | 4.8 |
| 弹幕按用户等级屏蔽 | P2 | 4.10 |
| 播放器同时观看人数 | P2 | 4.11 |
| 自定义快捷键 Toast 提示 | P2 | 4.12 |
| 缓存满阈值自动清理 | P2 | 4.5 |

---

**文档结束**

> 本 PRD 基于 BV 源码分析与 new BV 重构需求编写，将作为 new BV 开发的需求基线。开发过程中如需变更，请通过修订记录更新文档版本。
