package dev.frost819.newbv.biliapi.entity.video

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [VideoShot] 的单元测试。
 *
 * 通过 mock [BiliHttpApi] 验证 `fromVideoShot` 的图片下载、二进制解析逻辑，
 * 以及各种失败场景的处理。
 */
class VideoShotTest {
    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    @Test
    fun `fromVideoShot with valid data returns VideoShot`() =
        runTest {
            val imageBytes = byteArrayOf(1, 2, 3)
            val timeBinary =
                byteArrayOf(
                    0,
                    0,
                    0,
                    10,
                    0,
                    20,
                    0,
                    30,
                )
            coEvery { BiliHttpApi.download(any()) } returns imageBytes andThen timeBinary

            val httpVideoShot =
                dev.frost819.newbv.biliapi.http.entity.video.VideoShot(
                    pvData = "http://pv.test",
                    imgXLen = 10,
                    imgYLen = 10,
                    imgXSize = 160,
                    imgYSize = 90,
                    image = listOf("http://img1.test"),
                )

            val result = VideoShot.fromVideoShot(httpVideoShot)

            assertThat(result).isNotNull()
            assertThat(result!!.imageCountX).isEqualTo(10)
            assertThat(result.imageCountY).isEqualTo(10)
            assertThat(result.imageWidth).isEqualTo(160)
            assertThat(result.imageHeight).isEqualTo(90)
            assertThat(result.images).hasSize(1)
            assertThat(result.times).hasSize(3)
            assertThat(result.times[0]).isEqualTo(10.toUShort())
            assertThat(result.times[1]).isEqualTo(20.toUShort())
            assertThat(result.times[2]).isEqualTo(30.toUShort())
        }

    @Test
    fun `fromVideoShot with null image download returns null`() =
        runTest {
            coEvery { BiliHttpApi.download(any()) } throws RuntimeException("download failed")

            val httpVideoShot =
                dev.frost819.newbv.biliapi.http.entity.video.VideoShot(
                    pvData = "http://pv.test",
                    imgXLen = 10,
                    imgYLen = 10,
                    imgXSize = 160,
                    imgYSize = 90,
                    image = listOf("http://img1.test"),
                )

            val result = VideoShot.fromVideoShot(httpVideoShot)

            assertThat(result).isNull()
        }

    @Test
    fun `fromVideoShot with null pvData returns null`() =
        runTest {
            val imageBytes = byteArrayOf(1, 2, 3)
            coEvery { BiliHttpApi.download(any()) } returns imageBytes

            val httpVideoShot =
                dev.frost819.newbv.biliapi.http.entity.video.VideoShot(
                    pvData = null,
                    imgXLen = 10,
                    imgYLen = 10,
                    imgXSize = 160,
                    imgYSize = 90,
                    image = listOf("http://img1.test"),
                )

            val result = VideoShot.fromVideoShot(httpVideoShot)

            assertThat(result).isNull()
        }

    @Test
    fun `fromVideoShot with failed pvData download returns null`() =
        runTest {
            val imageBytes = byteArrayOf(1, 2, 3)
            coEvery { BiliHttpApi.download("http://img1.test") } returns imageBytes
            coEvery { BiliHttpApi.download("http://pv.test") } throws RuntimeException("pv download failed")

            val httpVideoShot =
                dev.frost819.newbv.biliapi.http.entity.video.VideoShot(
                    pvData = "http://pv.test",
                    imgXLen = 10,
                    imgYLen = 10,
                    imgXSize = 160,
                    imgYSize = 90,
                    image = listOf("http://img1.test"),
                )

            val result = VideoShot.fromVideoShot(httpVideoShot)

            assertThat(result).isNull()
        }

    @Test
    fun `fromVideoShot drops first time entry`() =
        runTest {
            val imageBytes = byteArrayOf(1, 2, 3)
            val timeBinary =
                byteArrayOf(
                    0,
                    99,
                    0,
                    10,
                    0,
                    20,
                )
            coEvery { BiliHttpApi.download(any()) } returns imageBytes andThen timeBinary

            val httpVideoShot =
                dev.frost819.newbv.biliapi.http.entity.video.VideoShot(
                    pvData = "http://pv.test",
                    imgXLen = 5,
                    imgYLen = 5,
                    imgXSize = 100,
                    imgYSize = 56,
                    image = listOf("http://img1.test"),
                )

            val result = VideoShot.fromVideoShot(httpVideoShot)

            assertThat(result).isNotNull()
            assertThat(result!!.times).hasSize(2)
            assertThat(result.times[0]).isEqualTo(10.toUShort())
            assertThat(result.times[1]).isEqualTo(20.toUShort())
        }

    @Test
    fun `fromVideoShot with multiple images maps all`() =
        runTest {
            val imageBytes = byteArrayOf(1, 2, 3)
            val timeBinary = byteArrayOf(0, 0, 0, 10)
            coEvery { BiliHttpApi.download(any()) } returns imageBytes andThen timeBinary

            val httpVideoShot =
                dev.frost819.newbv.biliapi.http.entity.video.VideoShot(
                    pvData = "http://pv.test",
                    imgXLen = 10,
                    imgYLen = 10,
                    imgXSize = 160,
                    imgYSize = 90,
                    image = listOf("http://img1.test", "http://img2.test", "http://img3.test"),
                )

            val result = VideoShot.fromVideoShot(httpVideoShot)

            assertThat(result).isNotNull()
            assertThat(result!!.images).hasSize(3)
        }
}
