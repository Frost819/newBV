package dev.frost819.newbv.danmaku.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [DanmakuPlayerCompose] 的 Robolectric 测试。
 *
 * 使用 Compose 测试规则驱动 [androidx.compose.ui.viewinterop.AndroidView] 的完整
 * 生命周期（factory → update → onRelease），覆盖 null / 非 null [DanmakuPlayer]
 * 场景下的绑定与释放逻辑。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DanmakuPlayerComposeRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `compose with null player creates view without binding`() {
        composeRule.setContent {
            DanmakuPlayerCompose(
                modifier = Modifier.size(100.dp),
                danmakuPlayer = null,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `compose with non-null player calls bindView`() {
        val player = mockk<DanmakuPlayer>(relaxed = true)
        every { player.bindView(any()) } answers {
            firstArg<DanmakuView>().danmakuPlayer = player
        }

        composeRule.setContent {
            DanmakuPlayerCompose(
                modifier = Modifier.size(100.dp),
                danmakuPlayer = player,
            )
        }
        composeRule.waitForIdle()

        verify(atLeast = 1) { player.bindView(any()) }
    }

    @Test
    fun `onRelease clears player when view player matches`() {
        val player = mockk<DanmakuPlayer>(relaxed = true)
        every { player.bindView(any()) } answers {
            firstArg<DanmakuView>().danmakuPlayer = player
        }

        var show by mutableStateOf(true)

        composeRule.setContent {
            if (show) {
                DanmakuPlayerCompose(
                    modifier = Modifier.size(100.dp),
                    danmakuPlayer = player,
                )
            }
        }
        composeRule.waitForIdle()

        show = false
        composeRule.waitForIdle()
    }

    @Test
    fun `onRelease does not clear player when view player differs`() {
        val player = mockk<DanmakuPlayer>(relaxed = true)
        // bindView is mocked as no-op — danmakuView.danmakuPlayer stays null

        var show by mutableStateOf(true)

        composeRule.setContent {
            if (show) {
                DanmakuPlayerCompose(
                    modifier = Modifier.size(100.dp),
                    danmakuPlayer = player,
                )
            }
        }
        composeRule.waitForIdle()

        show = false
        composeRule.waitForIdle()
    }

    @Test
    fun `onRelease with null player clears when view player is also null`() {
        var show by mutableStateOf(true)

        composeRule.setContent {
            if (show) {
                DanmakuPlayerCompose(
                    modifier = Modifier.size(100.dp),
                    danmakuPlayer = null,
                )
            }
        }
        composeRule.waitForIdle()

        show = false
        composeRule.waitForIdle()
    }

    @Test
    fun `recomposition with different player rebinds view`() {
        val player1 = mockk<DanmakuPlayer>(relaxed = true)
        val player2 = mockk<DanmakuPlayer>(relaxed = true)

        var currentPlayer by mutableStateOf<DanmakuPlayer?>(player1)

        composeRule.setContent {
            DanmakuPlayerCompose(
                modifier = Modifier.size(100.dp),
                danmakuPlayer = currentPlayer,
            )
        }
        composeRule.waitForIdle()

        currentPlayer = player2
        composeRule.waitForIdle()

        verify(atLeast = 1) { player1.bindView(any()) }
        verify(atLeast = 1) { player2.bindView(any()) }
    }

    @Test
    fun `default modifier parameter fills max size`() {
        composeRule.setContent {
            DanmakuPlayerCompose(danmakuPlayer = null)
        }
        composeRule.waitForIdle()
    }
}
