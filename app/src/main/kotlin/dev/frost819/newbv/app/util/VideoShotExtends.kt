package dev.frost819.newbv.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import dev.frost819.newbv.biliapi.entity.video.VideoShot
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

/**
 * 从 VideoShot 的 sprite sheet 中提取指定时间点的缩略图帧。
 *
 * 通过二分查找定位最近的帧索引，计算该帧在 sprite sheet 网格中的位置，
 * 返回包含完整 sprite sheet 和子矩形区域的 [SpriteFrame]。
 *
 * @param time 目标时间（秒）
 * @param cache 图片解码 LRU 缓存
 * @return 缩略图帧信息
 */
suspend fun VideoShot.getSpriteFrame(time: Int, cache: VideoShotImageCache): SpriteFrame {
    val index = findClosestValueIndex(times, time.toUShort())
    val singleImgCount = imageCountX * imageCountY
    val imagesIndex = index / singleImgCount
    val imageIndex = index % singleImgCount

    val spriteSheet = cache.getOrDecodeImage(
        imagesIndex,
        images[imagesIndex]!!,
    ).asImageBitmap()

    val cellWidth = spriteSheet.width / imageCountX
    val cellHeight = spriteSheet.height / imageCountY

    val left = (imageIndex % imageCountX) * cellWidth
    val top = (imageIndex / imageCountX) * cellHeight

    return SpriteFrame(
        spriteSheet = spriteSheet,
        srcRect = IntRect(left, top, left + cellWidth, top + cellHeight),
    )
}

/**
 * 二分查找最接近目标值的索引。
 */
private fun findClosestValueIndex(array: List<UShort>, target: UShort): Int {
    var left = 0
    var right = array.size - 1
    while (left < right) {
        val mid = left + (right - left) / 2
        if (array[mid] < target) {
            left = mid + 1
        } else {
            right = mid
        }
    }
    return left
}

/**
 * VideoShot 图片解码 LRU 缓存。
 *
 * 缓存最近解码的 3 张 sprite sheet 位图，避免重复解码。
 * 使用 [ConcurrentHashMap] 去重并发解码任务。
 * 位图使用 RGB_565 配置以节省内存（2 字节/像素）。
 */
class VideoShotImageCache {
    private val memoryCache = LruCache<Int, Bitmap>(3)
    private val activeTasks = ConcurrentHashMap<Int, Deferred<Bitmap>>()

    companion object {
        val bitmapOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
        }
    }

    /**
     * 获取或解码指定索引的 sprite sheet 位图。
     *
     * 如果缓存命中则直接返回，否则异步解码并缓存。
     * 相同索引的并发请求会复用同一个解码任务。
     *
     * @param imagesIndex sprite sheet 索引
     * @param imageData 图片二进制数据
     * @return 解码后的位图
     */
    suspend fun getOrDecodeImage(imagesIndex: Int, imageData: ByteArray): Bitmap = coroutineScope {
        memoryCache.get(imagesIndex)?.let { return@coroutineScope it }

        val task = activeTasks.getOrPut(imagesIndex) {
            async(Dispatchers.IO) {
                val decoded = BitmapFactory.decodeByteArray(
                    imageData, 0, imageData.size, bitmapOptions,
                )
                memoryCache.put(imagesIndex, decoded)
                decoded
            }
        }
        try {
            return@coroutineScope task.await()
        } finally {
            activeTasks.remove(imagesIndex)
        }
    }
}

/**
 * 缩略图帧信息。
 *
 * @property spriteSheet 完整的 sprite sheet 位图
 * @property srcRect 目标帧在 sprite sheet 中的矩形区域
 */
data class SpriteFrame(
    val spriteSheet: ImageBitmap,
    val srcRect: IntRect,
)
