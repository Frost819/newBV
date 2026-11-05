package dev.frost819.newbv.biliapi.entity.danmaku

import bilibili.community.service.dm.v1.dmSegConfig
import bilibili.community.service.dm.v1.dmWebViewReply
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [toDanmakuMeta] 的字段映射测试。
 *
 * 对应端点：GET /x/v2/dm/web/view（protobuf）。
 * 仅覆盖分段配置、关闭状态与弹幕数；账号屏蔽配置与举报屏蔽词不再由 dm/view 消费。
 */
class DanmakuMetaTest {
    @Test
    fun `maps segment config closed state and count`() {
        val reply =
            dmWebViewReply {
                state = 1
                count = 1278
                dmSge =
                    dmSegConfig {
                        pageSize = 360_000L
                        total = 3L
                    }
            }

        val meta = reply.toDanmakuMeta()

        assertThat(meta.segmentSizeMs).isEqualTo(360_000L)
        assertThat(meta.segTotal).isEqualTo(3)
        assertThat(meta.closed).isTrue()
        assertThat(meta.count).isEqualTo(1278L)
    }

    @Test
    fun `empty reply falls back to zero segment`() {
        val meta = dmWebViewReply {}.toDanmakuMeta()

        assertThat(meta.segmentSizeMs).isEqualTo(0L)
        assertThat(meta.segTotal).isEqualTo(0)
        assertThat(meta.closed).isFalse()
        assertThat(meta.count).isEqualTo(0L)
    }
}
