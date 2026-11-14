package dev.frost819.newbv.app.entity.player

/**
 * 播放顺序处理。
 *
 * 播放列表（合集 / 番剧分集）与每个条目内部的若干 P 都由接口原序给出：
 * 第 1 集在前、更新的一集在后。倒序播放要把两层一起翻——只翻列表不翻分 P，
 * 单个视频内部的几个 P 仍会正着走，观感上是「倒序里夹着正序」。
 *
 * 顺序只在这里决定一次，下游（分集面板渲染、上下集查找、自动连播）
 * 全部按索引取，避免三处各写一遍翻转逻辑后彼此不一致。
 *
 * @param reverse 是否倒序播放
 * @return 按播放顺序排列的新列表，不修改原列表
 */
fun List<VideoListItem>.inPlayOrder(reverse: Boolean): List<VideoListItem> {
    if (!reverse) return this
    return asReversed().map { item -> item.copy(ugcPages = item.ugcPages?.asReversed()) }
}
