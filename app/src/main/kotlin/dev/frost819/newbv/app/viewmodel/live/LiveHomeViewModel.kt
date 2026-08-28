package dev.frost819.newbv.app.viewmodel.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCardData
import dev.frost819.newbv.app.ui.component.livecard.formatOnlineCount
import dev.frost819.newbv.biliapi.http.entity.live.FollowLiveRoom
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveRoomItem
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * 直播主页 ViewModel。
 *
 * 管理三个区域的状态：我的关注、推荐分区、推荐信息流。
 */
@HiltViewModel
class LiveHomeViewModel
    @Inject
    constructor(
        private val liveRepository: LiveRepository,
    ) : ViewModel() {
        companion object {
            private const val LOAD_TIMEOUT_MS = 10_000L
        }

        private val _uiState = MutableStateFlow(LiveHomeUiState())
        val uiState: StateFlow<LiveHomeUiState> = _uiState.asStateFlow()

        init {
            loadFollowLive()
            loadAreaList()
            loadRecommend()
        }

        /**
         * 加载关注主播正在直播的房间列表。
         */
        fun loadFollowLive() {
            _uiState.update {
                it.copy(
                    followItems = emptyList(),
                    followLoading = true,
                    followError = false,
                )
            }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val response = liveRepository.getFollowLive()
                        response.rooms.filter { it.liveStatus == 1 }.map { it.toCardData() }
                    }
                }.onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            followItems = items,
                            followLoading = false,
                            followError = false,
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    _uiState.update {
                        it.copy(
                            followLoading = false,
                            followError = true,
                        )
                    }
                }
            }
        }

        /**
         * 加载分区列表。
         */
        fun loadAreaList() {
            if (_uiState.value.areaLoading) return

            _uiState.update { it.copy(areaLoading = true, areaError = false) }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        liveRepository.getLiveAreaList()
                    }
                }.onSuccess { areas ->
                    _uiState.update {
                        it.copy(
                            areaList = areas,
                            areaLoading = false,
                            areaError = false,
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    _uiState.update {
                        it.copy(
                            areaLoading = false,
                            areaError = true,
                        )
                    }
                }
            }
        }

        /**
         * 加载推荐直播间列表。
         */
        fun loadRecommend() {
            _uiState.update {
                it.copy(
                    recommendItems = emptyList(),
                    recommendLoading = true,
                    recommendError = false,
                    recommendHasMore = true,
                )
            }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val response = liveRepository.getLiveRecommend()
                        response.list.map { it.toCardData() }
                    }
                }.onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            recommendItems = items,
                            recommendHasMore = items.isNotEmpty(),
                            recommendLoading = false,
                            recommendError = false,
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    _uiState.update {
                        it.copy(
                            recommendLoading = false,
                            recommendError = true,
                        )
                    }
                }
            }
        }

        /**
         * 加载更多推荐直播间（下拉加载）。
         *
         * 推荐接口无分页参数，每次调用返回随机推荐列表。
         * 通过去重避免重复项；若连续 3 次返回全部为已存在项，则停止继续加载。
         */
        fun loadMoreRecommend() {
            if (_uiState.value.recommendLoading || !_uiState.value.recommendHasMore) return

            _uiState.update { it.copy(recommendLoading = true, recommendError = false) }

            viewModelScope.launch {
                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val existingIds =
                            _uiState.value.recommendItems
                                .map { it.roomId }
                                .toSet()
                        val newItems = mutableListOf<LiveRoomCardData>()
                        val seenInBatch = mutableSetOf<Long>()
                        var attempts = 0
                        while (newItems.isEmpty() && attempts < 3) {
                            val response = liveRepository.getLiveRecommend()
                            response.list
                                .map { it.toCardData() }
                                .filter { it.roomId !in existingIds }
                                .filter { seenInBatch.add(it.roomId) }
                                .let { newItems.addAll(it) }
                            attempts++
                        }
                        newItems
                    }
                }.onSuccess { newItems ->
                    _uiState.update {
                        it.copy(
                            recommendItems = it.recommendItems + newItems,
                            recommendHasMore = newItems.isNotEmpty(),
                            recommendLoading = false,
                            recommendError = false,
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    _uiState.update {
                        it.copy(
                            recommendLoading = false,
                            recommendError = true,
                        )
                    }
                }
            }
        }
    }

private fun FollowLiveRoom.toCardData(): LiveRoomCardData {
    val coverUrl = coverFromUser.ifBlank { keyframe }
    return LiveRoomCardData(
        roomId = roomId,
        title = title,
        uname = uname.ifBlank { nickname },
        uid = uid,
        cover = coverUrl,
        face = face,
        areaV2Name = areaV2Name,
        areaV2ParentName = areaV2ParentName,
        onlineString = formatOnlineCount(online),
        watchedString = "",
    )
}

private fun LiveRoomItem.toCardData(): LiveRoomCardData {
    val coverUrl = cover.ifBlank { keyframe.ifBlank { userCover } }
    return LiveRoomCardData(
        roomId = roomId.toLong(),
        title = title,
        uname = uname,
        uid = uid,
        cover = coverUrl,
        face = face,
        areaV2Name = areaV2Name,
        areaV2ParentName = areaV2ParentName,
        onlineString = formatOnlineCount(online),
        watchedString = watchedShow?.textSmall ?: "",
    )
}

data class LiveHomeUiState(
    val followItems: List<LiveRoomCardData> = emptyList(),
    val followLoading: Boolean = false,
    val followError: Boolean = false,
    val areaList: List<LiveAreaParent> = emptyList(),
    val areaLoading: Boolean = false,
    val areaError: Boolean = false,
    val recommendItems: List<LiveRoomCardData> = emptyList(),
    val recommendLoading: Boolean = false,
    val recommendError: Boolean = false,
    val recommendHasMore: Boolean = true,
)
