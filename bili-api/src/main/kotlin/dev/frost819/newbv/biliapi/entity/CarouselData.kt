package dev.frost819.newbv.biliapi.entity

import dev.frost819.newbv.biliapi.util.UrlUtil
import dev.frost819.newbv.biliapi.util.toBv
import io.ktor.http.Url

data class CarouselData(
    val items: List<CarouselItem>
) {
    companion object {
        fun fromPgcWebInitialStateData(data: dev.frost819.newbv.biliapi.http.entity.pgc.PgcWebInitialStateData): CarouselData {
            val result = mutableListOf<CarouselItem>()
            var needParseIdFromUrl = false
            // 电影、电视剧、综艺板块里的轮播图数据里没有直接包含 episodeId 和 seasonId
            if (listOf(1668, 1675, 1682).contains(data.modules.banner.moduleId))
                needParseIdFromUrl = true
            data.modules.banner.items.filter {
                it.episodeId != null
                        || (needParseIdFromUrl && it.link.contains("bangumi/play/ep"))
                        || (needParseIdFromUrl && it.link.contains("bangumi/play/ss"))
            }.forEach {
                var cover = it.bigCover ?: it.cover
                if (cover.startsWith("//")) cover = "https:$cover"
                var epidFromUrl: Int? = null
                var ssidFromUrl: Int? = null

                if (needParseIdFromUrl) {
                    val idStr = Url(it.link).rawSegments.last()
                    epidFromUrl =
                        idStr.substring(2).takeIf { idStr.startsWith("ep") }?.toIntOrNull()
                    ssidFromUrl =
                        idStr.substring(2).takeIf { idStr.startsWith("ss") }?.toIntOrNull()
                }

                result.add(
                    CarouselItem(
                        cover = cover,
                        title = it.title,
                        seasonId = it.seasonId ?: ssidFromUrl ?: -1,
                        episodeId = it.episodeId ?: epidFromUrl ?: -1
                    )
                )
            }
            return CarouselData(result)
        }

        fun fromUgcRegionDynamicBanner(data: dev.frost819.newbv.biliapi.http.entity.region.RegionDynamic.Banner): CarouselData {
            val result = mutableListOf<CarouselItem>()
            data.top.forEach { top ->
                if (!UrlUtil.isVideoUrl(top.uri)) return@forEach
                val avid = UrlUtil.parseAidFromUrl(top.uri)
                val bvid = avid.toBv()
                result.add(
                    CarouselItem(
                        cover = top.image,
                        title = top.title,
                        avid = avid,
                        bvid = bvid
                    )
                )
            }
            return CarouselData(result)
        }

        fun fromUgcRegionLocs(data: dev.frost819.newbv.biliapi.http.entity.region.RegionLocs): CarouselData {
            val result = mutableListOf<CarouselItem>()
            data.data.forEach { (_, value) ->
                value.filter { it.url.contains("/video/") }.forEach { item ->
                    result.add(
                        CarouselItem(
                            cover = item.pic,
                            title = item.title,
                            bvid = Url(item.url).rawSegments.last()
                        )
                    )
                }
            }
            return CarouselData(result)
        }
    }

    data class CarouselItem(
        val cover: String,
        val title: String,
        val seasonId: Int? = null,
        val episodeId: Int? = null,
        val avid: Long? = null,
        val bvid: String? = null
    )
}