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

**这不是 BV 的 1:1 复刻，而是架构重构 + 功能增强。**

> 原版 BV 的代码仍保留在本仓库的 [`feature`](../../tree/feature) 分支。

## 与 BV 的差异

### 架构

- 单 Activity + Navigation-Compose（禁止新增 Activity）
- 全面使用 Hilt 依赖注入（不再使用 Koin / 手动单例）
- 模块化拆分：`app` / `core` / `data` / `bili-api` / `player` / `danmaku` / `danmaku-engine` / `bili-subtitle`
- 仅接入 Media3 播放器（不引入 VLC），播放器抽象同时支持 VOD 与 Live
- 删除全部代理相关逻辑（ProxyArea / Ali CDN 替换等）
- 无 Firebase，崩溃监控改为本地日志 + 可选自建上报

### 测试

- 单元测试（JUnit 5 + MockK + Turbine + Truth）覆盖率目标 ≥ 80%
- 接口集成测试按 source set 与单元测试物理隔离（`src/integrationTest`）
- JaCoCo 覆盖率报告

## 构建

### 环境要求

- JDK 17（设置 `JAVA_HOME`）
- Android SDK（compileSdk 36）

### 命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 运行全部单元测试
./gradlew test

# 代码检查
./gradlew ktlintCheck detekt

# 覆盖率报告
./gradlew test jacocoAggregatedReport
```

### 测试凭证配置

接口集成测试需要 B 站账号凭证，复制 `local.properties.template` 为 `local.properties` 并填写（**不要提交**）：

```properties
test.sessdata=你的SESSDATA
test.bili_jct=你的bili_jct
test.uid=你的uid
```

## 开发文档

- [AGENTS.md](AGENTS.md) — AI 协作开发规范（架构决策、代码规范、测试策略、踩坑经验）

## 致谢

- [aaa1115910/bv](https://github.com/aaa1115910/bv) — 本项目源自 BV 的重构
- [akdanmaku](https://github.com/Frost819/AkDanmaku) — 弹幕渲染引擎

## License

[MIT](LICENSE) © aaa1115910, Frost819
