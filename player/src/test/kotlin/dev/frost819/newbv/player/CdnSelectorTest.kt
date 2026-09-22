package dev.frost819.newbv.player

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

/**
 * [OkHttpCdnSelector] 的单元测试。
 *
 * 测速依赖真实网络，这里只覆盖不依赖可达性的排序/过滤语义：
 * 空输入、单元素短路、空白过滤，以及全部不可达时保持入参顺序。
 */
class CdnSelectorTest {
    private val selector = OkHttpCdnSelector(OkHttpClient())

    @Test
    fun `rank empty returns empty`() =
        runTest {
            assertThat(selector.rank(emptyList())).isEmpty()
        }

    @Test
    fun `rank single url short-circuits`() =
        runTest {
            val url = "https://upos-sz-mirrorcos.bilivideo.com/v.m4s"
            assertThat(selector.rank(listOf(url))).containsExactly(url)
        }

    @Test
    fun `rank filters blank and duplicate urls`() =
        runTest {
            val url = "https://upos-sz-mirrorcos.bilivideo.com/v.m4s"
            assertThat(selector.rank(listOf("", "  ", url, url))).containsExactly(url)
        }

    @Test
    fun `rank preserves input order when all hosts are unreachable`() =
        runTest {
            // 端口 9 无服务，连接立即被拒；测速失败分数为 0，稳定排序保持原顺序
            val first = "https://127.0.0.1:9/first.m4s"
            val second = "https://127.0.0.1:9/second.m4s"
            assertThat(selector.rank(listOf(first, second))).containsExactly(first, second).inOrder()
        }
}
