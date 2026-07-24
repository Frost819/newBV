package dev.frost819.newbv.app

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Hilt 测试 Application 生成器。
 *
 * 生成 `HiltTestApplication_Application`，在插桩测试中替代 [BVApplication]，
 * 使用 Hilt 测试组件图。不含 [BVApplication] 的初始化逻辑（Prefs、CrashHandler 等），
 * 适合验证 Hilt 注入与 UI 基础功能。
 */
@CustomTestApplication(Application::class)
interface HiltTestApplication
