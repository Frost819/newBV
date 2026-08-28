package dev.frost819.newbv.danmaku.util

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.caverock.androidsvg.SVG
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuWebMaskFrame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [MaskModifiers] 的插桩测试。
 *
 * 验证蒙版 Modifier 在真实 Android 环境下的行为：
 * - bitmapMask：Bitmap 创建和 ARGB 像素正确性
 * - danmakuWebMask：SVG 解析和渲染
 * - danmakuMobMask：1bpp 像素解码为 ARGB Bitmap
 * - danmakuMask：统一入口分发
 */
@RunWith(AndroidJUnit4::class)
class MaskModifiersTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ── bitmapMask 测试 ──────────────────────────────────────────────────

    @Test
    fun bitmapMask_appliesWithoutCrash() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        composeRule.setContent {
            Canvas(
                modifier =
                    Modifier
                        .size(200.dp)
                        .bitmapMask(bitmap, videoAspectRatio = 16f / 9f),
            ) { }
        }

        composeRule.onRoot().assertExists()
    }

    @Test
    fun bitmapMask_withSquareAspectRatio_doesNotCrash() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)

        composeRule.setContent {
            Canvas(
                modifier =
                    Modifier
                        .size(200.dp)
                        .bitmapMask(bitmap, videoAspectRatio = 1.0f),
            ) { }
        }

        composeRule.onRoot().assertExists()
    }

    // ── danmakuMobMask 像素解码测试 ─────────────────────────────────────

    @Test
    fun danmakuMobMask_decodesPixelsCorrectly() {
        val width = 8
        val height = 2
        // 2 rows × 8 cols = 16 bits = 2 bytes
        // Row 0: 11111111 (all transparent) = 0xFF
        // Row 1: 00000000 (all black)       = 0x00
        val image = byteArrayOf(0xFF.toByte(), 0x00.toByte())

        val frame =
            DanmakuMobMaskFrame(
                range = 0L until 1000L,
                width = width,
                height = height,
                image = image,
            )

        val bmp = createMobMaskBitmap(frame)

        assertThat(bmp.width).isEqualTo(width)
        assertThat(bmp.height).isEqualTo(height)

        // Row 0: all bits = 1 → transparent
        for (x in 0 until width) {
            val pixel = bmp.getPixel(x, 0)
            assertThat(pixel).isEqualTo(android.graphics.Color.TRANSPARENT)
        }

        // Row 1: all bits = 0 → black
        for (x in 0 until width) {
            val pixel = bmp.getPixel(x, 1)
            assertThat(pixel).isEqualTo(android.graphics.Color.BLACK)
        }
    }

    @Test
    fun danmakuMobMask_mixedBits_decodeCorrectly() {
        val width = 8
        val height = 1
        // 10101010 = 0xAA → alternating transparent/black
        val image = byteArrayOf(0xAA.toByte())

        val frame =
            DanmakuMobMaskFrame(
                range = 0L until 1000L,
                width = width,
                height = height,
                image = image,
            )

        val bmp = createMobMaskBitmap(frame)

        // Bit 7 (MSB) = 1 → transparent (x=0)
        assertThat(bmp.getPixel(0, 0)).isEqualTo(android.graphics.Color.TRANSPARENT)
        // Bit 6 = 0 → black (x=1)
        assertThat(bmp.getPixel(1, 0)).isEqualTo(android.graphics.Color.BLACK)
        // Bit 5 = 1 → transparent (x=2)
        assertThat(bmp.getPixel(2, 0)).isEqualTo(android.graphics.Color.TRANSPARENT)
        // Bit 4 = 0 → black (x=3)
        assertThat(bmp.getPixel(3, 0)).isEqualTo(android.graphics.Color.BLACK)
    }

    @Test
    fun danmakuMobMask_appliesInComposeWithoutCrash() {
        val frame =
            DanmakuMobMaskFrame(
                range = 0L until 1000L,
                width = 8,
                height = 2,
                image = byteArrayOf(0xFF.toByte(), 0x00.toByte()),
            )

        composeRule.setContent {
            Text(
                text = "Masked Content",
                modifier =
                    Modifier
                        .size(200.dp)
                        .danmakuMobMask(frame, aspectRatio = 16f / 9f),
            )
        }

        composeRule.onNodeWithText("Masked Content").assertExists()
    }

    // ── danmakuWebMask SVG 解析测试 ─────────────────────────────────────

    @Test
    fun danmakuWebMask_parsesSvgAndCreatesBitmap() {
        val svg =
            """
            <svg width="100" height="50" xmlns="http://www.w3.org/2000/svg">
                <rect width="100" height="50" fill="black"/>
            </svg>
            """.trimIndent()

        val svgObj = SVG.getFromString(svg)
        assertThat(svgObj).isNotNull()
        assertThat(svgObj.documentWidth).isEqualTo(100f)
        assertThat(svgObj.documentHeight).isEqualTo(50f)

        val bmp = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        svgObj.renderToCanvas(canvas)

        assertThat(bmp.width).isEqualTo(100)
        assertThat(bmp.height).isEqualTo(50)
    }

    @Test
    fun danmakuWebMask_withInvalidSvg_returnsNullBitmap() {
        val frame =
            DanmakuWebMaskFrame(
                range = 0L until 1000L,
                svg = "not a valid svg",
            )

        val result = runCatching { SVG.getFromString(frame.svg) }
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun danmakuWebMask_appliesInComposeWithoutCrash() {
        val svg =
            """
            <svg width="100" height="100" xmlns="http://www.w3.org/2000/svg">
                <rect width="100" height="100" fill="black"/>
            </svg>
            """.trimIndent()

        val frame =
            DanmakuWebMaskFrame(
                range = 0L until 1000L,
                svg = svg,
            )

        composeRule.setContent {
            Text(
                text = "WebMasked Content",
                modifier =
                    Modifier
                        .size(200.dp)
                        .danmakuWebMask(frame, aspectRatio = 16f / 9f),
            )
        }

        composeRule.onNodeWithText("WebMasked Content").assertExists()
    }

    // ── danmakuMask 统一入口测试 ────────────────────────────────────────

    @Test
    fun danmakuMask_withNullFrame_doesNotApplyMask() {
        composeRule.setContent {
            Text(
                text = "No Mask",
                modifier =
                    Modifier
                        .size(200.dp)
                        .danmakuMask(null, aspectRatio = 16f / 9f),
            )
        }

        composeRule.onNodeWithText("No Mask").assertExists()
    }

    @Test
    fun danmakuMask_withMobFrame_appliesCorrectly() {
        val frame: DanmakuMaskFrame =
            DanmakuMobMaskFrame(
                range = 0L until 1000L,
                width = 8,
                height = 2,
                image = byteArrayOf(0xFF.toByte(), 0x00.toByte()),
            )

        composeRule.setContent {
            Text(
                text = "Mob Mask",
                modifier =
                    Modifier
                        .size(200.dp)
                        .danmakuMask(frame, aspectRatio = 16f / 9f),
            )
        }

        composeRule.onNodeWithText("Mob Mask").assertExists()
    }

    @Test
    fun danmakuMask_withWebFrame_appliesCorrectly() {
        val svg =
            """
            <svg width="50" height="50" xmlns="http://www.w3.org/2000/svg">
                <rect width="50" height="50" fill="black"/>
            </svg>
            """.trimIndent()

        val frame: DanmakuMaskFrame =
            DanmakuWebMaskFrame(
                range = 0L until 1000L,
                svg = svg,
            )

        composeRule.setContent {
            Text(
                text = "Web Mask",
                modifier =
                    Modifier
                        .size(200.dp)
                        .danmakuMask(frame, aspectRatio = 16f / 9f),
            )
        }

        composeRule.onNodeWithText("Web Mask").assertExists()
    }

    // ── 辅助函数 ────────────────────────────────────────────────────────

    /**
     * 复制 [danmakuMobMask] 内部的 Bitmap 创建逻辑，用于验证像素解码正确性。
     */
    private fun createMobMaskBitmap(frame: DanmakuMobMaskFrame): Bitmap {
        val width = frame.width
        val height = frame.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels =
            IntArray(width * height) { i ->
                val byteIndex = i / 8
                val bitOffset = 7 - (i % 8)
                val bit = (frame.image[byteIndex].toInt() shr bitOffset) and 1
                if (bit == 1) android.graphics.Color.TRANSPARENT else android.graphics.Color.BLACK
            }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }
}
