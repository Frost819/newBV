package dev.frost819.newbv.biliapi.entity

import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskType
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMobMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuWebMaskFrame
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test

class DanmakuMaskTest {
    private val webMaskFile = Any::class::class.java.getResource("/35496788838_30_0.webmask")
    private val mobMaskFile = Any::class::class.java.getResource("/35496788838_30_0.mobmask")
    private val webMaskOutputDir = File("")
    private val mobMaskOutputDir = File("")

    @Test
    fun `parse web mask file`() {
        val maskFile = webMaskFile!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)
        println(mask)
    }

    @Test
    fun `parse web mask file and output`() {
        val maskFile = webMaskFile!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.WebMask)
        val outputDir = webMaskOutputDir
        outputDir.mkdirs()
        mask.segments.forEachIndexed { index, danmakuMaskSegment ->
            val dir = File(outputDir, "$index")
            dir.mkdir()
            danmakuMaskSegment.frames.forEach { danmakuMaskFrame ->
                File(dir, "${danmakuMaskFrame.range}.svg")
                    .writeText((danmakuMaskFrame as DanmakuWebMaskFrame).svg)
            }
        }
    }

    @Test
    fun `parse mob mask file`() {
        val maskFile = mobMaskFile!!
        val binary = File(maskFile.toURI()).readBytes()
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.MobMask)
        println(mask)
    }

    @Test
    fun `parse mob mask file and output svg`() {
        // 1. 读取文件
        val maskFile = mobMaskFile!!
        val binary = File(maskFile.toURI()).readBytes()

        // 2. 解析 Mask
        val mask = DanmakuMask.fromBinary(binary, DanmakuMaskType.MobMask)

        // 3. 准备输出目录
        val outputDir = mobMaskOutputDir
        outputDir.mkdirs()

        mask.segments.forEachIndexed { index, danmakuMaskSegment ->
            val dir = File(outputDir, "$index")
            dir.mkdir()

            danmakuMaskSegment.frames.forEach { frame ->
                if (frame is DanmakuMobMaskFrame) {
                    // 4. 调用转换函数生成 SVG 内容
                    val svgContent = convertRawToSvg(frame.width, frame.height, frame.image)

                    // 5. 写入文件
                    File(dir, "${frame.range}.svg").writeText(svgContent)
                }
            }
        }
    }

    /**
     * 核心辅助函数：将二进制像素数据转为 SVG (内嵌 Base64 PNG)
     * 利用 java.awt 在单元测试环境下生成图片
     */
    private fun convertRawToSvg(width: Int, height: Int, rawData: ByteArray): String {
        // --- A. 判定模式 (1-bit vs 8-bit) ---
        // 逻辑同解析器，用于决定如何渲染像素
        val pixelCount = width * height
        val expected1Bit = pixelCount / 8
        val expected8Bit = pixelCount

        val is1Bit = abs(rawData.size - expected1Bit) < abs(rawData.size - expected8Bit)

        // --- B. 创建 BufferedImage ---
        // 为了通用性，我们统一创建 RGB 图片
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        // --- C. 填充像素 ---
        if (is1Bit) {
            // === 处理 1-bit 位图 ===
            // 规则：1 byte = 8 pixels, MSB first (高位在前)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixelIndex = y * width + x
                    val byteIndex = pixelIndex / 8
                    val bitIndex = 7 - (pixelIndex % 8)

                    if (byteIndex < rawData.size) {
                        // 提取 bit
                        val bit = (rawData[byteIndex].toInt() shr bitIndex) and 1
                        // 1 = 白色 (255), 0 = 黑色 (0)
                        val color = if (bit == 1) 0xFFFFFF else 0x000000
                        image.setRGB(x, y, color)
                    }
                }
            }
        } else {
            // === 处理 8-bit 灰度图 ===
            // 规则：1 byte = 1 pixel (0-255)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    if (index < rawData.size) {
                        val gray = rawData[index].toInt() and 0xFF
                        // 组装 RGB: R=gray, G=gray, B=gray
                        val rgb = (gray shl 16) or (gray shl 8) or gray
                        image.setRGB(x, y, rgb)
                    }
                }
            }
        }

        // --- D. 转为 PNG 并 Base64 编码 ---
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        val pngBytes = outputStream.toByteArray()

        // 单元测试运行在 PC JVM 上，可以直接用 java.util.Base64
        val base64Png = Base64.getEncoder().encodeToString(pngBytes)

        // --- E. 拼接 SVG 字符串 ---
        // image-rendering="pixelated" 保证低分辨率 mask 放大时不模糊
        return """
            <svg width="$width" height="$height" version="1.1" xmlns="http://www.w3.org/2000/svg">
                <rect width="100%" height="100%" fill="red" />
                <image width="$width" height="$height" image-rendering="pixelated" href="data:image/png;base64,$base64Png" />
            </svg>
        """.trimIndent()
    }
}