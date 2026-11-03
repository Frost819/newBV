package dev.frost819.newbv.player.impl.exo

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

/**
 * 播放器缓冲策略。
 *
 * ExoPlayer 默认把最多 50 秒的媒体样本常驻 Java 堆（`SampleQueue` 内的 ByteBuffer），
 * 字节阈值按轨道估算约 137MiB（视频 125MiB + 音频 12.5MiB）。堆上限只有 192MB 的电视
 * 几乎等于不设限：GC 持续抖动、掉帧，还挤占解码所需的缓冲。
 *
 * 策略只有两个变量：
 * - **字节预算**随设备堆上限缩放（堆的 1/6，夹在 16~96MiB），是真正的内存闸门；
 * - **时间上限**按堆规模分三档（20s / 30s / 40s），其余时间参数固定不变
 *   （起播 1s、卡顿恢复 3s、常驻目标 5s）。
 *
 * 两者取先到者：[DefaultLoadControl.Builder.setPrioritizeTimeOverSizeThresholds] 置 `false`，
 * 保证字节预算生效（否则时间窗未满足时会继续加载、继续占内存）。
 * 直播同样走该策略：缓冲越小越贴近直播边缘，也降低 `BEHIND_LIVE_WINDOW` 概率。
 */
@OptIn(UnstableApi::class)
internal object PlayerLoadControl {
    private const val MIB = 1024 * 1024

    /** 字节预算下限（16MiB）：极小堆设备也不能更低，否则连起播缓冲都不够。 */
    private const val MIN_TARGET_BUFFER_BYTES = 32L * MIB

    /** 字节预算上限（96MiB）：电视场景再大也没有收益，避免无谓占用。 */
    private const val MAX_TARGET_BUFFER_BYTES = 96L * MIB

    /** 堆上限（MB）→ 缓冲时间上限（毫秒），从大到小匹配，最后一档兜底。 */
    private val MAX_BUFFER_TIERS =
        listOf(
            512 to 60_000,
            256 to 60_000,
            0 to 60_000,
        )

    /**
     * 按堆上限推导缓冲参数。
     *
     * @param heapLimitBytes 进程最大堆字节数，通常取 `Runtime.getRuntime().maxMemory()`。
     */
    fun policyFor(heapLimitBytes: Long): PlayerBufferPolicy =
        PlayerBufferPolicy(
            // 字节预算：堆的 1/6，夹在上下限之间；余下 5/6 留给图片、弹幕、Compose 等
            targetBufferBytes =
                (heapLimitBytes / 6)
                    .coerceIn(MIN_TARGET_BUFFER_BYTES, MAX_TARGET_BUFFER_BYTES)
                    .toInt(),
            maxBufferMs = MAX_BUFFER_TIERS.first { heapLimitBytes / MIB >= it.first }.second,
        )

    /**
     * 按当前进程堆上限创建缓冲策略实例。
     *
     * @param heapLimitBytes 进程最大堆字节数，默认取当前进程的实际值。
     */
    fun create(heapLimitBytes: Long = Runtime.getRuntime().maxMemory()): DefaultLoadControl {
        val policy = policyFor(heapLimitBytes)
        return DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                policy.minBufferMs,
                policy.maxBufferMs,
                policy.bufferForPlaybackMs,
                policy.bufferForPlaybackAfterRebufferMs,
            ).setTargetBufferBytes(policy.targetBufferBytes)
            .setPrioritizeTimeOverSizeThresholds(false)
            // 不保留回看缓冲：4K 关键帧体积大，留在堆里不划算
            .setBackBuffer(0, false)
            .build()
    }
}

/**
 * 播放器缓冲参数。
 *
 * 只有 [targetBufferBytes] 与 [maxBufferMs] 随设备堆规模变化，其余时间参数与设备无关。
 *
 * @property targetBufferBytes 分配器可占用的字节上限（视频 + 音频）。
 * @property maxBufferMs 缓冲上限时长（毫秒），达到后停止加载。
 * @property minBufferMs 常驻缓冲目标时长（毫秒）：不足该值时继续加载。
 * @property bufferForPlaybackMs 起播 / seek 后开始播放所需的最小时长（毫秒）。
 * @property bufferForPlaybackAfterRebufferMs 卡顿恢复后重新起播所需的最小时长（毫秒）。
 */
internal data class PlayerBufferPolicy(
    val targetBufferBytes: Int,
    val maxBufferMs: Int,
    val minBufferMs: Int = 5_000,
    val bufferForPlaybackMs: Int = 1_000,
    val bufferForPlaybackAfterRebufferMs: Int = 3_000,
)
