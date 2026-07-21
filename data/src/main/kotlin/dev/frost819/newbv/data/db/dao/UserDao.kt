package dev.frost819.newbv.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.frost819.newbv.data.db.entity.UserEntity

/**
 * 账户数据访问对象。
 *
 * 提供多账户的增删改查，所有方法为 `suspend`，需在协程中调用。
 */
@Dao
interface UserDao {
    /** 查询全部已登录账户。 */
    @Query("SELECT * FROM user")
    suspend fun getAll(): List<UserEntity>

    /**
     * 按 B 站 UID 查找账户。
     *
     * @param uid 用户 UID。
     * @return 命中的账户，无匹配返回 `null`。
     */
    @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
    suspend fun findUserByUid(uid: Long): UserEntity?

    /** 插入一个或多个账户。 */
    @Insert
    suspend fun insert(vararg user: UserEntity)

    /** 删除一个或多个账户。 */
    @Delete
    suspend fun delete(vararg user: UserEntity)

    /** 更新账户信息（如刷新用户名、头像、凭证）。 */
    @Update
    suspend fun update(user: UserEntity)
}
