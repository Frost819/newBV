package dev.frost819.newbv.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账户实体。
 *
 * 对应数据库表 `user`，存储多账户登录后的凭证与基本信息。
 * 一个账户对应一行记录，以 [uid] 作为业务主键（数据库主键自增）。
 *
 * @property id 自增主键，插入时传 `null` 由数据库生成。
 * @property uid B 站用户 UID（业务唯一标识，非数据库主键）。
 * @property username 用户名，登录后从接口获取并更新。
 * @property avatar 用户头像 URL，登录后从接口获取并更新。
 * @property auth 登录凭证 JSON（含 SESSDATA / bili_jct / access_token 等），由调用方序列化。
 * @property lock 用户锁密码（空字符串表示未启用），用于启动解锁校验。
 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "uid") val uid: Long,
    @ColumnInfo(name = "username") var username: String,
    @ColumnInfo(name = "avatar") var avatar: String,
    @ColumnInfo(name = "auth") var auth: String,
    @ColumnInfo(name = "lock", defaultValue = "") var lock: String = "",
)
