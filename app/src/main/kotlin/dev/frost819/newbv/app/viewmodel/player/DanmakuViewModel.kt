package dev.frost819.newbv.app.viewmodel.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.danmaku.config.DanmakuState
import dev.frost819.newbv.danmaku.entity.DanmakuType as DanmakuEntityDanmakuType
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.DanmakuType as DataDanmakuType
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 弹幕 ViewModel。
 *
 * 管理弹幕播放器生命周期、弹幕数据加载（XML → DanmakuItemData）、
 * 弹幕配置（大小/透明度/区域/速度/类型/蒙版）、播放同步。
 *
 * 与 [PlayerViewModel] 的同步由 UI 层协调：
 * - 视频播放/暂停 → 调用 [play] / [pause]
 * - seek → 调用 [seekTo]
 * - 切换视频 → 调用 [release] + [init] + [loadDanmaku]
 */
@HiltViewModel
class DanmakuViewModel @Inject constructor(
    private val videoPlayRepository: VideoPlayRepository,
) : ViewModel() {

    private val logger = Loggers.get("DanmakuViewModel")

    /** 弹幕播放器实例，供 Compose `AndroidView` 绑定。 */
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
        private set

    private var danmakuConfig = DanmakuConfig()
    private val danmakuTypeFilter = TypeFilter()

    private val _danmakuState = MutableStateFlow(
        DanmakuState(
            scale = Prefs.defaultDanmakuScale,
            opacity = Prefs.defaultDanmakuOpacity,
            area = Prefs.defaultDanmakuArea,
            speedFactor = Prefs.defaultDanmakuSpeedFactor,
            maskEnabled = Prefs.defaultDanmakuMask,
            enabledTypes = Prefs.defaultDanmakuTypes.map { it.toDanmakuEntity() },
        ),
    )
    val danmakuState = _danmakuState.asStateFlow()

    /** 弹幕防遮挡蒙版数据，由 [loadDanmakuMask] 加载后存储。 */
    private val _danmakuMask = MutableStateFlow<DanmakuMask?>(null)
    val danmakuMask = _danmakuMask.asStateFlow()

    /** 初始化弹幕播放器。 */
    fun init() {
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
        initDanmakuConfig()
    }

    /** 释放弹幕播放器资源。 */
    fun release() {
        danmakuPlayer?.release()
        danmakuPlayer = null
        _danmakuMask.update { null }
    }

    /**
     * 切换视频时清空弹幕数据。
     *
     * 保留 [danmakuPlayer] 实例和配置，仅清空弹幕内容和蒙版。
     * 调用后应接着 [loadDanmaku] 加载新视频的弹幕。
     */
    fun clearDanmaku() {
        danmakuPlayer?.updateData(emptyList())
        _danmakuMask.update { null }
    }

    /**
     * 加载弹幕数据。
     *
     * 从 B 站 XML 弹幕接口获取弹幕，转换为 akdanmaku 的 [DanmakuItemData] 列表。
     *
     * @param cid 视频 CID
     */
    fun loadDanmaku(cid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = BiliHttpApi.getDanmakuXml(cid = cid)
                response.data.map {
                    DanmakuItemData(
                        danmakuId = it.dmid,
                        position = (it.time * 1000).toLong(),
                        content = it.text,
                        mode = when (it.type) {
                            4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                            5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                            else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                        },
                        textSize = it.size,
                        textColor = Color(it.color).toArgb(),
                    )
                }
            }.onSuccess { list ->
                danmakuPlayer?.updateData(list)
                logger.info { "Load danmaku success, size: ${list.size}" }
            }.onFailure { e ->
                logger.warn { "Load danmaku failed: $e" }
            }
        }
    }

    /**
     * 加载弹幕防遮挡蒙版。
     *
     * @param aid 视频 AV 号
     * @param cid 视频 CID
     */
    fun loadDanmakuMask(aid: Long, cid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                videoPlayRepository.getDanmakuMask(
                    aid = aid,
                    cid = cid,
                    preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                )
            }.onSuccess { mask ->
                _danmakuMask.update { mask }
                logger.info { "Load danmaku mask segments: ${mask?.segmentCount ?: 0}" }
            }.onFailure { e ->
                logger.warn { "Load danmaku mask failed: $e" }
            }
        }
    }

    /** 播放弹幕（与视频播放同步）。 */
    fun play() {
        danmakuPlayer?.start()
    }

    /** 暂停弹幕。 */
    fun pause() {
        danmakuPlayer?.pause()
    }

    /** 跳转到指定位置（毫秒），暂停弹幕等待缓冲。 */
    fun seekTo(time: Long) {
        danmakuPlayer?.seekTo(time)
        danmakuPlayer?.pause()
    }

    /** 更新弹幕播放速度。 */
    fun updateSpeed(speed: Float) {
        danmakuPlayer?.updatePlaySpeed(speed)
    }

    /** 切换弹幕开关。 */
    fun toggleDanmaku() {
        val current = _danmakuState.value.enabledTypes
        if (current.isEmpty()) {
            updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(DanmakuEntityDanmakuType.entries))
        } else {
            updateDanmakuState(DanmakuSettingAction.SetEnabledTypes(emptyList()))
        }
    }

    /**
     * 更新弹幕配置。
     *
     * 同时更新内存状态、akdanmaku 配置和 Prefs 持久化。
     */
    fun updateDanmakuState(action: DanmakuSettingAction) {
        val old = _danmakuState.value
        val new = when (action) {
            is DanmakuSettingAction.SetScale -> old.copy(scale = action.scale)
            is DanmakuSettingAction.SetOpacity -> old.copy(opacity = action.opacity)
            is DanmakuSettingAction.SetArea -> old.copy(area = action.area)
            is DanmakuSettingAction.SetSpeedFactor -> old.copy(speedFactor = action.factor)
            is DanmakuSettingAction.SetMaskEnabled -> old.copy(maskEnabled = action.enabled)
            is DanmakuSettingAction.SetEnabledTypes -> old.copy(enabledTypes = action.types)
        }
        if (old == new) return

        _danmakuState.update { new }

        if (new.enabledTypes != old.enabledTypes) {
            updateDanmakuConfigTypeFilter(new.enabledTypes)
            Prefs.defaultDanmakuTypes = new.enabledTypes.map { it.toDataDanmakuType() }
        }
        if (new.scale != old.scale) {
            updateDanmakuScale(new.scale)
            Prefs.defaultDanmakuScale = new.scale
        }
        if (new.speedFactor != old.speedFactor) {
            danmakuPlayer?.setDanmakuRollingSpeed(new.speedFactor)
            Prefs.defaultDanmakuSpeedFactor = new.speedFactor
        }
        if (new.area != old.area) {
            updateDanmakuArea(new.area)
            Prefs.defaultDanmakuArea = new.area
        }
        if (new.opacity != old.opacity) {
            Prefs.defaultDanmakuOpacity = new.opacity
        }
        if (new.maskEnabled != old.maskEnabled) {
            Prefs.defaultDanmakuMask = new.maskEnabled
        }
    }

    private fun initDanmakuConfig() {
        val types = Prefs.defaultDanmakuTypes.map { it.toDanmakuEntity() }
        val area = Prefs.defaultDanmakuArea
        val scale = Prefs.defaultDanmakuScale
        val factor = Prefs.defaultDanmakuSpeedFactor

        danmakuTypeFilter.clear()
        if (!types.contains(DanmakuEntityDanmakuType.All)) {
            val allTypes = DanmakuEntityDanmakuType.entries.toMutableList()
            allTypes.remove(DanmakuEntityDanmakuType.All)
            allTypes.removeAll(types)
            allTypes.mapNotNull {
                when (it) {
                    DanmakuEntityDanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuEntityDanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuEntityDanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }.forEach { danmakuTypeFilter.addFilterItem(it) }
        }

        danmakuConfig = danmakuConfig.copy(
            density = 120,
            textSizeScale = scale,
            screenPart = area,
            dataFilter = listOf(danmakuTypeFilter),
            rollingSpeedFactor = factor,
        )
        danmakuConfig.updateFilter()
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuConfigTypeFilter(enabledTypes: List<DanmakuEntityDanmakuType>) {
        danmakuTypeFilter.clear()
        if (!enabledTypes.contains(DanmakuEntityDanmakuType.All)) {
            val allTypes = DanmakuEntityDanmakuType.entries.toMutableList()
            allTypes.remove(DanmakuEntityDanmakuType.All)
            allTypes.removeAll(enabledTypes)
            allTypes.mapNotNull {
                when (it) {
                    DanmakuEntityDanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuEntityDanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuEntityDanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                    else -> null
                }
            }.forEach { danmakuTypeFilter.addFilterItem(it) }
        }
        danmakuConfig.updateFilter()
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuArea(area: Float) {
        danmakuConfig = danmakuConfig.copy(screenPart = area)
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuScale(scale: Float) {
        danmakuConfig = danmakuConfig.copy(textSizeScale = scale)
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    /** 将 data 层 DanmakuType 映射为 danmaku 模块的 DanmakuType。 */
    private fun DataDanmakuType.toDanmakuEntity(): DanmakuEntityDanmakuType =
        DanmakuEntityDanmakuType.entries.getOrElse(this.ordinal) { DanmakuEntityDanmakuType.All }

    /** 将 danmaku 模块的 DanmakuType 映射为 data 层 DanmakuType。 */
    private fun DanmakuEntityDanmakuType.toDataDanmakuType(): DataDanmakuType =
        DataDanmakuType.entries.getOrElse(this.ordinal) { DataDanmakuType.All }
}
