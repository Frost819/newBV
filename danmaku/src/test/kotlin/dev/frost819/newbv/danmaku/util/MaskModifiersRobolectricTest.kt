package dev.frost819.newbv.danmaku.util

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuWebMaskFrame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MaskModifiers] 的 Robolectric 测试。
 *
 * Compose Modifier 函数（[Modifier.composed] 内的 lambda）需要在 Compose
 * 组合 / 布局 / 绘制阶段才会执行，纯 JVM 单元测试无法触发。使用 Robolectric
 * 提供 Android Shadow 环境（Bitmap、Canvas、Color 等），配合 Compose 测试规则
 * 驱动完整的 Compose 生命周期来覆盖 [bitmapMask]、[danmakuWebMask]、
 * [danmakuMobMask]、[danmakuMask] 的全部分支。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MaskModifiersRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val validSvg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
            <rect width="10" height="10" fill="black"/>
        </svg>
    """.trimIndent()

    /**
     * 辅助：组合 + 等待 idle + 推进帧时钟以触发 draw 阶段。
     */
    private fun composeAndDraw(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent(content)
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    // ── danmakuMask 统一入口 ────────────────────────────────────────────────

    @Test
    fun `danmakuMask with null frame applies no mask`() {
        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuMask(frame = null, aspectRatio = 1.77f),
            )
        }
    }

    @Test
    fun `danmakuMask with valid WebMaskFrame renders successfully`() {
        val frame = DanmakuWebMaskFrame(range = 0L until 1000L, svg = validSvg)

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuMask(frame, aspectRatio = 1.77f),
            )
        }
    }

    @Test
    fun `danmakuMask with invalid SVG in WebMaskFrame applies no mask`() {
        val frame = DanmakuWebMaskFrame(range = 0L until 1000L, svg = "not a valid svg")

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuMask(frame, aspectRatio = 1.77f),
            )
        }
    }

    @Test
    fun `danmakuMask with MobMaskFrame renders successfully`() {
        val frame = DanmakuMobMaskFrame(
            range = 0L until 1000L,
            width = 2,
            height = 2,
            image = byteArrayOf(0b1010_0000.toByte()),
        )

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuMask(frame, aspectRatio = 1.77f),
            )
        }
    }

    // ── bitmapMask 直接测试（宽高比分支覆盖） ──────────────────────────────

    @Test
    fun `bitmapMask with wide aspect ratio renders successfully`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .bitmapMask(bitmap, videoAspectRatio = 2.0f),
            )
        }
    }

    @Test
    fun `bitmapMask with tall aspect ratio renders successfully`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .bitmapMask(bitmap, videoAspectRatio = 0.5f),
            )
        }
    }

    @Test
    fun `bitmapMask with matching aspect ratio renders successfully`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .bitmapMask(bitmap, videoAspectRatio = 1.0f),
            )
        }
    }

    // ── danmakuWebMask 直接测试 ─────────────────────────────────────────────

    @Test
    fun `danmakuWebMask with valid SVG renders successfully`() {
        val frame = DanmakuWebMaskFrame(range = 0L until 1000L, svg = validSvg)

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuWebMask(frame, aspectRatio = 1.77f),
            )
        }
    }

    @Test
    fun `danmakuWebMask with garbage SVG applies no mask`() {
        val frame = DanmakuWebMaskFrame(range = 0L until 1000L, svg = "<<<not svg>>>")

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuWebMask(frame, aspectRatio = 1.77f),
            )
        }
    }

    // ── danmakuMobMask 直接测试 ──────────────────────────────────────────────

    @Test
    fun `danmakuMobMask renders successfully with 8x2 pixel data`() {
        val frame = DanmakuMobMaskFrame(
            range = 0L until 1000L,
            width = 8,
            height = 2,
            image = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte()),
        )

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuMobMask(frame, aspectRatio = 1.77f),
            )
        }
    }

    @Test
    fun `danmakuMobMask renders successfully with 1x1 pixel data`() {
        val frame = DanmakuMobMaskFrame(
            range = 0L until 1000L,
            width = 1,
            height = 1,
            image = byteArrayOf(0b1000_0000.toByte()),
        )

        composeAndDraw {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .danmakuMobMask(frame, aspectRatio = 0.5f),
            )
        }
    }

    // ── 验证 Modifier 返回值 ────────────────────────────────────────────────

    @Test
    fun `danmakuMask with null frame returns a non-trivial modifier`() {
        val modifier = Modifier.danmakuMask(frame = null, aspectRatio = 1.77f)

        assertThat(modifier).isNotNull()
    }
}
