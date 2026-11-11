package dev.frost819.newbv.app.ui.component.videocard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 给 B 站封面地址追加缩略图参数。
 *
 * 封面原图常常上千像素，而卡片只有几百像素宽。请求缩略图能明显减少
 * 焦点移动、新卡片进入视口时的下载量与解码开销。
 *
 * 已经带 `@` 参数（接口返回的裁切图）或非 http 地址（本地占位）时原样返回，
 * 避免重复拼接出非法 URL。
 *
 * @param height 目标高度（B 站参数顺序是 `高度h_宽度w`）
 * @param width 目标宽度
 */
fun coverThumbnailUrl(cover: String, height: Int, width: Int): String {
    if (!cover.startsWith("http", ignoreCase = true) || cover.contains('@')) return cover
    return "$cover@${height}h_${width}w_1c.webp"
}

/** [coverThumbnailUrl] 的 Compose 版本，按封面地址缓存结果。 */
@Composable
fun rememberCoverThumbnail(cover: String, height: Int, width: Int): String =
    remember(cover, height, width) { coverThumbnailUrl(cover, height, width) }
