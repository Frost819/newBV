package dev.frost819.newbv.biliapi.http

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [extractPgcInitialStateJson] 的单元测试。
 *
 * 验证从 PGC 页面 HTML 中按 SSR 结构截取 `__INITIAL_STATE__` JSON，
 * 确保移除 jsoup 后行为与原实现一致。
 */
class PgcInitialStateParserTest {
    @Test
    fun `extracts json before function delimiter`() {
        val html =
            """
            <html><body>
            <script>window.__INITIAL_STATE__={"modules":{"banner":{"title":"番剧"}}};(function(){var a=1;})();</script>
            </body></html>
            """.trimIndent()

        val json = extractPgcInitialStateJson(html)

        assertThat(json).isEqualTo("""{"modules":{"banner":{"title":"番剧"}}}""")
    }

    @Test
    fun `falls back to script end and strips trailing semicolon`() {
        val html = """<script>window.__INITIAL_STATE__={"modules":{}};</script>"""

        val json = extractPgcInitialStateJson(html)

        assertThat(json).isEqualTo("""{"modules":{}}""")
    }

    @Test
    fun `throws when marker is missing`() {
        val html = "<html><body><script>var a = 1;</script></body></html>"

        val error = assertThrows<IllegalStateException> { extractPgcInitialStateJson(html) }

        assertThat(error).hasMessageThat().contains("initial state data cannot be found")
    }

    @Test
    fun `throws when extracted json is empty`() {
        val html = "<script>window.__INITIAL_STATE__=;(function(){})();</script>"

        val error = assertThrows<IllegalStateException> { extractPgcInitialStateJson(html) }

        assertThat(error).hasMessageThat().contains("initial state data is empty")
    }
}
