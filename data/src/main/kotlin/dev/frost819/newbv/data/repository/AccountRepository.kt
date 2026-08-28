package dev.frost819.newbv.data.repository

import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 账户仓库接口。
 *
 * 定义多账户管理与登录态操作的抽象，实现由 app 层提供（app 层可访问 bili-api
 * 进行网络登录，同时注入 [UserDao] 与 [Prefs]）。
 *
 * @see UserEntity
 */
interface AccountRepository {
    /** 查询全部已登录账户。 */
    suspend fun getAllUsers(): List<UserEntity>

    /**
     * 按 UID 查找账户。
     *
     * @param uid B 站用户 UID。
     * @return 命中的账户，无匹配返回 null。
     */
    suspend fun findUserByUid(uid: Long): UserEntity?

    /**
     * 添加或更新账户。
     *
     * 若 UID 已存在则更新，否则插入新记录。
     *
     * @param user 账户数据。
     */
    suspend fun upsertUser(user: UserEntity)

    /**
     * 删除指定账户。
     *
     * @param user 待删除的账户。
     */
    suspend fun deleteUser(user: UserEntity)

    /** 当前是否已登录（读取 Prefs）。 */
    fun isLogin(): Boolean

    /** 当前登录用户的 UID（读取 Prefs）。 */
    fun currentUid(): Long

    /**
     * 设置当前登录账户。
     *
     * 将账户凭证写入 Prefs（isLogin/uid/sessData/biliJct 等）。
     *
     * @param user 登录成功的账户数据。
     */
    suspend fun setCurrentUser(user: UserEntity)

    /** 退出登录，清空 Prefs 中的登录态。 */
    suspend fun logout()
}
