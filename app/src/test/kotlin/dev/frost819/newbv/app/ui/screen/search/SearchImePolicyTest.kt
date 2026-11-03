package dev.frost819.newbv.app.ui.screen.search

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [SearchImePolicy] 的单元测试。
 *
 * 关键约束：未启用系统输入法时**不能**碰系统 IME（否则自绘软键盘与系统键盘会打架）；
 * 启用后焦点进入搜索框必须唤起、离开必须收起。
 */
class SearchImePolicyTest {
    @Test
    fun `does nothing when system ime is disabled`() {
        // Then: 关闭时无论焦点如何都不干预系统输入法
        assertThat(SearchImePolicy.onFocusChanged(useSystemIme = false, isFocused = true))
            .isEqualTo(SearchImeAction.None)
        assertThat(SearchImePolicy.onFocusChanged(useSystemIme = false, isFocused = false))
            .isEqualTo(SearchImeAction.None)
    }

    @Test
    fun `shows ime when search field gains focus`() {
        // Then: 进入搜索框立即唤起系统输入法
        assertThat(SearchImePolicy.onFocusChanged(useSystemIme = true, isFocused = true))
            .isEqualTo(SearchImeAction.Show)
    }

    @Test
    fun `hides ime when search field loses focus`() {
        // Then: 焦点移到热搜/历史列表时收起，避免挡住内容
        assertThat(SearchImePolicy.onFocusChanged(useSystemIme = true, isFocused = false))
            .isEqualTo(SearchImeAction.Hide)
    }
}
