package dev.frost819.newbv.app.entity.player.shortcut

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [findShortcut] 的单元测试。
 *
 * 播放器根节点与播放器内弹窗共用该查找逻辑，需保证：命中绑定、过滤禁用键、未绑定返回 null。
 */
class PlayerShortcutBindingTest {
    private val openCommentsKey = KeyEvent.KEYCODE_F1
    private val openComments =
        PlayerCustomShortcut(
            keyCode = openCommentsKey,
            action = PlayerCustomShortcutAction.OpenComments,
        )
    private val bindings = mapOf(openCommentsKey to openComments)

    @Test
    fun `returns binding for bound key`() {
        val result = findShortcut(openCommentsKey, bindings)

        assertThat(result).isEqualTo(openComments)
    }

    @Test
    fun `returns null for unbound key`() {
        val result = findShortcut(KeyEvent.KEYCODE_F2, bindings)

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for forbidden key even when bound`() {
        val backBindings =
            mapOf(
                KeyEvent.KEYCODE_BACK to
                    PlayerCustomShortcut(KeyEvent.KEYCODE_BACK, PlayerCustomShortcutAction.OpenComments),
            )

        val result = findShortcut(KeyEvent.KEYCODE_BACK, backBindings)

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for empty bindings`() {
        val result = findShortcut(openCommentsKey, emptyMap())

        assertThat(result).isNull()
    }
}
