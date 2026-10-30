package dev.frost819.newbv.biliapi.http.entity.danmaku

import bilibili.community.service.dm.v1.danmakuAIFlag
import bilibili.community.service.dm.v1.danmakuElem
import bilibili.community.service.dm.v1.danmakuFlag
import bilibili.community.service.dm.v1.dmSegMobileReply
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [toDanmakuDataList] 的单元测试。
 *
 * 验证 seg 响应的 elems 映射，以及 ai_flag 评分按 dmid 对齐；
 * 未下发评分的弹幕兜底为 0（Web 通道实测 ai_flag 恒空）。
 */
class DanmakuDataListTest {
    @Test
    fun `maps elems and attaches ai flag score by dmid`() {
        val reply =
            dmSegMobileReply {
                elems +=
                    danmakuElem {
                        id = 1001L
                        content = "高分弹幕"
                        weight = 9
                    }
                elems +=
                    danmakuElem {
                        id = 1002L
                        content = "低分弹幕"
                        weight = 2
                    }
                aiFlag =
                    danmakuAIFlag {
                        dmFlags +=
                            danmakuFlag {
                                dmid = 1001L
                                flag = 8
                            }
                    }
            }

        val list = reply.toDanmakuDataList()

        assertThat(list).hasSize(2)
        assertThat(list[0].dmid).isEqualTo(1001L)
        assertThat(list[0].text).isEqualTo("高分弹幕")
        assertThat(list[0].level).isEqualTo(9)
        assertThat(list[0].aiFlagScore).isEqualTo(8)
        assertThat(list[1].dmid).isEqualTo(1002L)
        assertThat(list[1].level).isEqualTo(2)
        assertThat(list[1].aiFlagScore).isEqualTo(0)
    }

    @Test
    fun `empty reply yields empty list`() {
        val list = dmSegMobileReply {}.toDanmakuDataList()

        assertThat(list).isEmpty()
    }
}
