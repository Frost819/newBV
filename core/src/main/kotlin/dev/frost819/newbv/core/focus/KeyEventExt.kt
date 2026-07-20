package dev.frost819.newbv.core.focus

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** 按键事件是否为 KeyDown 类型。 */
fun KeyEvent.isKeyDown(): Boolean = type == KeyEventType.KeyDown

/** 按键事件是否为 KeyUp 类型。 */
fun KeyEvent.isKeyUp(): Boolean = type == KeyEventType.KeyUp

/** 按键是否为 D-Pad 上方向键。 */
fun KeyEvent.isDpadUp(): Boolean = key == Key.DirectionUp

/** 按键是否为 D-Pad 下方向键。 */
fun KeyEvent.isDpadDown(): Boolean = key == Key.DirectionDown

/** 按键是否为 D-Pad 左方向键。 */
fun KeyEvent.isDpadLeft(): Boolean = key == Key.DirectionLeft

/** 按键是否为 D-Pad 右方向键。 */
fun KeyEvent.isDpadRight(): Boolean = key == Key.DirectionRight

/** 按键是否为确认键（D-Pad 中心 / Enter）。 */
fun KeyEvent.isConfirm(): Boolean = key == Key.DirectionCenter || key == Key.Enter

/** 按键是否为返回键。 */
fun KeyEvent.isBack(): Boolean = key == Key.Back
