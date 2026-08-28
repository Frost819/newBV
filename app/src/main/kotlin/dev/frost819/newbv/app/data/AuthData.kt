package dev.frost819.newbv.app.data

import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.serialization.Serializable
import java.util.Date

/**
 * 登录凭证数据。
 *
 * 序列化为 JSON 存入 Room [dev.frost819.newbv.data.db.entity.UserEntity.auth] 字段。
 * 同时提供与 [Prefs] 的双向同步。
 *
 * @property uid 用户 UID。
 * @property uidCkMd5 UID 校验值。
 * @property sid 会话 ID。
 * @property biliJct CSRF token。
 * @property sessData SESSDATA Cookie。
 * @property tokenExpiredDate Token 过期时间。
 * @property accessToken App 接口 Access Token（TV QR 登录时有值）。
 * @property refreshToken App 接口 Refresh Token。
 */
@Serializable
data class AuthData(
    val uid: Long,
    val uidCkMd5: String,
    val sid: String,
    val biliJct: String,
    val sessData: String,
    val tokenExpiredDate: Long,
    val accessToken: String = "",
    val refreshToken: String = "",
) {
    companion object {
        /**
         * 从 JSON 字符串反序列化。
         *
         * @param json JSON 字符串。
         * @return 反序列化后的 [AuthData]。
         */
        fun fromJson(json: String): AuthData =
            kotlinx.serialization.json.Json
                .decodeFromString(serializer(), json)

        /**
         * 从当前 [Prefs] 构造 [AuthData]。
         *
         * 读取 Prefs 中已保存的登录凭证，组装为 [AuthData]。
         *
         * @return 当前 Prefs 中的凭证。
         */
        fun fromPrefs(): AuthData =
            AuthData(
                uid = Prefs.uid,
                uidCkMd5 = Prefs.uidCkMd5,
                sid = Prefs.sid,
                biliJct = Prefs.biliJct,
                sessData = Prefs.sessData,
                tokenExpiredDate = Prefs.tokenExpiredDate.time,
                accessToken = Prefs.accessToken,
                refreshToken = Prefs.refreshToken,
            )
    }

    /**
     * 序列化为 JSON 字符串。
     *
     * 用于存入 Room [dev.frost819.newbv.data.db.entity.UserEntity.auth] 字段。
     *
     * @return JSON 字符串。
     */
    fun toJson(): String =
        kotlinx.serialization.json.Json
            .encodeToString(serializer(), this)

    /**
     * 将凭证写入 [Prefs]。
     *
     * 在切换用户或登录成功后调用，使 bili-api 层能通过 Prefs 读取最新凭证。
     */
    fun saveToPrefs() {
        Prefs.uid = uid
        Prefs.uidCkMd5 = uidCkMd5
        Prefs.sid = sid
        Prefs.biliJct = biliJct
        Prefs.sessData = sessData
        Prefs.tokenExpiredDate = Date(tokenExpiredDate)
        Prefs.accessToken = accessToken
        Prefs.refreshToken = refreshToken
        Prefs.isLogin = true
    }
}
