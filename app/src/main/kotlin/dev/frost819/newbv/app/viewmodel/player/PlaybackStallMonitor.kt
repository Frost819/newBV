package dev.frost819.newbv.app.viewmodel.player

/**
 * 单次卡顿持续多久仍未恢复就换线（毫秒）。
 *
 * 卡顿恢复只需缓冲 3 秒媒体（见 `PlayerBufferPolicy.bufferForPlaybackAfterRebufferMs`），
 * 正常线路 1–2 秒内即可补齐；8 秒还补不齐说明该线路实际吞吐已不足码率的一半。
 */
internal const val STALL_SWITCH_TIMEOUT_MS = 8_000L

/** 统计「短时间内反复卡顿」的滑动窗口（毫秒）。 */
internal const val REPEATED_STALL_WINDOW_MS = 60_000L

/** 窗口内累计多少次播放中卡顿即换线。 */
internal const val REPEATED_STALL_THRESHOLD = 3

/** 拖动进度后多久内出现的缓冲视为拖动引起、不计入反复卡顿（毫秒）。 */
internal const val SEEK_GRACE_MS = 3_000L

/** 每次播放（一组 CDN 候选）最多因卡顿换线的次数。 */
internal const val MAX_STALL_SWITCHES = 2

/**
 * 点播「已起播后卡顿」的换线判定。
 *
 * 现有 CDN 回退只在播放器报网络错误时触发；但线路吞吐不足时播放器只会反复进入缓冲、
 * 并不报错，用户只能干等。本类根据缓冲事件判断是否该换到下一个候选 CDN：
 * - 起播缓冲（[onBufferingStarted] 的 `wasPlaying = false`）不计，起播慢由测速排序负责；
 * - 单次卡顿超过 [stallTimeoutMs] 仍未恢复 → 换线；
 * - [repeatWindowMs] 内播放中卡顿达到 [repeatThreshold] 次 → 立即换线；
 * - 拖动进度后 [seekGraceMs] 内的缓冲不计入反复卡顿（仍受单次超时约束）；
 * - 每组候选最多换 [maxSwitches] 次，换线后重新计数。
 *
 * 纯逻辑、不持有时钟：调用方传入单调时钟时间，便于单测。
 *
 * @param stallTimeoutMs 单次卡顿换线阈值（毫秒）
 * @param repeatWindowMs 反复卡顿统计窗口（毫秒）
 * @param repeatThreshold 窗口内触发换线的卡顿次数
 * @param seekGraceMs 拖动后不计入反复卡顿的时长（毫秒）
 * @param maxSwitches 每组候选的最大换线次数
 */
internal class PlaybackStallMonitor(
    private val stallTimeoutMs: Long = STALL_SWITCH_TIMEOUT_MS,
    private val repeatWindowMs: Long = REPEATED_STALL_WINDOW_MS,
    private val repeatThreshold: Int = REPEATED_STALL_THRESHOLD,
    private val seekGraceMs: Long = SEEK_GRACE_MS,
    private val maxSwitches: Int = MAX_STALL_SWITCHES,
) {
    /** 窗口内播放中卡顿的开始时间（升序）。 */
    private val stallStarts = ArrayDeque<Long>()

    /** 最近一次拖动进度的时间，null 表示本线路尚未拖动。 */
    private var lastSeekAtMs: Long? = null

    /** 本组候选已因卡顿换线的次数。 */
    private var switchCount = 0

    /** 缓冲开始时的判定结果。 */
    sealed interface Decision {
        /** 不处理（起播缓冲 / 已达换线上限）。 */
        data object Ignore : Decision

        /** 若 [timeoutMs] 后仍在缓冲则换线。 */
        data class SwitchIfStillStalled(
            val timeoutMs: Long,
        ) : Decision

        /** 短时间内反复卡顿，立即换线。 */
        data object SwitchNow : Decision
    }

    /**
     * 记录一次拖动进度。须在调用播放器 seek **之前**调用：ExoPlayer 会在 seek 调用内同步回调缓冲。
     *
     * @param nowMs 当前单调时钟时间（毫秒）
     */
    fun onSeek(nowMs: Long) {
        lastSeekAtMs = nowMs
    }

    /**
     * 播放器进入缓冲时调用，返回换线判定。
     *
     * @param nowMs 当前单调时钟时间（毫秒）
     * @param wasPlaying 进入缓冲前是否处于播放中；起播、换线重播、暂停中拖动都为 false
     * @return 换线判定
     */
    fun onBufferingStarted(
        nowMs: Long,
        wasPlaying: Boolean,
    ): Decision {
        if (!wasPlaying || switchCount >= maxSwitches) return Decision.Ignore

        val causedBySeek = lastSeekAtMs?.let { nowMs - it < seekGraceMs } == true
        if (!causedBySeek) {
            stallStarts.addLast(nowMs)
            while (stallStarts.first() <= nowMs - repeatWindowMs) stallStarts.removeFirst()
            if (stallStarts.size >= repeatThreshold) return Decision.SwitchNow
        }
        return Decision.SwitchIfStillStalled(stallTimeoutMs)
    }

    /** 已因卡顿换到下一条线路：计入换线次数，新线路的卡顿重新计数。 */
    fun onSwitched() {
        switchCount++
        stallStarts.clear()
        lastSeekAtMs = null
    }

    /** 重新解析播放地址（换片 / 换档 / 解码回退）时调用，新的一组候选重新获得换线机会。 */
    fun reset() {
        switchCount = 0
        stallStarts.clear()
        lastSeekAtMs = null
    }
}
