package dev.frost819.newbv.app.ui.component.player

import androidx.compose.ui.Modifier

/**
 * 条件修饰符扩展。
 *
 * 根据条件决定是否应用指定 modifier。
 */
internal fun Modifier.ifElse(
    condition: Boolean,
    modifier: Modifier,
): Modifier = if (condition) this.then(modifier) else this
