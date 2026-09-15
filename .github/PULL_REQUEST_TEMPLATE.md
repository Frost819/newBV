<!-- 提交前请阅读 AGENTS.md §7.3 PR 规范 -->

## 变更类型

<!-- 勾选一项 -->

- [ ] feat：新功能
- [ ] fix：修复 bug
- [ ] refactor：重构（不改功能）
- [ ] test：新增/修改测试
- [ ] docs：文档
- [ ] chore：构建/工具
- [ ] style：格式

## 关联 Issue

<!-- 如 Closes #123 -->

## 做了什么

<!-- 简述本次改动 -->

## 为什么

<!-- 动机/背景 -->

## 如何测试

<!-- 复现/验证步骤，或运行的命令 -->

## 自检清单

- [ ] 一个 PR 只含一个功能点，未混合无关改动
- [ ] 已通过 `./gradlew test`
- [ ] 已通过 `./gradlew ktlintCheck`
- [ ] 新增/修改功能已包含对应测试
- [ ] 公开 API 已补 KDoc 注释
- [ ] 未提交 `local.properties`、`google-services.json` 等敏感文件
- [ ] 未引入 Firebase / 代理 / VLC / Koin / 新增 Activity（AGENTS.md §9）
- [ ] 涉及路由跳转的可聚焦元素已接入 `FocusSaver`（如适用）
