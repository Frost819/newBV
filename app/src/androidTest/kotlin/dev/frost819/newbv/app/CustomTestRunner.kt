package dev.frost819.newbv.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * 自定义 JUnit Runner，用于 Hilt 插桩测试。
 *
 * 将 Application 替换为 Hilt 生成的测试 Application
 * （[HiltTestApplication] 生成的 `HiltTestApplication_BVApplication`），
 * 使测试使用 Hilt 测试组件图。
 */
class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        name: String,
        context: Context,
    ): Application =
        super.newApplication(
            cl,
            "dev.frost819.newbv.app.HiltTestApplication_Application",
            context,
        )
}
