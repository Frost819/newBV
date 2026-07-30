package dev.frost819.newbv.app.data

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.user.MyInfoData
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.data.db.dao.UserDao
import dev.frost819.newbv.data.db.entity.UserEntity
import dev.frost819.newbv.data.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AccountRepository] 的实现。
 *
 * 聚合三层数据源：
 * - [UserDao]（Room）：多账户持久化
 * - [Prefs]（DataStore）：当前登录用户凭证
 * - [AuthRepository]（内存）：bili-api 层鉴权状态
 *
 * 通过 [uiState] 暴露当前登录状态给 UI 层观察。
 *
 * @property userDao Room DAO。
 * @property authRepository bili-api 鉴权状态。
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
) : AccountRepository {

    /** UI 状态：当前登录用户信息。 */
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        initFromPrefs()
    }

    /**
     * 从 Prefs 初始化登录态。
     *
     * 在 Application 启动后首次构造时调用，
     * 将 Prefs 中已有的凭证同步到 [AuthRepository] 和 [uiState]。
     */
    private fun initFromPrefs() {
        if (Prefs.isLogin) {
            syncToAuthRepository(
                uid = Prefs.uid,
                sessData = Prefs.sessData,
                biliJct = Prefs.biliJct,
                accessToken = Prefs.accessToken,
            )
            _uiState.update {
                it.copy(
                    isLogin = true,
                    uid = Prefs.uid,
                    username = "",
                    avatar = "",
                )
            }
        }
    }

    /**
     * 将凭证同步到 [AuthRepository]。
     *
     * bili-api 层的 Repository 通过 [AuthRepository] 读取当前凭证，
     * 此方法在用户切换或登录后调用。
     *
     * @param uid 用户 UID。
     * @param sessData SESSDATA Cookie。
     * @param biliJct bili_jct CSRF token。
     * @param accessToken App 接口 token。
     */
    private fun syncToAuthRepository(
        uid: Long,
        sessData: String,
        biliJct: String,
        accessToken: String,
    ) {
        authRepository.mid = uid
        authRepository.sessionData = sessData
        authRepository.biliJct = biliJct
        authRepository.accessToken = accessToken
        authRepository.buvid3 = Prefs.buvid3
        authRepository.buvid = Prefs.buvid
    }

    override suspend fun getAllUsers(): List<UserEntity> = userDao.getAll()

    override suspend fun findUserByUid(uid: Long): UserEntity? = userDao.findUserByUid(uid)

    override suspend fun upsertUser(user: UserEntity) {
        val existing = userDao.findUserByUid(user.uid)
        if (existing != null) {
            existing.auth = user.auth
            userDao.update(existing)
        } else {
            userDao.insert(user)
        }
    }

    override suspend fun deleteUser(user: UserEntity) {
        userDao.delete(user)
    }

    override fun isLogin(): Boolean = Prefs.isLogin

    override fun currentUid(): Long = Prefs.uid

    /**
     * 设置当前登录用户。
     *
     * 执行步骤：
     * 1. 从 [UserEntity.auth] 反序列化 [AuthData]。
     * 2. 写入 Prefs（使 bili-api 层可读取）。
     * 3. 同步到 [AuthRepository]。
     * 4. 更新 [uiState]。
     *
     * @param user 登录成功的用户。
     */
    override suspend fun setCurrentUser(user: UserEntity) {
        val authData = AuthData.fromJson(user.auth)
        authData.saveToPrefs()
        syncToAuthRepository(
            uid = authData.uid,
            sessData = authData.sessData,
            biliJct = authData.biliJct,
            accessToken = authData.accessToken,
        )
        _uiState.update {
            it.copy(
                isLogin = true,
                uid = user.uid,
                username = user.username,
                avatar = user.avatar,
                level = 0,
                currentMin = 0,
                exp = 0,
                nextExp = 0,
            )
        }
        refreshUserInfo()
    }

    /**
     * 添加新登录用户。
     *
     * 若 UID 已存在则更新凭证，否则插入新记录。设置为当前用户。
     *
     * @param authData 登录返回的凭证。
     */
    suspend fun addUser(authData: AuthData) {
        val user = UserEntity(
            uid = authData.uid,
            username = "",
            avatar = "",
            auth = authData.toJson(),
        )
        upsertUser(user)
        authData.saveToPrefs()
        syncToAuthRepository(
            uid = authData.uid,
            sessData = authData.sessData,
            biliJct = authData.biliJct,
            accessToken = authData.accessToken,
        )
        _uiState.update {
            it.copy(
                isLogin = true,
                uid = authData.uid,
            )
        }
        refreshUserInfo()
    }

    /**
     * 从网络刷新当前用户信息（用户名、头像）。
     *
     * 调用 BiliHttpApi 获取用户自身信息，更新 Room 和 [uiState]。
     */
    suspend fun refreshUserInfo() {
        if (!Prefs.isLogin) return
        runCatching {
            val response: MyInfoData = BiliHttpApi.getUserSelfInfo(
                sessData = Prefs.sessData,
            ).getResponseData()
            val user = userDao.findUserByUid(Prefs.uid) ?: return
            user.username = response.name
            user.avatar = response.face
            userDao.update(user)
            _uiState.update {
                it.copy(
                    username = response.name,
                    avatar = response.face,
                    level = response.levelExp.currentLevel,
                    currentMin = response.levelExp.currentMin,
                    exp = response.levelExp.currentExp,
                    nextExp = response.levelExp.nextExp,
                )
            }
        }
    }

    /**
     * 从 Room 加载当前用户的用户名和头像（不请求网络）。
     */
    suspend fun reloadAvatar() {
        if (!Prefs.isLogin) return
        val user = userDao.findUserByUid(Prefs.uid) ?: return
        _uiState.update {
            it.copy(
                username = user.username,
                avatar = user.avatar,
            )
        }
    }

    override suspend fun logout() {
        val user = userDao.findUserByUid(Prefs.uid)
        if (user != null) {
            userDao.delete(user)
        }
        Prefs.apply {
            isLogin = false
            uid = 0L
            sid = ""
            sessData = ""
            biliJct = ""
            uidCkMd5 = ""
            accessToken = ""
            refreshToken = ""
            tokenExpiredDate = java.util.Date(0)
        }
        authRepository.apply {
            mid = null
            sessionData = null
            biliJct = null
            accessToken = null
        }
        _uiState.value = AccountUiState()
    }

    /**
     * 更新用户锁密码。
     *
     * @param uid 用户 UID。
     * @param lock 锁密码字符串（空字符串表示取消锁）。
     */
    suspend fun updateUserLock(uid: Long, lock: String) {
        val user = userDao.findUserByUid(uid) ?: return
        user.lock = lock
        userDao.update(user)
    }

    /**
     * 切换无痕模式。
     */
    fun toggleIncognitoMode() {
        Prefs.incognitoMode = !Prefs.incognitoMode
        _uiState.update { it.copy(incognitoMode = Prefs.incognitoMode) }
    }
}

/**
 * 账户 UI 状态。
 *
 * @property isLogin 是否已登录。
 * @property uid 当前用户 UID。
 * @property username 用户名。
 * @property avatar 头像 URL。
 * @property level 用户等级。
 * @property currentMin 当前等级经验最低值。
 * @property exp 当前经验值（总经验）。
 * @property nextExp 下一等级所需经验值（门槛，非剩余）。Lv6 满级时为 0。
 * @property incognitoMode 无痕模式。
 */
data class AccountUiState(
    val isLogin: Boolean = false,
    val uid: Long = 0L,
    val username: String = "",
    val avatar: String = "",
    val level: Int = 0,
    val currentMin: Int = 0,
    val exp: Int = 0,
    val nextExp: Int = 0,
    val incognitoMode: Boolean = false,
)
