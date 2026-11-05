package dev.frost819.newbv.app.data.danmaku

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.danmaku.DanmakuFilterRule
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.data.datastore.Prefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * [DanmakuFilterRepository] 的单元测试。
 *
 * 覆盖：缓存命中、并发去重、超时/失败降级、账号切换/登出失效、
 * 关键词/正则/用户规则的匹配（含纯数字 midHash 的判定）。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DanmakuFilterRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: DanmakuFilterRepository

    @BeforeEach
    fun setUp() {
        mockkObject(Prefs)
        every { Prefs.defaultDanmakuBlockByUserFilterEnabled } returns true
        mockkObject(BiliHttpApi)
        coEvery { BiliHttpApi.getDanmakuFilterRules() } returns emptyList()
        authRepository = AuthRepository()
        authRepository.mid = UID
        repository = DanmakuFilterRepository(authRepository = authRepository)
        repository.scope = CoroutineScope(testDispatcher + SupervisorJob())
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `local toggle off disables account rule filtering`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_KEYWORD, filter = "广告"))
            repository.ensureLoaded()
            assertThat(repository.shouldDrop(midHash = "", content = "这是广告")).isTrue()

            // 本地「账号屏蔽规则」关闭后，点播/直播共用入口都应失效
            every { Prefs.defaultDanmakuBlockByUserFilterEnabled } returns false
            assertThat(repository.shouldDrop(midHash = "", content = "这是广告")).isFalse()
            assertThat(repository.shouldDropLive(content = "这是广告", mid = 1L)).isFalse()
        }

    @Test
    fun `ensureLoaded compiles keyword regex and user rules`() =
        runTest(testDispatcher) {
            // Given
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(
                    rule(type = TYPE_KEYWORD, filter = "广告"),
                    rule(type = TYPE_REGEX, filter = "spo+iler"),
                    rule(type = TYPE_USER, filter = "5254a244"),
                    rule(type = TYPE_USER, filter = "54321"),
                    rule(type = TYPE_KEYWORD, filter = "已停用", opened = false),
                )

            // When
            val loaded = repository.ensureLoaded()

            // Then
            assertThat(loaded).isTrue()
            assertThat(repository.ruleCount.value).isEqualTo(4)
            assertThat(repository.hasRules).isTrue()
            assertThat(repository.shouldDrop(midHash = "", content = "这是广告")).isTrue()
            assertThat(repository.shouldDrop(midHash = "", content = "SPOILER 预警")).isTrue()
            assertThat(repository.shouldDrop(midHash = "5254A244", content = "任意内容")).isTrue()
            assertThat(repository.shouldDrop(midHash = crc32Hex("54321"), content = "任意内容")).isTrue()
            assertThat(repository.shouldDrop(midHash = "", content = "正常弹幕")).isFalse()
            // 停用规则不生效
            assertThat(repository.shouldDrop(midHash = "", content = "已停用")).isFalse()
        }

    @Test
    fun `eight-digit numeric rule is treated as midHash not raw uid`() =
        runTest(testDispatcher) {
            // 接口下发的 midHash 可能恰为纯数字；按 UID 二次 crc32 会导致该规则永不命中
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_USER, filter = "12345678"))

            repository.ensureLoaded()

            assertThat(repository.shouldDrop(midHash = "12345678", content = "任意内容")).isTrue()
            assertThat(repository.shouldDrop(midHash = crc32Hex("12345678"), content = "任意内容")).isFalse()
        }

    @Test
    fun `same account does not refetch rules`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_KEYWORD, filter = "广告"))

            assertThat(repository.ensureLoaded()).isTrue()
            assertThat(repository.ensureLoaded()).isTrue()

            coVerify(exactly = 1) { BiliHttpApi.getDanmakuFilterRules() }
        }

    @Test
    fun `concurrent calls share a single request`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<List<DanmakuFilterRule>>()
            coEvery { BiliHttpApi.getDanmakuFilterRules() } coAnswers { gate.await() }

            val first = async { repository.ensureLoaded() }
            val second = async { repository.ensureLoaded() }
            runCurrent()
            gate.complete(listOf(rule(type = TYPE_KEYWORD, filter = "广告")))

            assertThat(first.await()).isTrue()
            assertThat(second.await()).isTrue()
            coVerify(exactly = 1) { BiliHttpApi.getDanmakuFilterRules() }
        }

    @Test
    fun `timeout degrades without caching`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } coAnswers {
                delay(10_000)
                emptyList()
            }

            assertThat(repository.ensureLoaded()).isFalse()
            assertThat(repository.hasRules).isFalse()
            assertThat(repository.ruleCount.value).isEqualTo(0)
        }

    @Test
    fun `failure degrades and next call retries`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } throws IOException("network down")
            assertThat(repository.ensureLoaded()).isFalse()

            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_KEYWORD, filter = "广告"))
            assertThat(repository.ensureLoaded()).isTrue()
            assertThat(repository.shouldDrop(midHash = "", content = "这是广告")).isTrue()
            coVerify(exactly = 2) { BiliHttpApi.getDanmakuFilterRules() }
        }

    @Test
    fun `logged out skips request and clears cache`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_KEYWORD, filter = "广告"))
            repository.ensureLoaded()
            assertThat(repository.hasRules).isTrue()

            authRepository.mid = null
            assertThat(repository.ensureLoaded()).isFalse()
            assertThat(repository.hasRules).isFalse()
            assertThat(repository.ruleCount.value).isEqualTo(0)

            // 首次请求之外不再发请求
            coVerify(exactly = 1) { BiliHttpApi.getDanmakuFilterRules() }
        }

    @Test
    fun `in-flight result is discarded when account changes`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<List<DanmakuFilterRule>>()
            coEvery { BiliHttpApi.getDanmakuFilterRules() } coAnswers { gate.await() }

            val job = async { repository.ensureLoaded() }
            runCurrent()
            authRepository.mid = 999L
            gate.complete(listOf(rule(type = TYPE_KEYWORD, filter = "旧账号规则")))

            assertThat(job.await()).isFalse()
            assertThat(repository.hasRules).isFalse()
        }

    @Test
    fun `onAccountChanged clears old rules and prefetches new account`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returnsMany
                listOf(
                    listOf(rule(type = TYPE_KEYWORD, filter = "旧账号规则")),
                    listOf(rule(type = TYPE_KEYWORD, filter = "新账号规则")),
                )
            repository.ensureLoaded()
            assertThat(repository.shouldDrop(midHash = "", content = "旧账号规则")).isTrue()

            authRepository.mid = 999L
            repository.onAccountChanged(999L)
            advanceUntilIdle()

            assertThat(repository.shouldDrop(midHash = "", content = "旧账号规则")).isFalse()
            assertThat(repository.shouldDrop(midHash = "", content = "新账号规则")).isTrue()
            coVerify(exactly = 2) { BiliHttpApi.getDanmakuFilterRules() }
        }

    @Test
    fun `onAccountChanged null clears rules without request`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_KEYWORD, filter = "广告"))
            repository.ensureLoaded()

            repository.onAccountChanged(null)

            assertThat(repository.hasRules).isFalse()
            assertThat(repository.ruleCount.value).isEqualTo(0)
            coVerify(exactly = 1) { BiliHttpApi.getDanmakuFilterRules() }
        }

    @Test
    fun `prefetch loads rules on application scope`() =
        runTest(testDispatcher) {
            coEvery { BiliHttpApi.getDanmakuFilterRules() } returns
                listOf(rule(type = TYPE_KEYWORD, filter = "广告"))

            repository.prefetch()
            advanceUntilIdle()

            assertThat(repository.hasRules).isTrue()
            assertThat(repository.shouldDropLive(content = "这是广告", mid = 1L)).isTrue()
            assertThat(repository.shouldDropLive(content = "正常弹幕", mid = 1L)).isFalse()
        }

    private fun rule(
        type: Int,
        filter: String,
        opened: Boolean = true,
    ) = DanmakuFilterRule(type = type, filter = filter, opened = opened)

    private companion object {
        const val UID = 12345L
        const val TYPE_KEYWORD = 0
        const val TYPE_REGEX = 1
        const val TYPE_USER = 2
    }
}
