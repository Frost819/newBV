package dev.frost819.newbv.danmaku.util

import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskFrame
import dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMaskSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 弹幕蒙版帧查找器。
 *
 * 根据当前播放位置查找对应的 [DanmakuMaskFrame]。
 * 内部按需解压蒙版数据（只解压当前 segment），避免一次性加载全部帧。
 *
 * **使用注意**：
 * - 切集或开关切换时调用 [reset] 清除缓存
 * - [findFrame] 是挂起函数，内部解压操作在 [Dispatchers.Default] 执行
 *
 * @see DanmakuMask
 * @see DanmakuMaskFrame
 */
class DanmakuMaskFinder {
    @Volatile
    private var cachedSegment: DanmakuMaskSegment? = null

    /**
     * 清除缓存的 segment。
     * 在切集、开关切换等导致蒙版数据变化时调用。
     */
    fun reset() {
        cachedSegment = null
    }

    /**
     * 根据当前播放时间查找对应帧。
     *
     * @param mask         [DanmakuMask] 蒙版对象（从 [dev.frost819.newbv.biliapi.entity.danmaku.DanmakuMask] 获取）
     * @param currentTime  当前播放时间（毫秒）
     * @return 当前应渲染的 [DanmakuMaskFrame]，无则返回 null
     */
    suspend fun findFrame(
        mask: DanmakuMask,
        currentTime: Long,
    ): DanmakuMaskFrame? {
        val cached = cachedSegment
        if (cached == null || currentTime !in cached.range) {
            withContext(Dispatchers.Default) {
                cachedSegment = mask.getSegmentAt(currentTime)
            }
        }
        return cachedSegment?.frames?.lastOrNull { currentTime in it.range }
    }
}

/**
 * 计算弹幕蒙版轮询的休眠时间。
 *
 * 逻辑：
 * - 有蒙版 + 播放中：休眠到蒙版结束（限制在 20~300ms 以便响应 Seek）
 * - 播放中无蒙版：正常轮询间隔 100ms
 * - 暂停或异常：降低频率 200ms
 *
 * @param currentFrame 当前蒙版帧，null 表示无有效蒙版
 * @param currentTime  当前播放时间（毫秒）
 * @param isPlaying    是否正在播放
 * @return 下次轮询前应休眠的毫秒数
 */
fun calculateMaskDelay(
    currentFrame: DanmakuMaskFrame?,
    currentTime: Long,
    isPlaying: Boolean,
): Long =
    when {
        currentFrame != null && isPlaying -> {
            (currentFrame.range.last - currentTime).coerceIn(20L, 300L)
        }
        isPlaying -> 100L
        else -> 200L
    }
