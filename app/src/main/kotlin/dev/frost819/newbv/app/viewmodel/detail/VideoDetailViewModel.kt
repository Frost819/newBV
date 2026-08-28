package dev.frost819.newbv.app.viewmodel.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.VideoInfoRepository
import dev.frost819.newbv.app.entity.player.VideoListItem
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.FavoriteFolderMetadata
import dev.frost819.newbv.biliapi.entity.video.VideoDetail
import dev.frost819.newbv.biliapi.repositories.CoinRepository
import dev.frost819.newbv.biliapi.repositories.FavoriteRepository
import dev.frost819.newbv.biliapi.repositories.LikeRepository
import dev.frost819.newbv.biliapi.repositories.OneClickTripleActionRepository
import dev.frost819.newbv.biliapi.repositories.UserRepository
import dev.frost819.newbv.biliapi.repositories.VideoDetailRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
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

/** 网络请求超时时间（毫秒）。 */
private const val LOAD_TIMEOUT_MS = 15_000L

/**
 * 视频详情页 UI 状态。
 *
 * @property detail 视频详情数据，加载成功后非 null。
 * @property loading 是否正在加载。
 * @property error 是否加载失败。
 * @property errorTip 错误提示文本。
 * @property isLiked 是否已点赞。
 * @property isCoined 是否已投币。
 * @property isFavorite 是否已收藏。
 * @property isFollowing 是否已关注 UP 主。
 * @property favoriteFolders 用户收藏夹列表。
 * @property videoFavoriteFolderIds 视频已加入的收藏夹 ID 集合。
 * @property historyLastPlayedCid 播放器返回后的历史进度 CID。
 * @property historyLastPlayedTime 播放器返回后的历史进度时间（秒）。
 */
data class VideoDetailUiState(
    val detail: VideoDetail? = null,
    val loading: Boolean = false,
    val error: Boolean = false,
    val errorTip: String = "",
    val isLiked: Boolean = false,
    val isCoined: Boolean = false,
    val isFavorite: Boolean = false,
    val isFollowing: Boolean = false,
    val favoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    val videoFavoriteFolderIds: Set<Long> = emptySet(),
    val historyLastPlayedCid: Long = 0L,
    val historyLastPlayedTime: Int = 0,
)

/**
 * 视频详情页一次性 UI 事件。
 */
sealed interface VideoDetailUiEffect {
    /** 显示 Toast 消息。 */
    data class ShowToast(
        val message: String,
    ) : VideoDetailUiEffect

    /** 跳转到 PGC 番剧详情页。 */
    data class NavigateToSeason(
        val epid: Int,
    ) : VideoDetailUiEffect
}

/**
 * 视频详情页 ViewModel。
 *
 * 管理视频详情数据加载、点赞/投币/收藏操作、收藏夹数据获取。
 * 使用 [StateFlow] 暴露状态，[SharedFlow] 暴露一次性事件。
 *
 * @property videoDetailRepository 视频详情数据仓库。
 * @property likeRepository 点赞仓库。
 * @property coinRepository 投币仓库。
 * @property favoriteRepository 收藏仓库。
 * @property oneClickTripleActionRepository 一键三连仓库。
 * @property savedStateHandle Navigation 参数（用于读取 [VideoDetailRoute.aid]）。
 */
@HiltViewModel
class VideoDetailViewModel
    @Inject
    constructor(
        private val videoDetailRepository: VideoDetailRepository,
        private val likeRepository: LikeRepository,
        private val coinRepository: CoinRepository,
        private val favoriteRepository: FavoriteRepository,
        private val oneClickTripleActionRepository: OneClickTripleActionRepository,
        private val userRepository: UserRepository,
        private val videoInfoRepository: VideoInfoRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val logger = Loggers.get("VideoDetailViewModel")

        private val routeAid: Long = savedStateHandle.get<Long>("aid") ?: 0L
        val aid: Long = routeAid
        private val routeBvid: String = savedStateHandle.get<String>("bvid") ?: ""

        private val _uiState = MutableStateFlow(VideoDetailUiState())
        val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

        private fun prefApiType(): ApiType =
            if (Prefs.apiType == dev.frost819.newbv.data.datastore.ApiType.App) ApiType.App else ApiType.Web

        private val _uiEffect = MutableSharedFlow<VideoDetailUiEffect>()
        val uiEffect: SharedFlow<VideoDetailUiEffect> = _uiEffect.asSharedFlow()

        init {
            loadVideoDetail()
            viewModelScope.launch {
                videoInfoRepository.videoSharedState
                    .collect { state ->
                        val matched = state?.takeIf { it.aid == aid }
                        _uiState.update {
                            it.copy(
                                isLiked = matched?.liked ?: it.isLiked,
                                isCoined = matched?.coined ?: it.isCoined,
                                isFavorite = matched?.favorited ?: it.isFavorite,
                                historyLastPlayedCid = state?.lastPlayedCid ?: it.historyLastPlayedCid,
                                historyLastPlayedTime = state?.lastPlayedTime ?: it.historyLastPlayedTime,
                            )
                        }
                    }
            }
        }

        /**
         * 更新播放列表（单视频）。
         *
         * 从详情页跳转播放器前调用，将当前视频作为播放列表。
         *
         * @param aid 视频 AV 号
         * @param cid 视频 CID
         * @param title 视频标题
         */
        fun updateVideoList(
            aid: Long,
            cid: Long,
            title: String,
        ) {
            videoInfoRepository.updateVideoList(
                listOf(VideoListItem(aid = aid, cid = cid, title = title)),
            )
        }

        /**
         * 更新播放列表（UGC 合集分集）。
         *
         * 从详情页 UGC 合集跳转播放器前调用，将合集内所有视频作为播放列表。
         *
         * @param sectionIndex 合集分集索引
         */
        fun updateVideoList(sectionIndex: Int) {
            val detail = _uiState.value.detail ?: return
            val partVideoList =
                detail.ugcSeason?.sections?.getOrNull(sectionIndex)?.episodes?.map {
                    VideoListItem(aid = it.aid, cid = it.cid, title = it.title)
                } ?: return
            videoInfoRepository.updateVideoList(partVideoList)
        }

        /**
         * 加载视频详情数据。
         *
         * 获取视频信息并合并用户操作状态（点赞/投币/收藏）。
         * 超时或失败时标记 error，不崩溃。
         */
        fun loadVideoDetail() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(loading = true, error = false, errorTip = "")
                }

                runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) {
                        val detail =
                            videoDetailRepository.getVideoDetail(
                                aid = aid,
                                preferApiType = ApiType.Web,
                                bvid = routeBvid,
                            )
                        _uiState.update {
                            it.copy(
                                detail = detail,
                                isLiked = detail.userActions.like,
                                isCoined = detail.userActions.coin,
                                isFavorite = detail.userActions.favorite,
                            )
                        }

                        // 同步到 VideoInfoRepository（相关视频、历史进度）
                        videoInfoRepository.updateVideoDetail(detail)

                        if (Prefs.isLogin) {
                            fetchFavoriteFolders()
                            fetchFollowingStatus(detail.author.mid)
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException && error !is TimeoutCancellationException) {
                        throw error
                    }
                    logger.error(error) { "Failed to load video detail: $aid" }
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
         * 获取用户收藏夹列表及视频已收藏状态。
         */
        private suspend fun fetchFavoriteFolders() {
            runCatching {
                favoriteRepository.getAllFavoriteFolderMetadataList(
                    mid = Prefs.uid,
                    rid = aid,
                    preferApiType = prefApiType(),
                )
            }.onSuccess { folders ->
                val folderIds = folders.filter { it.videoInThisFav }.map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        favoriteFolders = folders,
                        videoFavoriteFolderIds = folderIds,
                    )
                }
            }.onFailure { error ->
                logger.error(error) { "Failed to fetch favorite folders" }
            }
        }

        /**
         * 获取当前用户是否已关注 UP 主。
         */
        private suspend fun fetchFollowingStatus(upMid: Long) {
            runCatching {
                userRepository.checkIsFollowing(mid = upMid)
            }.onSuccess { isFollowing ->
                _uiState.update { it.copy(isFollowing = isFollowing ?: false) }
            }.onFailure { error ->
                logger.error(error) { "Failed to fetch following status" }
            }
        }

        /**
         * 切换关注 UP 主状态。
         */
        fun toggleFollow() {
            val currentDetail = _uiState.value.detail ?: return
            val isFollowing = _uiState.value.isFollowing
            val preferApiType = prefApiType()
            viewModelScope.launch {
                runCatching {
                    if (isFollowing) {
                        userRepository.unfollowUser(mid = currentDetail.author.mid, preferApiType = preferApiType)
                    } else {
                        userRepository.followUser(mid = currentDetail.author.mid, preferApiType = preferApiType)
                    }
                }.onSuccess {
                    _uiState.update { it.copy(isFollowing = !isFollowing) }
                }.onFailure { error ->
                    logger.error(error) { "Failed to toggle follow" }
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast(
                            "${if (isFollowing) "取消关注" else "关注"}失败: ${error.message ?: "未知错误"}",
                        ),
                    )
                }
            }
        }

        /**
         * 切换视频点赞状态。
         *
         * @param like true 为点赞，false 为取消。
         */
        fun toggleLike(like: Boolean) {
            val currentDetail = _uiState.value.detail ?: return
            viewModelScope.launch {
                runCatching {
                    likeRepository.updateVideoLiked(
                        aid = currentDetail.aid,
                        bvid = currentDetail.bvid,
                        like = like,
                        preferApiType = prefApiType(),
                    )
                }.onSuccess {
                    _uiState.update { it.copy(isLiked = like) }
                    videoInfoRepository.updateVideoActionState(aid = currentDetail.aid, liked = like)
                }.onFailure { error ->
                    logger.error(error) { "Failed to toggle like" }
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast("点赞失败: ${error.message ?: "未知错误"}"),
                    )
                }
            }
        }

        /**
         * 投币（默认 1 枚）。
         */
        fun sendCoin() {
            val currentDetail =
                _uiState.value.detail ?: run {
                    logger.warn { "sendCoin: detail is null" }
                    return
                }
            logger.info { "Sending coin: aid=${currentDetail.aid}, bvid=${currentDetail.bvid}" }
            viewModelScope.launch {
                runCatching {
                    coinRepository.sendVideoCoin(
                        aid = currentDetail.aid,
                        bvid = currentDetail.bvid,
                        preferApiType = prefApiType(),
                    )
                }.onSuccess {
                    _uiState.update { it.copy(isCoined = true) }
                    videoInfoRepository.updateVideoActionState(aid = currentDetail.aid, coined = true)
                }.onFailure { error ->
                    logger.error(error) { "Failed to send coin" }
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast("投币失败: ${error.message ?: "未知错误"}"),
                    )
                }
            }
        }

        /**
         * 更新视频收藏夹（添加/移除收藏夹）。
         *
         * @param folderIds 目标收藏夹 ID 列表。
         */
        fun updateFavorite(folderIds: List<Long>) {
            val currentDetail = _uiState.value.detail ?: return
            val currentFolders = _uiState.value.favoriteFolders
            viewModelScope.launch {
                runCatching {
                    favoriteRepository.updateVideoToFavoriteFolder(
                        aid = currentDetail.aid,
                        addMediaIds = folderIds,
                        delMediaIds = currentFolders.map { it.id } - folderIds.toSet(),
                        preferApiType = prefApiType(),
                    )
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            isFavorite = folderIds.isNotEmpty(),
                            videoFavoriteFolderIds = folderIds.toSet(),
                        )
                    }
                }.onFailure { error ->
                    logger.error(error) { "Failed to update favorite" }
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast("收藏失败: ${error.message ?: "未知错误"}"),
                    )
                }
            }
            videoInfoRepository.updateVideoActionState(
                aid = currentDetail.aid,
                favorited = folderIds.isNotEmpty(),
            )
        }

        /**
         * 切换收藏状态（快速收藏/取消收藏到默认收藏夹）。
         *
         * 已收藏时取消所有收藏夹；未收藏时添加到默认收藏夹。
         * TODO(后续实现收藏夹选择弹窗)。
         */
        fun toggleFavorite() {
            val isFav = _uiState.value.isFavorite
            logger.info { "toggleFavorite called, isFavorite=$isFav, folders=${_uiState.value.favoriteFolders.size}" }
            if (isFav) {
                updateFavorite(emptyList())
            } else {
                val defaultFolderId =
                    _uiState.value.favoriteFolders
                        .firstOrNull { it.title == "默认收藏夹" }
                        ?.id
                if (defaultFolderId != null) {
                    updateFavorite(listOf(defaultFolderId))
                } else {
                    viewModelScope.launch {
                        _uiEffect.emit(
                            VideoDetailUiEffect.ShowToast("未找到默认收藏夹"),
                        )
                    }
                }
            }
        }

        /**
         * 一键三连（点赞 + 投币 + 收藏到默认收藏夹）。
         */
        fun oneClickTripleAction() {
            val currentDetail = _uiState.value.detail ?: return
            viewModelScope.launch {
                runCatching {
                    oneClickTripleActionRepository.sendVideoOneClickTripleAction(
                        aid = currentDetail.aid,
                        bvid = currentDetail.bvid,
                        preferApiType = prefApiType(),
                    )
                }.onSuccess { data ->
                    if (data != null) {
                        val defaultFolderId =
                            _uiState.value.favoriteFolders
                                .firstOrNull { it.title == "默认收藏夹" }
                                ?.id
                        _uiState.update {
                            it.copy(
                                isLiked = data.like,
                                isCoined = data.coin,
                                isFavorite = data.fav,
                                videoFavoriteFolderIds =
                                    defaultFolderId?.let { id ->
                                        it.videoFavoriteFolderIds + id
                                    } ?: it.videoFavoriteFolderIds,
                            )
                        }
                        videoInfoRepository.updateVideoActionState(
                            aid = currentDetail.aid,
                            liked = data.like,
                            coined = data.coin,
                            favorited = data.fav,
                        )
                        _uiEffect.emit(VideoDetailUiEffect.ShowToast("一键三连"))
                    }
                }.onFailure { error ->
                    logger.error(error) { "Failed to one-click triple action" }
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast("一键三连失败: ${error.message ?: "未知错误"}"),
                    )
                }
            }
        }
    }
