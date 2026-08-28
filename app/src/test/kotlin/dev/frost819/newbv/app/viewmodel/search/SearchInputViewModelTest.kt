package dev.frost819.newbv.app.viewmodel.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.search.Hotword
import dev.frost819.newbv.biliapi.repositories.SearchRepository
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.entity.SearchHistoryEntity
import dev.frost819.newbv.data.repository.SearchHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.Date

/**
 * [SearchInputViewModel] 的单元测试。
 *
 * 验证热搜词加载、搜索建议、搜索历史增删逻辑。
 * 使用 MockK mock [SearchRepository] 和 [SearchHistoryRepository]。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchInputViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var searchRepo: SearchRepository
    private lateinit var historyRepo: SearchHistoryRepository
    private lateinit var viewModel: SearchInputViewModel

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_search_input_vm", ".preferences_pb")
            file.deleteOnExit()
            testDataStore =
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = { file },
                )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Leave Prefs initialized
        }
    }

    private fun fakeHotword(keyword: String) =
        Hotword(
            keyword = keyword,
            showName = keyword,
            icon = null,
        )

    private fun fakeHistory(keyword: String) =
        SearchHistoryEntity(
            id = null,
            keyword = keyword,
            searchDate = Date(),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { Prefs.clear() }

        searchRepo = mockk()
        historyRepo = mockk()

        coEvery { searchRepo.getSearchHotwords(any(), any()) } returns
            listOf(fakeHotword("热词1"), fakeHotword("热词2"))
        coEvery { searchRepo.getSearchSuggest(any(), any()) } returns
            listOf("建议1", "建议2")
        coEvery { historyRepo.getHistories(any()) } returns emptyList()
        coEvery { historyRepo.addHistory(any()) } returns Unit
        coEvery { historyRepo.deleteHistory(any()) } returns Unit
        coEvery { historyRepo.clearAll() } returns Unit

        viewModel = SearchInputViewModel(searchRepo, historyRepo)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads hotwords and histories`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.hotwords).hasSize(2)
            assertThat(state.hotwords[0].keyword).isEqualTo("热词1")
            assertThat(state.histories).isEmpty()
        }

    @Test
    fun `updateKeyword with non-empty keyword loads suggests`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.updateKeyword("测试")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.keyword).isEqualTo("测试")
            assertThat(state.suggests).hasSize(2)
            assertThat(state.suggests[0]).isEqualTo("建议1")
        }

    @Test
    fun `updateKeyword with empty keyword clears suggests`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.updateKeyword("测试")
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.suggests).isNotEmpty()

            viewModel.updateKeyword("")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.suggests).isEmpty()
        }

    @Test
    fun `commitSearch adds history and reloads`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            coEvery { historyRepo.getHistories(any()) } returns listOf(fakeHistory("搜索词"))
            viewModel.commitSearch("搜索词")
            advanceUntilIdle()

            coVerify { historyRepo.addHistory("搜索词") }
            assertThat(viewModel.uiState.value.histories).hasSize(1)
        }

    @Test
    fun `commitSearch calls completion after history is persisted`() =
        runTest(testDispatcher) {
            var completed = false

            viewModel.commitSearch("搜索词") { completed = true }
            advanceUntilIdle()

            coVerify { historyRepo.addHistory("搜索词") }
            assertThat(completed).isTrue()
        }

    @Test
    fun `commitSearch with blank keyword does nothing`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.commitSearch("")
            advanceUntilIdle()

            coVerify(exactly = 0) { historyRepo.addHistory(any()) }
        }

    @Test
    fun `deleteHistory removes and reloads`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            coEvery { historyRepo.getHistories(any()) } returns listOf(fakeHistory("词2"))
            viewModel.deleteHistory("词1")
            advanceUntilIdle()

            coVerify { historyRepo.deleteHistory("词1") }
            assertThat(viewModel.uiState.value.histories).hasSize(1)
            assertThat(
                viewModel.uiState.value.histories[0]
                    .keyword,
            ).isEqualTo("词2")
        }

    @Test
    fun `clearAllHistories clears and reloads`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            coEvery { historyRepo.getHistories(any()) } returns emptyList()
            viewModel.clearAllHistories()
            advanceUntilIdle()

            coVerify { historyRepo.clearAll() }
            assertThat(viewModel.uiState.value.histories).isEmpty()
        }

    @Test
    fun `loadHotwords failure sets hotwordsError`() =
        runTest(testDispatcher) {
            coEvery { searchRepo.getSearchHotwords(any(), any()) } throws RuntimeException("network error")

            viewModel.refreshHotwords()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.hotwordsError).isTrue()
            assertThat(state.isLoadingHotwords).isFalse()
        }

    @Test
    fun `refreshHotwords clears error and retries`() =
        runTest(testDispatcher) {
            // First: fail
            coEvery { searchRepo.getSearchHotwords(any(), any()) } throws RuntimeException("error")
            viewModel.refreshHotwords()
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.hotwordsError).isTrue()

            // Then: recover
            coEvery { searchRepo.getSearchHotwords(any(), any()) } returns listOf(fakeHotword("恢复"))
            viewModel.refreshHotwords()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.hotwordsError).isFalse()
            assertThat(viewModel.uiState.value.hotwords).hasSize(1)
        }

    @Test
    fun `hotwords state emits updates`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.uiState.test {
                assertThat(awaitItem().hotwords).isNotEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
