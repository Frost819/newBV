package dev.frost819.newbv.biliapi.entity.video

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.entity.video.RelatedVideoInfo
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * [RelatedVideo.fromRelate]（HTTP `RelatedVideoInfo`）→ Domain 转换的单元测试。
 *
 * 使用的是真实 `/x/web-interface/wbi/view/detail` 响应中 `Related` 条目的 JSON 片段，
 * 验证 `redirect_url` 反序列化与 EP ID 解析（Web 模式番剧跳转）。
 */
class RelatedVideoHttpTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fromRelate http with redirect url sets jumpToSeason and extracts epid`() {
        val info = json.decodeFromString<RelatedVideoInfo>(REAL_RELATED_ITEM)
        val related = RelatedVideo.fromRelate(info)

        assertThat(related.jumpToSeason).isTrue()
        assertThat(related.epid).isEqualTo(2633345)
    }

    @Test
    fun `fromRelate http without redirect url does not jump to season`() {
        val info =
            json
                .decodeFromString<RelatedVideoInfo>(REAL_RELATED_ITEM)
                .copy(redirectUrl = null)
        val related = RelatedVideo.fromRelate(info)

        assertThat(related.jumpToSeason).isFalse()
        assertThat(related.epid).isNull()
    }

    private companion object {
        /**
         * 真实 `Related` 条目（aid=115467863852754），仅追加了 OGV 条目的 `redirect_url` 字段。
         */
        val REAL_RELATED_ITEM =
            """
            {"aid":115467863852754,"videos":1,"tid":168,"tname":"国产原创相关","copyright":1,
            "pic":"http://i0.hdslb.com/bfs/archive/6c49c7378bab7df11641e18b88bb1f1576bdb50f.jpg",
            "title":"猪猪侠剧情讲解","pubdate":1761907200,"ctime":1761900146,"desc":"测试","state":0,
            "duration":2074,"mission_id":4047808,
            "rights":{"bp":0,"elec":0,"download":0,"movie":0,"pay":0,"hd5":0,"no_reprint":1,"autoplay":1,
            "ugc_pay":0,"is_cooperation":0,"ugc_pay_preview":0,"no_background":0,"arc_pay":0,"pay_free_watch":0},
            "owner":{"mid":31261235,"name":"L另唐","face":"https://i2.hdslb.com/bfs/face/abc.jpg"},
            "stat":{"aid":115467863852754,"view":17931694,"danmaku":3460,"reply":1050,"favorite":102933,
            "coin":15630,"share":739,"now_rank":0,"his_rank":34,"like":131105,"dislike":0,"vt":0,"vv":17931694},
            "dynamic":"","cid":33554698184,"dimension":{"width":1920,"height":1080,"rotate":0},
            "season_id":3133,"short_link_v2":"https://b23.tv/BV1o21NBxEiT","bvid":"BV1o21NBxEiT",
            "season_type":0,"is_ogv":false,"ogv_info":null,"rcmd_reason":"",
            "redirect_url":"https://www.bilibili.com/bangumi/play/ep2633345?theme=movie"}
            """.trimIndent()
    }
}
