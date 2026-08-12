package dev.frost819.newbv.app.viewmodel.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.http.entity.live.LiveAreaParent
import dev.frost819.newbv.biliapi.http.entity.live.LiveRoomItem
import dev.frost819.newbv.biliapi.repositories.LiveRepository
import dev.frost819.newbv.app.ui.component.livecard.LiveRoomCardData
import dev.frost819.newbv.app.ui.component.livecard.formatOnlineCount
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
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
 * 管理 2 个 Tab 的状态：推荐（直播间列表）和分区（分区入口列表）。
 */
@HiltViewModel
class LiveHomeViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
) : ViewModel() {

    companion object {
        private const val LOAD_TIMEOUT_MS = 10_000L
    }

    private val _uiState = MutableStateFlow(LiveHomeUiState())
    val uiState: StateFlow<LiveHomeUiState> = _uiState.asStateFlow()

    init {
        loadRecommend()
        loadAreaList()
    }

    /**
     * 加载推荐直播间列表。
     */
    fun loadRecommend() {
        if (_uiState.value.recommendLoading) return

        _uiState.update { it.copy(recommendLoading = true, recommendError = false) }

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
    val recommendItems: List<LiveRoomCardData> = emptyList(),
    val recommendLoading: Boolean = false,
    val recommendError: Boolean = false,
    val areaList: List<LiveAreaParent> = emptyList(),
    val areaLoading: Boolean = false,
    val areaError: Boolean = false,
)
