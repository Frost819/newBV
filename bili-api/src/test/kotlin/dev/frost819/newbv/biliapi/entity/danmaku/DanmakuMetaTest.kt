package dev.frost819.newbv.biliapi.entity.danmaku

import bilibili.community.service.dm.v1.danmakuFlagConfig
import bilibili.community.service.dm.v1.danmuWebPlayerConfig
import bilibili.community.service.dm.v1.dmSegConfig
import bilibili.community.service.dm.v1.dmWebViewReply
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [toDanmakuMeta] 的字段映射测试。
 *
 * 对应端点：GET /x/v2/dm/web/view（protobuf）。
 * 覆盖分段配置、关闭状态、用户屏蔽开关、AI 等级与举报屏蔽词的映射与缺省兜底。
 */
class DanmakuMetaTest {
    @Test
    fun `maps segment config and user filter config`() {
        val reply =
            dmWebViewReply {
                state = 1
                count = 1278
                dmSge =
                    dmSegConfig {
                        pageSize = 360_000L
                        total = 3L
                    }
                flag =
                    danmakuFlagConfig {
                        recFlag = 3
                        recSwitch = 1
                    }
                playerConfig =
                    danmuWebPlayerConfig {
                        // 实测语义：true 表示未屏蔽
                        blocktop = false
                        blockscroll = false
                        blockbottom = false
                        blockcolor = true
                        blockspecial = true
                        aiSwitch = false
                        aiLevel = 2
                        aiLevelV2 = 1
                    }
                reportFilterContent += "剧透"
                reportFilterContent += "广告"
            }

        val meta = reply.toDanmakuMeta()

        assertThat(meta.segmentSizeMs).isEqualTo(360_000L)
        assertThat(meta.segTotal).isEqualTo(3)
        assertThat(meta.closed).isTrue()
        assertThat(meta.count).isEqualTo(1278L)

        val config = meta.filterConfig
        assertThat(config.hasPlayerConfig).isTrue()
        assertThat(config.hasValidConfig).isTrue()
        assertThat(config.blockTop).isFalse()
        assertThat(config.blockScroll).isFalse()
        assertThat(config.blockBottom).isFalse()
        assertThat(config.blockColor).isTrue()
        assertThat(config.blockSpecial).isTrue()
        // recSwitch=1 优先于 playerConfig.aiSwitch
        assertThat(config.aiSwitch).isTrue()
        // recFlag 优先于 playerConfig.aiLevel
        assertThat(config.aiLevel).isEqualTo(3)
        assertThat(config.aiLevelV2).isEqualTo(1)
        assertThat(config.reportFilterContent).containsExactly("剧透", "广告").inOrder()
    }

    @Test
    fun `flag only reply keeps player config defaults`() {
        val meta =
            dmWebViewReply {
                flag = danmakuFlagConfig { recFlag = 7 }
            }.toDanmakuMeta()

        val config = meta.filterConfig
        assertThat(config.hasValidConfig).isTrue()
        assertThat(config.hasPlayerConfig).isFalse()
        assertThat(config.aiLevel).isEqualTo(7)
        assertThat(config.aiSwitch).isFalse()
        assertThat(config.blockColor).isFalse()
        assertThat(config.blockSpecial).isFalse()
    }

    @Test
    fun `player config only reply falls back to ai level`() {
        val meta =
            dmWebViewReply {
                playerConfig =
                    danmuWebPlayerConfig {
                        aiLevel = 4
                        aiSwitch = true
                    }
            }.toDanmakuMeta()

        val config = meta.filterConfig
        assertThat(config.hasValidConfig).isTrue()
        assertThat(config.hasPlayerConfig).isTrue()
        assertThat(config.aiLevel).isEqualTo(4)
        assertThat(config.aiSwitch).isTrue()
    }

    @Test
    fun `empty reply falls back to zero segment and invalid config`() {
        val meta = dmWebViewReply {}.toDanmakuMeta()

        assertThat(meta.segmentSizeMs).isEqualTo(0L)
        assertThat(meta.segTotal).isEqualTo(0)
        assertThat(meta.closed).isFalse()
        assertThat(meta.count).isEqualTo(0L)
        assertThat(meta.filterConfig.hasValidConfig).isFalse()
        assertThat(meta.filterConfig.hasPlayerConfig).isFalse()
        assertThat(meta.filterConfig.aiLevel).isEqualTo(0)
        assertThat(meta.filterConfig.aiLevelV2).isEqualTo(0)
        assertThat(meta.filterConfig.reportFilterContent).isEmpty()
    }
}
