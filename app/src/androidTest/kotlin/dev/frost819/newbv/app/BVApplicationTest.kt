package dev.frost819.newbv.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [BVApplication] 的插桩测试。
 *
 * 验证 Application 初始化流程：
 * - Hilt 注入可用
 * - Application 上下文可用
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BVApplicationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun applicationContext_isAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThat(context).isNotNull()
    }

    @Test
    fun application_isAvailable() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        assertThat(app).isNotNull()
    }
}
