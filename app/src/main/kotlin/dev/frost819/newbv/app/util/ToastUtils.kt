package dev.frost819.newbv.app.util

import android.content.Context
import android.widget.Toast

/**
 * 统一 Toast 工具。
 *
 * 全局通过此对象调用 Toast，避免各处直接使用 [Toast.makeText] 导致风格不一致。
 *
 * 典型用法：
 * ```kotlin
 * ToastUtils.show(context, "操作失败")
 * ```
 */
object ToastUtils {
    /**
     * 显示短时间 Toast。
     *
     * @param context 任意 [Context]。
     * @param message 要显示的消息。
     */
    fun show(
        context: Context,
        message: String,
    ) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
