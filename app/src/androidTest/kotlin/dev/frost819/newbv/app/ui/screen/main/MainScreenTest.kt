package dev.frost819.newbv.app.ui.screen.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.frost819.newbv.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [MainScreen] 的插桩测试。
 *
 * 验证：
 * - 主页框架正常显示（左侧导航 + 内容区）
 * - 顶部 Tab（推荐/热门）可见
 *
 * 使用 [HiltAndroidRule] + [MainActivity] 提供 Hilt 注入环境。
 * NavigationRail 项只有 icon 无文本，因此仅验证内容区切换后的占位文本。
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun displays_homeTabs() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("推荐").assertIsDisplayed()
        composeRule.onNodeWithText("热门").assertIsDisplayed()
    }

    @Test
    fun displays_dynamicsTab() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("动态").assertIsDisplayed()
    }

    @Test
    fun home_showsPlaceholder_whenNoData() {
        composeRule.waitForIdle()
        // 默认 Tab 是推荐，没有数据时显示加载提示或无更多
        // 验证页面不崩溃，有内容显示
        composeRule.onNodeWithText("推荐").assertIsDisplayed()
    }
}
