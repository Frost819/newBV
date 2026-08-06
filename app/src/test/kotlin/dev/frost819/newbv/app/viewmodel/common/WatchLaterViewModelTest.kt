package dev.frost819.newbv.app.viewmodel.common

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.biliapi.repositories.ToViewRepository
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * [WatchLaterViewModel] 的单元测试。
 *
 * 验证添加稍后再看的成功/失败效果。
 * 使用 MockK mock [ToViewRepository]，mockkObject mock [Prefs]。
 */
class WatchLaterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var toViewRepository: ToViewRepository
    private lateinit var viewModel: WatchLaterViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        toViewRepository = mockk()

        mockkObject(Prefs)
        every { Prefs.apiType } returns DataApiType.Web
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Prefs)
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WatchLaterViewModel =
        WatchLaterViewModel(toViewRepository = toViewRepository)

    @Test
    fun `addToView success emits ShowToast`() = runTest(testDispatcher) {
        coEvery {
            toViewRepository.addToView(aid = 1L, bvid = null, preferApiType = BiliApiType.Web)
        } returns Unit

        viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.addToView(aid = 1L)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(WatchLaterEffect.ShowToast::class.java)
            assertThat((effect as WatchLaterEffect.ShowToast).message).isEqualTo("已添加到稍后再看")
        }

        coVerify(exactly = 1) {
            toViewRepository.addToView(aid = 1L, bvid = null, preferApiType = BiliApiType.Web)
        }
    }

    @Test
    fun `addToView success with bvid emits ShowToast`() = runTest(testDispatcher) {
        coEvery {
            toViewRepository.addToView(aid = 1L, bvid = "BV1xxx", preferApiType = BiliApiType.Web)
        } returns Unit

        viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.addToView(aid = 1L, bvid = "BV1xxx")
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(WatchLaterEffect.ShowToast::class.java)
            assertThat((effect as WatchLaterEffect.ShowToast).message).isEqualTo("已添加到稍后再看")
        }
    }

    @Test
    fun `addToView failure emits ShowToast with error message`() = runTest(testDispatcher) {
        coEvery {
            toViewRepository.addToView(any(), any(), any())
        } throws IOException("network error")

        viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.addToView(aid = 1L)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(WatchLaterEffect.ShowToast::class.java)
            assertThat((effect as WatchLaterEffect.ShowToast).message).contains("添加失败")
            assertThat((effect as WatchLaterEffect.ShowToast).message).contains("network error")
        }
    }

    @Test
    fun `addToView failure with null message emits ShowToast with unknown error`() = runTest(testDispatcher) {
        coEvery {
            toViewRepository.addToView(any(), any(), any())
        } throws Exception()

        viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.addToView(aid = 1L)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(WatchLaterEffect.ShowToast::class.java)
            assertThat((effect as WatchLaterEffect.ShowToast).message).contains("未知错误")
        }
    }

    @Test
    fun `addToView uses App apiType when Prefs apiType is App`() = runTest(testDispatcher) {
        every { Prefs.apiType } returns DataApiType.App

        coEvery {
            toViewRepository.addToView(aid = 1L, bvid = null, preferApiType = BiliApiType.App)
        } returns Unit

        viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.addToView(aid = 1L)
            advanceUntilIdle()

            awaitItem()
        }

        coVerify {
            toViewRepository.addToView(aid = 1L, bvid = null, preferApiType = BiliApiType.App)
        }
    }
}
