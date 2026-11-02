package dev.frost819.newbv.app.data

import dev.frost819.newbv.app.entity.player.SponsorActionType
import dev.frost819.newbv.app.entity.player.SponsorSegment
import dev.frost819.newbv.app.network.SponsorBlockApi
import dev.frost819.newbv.app.network.entity.SponsorSegmentDto
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 空降片段仓库。
 *
 * 封装 SponsorBlock 查询的完整管道：bvid → SHA-256 前缀查询 → 响应内按
 * videoID 筛选 → 按 CID 过滤多 P → 归一化（去重/排序/过滤无效片段）。
 *
 * 内存缓存按 `bvid + 分类集合` 为键，TTL 内复用，避免每次起播都请求外部服务；
 * 仅磁盘无关（进程内），重启后自然重新拉取。
 */
@Singleton
class SponsorBlockRepository
    @Inject
    constructor() {
        private data class CacheEntry(
            val segments: List<SponsorSegment>,
            val timestamp: Long,
        )

        private val cache = ConcurrentHashMap<String, CacheEntry>()

        /**
         * 查询指定视频/分 P 的空降片段（已归一化、按起点升序）。
         *
         * @param bvid 视频 BV 号
         * @param cid 当前分 P CID；> 0 时仅返回该分 P 的片段，<= 0 返回全部分 P
         * @param categories 需要查询的分类 API 名列表；为空时直接返回空列表，
         *   不发起请求（空列表若发给服务端会被理解为"全部分类"）
         * @return 归一化后的片段列表；视频无片段时为空列表
         * @throws Exception 网络或服务端错误（调用方自行降级为无片段）
         */
        suspend fun getSkipSegments(
            bvid: String,
            cid: Long,
            categories: List<String>,
        ): List<SponsorSegment> {
            if (categories.isEmpty()) return emptyList()
            val cacheKey =
                buildString {
                    append(bvid)
                    append(':')
                    append(categories.sorted().joinToString(","))
                }
            val cached =
                cache[cacheKey]?.takeIf {
                    System.currentTimeMillis() - it.timestamp < CACHE_TTL_MS
                }
            val segments =
                if (cached != null) {
                    cached.segments
                } else {
                    val videos = SponsorBlockApi.getSkipSegments(bvid, categories)
                    val raw = videos.firstOrNull { it.videoID == bvid }?.segments.orEmpty()
                    normalize(raw).also { normalized ->
                        cache[cacheKey] = CacheEntry(normalized, System.currentTimeMillis())
                    }
                }
            return filterByCid(segments, cid)
        }

        /**
         * 归一化原始片段：
         * 1. 丢弃未知 actionType 与终点不大于起点的片段；
         * 2. 按 UUID 去重（UUID 空时用 `category:start:end` 兜底键），代表按
         *    锁定 > 票数 > 起点更早 > 片段更短 选取；
         * 3. 按起点升序。
         */
        private fun normalize(raw: List<SponsorSegmentDto>): List<SponsorSegment> =
            raw
                .mapNotNull { dto ->
                    val actionType = SponsorActionType.fromValue(dto.actionType) ?: return@mapNotNull null
                    if (dto.endMs <= dto.startMs) return@mapNotNull null
                    SponsorSegment(
                        uuid = dto.uuid,
                        category = dto.category,
                        actionType = actionType,
                        startMs = dto.startMs,
                        endMs = dto.endMs,
                        cid = dto.cid,
                        votes = dto.votes,
                        locked = dto.locked != 0,
                    )
                }.groupBy { segment ->
                    segment.uuid.ifEmpty { "${segment.category}:${segment.startMs}:${segment.endMs}" }
                }.values
                .map { group ->
                    group.maxWith(
                        compareBy<SponsorSegment> { it.locked }
                            .thenBy { it.votes }
                            .thenBy { -it.startMs }
                            .thenBy { -(it.endMs - it.startMs) },
                    )
                }.sortedBy { it.startMs }

        /**
         * 多 P 视频 CID 过滤；cid 无效（<= 0）时不过滤（取全部分 P）。
         * 与 MyTVB 一致采用严格匹配：无 cid 标注的片段在多 P 下无法定位归属，丢弃。
         */
        private fun filterByCid(
            segments: List<SponsorSegment>,
            cid: Long,
        ): List<SponsorSegment> =
            if (cid <= 0L) {
                segments
            } else {
                segments.filter { it.cid == cid.toString() }
            }

        private companion object {
            /** 缓存有效期：社区片段变化频率低，30 分钟内复用。 */
            const val CACHE_TTL_MS = 30 * 60 * 1000L
        }
    }
