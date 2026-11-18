package dev.frost819.newbv.app.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.app.entity.player.inPlayOrder
import dev.frost819.newbv.biliapi.entity.video.RelatedVideo
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 视频列表 ViewModel。
 *
 * 管理分集列表和相关视频，提供上下集查找功能。
 * 观察 [VideoInfoRepository] 的共享状态，自动同步更新。
 *
 * @see VideoListState
 */
@HiltViewModel
class VideoListViewModel
    @Inject
    constructor(
        private val videoInfoRepository: VideoInfoRepository,
    ) : ViewModel() {
        private val _videoListState = MutableStateFlow(VideoListState())
        val videoListState = _videoListState.asStateFlow()

        /** 当前播放视频 AV 号（驱动相关视频归属，随播放器内切视频变化）。 */
        private val currentAid = MutableStateFlow(0L)

        /**
         * 设置当前播放视频 AV 号，用于确定相关视频归属。
         *
         * 播放器内切换视频（相关视频、上下集）不会改变导航路由参数，
         * 因此每次切换后都必须重新调用，否则相关视频会停留在上一个视频的列表。
         *
         * @param aid 当前视频 AV 号
         */
        fun setCurrentAid(aid: Long) {
            currentAid.value = aid
        }

        init {
            viewModelScope.launch {
                // 顺序在这里定一次：列表与各条目内的分 P 都按当前播放顺序排列后放进 state，
                // 分集面板渲染、上下集查找、自动连播都按索引取，天然一致。
                // 与 playOrderReversedFlow 合并，切换正序/倒序时面板立即重排。
                combine(videoInfoRepository.videoList, Prefs.playOrderReversedFlow) { list, reverse ->
                    list.inPlayOrder(reverse)
                }.collect { list ->
                    if (list.isEmpty()) return@collect
                    _videoListState.update { it.copy(videoList = list) }
                }
            }
            viewModelScope.launch {
                // 直接取详情内的 relatedVideos：详情（含相关视频）由 repository 整体原子更新，
                // 按 aid 校验归属可避免上一个视频的列表串入当前视频（快速切视频时不显示旧数据）
                combine(currentAid, videoInfoRepository.videoDetail) { aid, detail ->
                    if (aid != 0L && detail?.aid == aid) detail.relatedVideos else emptyList()
                }.distinctUntilChanged()
                    .collect { list ->
                        _videoListState.update { it.copy(relatedVideos = list) }
                    }
            }
        }

        /**
         * 查找下一个播放目标。
         *
         * 取 state 里已排好序的列表（见 init），因此倒序时
         * 「下一集」自然就是列表里更靠前的那一集，无需再翻一次。
         *
         * @param currentAid 当前视频 AV 号
         * @param currentCid 当前视频 CID
         * @return 下一个播放项，null 表示没有下一集
         */
        fun findNextVideo(
            currentAid: Long,
            currentCid: Long,
        ): VideoListItem? {
            val list = _videoListState.value.videoList
            val index = list.indexOfFirst { it.aid == currentAid }
            if (index == -1) return null

            val current = list.getOrNull(index) ?: return null

            // 先查找当前视频的分 P
            if (current.ugcPages?.isNotEmpty() == true) {
                val innerIndex = current.ugcPages.indexOfFirst { it.cid == currentCid }
                if (innerIndex != -1 && innerIndex + 1 < current.ugcPages.size) {
                    return VideoListItem(
                        aid = current.aid,
                        cid = current.ugcPages[innerIndex + 1].cid,
                        title = current.ugcPages[innerIndex + 1].title,
                    )
                }
            }

            // 跳到下一个视频时，若它有多个分 P，要从该方向上的第一个 P 开始。
            // 列表已按播放顺序排好，倒序时 first() 就是原顺序里的最后一 P，
            // 与 findPreviousVideo 取 lastOrNull() 正好对称。
            if (index + 1 < list.size) {
                val next = list[index + 1]
                val nextFirstPage = next.ugcPages?.firstOrNull()
                return if (nextFirstPage != null) {
                    VideoListItem(
                        aid = next.aid,
                        cid = nextFirstPage.cid,
                        title = nextFirstPage.title,
                    )
                } else {
                    next
                }
            }
            return null
        }

        /**
         * 查找上一个播放目标。
         *
         * @param currentAid 当前视频 AV 号
         * @param currentCid 当前视频 CID
         * @return 上一个播放项，null 表示没有上一集
         */
        fun findPreviousVideo(
            currentAid: Long,
            currentCid: Long,
        ): VideoListItem? {
            val list = _videoListState.value.videoList
            val index = list.indexOfFirst { it.aid == currentAid }
            if (index == -1) return null

            val current = list.getOrNull(index) ?: return null

            // 先查找当前视频的分 P
            if (current.ugcPages?.isNotEmpty() == true) {
                val innerIndex = current.ugcPages.indexOfFirst { it.cid == currentCid }
                if (innerIndex > 0) {
                    return VideoListItem(
                        aid = current.aid,
                        cid = current.ugcPages[innerIndex - 1].cid,
                        title = current.ugcPages[innerIndex - 1].title,
                    )
                }
            }

            // 查找列表中的上一个视频
            if (index > 0) {
                val prev = list[index - 1]
                val prevLastPage = prev.ugcPages?.lastOrNull()
                return if (prevLastPage != null) {
                    VideoListItem(aid = prev.aid, cid = prevLastPage.cid, title = prevLastPage.title)
                } else {
                    prev
                }
            }
            return null
        }
    }

/**
 * 视频列表状态。
 */
data class VideoListState(
    val videoList: List<VideoListItem> = emptyList(),
    val relatedVideos: List<RelatedVideo> = emptyList(),
)
