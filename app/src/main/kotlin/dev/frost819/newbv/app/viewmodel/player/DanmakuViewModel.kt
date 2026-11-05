package dev.frost819.newbv.app.viewmodel.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TextColorFilter
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.app.data.danmaku.DanmakuFilterRepository
import dev.frost819.newbv.app.ui.action.player.DanmakuSettingAction
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMeta
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuData
import dev.frost819.newbv.biliapi.repositories.VideoPlayRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.danmaku.config.DanmakuState
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.math.abs
import dev.frost819.newbv.danmaku.entity.DanmakuType as DanmakuEntityDanmakuType
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.DanmakuType as DataDanmakuType

/**
 * 弹幕 ViewModel。
 *
 * 管理弹幕播放器生命周期、弹幕数据加载（分段 → [DanmakuItemData]）、
 * 弹幕配置（大小/透明度/区域/速度/类型/蒙版）、播放同步。
 *
 * 弹幕数据采用分段加载（每段时长由 dm/view 元数据决定，通常 6 分钟）：
 * - [loadDanmaku] 获取元数据并按初始位置加载首段
 * - [onVideoPositionChanged] 由 UI 层喂入播放进度，内部按段号去重后驱动加载
 * - 每次加载目标段及预取下一段，已加载/加载中的段自动跳过
 * - 失败重试若干次后放弃（不降级 XML），进度进入下一段时自然恢复
 *
 * 与 [PlayerViewModel] 的同步由 UI 层协调：
 * - 播放进度 → 调用 [onVideoPositionChanged]：既驱动分段加载，也自动对齐弹幕
 *   引擎时钟（位置跳变超过阈值时 seek 引擎），因此任何改变视频位置的入口
 *   （断点续播、切集、用户 seek、回到开头）都无需单独通知本 VM
 * - 视频播放/暂停/缓冲 → 调用 [play] / [pause]
 * - 播放倍速 → 调用 [updateSpeed]：引擎时钟按倍速缩放，与视频同速，避免弹幕乱跳
 * - 切换视频 → 调用 [clearDanmaku] + [loadDanmaku]
 */
@HiltViewModel
class DanmakuViewModel
    @Inject
    constructor(
        private val videoPlayRepository: VideoPlayRepository,
        private val danmakuFilterRepository: DanmakuFilterRepository,
    ) : ViewModel() {
        private val logger = Loggers.get("DanmakuViewModel")

        /**
         * 弹幕播放器实例，供 Compose `AndroidView` 绑定。
         * internal set 供单元测试注入 MockK 引擎，生产代码仅在 [init] / [release] 中赋值。
         */
        var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
            internal set

        /**
         * 引擎是否应处于运行态（由最近一次 [play]/[pause] 决定）。
         *
         * `DanmakuPlayer.seekTo` 内部会 `timer.start()` 解暂停，对齐时钟时需据此还原，
         * 否则暂停/缓冲期间喂入跳变位置会让弹幕继续滚动。
         */
        private var danmakuRunning = false

        private var danmakuConfig = DanmakuConfig()
        private val danmakuTypeFilter = TypeFilter()
        private val danmakuTextColorFilter = TextColorFilter()

        private val _danmakuState =
            MutableStateFlow(
                DanmakuState(
                    scale = Prefs.defaultDanmakuScale,
                    opacity = Prefs.defaultDanmakuOpacity,
                    area = Prefs.defaultDanmakuArea,
                    speedFactor = Prefs.defaultDanmakuSpeedFactor,
                    maskEnabled = Prefs.defaultDanmakuMask,
                    enabledTypes = Prefs.defaultDanmakuTypes.map { it.toDanmakuEntity() },
                    blockColorfulEnabled = Prefs.defaultDanmakuBlockColorfulEnabled,
                    blockRepeatEnabled = Prefs.defaultDanmakuBlockRepeatEnabled,
                    blockByUserFilterEnabled = Prefs.defaultDanmakuBlockByUserFilterEnabled,
                    aiCloudLevel = Prefs.defaultDanmakuAiCloudLevel,
                ),
            )
        val danmakuState = _danmakuState.asStateFlow()

        /** 弹幕防遮挡蒙版数据，由 [loadDanmakuMask] 加载后存储。 */
        private val _danmakuMask = MutableStateFlow<DanmakuMask?>(null)
        val danmakuMask = _danmakuMask.asStateFlow()

        /** 蒙版请求独立于弹幕数据请求，切集时 cancel 旧请求即可。 */
        private var maskFetchJob: Job? = null

        // === 分段加载状态 ===

        /** 当前视频 aid。 */
        private var currentAid = 0L

        /** 当前视频 cid；<= 0 表示尚未加载视频。 */
        private var currentCid = 0L

        /** 每段时长（毫秒），由 dm/view 元数据决定，获取失败时回退默认值。 */
        private var segmentSizeMs = DEFAULT_SEGMENT_SIZE_MS

        /** 分段总数；0 表示未知，不用于越界限制。 */
        private var segmentTotal = 0

        /** 当前视频弹幕是否已关闭（dm/view state==1），关闭时不加载任何分段。 */
        private var danmakuClosed = false

        /** 已加载的分段号集合。 */
        private val loadedSegments = mutableSetOf<Int>()

        /** 加载中的段号，防止同一分段并发重复请求。 */
        private val loadingSegments = mutableSetOf<Int>()

        /**
         * 切集防陈旧计数：[loadDanmaku] 每次自增，
         * 旧协程返回后凭此比对丢弃结果，避免旧视频数据污染新视频。
         */
        private var loadGeneration = 0

        /** UI 层喂入的播放进度（毫秒），驱动分段加载；conflated，仅保留最新值。 */
        private val currentTimeFlow = MutableStateFlow(0L)

        /** 分段加载监听 Job，[loadDanmaku] 拿到元数据后启动。 */
        private var segmentWatchJob: Job? = null

        /**
         * 分段过滤/合并使用的调度器。
         *
         * 默认 [Dispatchers.Default]（单段可达数千条 × N 条规则，属 CPU 密集操作，
         * 放主线程会掉帧）；单元测试替换为测试调度器以配合 `advanceUntilIdle`。
         */
        internal var filterDispatcher: CoroutineDispatcher = Dispatchers.Default

        init {
            // 账号规则由应用级仓库统一缓存/预热：这里只镜像菜单展示条数，
            // 并在规则晚于分段就绪时重载当前分段，保证已渲染弹幕也按新规则过滤
            viewModelScope.launch {
                danmakuFilterRepository.ruleCount.collect { count ->
                    _danmakuState.update { it.copy(userFilterRuleCount = count) }
                }
            }
            viewModelScope.launch {
                danmakuFilterRepository.version.drop(1).collect {
                    if (_danmakuState.value.blockByUserFilterEnabled && danmakuFilterRepository.hasRules) {
                        reloadCurrentSegments()
                    }
                }
            }
        }

        /** 初始化弹幕播放器。 */
        fun init() {
            if (danmakuPlayer != null) return
            danmakuPlayer = DanmakuPlayer(SimpleRenderer())
            initDanmakuConfig()
        }

        /** 释放弹幕播放器资源。 */
        fun release() {
            maskFetchJob?.cancel()
            maskFetchJob = null
            segmentWatchJob?.cancel()
            segmentWatchJob = null
            danmakuPlayer?.release()
            danmakuPlayer = null
            _danmakuMask.update { null }
        }

        override fun onCleared() {
            release()
            super.onCleared()
        }

        /**
         * 切换视频时清空弹幕数据。
         *
         * 保留 [danmakuPlayer] 实例和配置，清空弹幕内容、蒙版和分段状态。
         * 调用后应接着 [loadDanmaku] 加载新视频的弹幕。
         */
        fun clearDanmaku() {
            maskFetchJob?.cancel()
            maskFetchJob = null
            segmentWatchJob?.cancel()
            segmentWatchJob = null
            loadGeneration++
            currentAid = 0L
            currentCid = 0L
            loadedSegments.clear()
            loadingSegments.clear()
            danmakuClosed = false
            currentTimeFlow.value = 0L
            // 引擎 updateData 是增量语义，必须调用 clearData 才能真正清空旧数据
            danmakuPlayer?.clearData()
            _danmakuMask.update { null }
        }

        /**
         * 加载弹幕数据（分段加载）。
         *
         * 流程：重置分段状态 → 获取 dm/view 元数据（失败回退默认 6 分钟分段）
         * → 启动进度监听 → 由初始位置触发首段加载。
         *
         * @param aid 视频 AV 号
         * @param cid 视频 CID
         * @param initialPositionMs 初始播放位置（毫秒，如断点续播位置），
         *        决定首个加载的分段；省略时从第 1 段开始
         */
        fun loadDanmaku(
            aid: Long,
            cid: Long,
            initialPositionMs: Long = 0L,
        ) {
            val generation = ++loadGeneration
            // 同步重置状态：切集后旧分段数据立即失效
            currentAid = aid
            currentCid = cid
            segmentSizeMs = DEFAULT_SEGMENT_SIZE_MS
            segmentTotal = 0
            danmakuClosed = false
            loadedSegments.clear()
            loadingSegments.clear()
            currentTimeFlow.value = initialPositionMs.coerceAtLeast(0L)
            danmakuPlayer?.clearData()

            viewModelScope.launch {
                // 元数据决定分段大小与总数；失败仅记日志，回退默认分段大小继续加载
                val meta =
                    runCatching {
                        withTimeout(SEGMENT_FETCH_TIMEOUT_MS) {
                            videoPlayRepository.getDanmakuMeta(aid = aid, cid = cid)
                        }
                    }.getOrElse { e ->
                        if (e is CancellationException && e !is TimeoutCancellationException) throw e
                        logger.warn { "Load danmaku meta failed, fallback to default segment size: $e" }
                        DanmakuMeta.DEFAULT
                    }
                if (generation != loadGeneration) return@launch

                if (meta.segmentSizeMs > 0) segmentSizeMs = meta.segmentSizeMs
                segmentTotal = meta.segTotal
                if (meta.closed) {
                    danmakuClosed = true
                    logger.info { "Danmaku closed for cid=$cid, skip segment loading" }
                    return@launch
                }

                logger.info {
                    "Danmaku meta: segmentSizeMs=$segmentSizeMs, segTotal=$segmentTotal, count=${meta.count}"
                }

                // 通常 App 启动时已预热完成；未完成时异步补拉，不阻塞首段弹幕
                danmakuFilterRepository.prefetch()

                startSegmentWatcher()
                // 触发初始段加载：watcher 订阅 StateFlow 时会立即收到当前值
                currentTimeFlow.value = initialPositionMs.coerceAtLeast(0L)
            }
        }

        /**
         * 由 UI 层喂入视频播放位置（毫秒），弹幕时间轴的唯一同步入口。
         *
         * - 驱动分段加载：内部换算段号后去重，仅段号变化时触发请求，高频调用安全
         * - 对齐引擎时钟：引擎按自身时钟渲染、分段只加载目标段附近，时钟一旦
         *   偏离视频位置（断点续播 seek、切集、用户 seek 等）窗口内将无弹幕可
         *   渲染且不会自愈，因此位置跳变时必须显式对齐（见 [reconcileEngineTime]）
         */
        fun onVideoPositionChanged(timeMs: Long) {
            currentTimeFlow.value = timeMs
            reconcileEngineTime(videoTimeMs = timeMs, player = danmakuPlayer)
        }

        /**
         * 对齐弹幕引擎时钟：与视频位置偏差超过 [ENGINE_TIME_JUMP_THRESHOLD_MS] 时 seek 引擎。
         *
         * [player] 参数化仅为可测性（单元测试注入 MockK 引擎），生产调用方传 [danmakuPlayer]。
         *
         * 注意 `DanmakuPlayer.seekTo` 会经 `DanmakuTimer.start()` 把引擎从暂停态解开，
         * 因此 seek 后需按 [danmakuRunning] 还原暂停态，避免暂停/缓冲期间弹幕继续滚动。
         */
        internal fun reconcileEngineTime(
            videoTimeMs: Long,
            player: DanmakuPlayer?,
        ) {
            if (player == null) return
            val engineTimeMs = player.getCurrentTimeMs()
            if (abs(videoTimeMs - engineTimeMs) <= ENGINE_TIME_JUMP_THRESHOLD_MS) return
            logger.info { "Align danmaku engine clock: video=$videoTimeMs ms, engine=$engineTimeMs ms" }
            player.seekTo(videoTimeMs)
            if (!danmakuRunning) player.pause()
        }

        /**
         * 启动分段加载监听：播放进度换算段号，段号变化时加载目标段及预取段。
         *
         * 用响应式 Flow 而非定时轮询（AGENTS.md 11.5.4）：无 VM 内自调度循环，
         * 测试可安全使用 advanceUntilIdle。
         */
        private fun startSegmentWatcher() {
            segmentWatchJob?.cancel()
            segmentWatchJob =
                viewModelScope.launch {
                    currentTimeFlow
                        .map { segmentIndexOf(it) }
                        .distinctUntilChanged()
                        .collectLatest { targetSeg ->
                            ensureSegments(targetSeg)
                        }
                }
        }

        /**
         * 由播放位置换算分段索引（从 1 开始）。
         *
         * @param timeMs 播放位置（毫秒）
         */
        internal fun segmentIndexOf(timeMs: Long): Int =
            if (segmentSizeMs <= 0) {
                1
            } else {
                (timeMs / segmentSizeMs).toInt() + 1
            }

        /**
         * 确保目标段及预取段（[DEFAULT_SEGMENT_PREFETCH_AHEAD] 段之后）已加载。
         *
         * 已加载/加载中/越界（超过 [segmentTotal]）的段自动跳过。
         * 段内顺序串行请求，避免同一时刻多段并发。
         */
        private suspend fun ensureSegments(targetSeg: Int) {
            if (danmakuClosed || currentCid <= 0L) return
            val toLoad = (targetSeg..(targetSeg + DEFAULT_SEGMENT_PREFETCH_AHEAD)).filter { canLoadSegment(it) }
            for (seg in toLoad) {
                fetchSegmentWithRetry(seg)
            }
        }

        /**
         * 判断分段是否可以发起加载。
         *
         * @return true 表示可加载（同时标记为加载中）；false 表示已加载/加载中/越界
         */
        private fun canLoadSegment(segmentIndex: Int): Boolean {
            if (segmentIndex <= 0) return false
            if (segmentTotal > 0 && segmentIndex > segmentTotal) return false
            if (segmentIndex in loadedSegments) return false
            if (segmentIndex in loadingSegments) return false
            loadingSegments.add(segmentIndex)
            return true
        }

        /** 加载期过滤开关变化后重载当前段：已加载分段不会随开关变化。 */
        private fun reloadCurrentSegments() {
            val player = danmakuPlayer ?: return
            if (loadedSegments.isEmpty() && loadingSegments.isEmpty()) return
            val targetSeg = segmentIndexOf(currentTimeFlow.value)
            viewModelScope.launch {
                loadGeneration++
                player.clearData()
                loadedSegments.clear()
                loadingSegments.clear()
                logger.info { "Reload current segments: targetSeg=$targetSeg" }
                ensureSegments(targetSeg)
            }
        }

        /**
         * 拉取单个分段并追加到弹幕引擎，带超时与重试。
         *
         * 重试耗尽后放弃该段（记日志），进度进入其他段时自然恢复。
         * [TimeoutCancellationException] 视为普通失败参与重试；
         * 其余 [CancellationException] 属外层取消（collectLatest 切段 / 切集），必须上抛。
         */
        private suspend fun fetchSegmentWithRetry(segmentIndex: Int) {
            // 发起时的 generation：重载/切集后凭此丢弃在途结果
            val generation = loadGeneration
            try {
                repeat(SEGMENT_MAX_ATTEMPTS) { attempt ->
                    try {
                        val dataList =
                            withTimeout(SEGMENT_FETCH_TIMEOUT_MS) {
                                videoPlayRepository.getDanmakuSegment(
                                    aid = currentAid,
                                    cid = currentCid,
                                    segmentIndex = segmentIndex,
                                    preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                                )
                            }
                        val mergeEnabled = _danmakuState.value.blockRepeatEnabled
                        val (items, keptCount) =
                            withContext(filterDispatcher) {
                                val filteredList = dataList.filterForDisplay()
                                filteredList.toDanmakuItems(mergeEnabled) to filteredList.size
                            }
                        if (generation != loadGeneration) return
                        // akdanmaku 内部维护有序数据并懒排序，无需预先排序
                        danmakuPlayer?.updateData(items)
                        loadedSegments.add(segmentIndex)
                        logger.info {
                            "Load danmaku segment $segmentIndex: raw=${dataList.size}, kept=$keptCount"
                        }
                        return
                    } catch (e: TimeoutCancellationException) {
                        if (attempt == SEGMENT_MAX_ATTEMPTS - 1) {
                            logger.warn {
                                "Load danmaku segment $segmentIndex timeout after $SEGMENT_MAX_ATTEMPTS attempts"
                            }
                            return
                        }
                        delay(RETRY_BACKOFF_BASE_MS shl attempt)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (attempt == SEGMENT_MAX_ATTEMPTS - 1) {
                            logger.warn {
                                "Load danmaku segment $segmentIndex failed after $SEGMENT_MAX_ATTEMPTS attempts: $e"
                            }
                            return
                        }
                        delay(RETRY_BACKOFF_BASE_MS shl attempt)
                    }
                }
            } finally {
                // generation 已变说明重载/切集，同段标记属于新请求，不能误删
                if (generation == loadGeneration) loadingSegments.remove(segmentIndex)
            }
        }

        private fun DanmakuData.toDanmakuItemData(content: String = text): DanmakuItemData =
            DanmakuItemData(
                danmakuId = dmid,
                position = (time * 1000).toLong(),
                content = content,
                mode =
                    when (type) {
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                textSize = size,
                textColor = color,
            )

        /**
         * 将过滤后的分段数据转换为渲染数据。
         *
         * [mergeEnabled] 开启时，把同一文本在 [MERGE_WINDOW_SECONDS] 窗口内的多条弹幕
         * 合并为一条「文本 ×N」，其余同组条目直接丢弃；关闭时原样保留每一条。
         */
        internal fun List<DanmakuData>.toDanmakuItems(mergeEnabled: Boolean): List<DanmakuItemData> {
            if (!mergeEnabled) return map { it.toDanmakuItemData() }
            if (isEmpty()) return emptyList()

            val result = ArrayList<DanmakuItemData>(size)
            for (group in groupBy { it.text }.values) {
                if (group.size == 1) {
                    result += group[0].toDanmakuItemData()
                    continue
                }
                val sorted = group.sortedBy { it.time }
                var i = 0
                while (i < sorted.size) {
                    val baseTime = sorted[i].time
                    var j = i
                    while (j < sorted.size && sorted[j].time - baseTime <= MERGE_WINDOW_SECONDS) j++
                    val windowSize = j - i
                    result +=
                        if (windowSize == 1) {
                            sorted[i].toDanmakuItemData()
                        } else {
                            sorted[i].toDanmakuItemData(content = "${sorted[i].text} ×$windowSize")
                        }
                    i = j
                }
            }
            return result
        }

        /** 按当前设置过滤分段数据（进引擎前的加载级过滤）。 */
        private fun List<DanmakuData>.filterForDisplay(): List<DanmakuData> {
            val state = _danmakuState.value
            val kept = ArrayList<DanmakuData>(size)
            for (item in this) {
                // 高级(7)/代码(8)/BAS(9) 弹幕引擎无渲染器，落到滚动等于把原始 JSON/JS/BAS 当文本滚屏，直接丢弃
                if (item.type in SPECIAL_DANMAKU_MODES) continue
                if (danmakuFilterRepository.shouldDrop(midHash = item.midHash, content = item.text)) continue
                if (state.aiCloudLevel > 0 && item.shouldDropByAi(state.aiCloudLevel)) continue
                kept += item
            }
            return kept
        }

        /**
         * [aiLevel] 直接作为丢弃阈值（评分低于该值即过滤）。
         *
         * Web `seg.so` 实测不下发 ai_flag（[DanmakuData.aiFlagScore] 恒为 0），
         * 实际依据是 weight（[DanmakuData.level] 实测 1-11）；两者非 0 时取绝对值较大者。
         */
        internal fun DanmakuData.shouldDropByAi(aiLevel: Int): Boolean {
            val score =
                sequenceOf(level, aiFlagScore)
                    .filter { it != 0 }
                    .maxOfOrNull { abs(it) }
                    ?: return false
            return score < aiLevel
        }

        /** 加载当前视频的防遮挡蒙版；关闭状态下不请求，开关打开后由 [updateDanmakuState] 补加载。 */
        fun loadDanmakuMask() {
            maskFetchJob?.cancel()
            maskFetchJob = null
            val aid = currentAid
            val cid = currentCid
            if (cid <= 0L) {
                logger.info { "No current video, skip loading danmaku mask" }
                return
            }
            if (!_danmakuState.value.maskEnabled) {
                logger.info { "Danmaku mask disabled, skip loading (aid=$aid, cid=$cid)" }
                return
            }
            maskFetchJob =
                viewModelScope.launch {
                    try {
                        val mask =
                            withTimeout(LOAD_TIMEOUT_MS) {
                                videoPlayRepository.getDanmakuMask(
                                    aid = aid,
                                    cid = cid,
                                    preferApiType = if (Prefs.apiType == DataApiType.App) ApiType.App else ApiType.Web,
                                )
                            }
                        _danmakuMask.update { mask }
                        logger.info { "Load danmaku mask segments: ${mask?.segmentCount ?: 0}" }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logger.warn { "Load danmaku mask failed: $e" }
                    }
                }
        }

        /** 播放弹幕（与视频播放同步）。 */
        fun play() {
            danmakuRunning = true
            danmakuPlayer?.start()
        }

        /** 暂停弹幕。 */
        fun pause() {
            danmakuRunning = false
            danmakuPlayer?.pause()
        }

        /**
         * 更新弹幕播放速度，与视频播放速度保持一致。
         *
         * 由 UI 层随播放器的 `playSpeed` 状态驱动：起播时的默认倍速与后续手动切换
         * 都经此应用到引擎（引擎时钟按倍速缩放，避免与视频不同速导致弹幕乱跳）。
         */
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
         * 应用弹幕设置动作。
         *
         * 状态映射由 [applyTo] 负责，副作用按动作类型分发：
         * - 引擎级过滤（类型/彩色/合并重复）：只更新对应 filter，不重下分段
         * - 加载级过滤（账号规则/AI 等级）：重载当前分段，才能作用到已加载弹幕
         * - 显示参数与蒙版：更新引擎配置或按需加载资源
         */
        fun updateDanmakuState(action: DanmakuSettingAction) {
            val old = _danmakuState.value
            val new = action.applyTo(old)
            if (new == old) return

            _danmakuState.update { new }

            when (action) {
                is DanmakuSettingAction.SetEnabledTypes -> {
                    updateDanmakuConfigTypeFilter(new.enabledTypes)
                    applyDataFilters(new)
                    Prefs.defaultDanmakuTypes = new.enabledTypes.map { it.toDataDanmakuType() }
                    logger.info { "Danmaku enabled types updated: ${new.enabledTypes}" }
                }

                is DanmakuSettingAction.SetBlockColorfulEnabled -> {
                    updateDanmakuConfigColorFilter(new.blockColorfulEnabled)
                    applyDataFilters(new)
                    Prefs.defaultDanmakuBlockColorfulEnabled = new.blockColorfulEnabled
                    logger.info { "Danmaku blockColorfulEnabled updated: ${new.blockColorfulEnabled}" }
                }

                is DanmakuSettingAction.SetBlockRepeatEnabled -> {
                    Prefs.defaultDanmakuBlockRepeatEnabled = new.blockRepeatEnabled
                    reloadCurrentSegments()
                    logger.info { "Danmaku blockRepeatEnabled updated: ${new.blockRepeatEnabled}" }
                }

                is DanmakuSettingAction.SetBlockByUserFilterEnabled -> {
                    Prefs.defaultDanmakuBlockByUserFilterEnabled = new.blockByUserFilterEnabled
                    reloadCurrentSegments()
                }

                is DanmakuSettingAction.SetAiCloudLevel -> {
                    Prefs.defaultDanmakuAiCloudLevel = new.aiCloudLevel
                    reloadCurrentSegments()
                }

                is DanmakuSettingAction.SetScale -> {
                    updateDanmakuScale(new.scale)
                    Prefs.defaultDanmakuScale = new.scale
                }

                is DanmakuSettingAction.SetSpeedFactor -> {
                    danmakuPlayer?.setDanmakuRollingSpeed(new.speedFactor)
                    Prefs.defaultDanmakuSpeedFactor = new.speedFactor
                }

                is DanmakuSettingAction.SetArea -> {
                    updateDanmakuArea(new.area)
                    Prefs.defaultDanmakuArea = new.area
                }

                is DanmakuSettingAction.SetOpacity -> Prefs.defaultDanmakuOpacity = new.opacity

                is DanmakuSettingAction.SetMaskEnabled -> {
                    Prefs.defaultDanmakuMask = new.maskEnabled
                    // 蒙版只在开启时请求：关闭期间不下载，打开后若当前视频尚未加载则补加载
                    if (new.maskEnabled && _danmakuMask.value == null && currentCid > 0L) {
                        loadDanmakuMask()
                    }
                }
            }
        }

        /** 将设置动作映射为新的弹幕状态（纯函数，无副作用）。 */
        private fun DanmakuSettingAction.applyTo(state: DanmakuState): DanmakuState =
            when (this) {
                is DanmakuSettingAction.SetScale -> state.copy(scale = scale)
                is DanmakuSettingAction.SetOpacity -> state.copy(opacity = opacity)
                is DanmakuSettingAction.SetArea -> state.copy(area = area)
                is DanmakuSettingAction.SetSpeedFactor -> state.copy(speedFactor = factor)
                is DanmakuSettingAction.SetMaskEnabled -> state.copy(maskEnabled = enabled)
                is DanmakuSettingAction.SetEnabledTypes -> state.copy(enabledTypes = types)
                is DanmakuSettingAction.SetBlockColorfulEnabled -> state.copy(blockColorfulEnabled = enabled)
                is DanmakuSettingAction.SetBlockRepeatEnabled -> state.copy(blockRepeatEnabled = enabled)
                is DanmakuSettingAction.SetBlockByUserFilterEnabled -> state.copy(blockByUserFilterEnabled = enabled)
                is DanmakuSettingAction.SetAiCloudLevel -> state.copy(aiCloudLevel = level)
            }

        /** 初始化构建引擎配置：只在 [init] 时调用一次。 */
        private fun initDanmakuConfig() {
            val state = _danmakuState.value
            updateDanmakuConfigTypeFilter(state.enabledTypes)
            updateDanmakuConfigColorFilter(state.blockColorfulEnabled)
            danmakuConfig =
                danmakuConfig.copy(
                    density = 120,
                    textSizeScale = state.scale,
                    screenPart = state.area,
                    rollingSpeedFactor = state.speedFactor,
                )
            applyDataFilters(state)
            logger.info {
                "Danmaku filter chain initialized: types=${state.enabledTypes}, " +
                    "colorful=${state.blockColorfulEnabled}, repeat=${state.blockRepeatEnabled}, " +
                    "aiCloudLevel=${state.aiCloudLevel}"
            }
        }

        /** 只重建类型过滤器；启用类型变化时调用。 */
        private fun updateDanmakuConfigTypeFilter(types: List<DanmakuEntityDanmakuType>) {
            danmakuTypeFilter.clear()
            if (!types.contains(DanmakuEntityDanmakuType.All)) {
                val allTypes = DanmakuEntityDanmakuType.entries.toMutableList()
                allTypes.remove(DanmakuEntityDanmakuType.All)
                allTypes.removeAll(types)
                allTypes
                    .mapNotNull {
                        when (it) {
                            DanmakuEntityDanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                            DanmakuEntityDanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                            DanmakuEntityDanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                            else -> null
                        }
                    }.forEach { danmakuTypeFilter.addFilterItem(it) }
            }
        }

        /** 只更新彩色弹幕过滤器；开关变化时调用。 */
        private fun updateDanmakuConfigColorFilter(enabled: Boolean) {
            danmakuTextColorFilter.filterColor.clear()
            if (enabled) {
                danmakuTextColorFilter.filterColor.add(0xFFFFFF)
            }
        }

        /**
         * 把当前已注册的 filter 应用为 [DanmakuConfig.dataFilter] 并通知引擎重新过滤。
         *
         * 只重组列表、不重算各 filter 内容；初始化与运行时切换共用，避免出现「rebuild」歧义。
         */
        private fun applyDataFilters(state: DanmakuState) {
            danmakuConfig =
                danmakuConfig.copy(
                    dataFilter =
                        buildList {
                            add(danmakuTypeFilter)
                            if (state.blockColorfulEnabled) add(danmakuTextColorFilter)
                        },
                )
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

        companion object {
            private const val LOAD_TIMEOUT_MS = 10_000L

            /**
             * 引擎时钟与视频位置的对齐阈值（毫秒）。
             *
             * 下限须高于 10Hz 进度喂入的正常采样抖动（约 200-300ms），
             * 避免正常播放中频繁 seek 引擎；上限远低于用户可感知的弹幕错位。
             */
            private const val ENGINE_TIME_JUMP_THRESHOLD_MS = 500L

            /** 分段请求超时（毫秒）。 */
            private const val SEGMENT_FETCH_TIMEOUT_MS = 10_000L

            /** dm/view 元数据不可用时的默认分段大小：6 分钟。 */
            private const val DEFAULT_SEGMENT_SIZE_MS = 360_000L

            /** 预取目标段之后的分段数。 */
            private const val DEFAULT_SEGMENT_PREFETCH_AHEAD = 1

            /** 单段最大尝试次数（首次 + 重试）。 */
            private const val SEGMENT_MAX_ATTEMPTS = 3

            /** 重试退避基数（毫秒），按指数递增：1s、2s。 */
            private const val RETRY_BACKOFF_BASE_MS = 1_000L

            /**
             * 高级(7)/代码(8)/BAS(9) 弹幕，引擎无对应渲染器。
             *
             * 不做成用户开关：这些弹幕一旦按滚动文本渲染就是原始 JSON/JS/BAS 源码，
             * 没有「保留」的合理场景，直接丢弃。
             */
            private val SPECIAL_DANMAKU_MODES = setOf(7, 8, 9)

            /** 重复弹幕合并的时间窗（秒），窗口内相同文本的弹幕合并为一条。 */
            private const val MERGE_WINDOW_SECONDS = 5f
        }
    }
