package dev.frost819.newbv.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 视频同时观看人数（在线人数）— App 端。
 *
 * 对应接口：GET https://app.bilibili.com/x/v2/view/video/online
 * 文档：docs/bilibili-API-collect-master/docs/video/online.md
 *
 * App 端无 show_switch 展示开关，直接返回服务端预格式化的文案。
 *
 * @property online 在线人数信息
 */
@Serializable
data class OnlineTotalApp(
    val online: Online = Online(),
) {
    /**
     * 所有终端总计人数信息。
     *
     * @property totalText 服务端预格式化文案（如 `"8.8万+人在看"`）
     */
    @Serializable
    data class Online(
        @SerialName("total_text")
        val totalText: String = "",
    )
}
