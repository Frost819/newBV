package dev.frost819.newbv.app.data

import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用级视频信息共享仓库。
 *
 * 在详情页加载视频详情后，播放器页面通过本仓库获取已缓存的视频列表和详情，
 * 避免重复请求。若播放器直接打开（如从外部入口），则由 [PlayerViewModel] 自行加载。
 *
 * 生命周期：Hilt `@Singleton`，随应用进程存活。
 */
@Singleton
class VideoInfoRepository @Inject constructor(
    private val videoDetailRepository: VideoDetailRepository,
) {
    private val logger = KotlinLogging.logger { }

    private val _videoList = MutableStateFlow<List<VideoListItem>>(emptyList())
    val videoList = _videoList.asStateFlow()

    private val _videoDetail = MutableStateFlow<VideoDetail?>(null)
    val videoDetail = _videoDetail.asStateFlow()

    private val _relatedVideos = MutableStateFlow<List<RelatedVideo>>(emptyList())
    val relatedVideos = _relatedVideos.asStateFlow()

    /** 最近播放的 CID。 */
    private val _lastPlayedCid = MutableStateFlow(0L)
    val lastPlayedCid = _lastPlayedCid.asStateFlow()

    /** 最近播放位置（秒）。 */
    private val _lastPlayedTime = MutableStateFlow(0)
    val lastPlayedTime = _lastPlayedTime.asStateFlow()

    /**
     * 更新视频详情（同步相关视频和历史进度）。
     *
     * 供详情页 ViewModel 在加载完成后调用，确保播放器页面能获取相关视频数据。
     *
     * @param detail 视频详情
     */
    fun updateVideoDetail(detail: VideoDetail) {
        _videoDetail.update { detail }
        _relatedVideos.update { detail.relatedVideos }
        _lastPlayedCid.update { detail.history.lastPlayedCid }
        _lastPlayedTime.update { detail.history.progress }
    }

    /**
     * 加载视频详情并更新共享状态。
     *
     * @param aid 视频 AV 号
     * @param preferApiType 接口类型
     */
    suspend fun loadVideoDetail(aid: Long, preferApiType: ApiType = ApiType.Web) {
        runCatching {
            val detail = videoDetailRepository.getVideoDetail(aid = aid, preferApiType = preferApiType)
            _videoDetail.update { detail }
            _relatedVideos.update { detail.relatedVideos }
            _lastPlayedCid.update { detail.history.lastPlayedCid }
            _lastPlayedTime.update { detail.history.progress }
            logger.info { "Loaded video detail: aid=$aid, related=${detail.relatedVideos.size}" }
        }.onFailure { e ->
            logger.error(e) { "Failed to load video detail: aid=$aid" }
        }
    }

    /**
     * 更新视频列表。
     *
     * @param items 新的视频列表
     */
    fun updateVideoList(items: List<VideoListItem>) {
        _videoList.update { items }
    }

    /**
     * 为列表中的每个视频加载 UGC 分 P 信息。
     *
     * 仅对有多分 P 的视频生效。
     *
     * @param preferApiType 接口类型
     */
    suspend fun updateUgcPages(preferApiType: ApiType = ApiType.Web) {
        _videoList.update { oldList ->
            oldList.map { item ->
                runCatching {
                    val pages = videoDetailRepository.getUgcPages(aid = item.aid, preferApiType = preferApiType)
                    if (pages.size > 1) item.copy(ugcPages = pages) else item
                }.getOrElse { item }
            }
        }
    }

    /**
     * 更新播放历史。
     *
     * @param progress 播放进度（秒），-1 表示已看完
     * @param lastPlayedCid 最近播放的 CID
     */
    fun updateHistory(progress: Int, lastPlayedCid: Long) {
        _lastPlayedCid.update { lastPlayedCid }
        _lastPlayedTime.update { progress }
    }

    /** 重置所有状态。 */
    fun reset() {
        _videoList.update { emptyList() }
        _videoDetail.update { null }
        _relatedVideos.update { emptyList() }
        _lastPlayedCid.update { 0L }
        _lastPlayedTime.update { 0 }
    }
}
