package dev.frost819.newbv.app.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.action.player.SubtitleSettingAction
import dev.frost819.newbv.app.ui.state.player.SubtitleState
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.video.Subtitle
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.bilisubtitle.SubtitleParser
import dev.frost819.newbv.data.datastore.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dev.frost819.newbv.data.datastore.ApiType as DataApiType

/**
 * 字幕 ViewModel。
 *
 * 管理字幕列表加载、字幕轨道选择、字幕显示配置（字号/透明度/间距）。
 *
 * @param videoPlayRepository 字幕数据仓库
 */
@HiltViewModel
class SubtitleViewModel @Inject constructor(
    private val videoPlayRepository: VideoPlayRepository,
) : ViewModel() {

    private val logger = KotlinLogging.logger { }

    private val _subtitleState = MutableStateFlow(
        SubtitleState(
            fontSize = Prefs.defaultSubtitleFontSize,
            opacity = Prefs.defaultSubtitleBackgroundOpacity,
            bottomPadding = Prefs.defaultSubtitleBottomPadding,
        ),
    )
    val subtitleState = _subtitleState.asStateFlow()

    private val _subtitleList = MutableStateFlow<List<Subtitle>>(emptyList())
    val subtitleList = _subtitleList.asStateFlow()

    private val _subtitleId = MutableStateFlow(-1L)
    val subtitleId = _subtitleId.asStateFlow()

    private val _subtitleData = MutableStateFlow<List<dev.frost819.newbv.bilisubtitle.entity.SubtitleItem>>(emptyList())
    val subtitleData = _subtitleData.asStateFlow()

    /**
     * 加载字幕列表。
     *
     * @param aid 视频 AV 号
     * @param cid 视频 CID
     */
    fun loadSubtitleList(aid: Long, cid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val apiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web
                videoPlayRepository.getSubtitle(aid = aid, cid = cid, preferApiType = apiType)
            }.onSuccess { list ->
                _subtitleList.update { list }
                logger.info { "Update subtitle size: ${list.size}" }
            }.onFailure { e ->
                logger.warn { "Update subtitle failed: $e" }
            }
        }
    }

    /**
     * 选择并加载字幕轨道。
     *
     * @param id 字幕 ID，-1 表示关闭字幕
     */
    fun selectSubtitle(id: Long) {
        if (id == -1L) {
            _subtitleId.update { -1L }
            _subtitleData.update { emptyList() }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val subtitle = _subtitleList.value.find { it.id == id } ?: return@runCatching
                logger.info { "Subtitle url: ${subtitle.url}" }
                val client = HttpClient(OkHttp)
                val responseText = client.get(subtitle.url).bodyAsText()
                val data = SubtitleParser.fromBccString(responseText)
                _subtitleId.update { id }
                _subtitleData.update { data }
            }.onFailure { e ->
                logger.warn { "Load subtitle failed: $e" }
            }
        }
    }

    /** 切换字幕开关。 */
    fun toggleSubtitle() {
        if (_subtitleId.value != -1L) {
            selectSubtitle(-1L)
            return
        }
        _subtitleList.value.firstOrNull { it.id != -1L }?.let { selectSubtitle(it.id) }
    }

    /**
     * 更新字幕显示配置。
     *
     * 同时更新内存状态和 Prefs 持久化。
     */
    fun updateSubtitleState(action: SubtitleSettingAction) {
        val old = _subtitleState.value
        val new = when (action) {
            is SubtitleSettingAction.SetFontSize -> old.copy(fontSize = action.sp)
            is SubtitleSettingAction.SetOpacity -> old.copy(opacity = action.opacity)
            is SubtitleSettingAction.SetBottomPadding -> old.copy(bottomPadding = action.dp)
        }
        if (old == new) return
        _subtitleState.update { new }

        when (action) {
            is SubtitleSettingAction.SetFontSize -> Prefs.defaultSubtitleFontSize = action.sp
            is SubtitleSettingAction.SetOpacity -> Prefs.defaultSubtitleBackgroundOpacity = action.opacity
            is SubtitleSettingAction.SetBottomPadding -> Prefs.defaultSubtitleBottomPadding = action.dp
        }
    }
}
