package dev.frost819.newbv.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.user.MyInfoData
import dev.frost819.newbv.biliapi.http.entity.user.LevelInfo
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.dao.UserDao
import dev.frost819.newbv.data.db.entity.UserEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

/**
 * [AccountRepositoryImpl] 的单元测试。
 *
 * 验证多账户管理、登录态持久化、凭证同步逻辑。
 * 使用 MockK mock [UserDao] 和 [BiliHttpApi]（静态 mock）。
 * Prefs 初始化一次，每个测试前 clear 重置。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountRepositoryImplTest {

    private lateinit var userDao: UserDao
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: AccountRepositoryImpl

    companion object {
        private lateinit var testDataStore: DataStore<Preferences>

        @JvmStatic
        @BeforeAll
        fun initPrefs() {
            Prefs.resetForTesting()
            val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
            val file = File.createTempFile("test_account_repo", ".preferences_pb")
            file.deleteOnExit()
            testDataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            Prefs.init(testDataStore)
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            // Leave Prefs initialized to avoid async write exceptions
        }
    }

    @BeforeEach
    fun setUp() {
        runBlocking { Prefs.clear() }

        userDao = mockk(relaxed = true)
        authRepository = AuthRepository()

        mockkObject(BiliHttpApi)
        val myInfoData = mockk<MyInfoData>(relaxed = true)
        every { myInfoData.name } returns "testuser"
        every { myInfoData.face } returns "http://example.com/avatar.png"
        every { myInfoData.levelExp } returns LevelInfo(
            currentLevel = 6,
            currentMin = 50,
            currentExp = 100,
            nextExp = 200,
        )
        val mockResponse = BiliResponse<MyInfoData>(
            code = 0,
            message = "ok",
            data = myInfoData,
        )
        coEvery { BiliHttpApi.getUserSelfInfo() } returns mockResponse

        repository = AccountRepositoryImpl(userDao, authRepository)
    }

    @AfterEach
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `getAllUsers delegates to userDao`() = runTest {
        val users = listOf(
            UserEntity(uid = 1L, username = "user1", avatar = "", auth = "{}"),
            UserEntity(uid = 2L, username = "user2", avatar = "", auth = "{}"),
        )
        coEvery { userDao.getAll() } returns users

        val result = repository.getAllUsers()

        assertThat(result).hasSize(2)
        assertThat(result[0].username).isEqualTo("user1")
    }

    @Test
    fun `findUserByUid delegates to userDao`() = runTest {
        val user = UserEntity(uid = 100L, username = "test", avatar = "url", auth = "{}")
        coEvery { userDao.findUserByUid(100L) } returns user

        val result = repository.findUserByUid(100L)

        assertThat(result?.username).isEqualTo("test")
    }

    @Test
    fun `findUserByUid returns null when not found`() = runTest {
        coEvery { userDao.findUserByUid(any()) } returns null

        val result = repository.findUserByUid(999L)

        assertThat(result).isNull()
    }

    @Test
    fun `upsertUser updates auth when user exists`() = runTest {
        val existing = UserEntity(id = 1, uid = 100L, username = "old", avatar = "old_url", auth = "{}")
        val updated = UserEntity(id = null, uid = 100L, username = "", avatar = "", auth = "new_auth")
        coEvery { userDao.findUserByUid(100L) } returns existing

        repository.upsertUser(updated)

        coVerify { userDao.update(existing) }
        assertThat(existing.auth).isEqualTo("new_auth")
    }

    @Test
    fun `upsertUser inserts when user does not exist`() = runTest {
        val newUser = UserEntity(uid = 200L, username = "fresh", avatar = "", auth = "{}")
        coEvery { userDao.findUserByUid(200L) } returns null

        repository.upsertUser(newUser)

        coVerify { userDao.insert(newUser) }
    }

    @Test
    fun `deleteUser delegates to userDao`() = runTest {
        val user = UserEntity(uid = 100L, username = "test", avatar = "", auth = "{}")

        repository.deleteUser(user)

        coVerify { userDao.delete(user) }
    }

    @Test
    fun `isLogin reads from Prefs`() {
        Prefs.isLogin = false
        assertThat(repository.isLogin()).isFalse()

        Prefs.isLogin = true
        assertThat(repository.isLogin()).isTrue()
    }

    @Test
    fun `currentUid reads from Prefs`() {
        Prefs.uid = 42L
        assertThat(repository.currentUid()).isEqualTo(42L)
    }

    @Test
    fun `setCurrentUser saves auth to prefs and syncs to AuthRepository`() = runTest {
        val authData = AuthData(
            uid = 100L,
            uidCkMd5 = "ckmd5",
            sid = "sid-123",
            biliJct = "jct-token",
            sessData = "sess-data",
            tokenExpiredDate = System.currentTimeMillis() + 86400000,
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )
        val user = UserEntity(
            uid = 100L,
            username = "testuser",
            avatar = "http://example.com/avatar.png",
            auth = authData.toJson(),
        )
        coEvery { userDao.findUserByUid(100L) } returns user

        repository.setCurrentUser(user)

        assertThat(Prefs.isLogin).isTrue()
        assertThat(Prefs.uid).isEqualTo(100L)
        assertThat(Prefs.sessData).isEqualTo("sess-data")
        assertThat(Prefs.biliJct).isEqualTo("jct-token")
        assertThat(Prefs.accessToken).isEqualTo("access-token")

        assertThat(authRepository.mid).isEqualTo(100L)
        assertThat(authRepository.sessionData).isEqualTo("sess-data")
        assertThat(authRepository.biliJct).isEqualTo("jct-token")
        assertThat(authRepository.accessToken).isEqualTo("access-token")
    }

    @Test
    fun `addUser persists to DB and sets current user`() = runTest {
        val authData = AuthData(
            uid = 200L,
            uidCkMd5 = "md5",
            sid = "sid",
            biliJct = "jct",
            sessData = "sess",
            tokenExpiredDate = System.currentTimeMillis() + 86400000,
        )
        coEvery { userDao.findUserByUid(200L) } returns null

        repository.addUser(authData)

        coVerify { userDao.insert(any()) }
        assertThat(Prefs.isLogin).isTrue()
        assertThat(Prefs.uid).isEqualTo(200L)
        assertThat(authRepository.mid).isEqualTo(200L)
    }

    @Test
    fun `logout clears Prefs and AuthRepository`() = runTest {
        Prefs.isLogin = true
        Prefs.uid = 100L
        Prefs.sessData = "sess"
        Prefs.biliJct = "jct"
        authRepository.mid = 100L
        authRepository.sessionData = "sess"

        coEvery { userDao.findUserByUid(100L) } returns null

        repository.logout()

        assertThat(Prefs.isLogin).isFalse()
        assertThat(Prefs.uid).isEqualTo(0L)
        assertThat(Prefs.sessData).isEmpty()
        assertThat(authRepository.mid).isNull()
        assertThat(authRepository.sessionData).isNull()
    }

    @Test
    fun `logout deletes user from DB if exists`() = runTest {
        val user = UserEntity(uid = 100L, username = "test", avatar = "", auth = "{}")
        Prefs.isLogin = true
        Prefs.uid = 100L
        coEvery { userDao.findUserByUid(100L) } returns user

        repository.logout()

        coVerify { userDao.delete(user) }
    }

    @Test
    fun `updateUserLock updates lock field in DB`() = runTest {
        val user = UserEntity(uid = 100L, username = "test", avatar = "", auth = "{}")
        coEvery { userDao.findUserByUid(100L) } returns user

        repository.updateUserLock(100L, "udlr")

        coVerify { userDao.update(match { it.lock == "udlr" }) }
    }

    @Test
    fun `toggleIncognitoMode toggles Prefs`() {
        val initial = Prefs.incognitoMode
        repository.toggleIncognitoMode()
        assertThat(Prefs.incognitoMode).isEqualTo(!initial)
        repository.toggleIncognitoMode()
        assertThat(Prefs.incognitoMode).isEqualTo(initial)
    }

    @Test
    fun `initFromPrefs syncs when already logged in`() {
        Prefs.isLogin = true
        Prefs.uid = 300L
        Prefs.sessData = "test-sess"
        Prefs.biliJct = "test-jct"
        Prefs.accessToken = "test-token"

        val newRepo = AccountRepositoryImpl(userDao, authRepository)

        assertThat(authRepository.mid).isEqualTo(300L)
        assertThat(authRepository.sessionData).isEqualTo("test-sess")
        assertThat(authRepository.biliJct).isEqualTo("test-jct")
        assertThat(authRepository.accessToken).isEqualTo("test-token")
        assertThat(newRepo.uiState.value.isLogin).isTrue()
        assertThat(newRepo.uiState.value.uid).isEqualTo(300L)
    }

    @Test
    fun `initFromPrefs does nothing when not logged in`() {
        Prefs.isLogin = false

        val newRepo = AccountRepositoryImpl(userDao, authRepository)

        assertThat(authRepository.mid).isNull()
        assertThat(newRepo.uiState.value.isLogin).isFalse()
    }
}
