package dev.frost819.newbv.biliapi.repositories

import bilibili.pagination.paginationReply
import bilibili.polymer.app.search.v1.item
import bilibili.polymer.app.search.v1.searchAllResponse
import bilibili.polymer.app.search.v1.searchBangumiCard
import bilibili.polymer.app.search.v1.searchUpperCard
import bilibili.polymer.app.search.v1.searchVideoCard
import bilibili.polymer.app.search.v1.share
import bilibili.polymer.app.search.v1.video
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SearchAllResult.fromGrpc] 的单元测试。
 *
 * 验证 gRPC SearchAllResponse 中 AV / BANGUMI / AUTHOR / 未知卡片类型的分类转换逻辑，
 * 以及 pagination.next 的页码解析与 hasMore 计算。
 */
class SearchAllResultGrpcTest {
    @Test
    fun `fromGrpc maps video bangumi and author items`() {
        val reply =
            searchAllResponse {
                keyword = "测试"
                item +=
                    item {
                        param = "100"
                        av =
                            searchVideoCard {
                                title = "视频1"
                                cover = "http://cover1.test"
                                author = "UP1"
                                mid = 1L
                                duration = "10:00"
                                play = 1000
                                danmaku = 50
                                share =
                                    share {
                                        video =
                                            video {
                                                bvid = "BV100"
                                            }
                                    }
                            }
                    }
                item +=
                    item {
                        bangumi =
                            searchBangumiCard {
                                title = "番剧1"
                                cover = "http://cover2.test"
                                rating = 9.0
                                seasonId = 20000L
                            }
                    }
                item +=
                    item {
                        param = "999"
                        author =
                            searchUpperCard {
                                title = "用户1"
                                cover = "http://avatar.test"
                                sign = "签名"
                            }
                    }
                pagination =
                    paginationReply {
                        next = "2"
                    }
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.keyword).isEqualTo("测试")
        assertThat(result.videos).hasSize(1)
        assertThat(result.videos[0].aid).isEqualTo(100L)
        assertThat(result.videos[0].title).isEqualTo("视频1")
        assertThat(result.pgcs).hasSize(1)
        assertThat(result.pgcs[0].title).isEqualTo("番剧1")
        assertThat(result.users).hasSize(1)
        assertThat(result.users[0].mid).isEqualTo(999L)
        assertThat(result.page).isEqualTo(2)
        assertThat(result.hasMore).isTrue()
    }

    @Test
    fun `fromGrpc with empty items returns empty lists`() {
        val reply =
            searchAllResponse {
                keyword = ""
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
        assertThat(result.page).isEqualTo(1)
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromGrpc with non-numeric pagination next returns page 1 and hasMore false`() {
        val reply =
            searchAllResponse {
                keyword = "test"
                pagination =
                    paginationReply {
                        next = "abc"
                    }
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.page).isEqualTo(1)
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromGrpc with zero pagination next returns page 1 and hasMore false`() {
        val reply =
            searchAllResponse {
                keyword = "test"
                pagination =
                    paginationReply {
                        next = "0"
                    }
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.page).isEqualTo(1)
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromGrpc with null pagination returns defaults`() {
        val reply =
            searchAllResponse {
                keyword = "test"
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.page).isEqualTo(1)
        assertThat(result.hasMore).isFalse()
    }

    @Test
    fun `fromGrpc ignores unknown card types`() {
        val reply =
            searchAllResponse {
                keyword = "test"
                item +=
                    item {
                        // No card item set → unknown type
                    }
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.videos).isEmpty()
        assertThat(result.pgcs).isEmpty()
        assertThat(result.users).isEmpty()
    }

    @Test
    fun `fromGrpc maps multiple video items`() {
        val reply =
            searchAllResponse {
                keyword = "多视频"
                item +=
                    item {
                        param = "100"
                        av =
                            searchVideoCard {
                                title = "视频A"
                                cover = ""
                                author = "UP_A"
                                mid = 1L
                                duration = "5:00"
                                play = 100
                                danmaku = 10
                                share = share { video = video { bvid = "BVA" } }
                            }
                    }
                item +=
                    item {
                        param = "200"
                        av =
                            searchVideoCard {
                                title = "视频B"
                                cover = ""
                                author = "UP_B"
                                mid = 2L
                                duration = "10:00"
                                play = 200
                                danmaku = 20
                                share = share { video = video { bvid = "BVB" } }
                            }
                    }
                pagination =
                    paginationReply {
                        next = "3"
                    }
            }

        val result = SearchAllResult.fromGrpc(reply)

        assertThat(result.videos).hasSize(2)
        assertThat(result.videos[0].title).isEqualTo("视频A")
        assertThat(result.videos[1].title).isEqualTo("视频B")
        assertThat(result.page).isEqualTo(3)
        assertThat(result.hasMore).isTrue()
    }
}
