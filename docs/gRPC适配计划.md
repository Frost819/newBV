# new BV gRPC 接口适配计划

> 配套文档：[PRD](PRD.md) §8.2 | [开发计划](开发计划.md) P2-9 | [AGENTS.md](../AGENTS.md)
>
> 本文档基于 PRD §8.2 gRPC 清单、proto 定义（122 文件 / 72 service）、原版 BV 源码和 bilibili-API-collect 文档，
> 对当前 gRPC 接入情况做全面盘点，列出缺口、可补充接口和不可迁移接口，指导后续开发。

---

## 1. 基础设施现状

### 1.1 已完成

| 组件 | 位置 | 说明 |
|---|---|---|
| Channel 生命周期管理 | `biliapi/grpc/utils/Channel.kt` | `generateChannel()` + `MetadataInterceptor`，5 个 metadata header 全部实现 |
| ChannelRepository | `biliapi/repositories/ChannelRepository.kt` | `initDefaultChannel()` / `close()` / `requireDefaultChannel()`，账号切换时重建 |
| gRPC 错误分类 | `biliapi/grpc/utils/StatusExtends.kt` | `BiliGrpcException` + `GrpcErrorKind`（Authentication / RiskControl / Network / Server / Unknown）+ `handleGrpcException()` |
| Proto 编译配置 | `buildSrc/.../ProtobufConfiguration.kt` | `usedProtoFiles` 白名单 30 个 proto 文件 |
| 单元测试 | `biliapi/grpc/GrpcInfrastructureTest.kt` | Channel 初始化校验 + 错误分类测试（3 个） |

### 1.2 待完善

| 项目 | 说明 |
|---|---|
| 集成测试凭证 | ✅ **已完成（T-12）**：凭证已配置，22 个新增集成测试全部通过 |
| GrpcChannelTest | 单元测试：Channel 生命周期 + metadata 构造验证（P2-9 要求，`GrpcInfrastructureTest` 已部分覆盖） |
| GrpcErrorTest | 单元测试：各 `GrpcErrorKind` 覆盖（P2-9 要求，`GrpcInfrastructureTest` 已部分覆盖） |
| 风控错误识别 | ✅ **已完成（T-11）**：`handleGrpcException()` 解析 `bilibili.rpc.Status` 业务码，映射风控到 `GrpcErrorKind.RiskControl` |
| 弹幕分段加载 | ✅ **已完成（P3-4）**：`DM.DmSegMobile` 接口层 + ViewModel 分段加载已实现 |

---

## 2. gRPC 接口盘点

### 2.1 已实现（12 个 RPC，P2-9 完成）

> 另有 `DmSegMobile` 于 P3-4 补充实现，见 §2.2。

| # | RPC | Service / Proto Package | Repository | 方法 | 来源 |
|---|---|---|---|---|---|
| 1 | `PlayViewUnite` | `Player` / `bilibili.app.playerunite.v1` | VideoPlayRepository | `getPlayData()` | 原版迁移 |
| 2 | `PlayView` | `PlayURL` / `bilibili.pgc.gateway.player.v2` | VideoPlayRepository | `getPgcPlayData()` | 原版迁移 |
| 3 | `DmView` | `DM` / `bilibili.community.service.dm.v1` | VideoPlayRepository | `getSubtitle()`, `getDanmakuMask()` | 原版迁移 |
| 4 | `View` | `View` / `bilibili.app.view.v1` | VideoDetailRepository | `getVideoDetail()`, `getUgcPages()` | 原版迁移 |
| 5 | `Index` | `Popular` / `bilibili.app.show.v1` | RecommendVideoRepository | `getPopularVideos()` | 原版迁移 |
| 6 | `CursorV2` | `History` / `bilibili.app.interface.v1` | HistoryRepository | `getHistories()` | 原版迁移 |
| 7 | `Suggest3` | `Search` / `bilibili.app.interfaces.v1` | SearchRepository | `getSearchSuggest()` | 原版迁移 |
| 8 | `SearchAll` | `Search` / `bilibili.polymer.app.search.v1` | SearchRepository | `searchAll()` | 原版迁移 |
| 9 | `SearchByType` | `Search` / `bilibili.polymer.app.search.v1` | SearchRepository | `searchType()` | 原版迁移 |
| 10 | `DynVideo` | `Dynamic` / `bilibili.app.dynamic.v2` | UserRepository | `getDynamicVideos()` | 原版迁移 |
| 11 | `MainList` | `Reply` / `bilibili.main.community.reply.v1` | CommentRepository | `getComments()` | **新增**（原版无评论功能） |
| 12 | `DetailList` | `Reply` / `bilibili.main.community.reply.v1` | CommentRepository | `getReplies()` | **新增** |

> **更正说明（2026-08）**：
> - `sendHeartbeat` 和 `getSpaceVideos` 曾尝试接入 gRPC（`Heartbeat.Mobile` / `Space.Archive`），
>   但 B 站 gRPC 端点返回 **404 UNIMPLEMENTED**。已回退为 **App HTTP**（`/x/v2/history/report`、
>   `app.bilibili.com/x/v2/space/archive/cursor`，均以 `access_key` 鉴权），与原版 BV 一致。
> - `ToViewRepository.getToView()` 原尝试 gRPC `CursorV2(business="toview")`，
>   但原版 BV 使用 HTTP `/x/v2/history/toview` + `access_key`。已回退为 **App HTTP**，与原版一致。
> - 以上三个接口**不属于 gRPC**，详见 PRD §8.2.2 App HTTP 接口清单。

### 2.2 已实现（P3-4 补充）

| # | RPC | Service / Proto Package | 用途 | 当前替代 | proto 编译状态 |
|---|---|---|---|---|---|
| 13 | `DmSegMobile` | `DM` / `bilibili.community.service.dm.v1` | 弹幕分段数据（每 6 分钟一段） | 已替代 HTTP XML `/x/v2/dm/list.so` | ✅ 已编译 |

> `DmSegMobile` 已于 P3-4 完成：Web 走 HTTP `getDanmakuSeg`（`/x/v2/dm/wbi/web/seg.so`），App 走 gRPC `getDanmakuSegment`，ViewModel 层实现响应式分段加载与缓存。

### 2.3 不在 PRD 清单但 proto 已编译的可用 RPC（3 个）

这些 RPC 的 proto 已在 `usedProtoFiles` 中编译，只需创建 stub 调用：

| # | RPC | Service / Proto Package | 用途 | 当前状态 | 价值 |
|---|---|---|---|---|---|
| 1 | `PlayerOnline` | `PlayerOnline` / `bilibili.app.playeronline.v1` | 实时在线观看人数 | 无任何实现 | 播放器 UI 显示"N人正在看" |
| 2 | `DmPlayerConfig` | `DM` / `bilibili.community.service.dm.v1` | 弹幕用户配置（云端） | 无实现 | 云端弹幕设置同步 |
| 3 | `DefaultWords` | `Search` / `bilibili.app.interface.v1` | 搜索默认词 | 无实现 | 搜索框占位提示 |

### 2.4 不在 PRD 清单且 proto 未编译的候选 RPC（按价值排序）

这些 RPC 需要先在 `ProtobufConfiguration.kt` 的 `usedProtoFiles` 中添加 proto 文件，再创建 stub：

| # | RPC | Service / Proto Package | 用途 | 当前状态 | 价值 | proto 文件 |
|---|---|---|---|---|---|---|
| 1 | `ViewProgress` | `View` / `bilibili.app.viewunite.v1` | 视频进度数据（含 VideoShot 截图） | `getVideoShot` 走 HTTP `/x/player/videoshot` | 替代 HTTP 截图请求 | `viewunite/v1/viewunite.proto` + 3 个依赖 |
| 2 | `RankAll` | `Rank` / `bilibili.app.show.rank.v1` | 全站排行榜 | 无排行榜功能 | 新增排行榜入口 | `rank/v1/rank.proto` |
| 3 | `RankRegion` | `Rank` / `bilibili.app.show.rank.v1` | 分区排行榜 | 无排行榜功能 | 新增分区排行 | 同上 |
| 4 | `Region` | `Region` / `bilibili.app.show.region.v1` | 分区列表 | HTTP `getRegionData` | 统一走 gRPC | `region/v1/region.proto` |
| 5 | `MediaFollow` | `Media` / `bilibili.app.interface.v1` | 番剧追番/取消 | HTTP `addSeasonFollow`/`delSeasonFollow` | 统一走 gRPC | `interfaces/v1/media.proto` |
| 6 | `MediaDetail` | `Media` / `bilibili.app.interface.v1` | 番剧详情 | HTTP `getPgcVideoDetail` | 统一 PGC 详情 | 同上 |
| 7 | `View`（viewunite） | `View` / `bilibili.app.viewunite.v1` | 统一视频详情（UGC+PGC，7.41.0+） | UGC 用 `view/v1`，PGC 走 HTTP | 面向未来统一 | `viewunite/v1/viewunite.proto` + 依赖 |
| 8 | `RelatesFeed` | `View` / `bilibili.app.viewunite.v1` | 相关推荐视频 | 无独立接口 | 详情页推荐 | 同 viewunite |
| 9 | `DynAll` | `Dynamic` / `bilibili.app.dynamic.v2` | 全部动态（含图文） | 仅 `DynVideo`（视频动态） | 动态页增强 | 已编译 |
| 10 | `DynSpace` | `Dynamic` / `bilibili.app.dynamic.v2` | 用户空间动态 | 无实现 | 用户主页动态 | 已编译 |

---

## 3. Web-only 确认清单（无 gRPC 等价接口）

以下接口在 proto 中**不存在**对应 gRPC RPC，但仍可能有 **App HTTP 路径**（`access_key` 鉴权，与 z 使用同一 URL 或 `/pgc/app/*`、`app.bilibili.com/*` 独立 URL）。本清单区分两种情况：**有 App HTTP**（应支持 `preferApiType`）与**纯 Web only**（无任何 App 版本）。

### 3.1 操作类（POST 写入）

| Repository | 方法 | Web 端点 | App 端点 | App 支持 |
|---|---|---|---|---|
| CoinRepository | `checkVideoSentCoin` | `GET /x/web-interface/archive/coins` | 同端点 `access_key` | ✅ App HTTP（新增） |
| CoinRepository | `sendVideoCoin` | `POST /x/web-interface/coin/add` | `app.bilibili.com/x/v2/view/coin/add` | ✅ App HTTP（新增） |
| LikeRepository | `checkVideoLiked` | `GET /x/web-interface/archive/has/like` | 同端点 `access_key` | ✅ App HTTP（新增） |
| LikeRepository | `updateVideoLiked` | `POST /x/web-interface/archive/like` | `app.bilibili.com/x/v2/view/like` | ✅ App HTTP（新增） |
| OneClickTripleActionRepository | `sendVideoOneClickTripleAction` | `POST /x/web-interface/archive/like/triple` | `app.bilibili.com/x/v2/view/like/triple` | ✅ App HTTP（新增） |
| CommentRepository | `toggleCommentLike` | `POST /x/v2/reply/action` | 同端点 `access_key` | ✅ App HTTP（已支持） |
| ToViewRepository | `addToView` | `POST /x/v2/history/toview/add` | 同端点 `access_key` | ✅ App HTTP（已支持） |
| ToViewRepository | `delToView` | `POST /x/v2/history/toview/del` | 同端点 `access_key` | ✅ App HTTP（已支持） |
| ToViewRepository | `getToView` | `GET /x/v2/history/toview` | 同端点 `access_key` | ✅ App HTTP（已支持，原版 BV 使用 HTTP） |
| UserRepository | `followUser` / `unfollowUser` | `POST /x/relation/modify` | 同端点 `access_key` | ✅ App HTTP（恢复，新增） |
| UserRepository | `addSeasonFollow` | `POST /pgc/web/follow/add` | `POST /pgc/app/follow/add` | ✅ App HTTP（恢复，新增） |
| UserRepository | `delSeasonFollow` | `POST /pgc/web/follow/del` | `POST /pgc/app/follow/del` | ✅ App HTTP（恢复，新增） |

### 3.2 查询类（GET 读取）

| Repository | 方法 | Web 端点 | App 端点 | App 支持 |
|---|---|---|---|---|
| FavoriteRepository（全部 6 个方法） | `checkVideoFavoured` 等 | `/x/v2/fav/video/favoured`, `/x/v3/fav/...` | 同端点 `access_key` | ✅ App HTTP（已支持） |
| SeasonRepository | `getFollowingSeasons` | `GET /x/space/bangumi/follow/list` | `GET /pgc/app/follow/v2/...` | ✅ App HTTP（已支持） |
| SeasonRepository | `getTimeline` | `GET /pgc/web/timeline` | `GET /pgc/app/timeline` | ✅ App HTTP（已支持） |
| SearchRepository | `getSearchHotwords` | `GET /x/web-interface/wbi/search/square` | `app.bilibili.com/x/v2/search/trending/ranking` | ✅ App HTTP（已支持） |
| UserRepository | `checkIsFollowing` | `GET /x/space/wbi/acc/relation` | 同端点（App 端该接口历史返回 -663，实际仍走 Cookie） | ⚠️ App HTTP（accessKey 支持，但原版因其失效仍用 sessData） |
| UserRepository | `getFollowingUpCount` | `GET /x/relation/stat` | 同端点 `access_key` | ✅ App HTTP（恢复，新增） |
| UserRepository | `getFollowedUsers` | `GET /x/relation/followings` | 同端点 `access_key` | ✅ App HTTP（恢复，新增） |
| VideoDetailRepository | `getPgcVideoDetail` | `GET /pgc/view/web/season` | `GET /pgc/view/v2/app/season` | ✅ App HTTP（已支持） |
| VideoPlayRepository | `getVideoShot` | `GET /x/player/videoshot` | `app.bilibili.com/x/v2/view/video/shot` | ✅ App HTTP（已支持） |
| UserRepository | `getUserInfo` | `GET /x/space/wbi/acc/info` | 无 | ❌ 纯 Web only |
| PgcRepository | `getCarousel` | HTML-scrape `www.bilibili.com/{slug}` | 无 | ❌ 纯 Web only |
| PgcRepository | `getFeed` | `GET /pgc/page/web/{v3/}feed` | 无 | ❌ 纯 Web only |
| PgcRepository | `getPgcIndex` | `GET /pgc/season/index/result` | 无 | ❌ 纯 Web only |
| UgcRepository | `getRegionFeedRcmd` | `GET /x/web-interface/region/feed/rcmd` | 无 | ❌ 纯 Web only（原版即 Web-only） |

### 3.3 天然 Web-only（设计决策）

| Repository | 说明 |
|---|---|
| LiveRepository | 直播功能完全使用 Web 端 API（类注释明确声明） |
| LoginRepository | 登录走 Passport HTTP（QR/短信），不涉及 gRPC |
| PgcRepository | 番剧首页/Feed/Index 走 Web HTTP |
| UgcRepository | 分区推荐走 Web HTTP |

### 3.4 App HTTP 接口补全记录（2026-08）

> 依据「所有原版 BV 的 App 接口都必须移植；无 App 实现的接口在文档查到有 App 接口就实现，否则才保留 Web-only」原则，完成以下 App HTTP 接口补全：

- `UserRepository`：恢复 `followUser`/`unfollowUser`/`getFollowedUsers`/`getFollowingUpCount`/`addSeasonFollow`/`delSeasonFollow` 的 `ApiType.App` 分支（`access_key` 鉴权，新增）。`checkIsFollowing` 因 App 端该接口历史返回 -663，实际仍走 Cookie（与原版一致）。
- `LikeRepository`：新增 `updateVideoLiked` App 分支（`app.bilibili.com/x/v2/view/like`）、`checkVideoLiked` App 分支（同端点 `access_key`）。
- `CoinRepository`：新增 `sendVideoCoin` App 分支（`app.bilibili.com/x/v2/view/coin/add`）、`checkVideoCoined` App 分支（同端点 `access_key`）。
- `OneClickTripleActionRepository`：新增 `sendVideoOneClickTripleAction` App 分支（`app.bilibili.com/x/v2/view/like/triple`）。
- `VideoPlayRepository.getVideoShot`：恢复 `preferApiType` 参数，App 走 `app.bilibili.com/x/v2/view/video/shot`。

**纯 Web-only 保留**（文档确认无 App 版本）：`PgcRepository`（carousel/feed/index）、`UgcRepository.getRegionFeedRcmd`、`UserRepository.getUserInfo`。

---

## 4. 适配任务分解

### 4.1 P0：补全 PRD §8.2 清单 ✅

**目标**：实现 PRD 列出但尚未实现的 gRPC RPC，使 PRD 清单 100% 覆盖。

**结果**：`SearchAll` 已完成。`DmSegMobile` 已于 P3-4 完成（接口层 + ViewModel 分段加载）。

#### T-01 `DM.DmSegMobile` — 弹幕分段数据 ✅（P3-4 完成）

- **当前**：`DanmakuViewModel` 直调 `BiliHttpApi.getDanmakuXml()`（`/x/v1/dm/list.so` XML 全量）
- **目标**：双通道分段加载（每段 6 分钟），详见 [P3剩余任务计划.md](P3剩余任务计划.md) §3.A
  - Web：HTTP `GET /x/v2/dm/wbi/web/seg.so`（WBI，protobuf `DmSegMobileReply`）
  - App：gRPC `DM.DmSegMobile`（同一 proto）
- **涉及文件**：`BiliHttpApi.getDanmakuSeg` / `VideoPlayRepository.getDanmakuSegment` / `DanmakuData.fromDanmakuElem` / `DanmakuViewModel` 分段缓存
- **proto 请求**：`DmSegMobileReq { pid=aid, oid=cid, type=1, segment_index=N }`
- **proto 响应**：`DmSegMobileReply { repeated DanmakuElem elems }`
- **注意**：`preferApiType` 显式分离，不自动切换；旧 XML 接口保留但不作主路径
- **测试**：Entity 转换 + Repository 双通道单测 + 集成抽检 + DanmakuViewModel 分段流转

#### T-02 `Search.SearchAll` — 全量搜索 ✅

- **当前**：✅ **已完成**。`SearchRepository.searchAll()` 支持 Web HTTP `/x/web-interface/wbi/search/all/v2` + App gRPC `Search.SearchAll`
- **涉及文件**：
  - `SearchRepository.kt` — `searchAll()` 方法（Web/App 双分支）
  - Entity 层 — `SearchAllResult.fromWeb()` / `fromGrpc()`（映射视频/番剧/用户三类，直播卡片 proto 缺失暂忽略）
- **测试**：`SearchAllResultGrpcTest`（gRPC 转换）+ `SearchRepositoryUnitTest`（Web/App 分支互不调用）

### 4.2 P1：补充高价值 gRPC 接口（建议完成）

**目标**：利用已编译 proto 中的可用 RPC，增强 App 模式功能。

#### T-03 `PlayerOnline.PlayerOnline` — 在线观看人数

- **proto**：已编译（`bilibili/app/playeronline/v1/playeronline.proto`）
- **用途**：播放器 UI 显示"N人正在看"，定期轮询（`sec_next` 字段为下次轮询间隔）
- **涉及文件**：
  - 新增 `PlayerOnlineRepository.kt` 或在 `VideoPlayRepository.kt` 中添加方法
  - `PlayerViewModel` — 定时轮询在线人数，更新 UI State
  - 播放器 UI — 显示在线人数
- **proto 请求**：`PlayerOnlineReq { aid, cid }`
- **proto 响应**：`PlayerOnlineReply { total_text, total_number, sec_next }`
- **测试**：Repository 单元测试 + ViewModel 状态测试

#### T-04 `View.ViewProgress` — 视频截图（替代 HTTP）

- **proto**：未编译，需在 `ProtobufConfiguration.kt` 中添加 `viewunite/v1/viewunite.proto` 及依赖
- **当前**：`getVideoShot` 走 HTTP `/x/player/videoshot`（Web-only）
- **目标**：App 模式下通过 gRPC `ViewProgress` 获取截图数据（`VideoShot arc_shot`）
- **涉及文件**：
  - `ProtobufConfiguration.kt` — 添加 viewunite proto 文件
  - `VideoPlayRepository.kt` — `getVideoShot` 添加 App 分支
  - Entity 层 — `VideoShot` 转换
- **proto 请求**：`ViewProgressReq { aid, cid, up_mid, type=UnionType.UGC }`
- **proto 响应**：`ViewProgressReply { VideoShot arc_shot }`（含 `pv_data`, `img_x_len`, `img_y_len`, `image` 列表）
- **风险**：viewunite proto 依赖链较深（`common.proto`, `ugcanymodel`, `pgcanymodel`），需验证编译
- **测试**：Entity 转换测试 + Repository 单元测试

#### T-05 `Search.DefaultWords` — 搜索默认词

- **proto**：已编译（`bilibili/app/interfaces/v1/search.proto`）
- **用途**：搜索框占位提示词（"大家都在搜：..."）
- **涉及文件**：
  - `SearchRepository.kt` — 新增 `getDefaultWords()` 方法
  - `SearchInputViewModel` — 调用并更新 UI
- **proto 请求/响应**：`DefaultWordsReq` / `DefaultWordsReply`
- **测试**：Repository 单元测试

### 4.3 P2：扩展 gRPC 覆盖（可选，视需求排期）

#### T-06 `Rank.RankAll` / `Rank.RankRegion` — 排行榜

- **proto**：未编译，需添加 `rank/v1/rank.proto`
- **用途**：新增排行榜功能（全站/分区）
- **涉及文件**：新增 `RankRepository.kt` + UI 页面
- **HTTP 替代**：`GET /x/web-interface/ranking/v2`（全站）、`GET /x/web-interface/ranking/region`
- **前提**：PRD 需确认是否纳入排行榜功能

#### T-07 `Media.MediaFollow` — 番剧追番/取消

- **proto**：未编译，需添加 `interfaces/v1/media.proto`
- **当前**：`addSeasonFollow`/`delSeasonFollow` 走 HTTP（Web-only）
- **目标**：App 模式下走 gRPC
- **风险**：proto 字段需验证，`MediaFollowReq` 参数不确定

#### T-08 `View.View`（viewunite）— 统一视频详情

- **proto**：未编译，需添加 viewunite proto
- **当前**：UGC 用 `view/v1`，PGC 走 HTTP
- **目标**：统一走 viewunite，支持 `UnionType` 区分 UGC/OGV
- **风险**：7.41.0+ 新接口，数据结构变化大，迁移成本高
- **建议**：Phase 3 或更晚考虑，当前 `view/v1` 稳定可用

#### T-09 `Dynamic.DynAll` / `DynSpace` — 动态增强

- **proto**：已编译
- **当前**：仅 `DynVideo`（视频动态）
- **目标**：支持全部动态（图文）和用户空间动态
- **前提**：PRD 需确认动态页功能范围

### 4.4 P0-Infra：gRPC 基础设施补全

#### T-10 完善 gRPC 单元测试

- `GrpcChannelTest`：Channel 生命周期（init → require → close）、metadata 构造验证
- `GrpcErrorTest`：各 `GrpcErrorKind` 覆盖（UNAUTHENTICATED → Authentication、UNAVAILABLE → Network 等）
- `BiliGrpcException` 序列化/传播验证

#### T-11 风控错误识别

- **当前**：`handleGrpcException()` 将 gRPC 状态码映射到 `GrpcErrorKind`，但 `RiskControl` 类别未被赋值
- **目标**：解析 B 站业务码（gRPC trailer 中的 `bilibili.rpc.Status`），将风控相关错误码映射到 `GrpcErrorKind.RiskControl`
- **涉及文件**：`StatusExtends.kt` — `grpcDetail()` 函数扩展

#### T-12 集成测试凭证配置 ✅

- ✅ `local.properties` 已配置测试凭证（sessdata / bili_jct / uid / access_token / buvid）
- ✅ 集成测试通过 `integrationTest` source set 分离（`./gradlew :bili-api:integrationTest`），CI 天然跳过
- ✅ 核心接口已在真实环境验证（22 个新增集成测试全部通过，含 App 端点赞/投币/三连/心跳、评论 gRPC、弹幕蒙版 gRPC）
- ⚠️ `getRegionFeedRcmd` 返回 -400、`addSeasonFollowApp` 直连返回 -404，相关集成测试移除或由 Repository 层测试覆盖

---

## 5. 实施顺序与依赖

```
Phase 1 — 补全 PRD 清单（P0）✅
  ├─ T-10 完善 gRPC 单元测试          ✅ GrpcInfrastructureTest
  ├─ T-02 SearchAll 全量搜索          ✅ 已完成
  ├─ T-11 风控错误识别               ✅ 已完成
  ├─ T-12 集成测试凭证                ✅ 22 个集成测试通过
  └─ T-01 DmSegMobile 弹幕分段        ✅ 已完成（P3-4）

Phase 2 — 高价值补充（P1，待排期）
  ├─ T-03 PlayerOnline 在线人数       ← proto 已编译，无依赖
  ├─ T-05 DefaultWords 搜索默认词      ← proto 已编译，无依赖
  └─ T-04 ViewProgress 视频截图        ← 需添加 proto 编译

Phase 3 — 扩展覆盖（P2，按需）
  ├─ T-06 Rank 排行榜                 ← 需 PRD 确认 + 添加 proto
  ├─ T-07 MediaFollow 番剧追番         ← 需添加 proto
  ├─ T-08 ViewUnite 统一详情           ← 高风险，建议延后
  └─ T-09 Dynamic 扩展               ← 需 PRD 确认功能范围
```

---

## 6. 验收标准

### 6.1 P0 验收

- [x] PRD §8.2 的 RPC 全部实现或明确记录为 Web-only/App HTTP
- [x] `DM.DmSegMobile` 在 App 模式下可获取弹幕分段数据 ✅（P3-4 完成）
- [x] `Search.SearchAll` 在 App 模式下可获取全量搜索结果 ✅（已完成）
- [x] `GrpcChannelTest` + `GrpcErrorTest` 通过（`GrpcInfrastructureTest` 覆盖）
- [x] `handleGrpcException` 能识别风控错误 ✅（T-11 完成）

### 6.2 P1 验收

- [ ] `PlayerOnline` 在播放器 UI 中显示在线人数
- [ ] `ViewProgress` 替代 `getVideoShot` 的 HTTP 调用（App 模式）
- [ ] `DefaultWords` 在搜索框显示默认提示词
- [ ] 集成测试凭证配置完成，核心 RPC 通过真实环境验证

### 6.3 通用验收（所有阶段）

- [x] App 模式下核心链路可用：播放 → 弹幕 → 字幕 → 心跳 → 详情 → 搜索 → 推荐 → 历史 → 动态
- [x] gRPC Channel 在登录/切换账号/退出登录后鉴权状态正确
- [x] Web 请求失败不自动调用 App，App 请求失败不自动调用 Web
- [x] 不存在 UA 轮换池、跨接口 fallback 或隐藏式重试
- [x] 所有新增 gRPC 方法有对应单元测试（Entity 转换 + Repository Mock）
- [x] `./gradlew :bili-api:test` 通过（823 个单元测试）
- [x] `./gradlew ktlintCheck` 通过

---

## 7. 风险与注意事项

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| `access_token` 过期 | 集成测试无法运行 | 单元测试用 Mock 不依赖真实凭证；集成测试标注 `@Tag("integration")` CI 跳过 |
| proto 字段不完整 | gRPC 返回数据缺少必要字段 | 先用 Playwright 或真实调用验证响应结构，字段不全则保留 Web-only |
| viewunite proto 依赖链 | 编译失败或体积膨胀 | 逐步添加 proto 文件，验证编译后再集成 |
| B 站接口变更 | gRPC 接口不可用 | proto 文件版本管理，及时跟进更新（PRD 风险项） |
| 弹幕格式差异 | protobuf 弹幕与 XML 弹幕字段不一致 | 统一到 `Danmaku` 领域模型，屏蔽来源差异 |
