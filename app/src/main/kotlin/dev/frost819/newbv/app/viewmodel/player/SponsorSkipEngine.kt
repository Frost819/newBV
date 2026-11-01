package dev.frost819.newbv.app.viewmodel.player

import dev.frost819.newbv.app.entity.player.SponsorActionType
import dev.frost819.newbv.app.entity.player.SponsorCategories
import dev.frost819.newbv.app.entity.player.SponsorSegment

/**
 * 空降片段跳过状态机。
 *
 * 纯 Kotlin 实现（无 Android 依赖），由 [PlayerViewModel] 在播放进度更新时驱动。
 *
 * 核心语义：
 * - 只在**向前播放跨入片段起点**时自动触发（[SkipAction.AUTO_SKIP] / [SkipAction.MUTE]），
 *   起播即落在片段内或回退进入片段内不自动触发（返回 [SkipAction.SHOW_BUTTON]，由调用方决定提示）；
 * - 每个片段只消费一次（UUID 记账）；
 * - 自动跳过后的 3 秒宽限期内不检测回拉，避免 seek 落点回传被误判为用户回退；
 * - 用户回拉超过 2 秒视为重新观看，清除尚未越过片段的消费记录（已越过的保留）。
 *
 * 移植自 MyTVB `SponsorBlockUseCase`，去掉未启用的手动跳过按钮分支后收敛为只读决策器。
 */
class SponsorSkipEngine {
    /** 检测结果动作。 */
    enum class SkipAction {
        /** 自动跳过：seek 到片段终点。 */
        AUTO_SKIP,

        /** 自动静音：片段内静音，越过终点后恢复。 */
        MUTE,

        /** 非跨入命中（起播即在片段内/回退进入）：当前版本不自动处理。 */
        SHOW_BUTTON,
    }

    /**
     * 单次检测结果。
     *
     * @property segment 命中的片段
     * @property action 建议动作
     */
    data class SkipResult(
        val segment: SponsorSegment,
        val action: SkipAction,
    )

    private var segments: List<SponsorSegment> = emptyList()
    private var nextSegmentIndex: Int = 0
    private val skippedIds = mutableSetOf<String>()
    private var lastPositionMs: Long = 0
    private var lastAutoSkipTime: Long = 0

    /**
     * 装载已归一化的片段（替换旧数据并复位状态）。
     *
     * @param rawSegments 仓库归一化产物；内部再按起点排序兜底
     */
    fun loadSegments(rawSegments: List<SponsorSegment>) {
        reset()
        segments = rawSegments.sortedBy { it.startMs }
    }

    /**
     * 检测当前位置是否命中片段并给出动作。
     *
     * @param positionMs 当前播放位置（毫秒）
     * @param nowMs 当前时钟（毫秒），默认取系统时间；测试注入虚拟时间用
     * @return 命中结果；未命中任何片段返回 null
     */
    fun checkPosition(
        positionMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): SkipResult? {
        if (segments.isEmpty()) return null

        val isGracePeriod = nowMs - lastAutoSkipTime < GRACE_PERIOD_MS
        val prevMs = lastPositionMs

        if (!isGracePeriod) {
            if (positionMs < lastPositionMs - SEEK_BACK_THRESHOLD_MS) {
                resetSkippedForSeek(positionMs)
            }
            lastPositionMs = positionMs
        } else {
            // 宽限期内只允许位置前进：跳过落点附近的微小回传不触发回拉重置
            if (positionMs > lastPositionMs) {
                lastPositionMs = positionMs
            }
        }

        while (nextSegmentIndex < segments.size) {
            val candidate = segments[nextSegmentIndex]
            if (candidate.uuid in skippedIds || positionMs > candidate.endMs) {
                nextSegmentIndex++
                continue
            }
            break
        }

        val segment =
            segments
                .getOrNull(nextSegmentIndex)
                ?.takeIf { positionMs in it.startMs..it.endMs }
                ?: return null

        // 只在向前播放跨入片段起点时自动处理，回退进片段内不触发
        val justEntered = !isGracePeriod && prevMs <= segment.startMs && positionMs > segment.startMs

        val action =
            when {
                justEntered && segment.actionType == SponsorActionType.MUTE -> {
                    skippedIds.add(segment.uuid)
                    nextSegmentIndex++
                    // 静音不 seek、无位置 glitch，无需进入宽限期
                    SkipAction.MUTE
                }
                justEntered -> {
                    skippedIds.add(segment.uuid)
                    lastAutoSkipTime = nowMs
                    nextSegmentIndex++
                    SkipAction.AUTO_SKIP
                }
                else -> SkipAction.SHOW_BUTTON
            }

        return SkipResult(segment, action)
    }

    /**
     * 用户主动 seek：重定位游标、清除尚未越过片段的消费记录（允许拖回片段内重播）。
     *
     * @param positionMs seek 目标位置（毫秒）
     */
    fun onUserSeek(positionMs: Long) {
        resetSkippedForSeek(positionMs)
        nextSegmentIndex =
            segments.indexOfFirst { positionMs <= it.endMs }.takeIf { it >= 0 } ?: segments.size
        lastPositionMs = positionMs
        lastAutoSkipTime = 0
    }

    /** 清空全部状态（切集/停用）。 */
    fun reset() {
        segments = emptyList()
        nextSegmentIndex = 0
        skippedIds.clear()
        lastPositionMs = 0
        lastAutoSkipTime = 0
    }

    /**
     * 回拉 seek 后清除消费记录：仅保留终点已在目标位置之前的片段
     * （已越过的片段不重复触发），待播片段重新武装。
     */
    private fun resetSkippedForSeek(positionMs: Long) {
        val keep =
            skippedIds.filterTo(mutableSetOf()) { id ->
                segments.firstOrNull { it.uuid == id }?.let { positionMs > it.endMs } ?: true
            }
        skippedIds.clear()
        skippedIds.addAll(keep)
    }

    private companion object {
        /** 自动跳过后的宽限期：期内不检测回拉，防 seek 落点回传误判。 */
        const val GRACE_PERIOD_MS = 3_000L

        /** 回拉超过该阈值视为用户主动回看，清除待播片段的消费记录。 */
        const val SEEK_BACK_THRESHOLD_MS = 2_000L
    }
}

/**
 * 空降跳转目标位置钳制。
 *
 * 自动跳过 seek 到片段尾时，防止目标贴着视频总时长导致 Media3 直接进入
 * STATE_ENDED（画面定格/提前触发播放结束）。片尾类片段顶到末尾是期望行为
 * （自然播完触发 ENDED）。
 */
object SponsorSkipTargetPolicy {
    private const val TAIL_GUARD_MS = 1_000L

    /**
     * 计算自动跳过的实际目标位置。
     *
     * @param requestedMs 片段终点（毫秒）
     * @param durationMs 视频总时长（毫秒）；<= 0 时原样返回（时长未就绪）
     * @param category 片段分类（outro 类允许顶到片尾）
     * @return 钳制后的目标位置（毫秒）
     */
    fun resolveSkipTargetPositionMs(
        requestedMs: Long,
        durationMs: Long,
        category: String,
    ): Long {
        val requested = requestedMs.coerceAtLeast(0L)
        if (durationMs <= 0L) return requested
        return if (category == SponsorCategories.OUTRO) {
            requested.coerceIn(0L, durationMs)
        } else {
            requested.coerceIn(0L, (durationMs - TAIL_GUARD_MS).coerceAtLeast(0L))
        }
    }
}
