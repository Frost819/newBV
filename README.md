<div align="center">

<img src="app/src/main/res/drawable/ic_banner.webp" style="border-radius: 24px; margin-top: 32px;"/>

# newBV

**BV 的架构重构版**

[![Android Sdk Require](https://img.shields.io/badge/Android-5.0%2B-informational?logo=android)](https://apilevels.com/#:~:text=Jetpack%20Compose%20requires%20a%20minSdk%20of%2021%20or%20higher)
[![License](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)

**newBV 不支持在中国大陆地区内使用，如有相关使用需求请使用 [云视听小电视](https://app.bilibili.com)**

</div>

---

newBV 是基于 [BV](https://github.com/aaa1115910/bv) 重构的 [哔哩哔哩](https://www.bilibili.com) 第三方 `Android TV`
客户端，使用 `Jetpack Compose` 开发，支持 `Android 5.0+`（minSdk 21）。

**这不是 BV 的 1:1 复刻，而是架构重构 + 功能增强。保留了原版BV核心使用体验的同时，新增了许多呼声很高的功能，并重构了软件架构。**

> 原版 BV 的代码仍保留在本仓库的 [`bv-feature`](../../tree/bv-feature) 分支。

## 基于 BV 的升级点

### 功能升级

- **直播观看**：直播首页（推荐 / 分区 / 关注 / 直播排行榜）、直播播放（HLS/FLV，多线路手动切换）、WebSocket 实时弹幕、实时人气
- **评论**：详情页评论预览、独立评论弹窗（全部 / 热门、无限滚动、楼中楼、评论点赞）、播放器内评论
- **缓存自动清理**：缓存阈值可调（50/100/200/500MB / 不限制）、LRU 自动清理、手动清理、日志占用查看及清理
- **触屏适配**：D-pad / 触屏运行时双模式，播放器手势（快进 / 亮度 / 音量）
- **主题**：黑夜 / 白天 / 跟随系统三档，全新品牌配色
- **视频卡片已播进度条**：历史 / 稍后再看 / UP 主页卡片显示观看进度
- **播放器增强**：点赞·投币·收藏快捷键（长按一键三连）、同时观看人数、自定义快捷键触发提示、弹幕分段加载（基于AkDanmaku自研 `danmaku-engine`）
- **诊断与崩溃**：关键操作诊断日志 + 局域网日志网页端；本地崩溃日志 + 可选自建端点上报

### 架构

- 单 Activity + Navigation-Compose（禁止新增 Activity）
- 完全基于 StateFlow 重构，单向数据流（UDF）
- Hilt 依赖注入
- 仅 Media3 播放器，VOD + Live 双兼容
- 完全删除代理逻辑（ProxyArea / Ali CDN 等）
- 模块化拆分：`app` / `core` / `data` / `bili-api` / `player` / `danmaku` / `danmaku-engine` / `bili-subtitle`

### 测试

- 补全单元测试
- 补全接口集成测试

## 构建

### 环境要求

- JDK 17
- Android SDK（compileSdk 36）

## 开发文档

- [AGENTS.md](AGENTS.md) — AI 协作开发规范（架构决策、代码规范、测试策略、踩坑经验）

## 致谢

- [aaa1115910/bv](https://github.com/aaa1115910/bv) — 本项目源自 BV 的重构
- [cat3399/blbl](https://github.com/cat3399/blbl) — 部分实现（弹幕分段加载等）参考 / 移植自该项目
- [akdanmaku](https://github.com/Frost819/AkDanmaku) — 弹幕渲染引擎

## License

[MIT](LICENSE) © aaa1115910, Frost819
