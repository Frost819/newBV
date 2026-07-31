package dev.frost819.newbv.app.viewmodel.pgc

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.season.Episode
import dev.frost819.newbv.biliapi.entity.video.season.SeasonDetail
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val LOAD_TIMEOUT_MS = 15_000L

/**
 * 番剧详情页 UI 状态。
 *
 * @property seasonDetail 番剧详情数据，加载成功后非 null。
 * @property loading 是否正在加载。
 * @property error 是否加载失败。
 * @property errorTip 错误提示文本。
 * @property isFollowing 是否已追番。
 */
data class SeasonDetailUiState(
    val seasonDetail: SeasonDetail? = null,
    val loading: Boolean = false,
    val error: Boolean = false,
    val errorTip: String = "",
    val isFollowing: Boolean = false,
)

/**
 * 番剧详情页一次性 UI 事件。
 */
sealed interface SeasonDetailUiEffect {
    /** 显示 Toast 消息。 */
    data class ShowToast(val message: String) : SeasonDetailUiEffect

    /** 跳转到播放器。 */
    data class NavigateToPlayer(
        val aid: Long,
        val cid: Long,
        val title: String,
        val cover: String,
        val epid: Int?,
    ) : SeasonDetailUiEffect
}

/**
 * 番剧详情页 ViewModel。
 *
 * 管理番剧详情数据加载、追番/取消追番操作、播放跳转逻辑。
 *
 * @param videoDetailRepository 视频详情仓库（含 PGC 详情获取）。
 * @param userRepository 用户仓库（含追番操作）。
 * @param savedStateHandle Navigation 参数（用于读取 [PgcFeatureRoute.seasonId]）。
 */
@HiltViewModel
class SeasonDetailViewModel @Inject constructor(
    private val videoDetailRepository: VideoDetailRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val logger = KotlinLogging.logger("SeasonDetailViewModel")

    private val seasonId: Int = savedStateHandle.get<Long>("seasonId")?.toInt() ?: 0

    private val _uiState = MutableStateFlow(SeasonDetailUiState())
    val uiState: StateFlow<SeasonDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SeasonDetailUiEffect>()
    val uiEffect: SharedFlow<SeasonDetailUiEffect> = _uiEffect.asSharedFlow()

    init {
        loadSeasonDetail()
    }

    /**
     * 加载番剧详情数据。
     *
     * 获取番剧信息并读取用户追番状态。
     * 超时或失败时标记 error，不崩溃。
     */
    fun loadSeasonDetail() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, error = false, errorTip = "")
            }

            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    val detail = videoDetailRepository.getPgcVideoDetail(
                        seasonId = seasonId,
                        preferApiType = ApiType.Web,
                    )
                    _uiState.update {
                        it.copy(
                            seasonDetail = detail,
                            isFollowing = detail.userStatus.follow,
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to load season detail: $seasonId" }
                _uiState.update {
                    it.copy(
                        error = true,
                        errorTip = error.localizedMessage ?: "加载失败",
                    )
                }
            }

            _uiState.update { it.copy(loading = false) }
        }
    }

    /**
     * 切换追番状态。
     */
    fun toggleFollow() {
        val currentDetail = _uiState.value.seasonDetail ?: return
        val isFollowing = _uiState.value.isFollowing
        viewModelScope.launch {
            runCatching {
                if (isFollowing) {
                    userRepository.delSeasonFollow(
                        seasonId = currentDetail.seasonId,
                        preferApiType = ApiType.Web,
                    )
                } else {
                    userRepository.addSeasonFollow(
                        seasonId = currentDetail.seasonId,
                        preferApiType = ApiType.Web,
                    )
                }
            }.onSuccess { toast ->
                _uiState.update { it.copy(isFollowing = !isFollowing) }
                if (toast.isNotEmpty()) {
                    _uiEffect.emit(SeasonDetailUiEffect.ShowToast(toast))
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to toggle season follow" }
                _uiEffect.emit(
                    SeasonDetailUiEffect.ShowToast(
                        "${if (isFollowing) "取消追番" else "追番"}失败: ${error.message ?: "未知错误"}"
                    )
                )
            }
        }
    }

    /**
     * 点击播放按钮。
     *
     * 如果有观看记录，续播上次的分集；否则播放第一集。
     */
    fun onPlay() {
        val detail = _uiState.value.seasonDetail ?: return
        val progress = detail.userStatus.progress

        if (progress != null) {
            val lastEp = findEpisodeById(detail, progress.lastEpId)
            if (lastEp != null) {
                emitNavigateToPlayer(lastEp)
                return
            }
        }

        val firstEp = detail.episodes.firstOrNull()
        if (firstEp != null) {
            emitNavigateToPlayer(firstEp)
        }
    }

    /**
     * 点击某个分集播放。
     */
    fun onPlayEpisode(episode: Episode) {
        emitNavigateToPlayer(episode)
    }

    /**
     * 切换到同系列的其他季。
     */
    fun onSwitchSeason(targetSeasonId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, error = false, errorTip = "")
            }

            runCatching {
                withTimeout(LOAD_TIMEOUT_MS) {
                    val detail = videoDetailRepository.getPgcVideoDetail(
                        seasonId = targetSeasonId,
                        preferApiType = ApiType.Web,
                    )
                    _uiState.update {
                        it.copy(
                            seasonDetail = detail,
                            isFollowing = detail.userStatus.follow,
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                logger.error(error) { "Failed to switch season: $targetSeasonId" }
                _uiState.update {
                    it.copy(
                        error = true,
                        errorTip = error.localizedMessage ?: "加载失败",
                    )
                }
            }

            _uiState.update { it.copy(loading = false) }
        }
    }

    /**
     * 在正片和附加分集中查找指定 epid 的分集。
     */
    private fun findEpisodeById(detail: SeasonDetail, epId: Int): Episode? {
        detail.episodes.forEach { if (it.epid == epId) return it }
        detail.sections.forEach { section ->
            section.episodes.forEach { if (it.epid == epId) return it }
        }
        return null
    }

    private fun emitNavigateToPlayer(episode: Episode) {
        viewModelScope.launch {
            _uiEffect.emit(
                SeasonDetailUiEffect.NavigateToPlayer(
                    aid = episode.aid,
                    cid = episode.cid,
                    title = episode.title,
                    cover = episode.cover,
                    epid = episode.epid,
                )
            )
        }
    }
}
