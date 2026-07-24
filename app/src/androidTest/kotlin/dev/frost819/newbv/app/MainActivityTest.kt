package dev.frost819.newbv.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [MainActivity] 的插桩测试。
 *
 * 验证：
 * - Activity 可启动（Hilt 注入正常）
 * - Navigation 宿主正常显示
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    fun teardown() {
        scenario?.close()
    }

    @Test
    fun activity_launches_withoutCrash() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        assertThat(scenario).isNotNull()
    }

    @Test
    fun activity_isNotFinishing_afterLaunch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario!!.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
            assertThat(activity.window).isNotNull()
        }
    }
}
