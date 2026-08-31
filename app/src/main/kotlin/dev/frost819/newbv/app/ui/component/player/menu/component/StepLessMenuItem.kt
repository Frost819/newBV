package dev.frost819.newbv.app.ui.component.player.menu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface

/**
 * 无极滑块菜单项（Float 版本）。
 *
 * D-Pad 上/下键调整值，按步进 [step] 递增/递减，范围限制在 [range] 内。
 * 中间显示当前值的格式化文本，上下显示箭头图标。
 *
 * @param modifier 修饰符
 * @param value 当前值
 * @param text 格式化的显示文本
 * @param step 步进值
 * @param range 取值范围
 * @param onValueChange 值变化回调
 * @param onFocusBackToParent 返回父级回调
 */
@Composable
fun StepLessMenuItem(
    modifier: Modifier = Modifier,
    value: Float = 1f,
    text: String,
    step: Float = 0.01f,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    onFocusBackToParent: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (it.key == Key.DirectionRight) {
                        onFocusBackToParent()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = {
                    if (value >= range.endInclusive - step) {
                        onValueChange(range.endInclusive)
                    } else {
                        onValueChange(value + step)
                    }
                },
            ) {
                Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = "增加")
            }
            MenuListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            when (it.key) {
                                Key.DirectionUp -> {
                                    if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                    if (value >= range.endInclusive - step) {
                                        onValueChange(range.endInclusive)
                                    } else {
                                        onValueChange(value + step)
                                    }
                                    true
                                }

                                Key.DirectionDown -> {
                                    if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                    if (value - step <= range.start) {
                                        onValueChange(range.start)
                                    } else {
                                        onValueChange(value - step)
                                    }
                                    true
                                }

                                else -> false
                            }
                        },
                text = text,
                selected = false,
                onClick = {},
            )
            Surface(
                onClick = {
                    if (value - step <= range.start) {
                        onValueChange(range.start)
                    } else {
                        onValueChange(value - step)
                    }
                },
            ) {
                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = "减少")
            }
        }
    }
}

/**
 * 无极滑块菜单项（Int 版本）。
 *
 * @param modifier 修饰符
 * @param value 当前值
 * @param text 格式化的显示文本
 * @param step 步进值
 * @param range 取值范围
 * @param onValueChange 值变化回调
 * @param onFocusBackToParent 返回父级回调
 */
@Composable
fun StepLessMenuItem(
    modifier: Modifier = Modifier,
    value: Int = 100,
    text: String,
    step: Int = 1,
    range: IntRange = 0..100,
    onValueChange: (Int) -> Unit,
    onFocusBackToParent: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                    if (it.key == Key.DirectionRight) {
                        onFocusBackToParent()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = {
                    if (value >= range.last - step) {
                        onValueChange(range.last)
                    } else {
                        onValueChange(value + step)
                    }
                },
            ) {
                Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = "增加")
            }
            MenuListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            when (it.key) {
                                Key.DirectionUp -> {
                                    if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                    if (value >= range.last - step) {
                                        onValueChange(range.last)
                                    } else {
                                        onValueChange(value + step)
                                    }
                                    true
                                }

                                Key.DirectionDown -> {
                                    if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                    if (value - step <= range.first) {
                                        onValueChange(range.first)
                                    } else {
                                        onValueChange(value - step)
                                    }
                                    true
                                }

                                else -> false
                            }
                        },
                text = text,
                selected = false,
                onClick = {},
            )
            Surface(
                onClick = {
                    if (value - step <= range.first) {
                        onValueChange(range.first)
                    } else {
                        onValueChange(value - step)
                    }
                },
            ) {
                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = "减少")
            }
        }
    }
}
