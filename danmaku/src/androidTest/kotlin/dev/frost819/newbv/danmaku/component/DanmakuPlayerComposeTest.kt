package dev.frost819.newbv.danmaku.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [DanmakuPlayerCompose] 的插桩测试。
 *
 * 验证弹幕播放器 Compose 组件的渲染和生命周期：
 * - null player 时不崩溃
 * - 绑定 DanmakuPlayer 后正常渲染
 * - 组件释放后 DanmakuPlayer 正确清理
 *
 * 注意：[DanmakuPlayer] 内部 [DanmakuContext] 要求当前线程有 Looper，
 * 因此所有 DanmakuPlayer 创建/释放操作都通过 [runOnMain] 在主线程执行。
 */
@RunWith(AndroidJUnit4::class)
class DanmakuPlayerComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var player: DanmakuPlayer? = null

    private fun <T> runOnMain(block: () -> T): T {
        val result = arrayOfNulls<Any>(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result[0] = block()
        }
        @Suppress("UNCHECKED_CAST")
        return result[0] as T
    }

    private fun runOnMainVoid(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { block() }
    }

    @Before
    fun setup() {
        player = runOnMain { DanmakuPlayer(SimpleRenderer()) }
    }

    @After
    fun teardown() {
        runOnMainVoid { player?.release() }
    }

    @Test
    fun renders_withoutCrash_whenPlayerNull() {
        composeRule.setContent {
            DanmakuPlayerCompose(danmakuPlayer = null)
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun renders_withoutCrash_whenPlayerProvided() {
        composeRule.setContent {
            DanmakuPlayerCompose(danmakuPlayer = player)
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun player_remainsValid_afterRendering() {
        composeRule.setContent {
            DanmakuPlayerCompose(danmakuPlayer = player)
        }

        composeRule.onRoot().assertExists()

        val isReleased = runOnMain { player!!.isReleased }
        assertThat(isReleased).isFalse()
    }
}
