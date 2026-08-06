package dev.frost819.newbv.biliapi.entity.user

import dev.frost819.newbv.biliapi.http.entity.user.UserInfoData

/**
 * 用户空间页所需的用户信息。
 *
 * 聚合 [UserInfoData] 中 UserSpaceScreen 需要展示的字段。
 *
 * @param mid 用户 mid
 * @param name 昵称
 * @param face 头像链接
 * @param sign 签名
 * @param level 当前等级
 * @param topPhoto 主页头图链接
 * @param isFollowed 是否已关注此用户
 */
data class UserSpaceInfo(
    val mid: Long,
    val name: String,
    val face: String,
    val sign: String,
    val level: Int,
    val topPhoto: String,
    val isFollowed: Boolean,
) {
    companion object {
        fun fromUserInfoData(data: UserInfoData) =
            UserSpaceInfo(
                mid = data.mid,
                name = data.name,
                face = data.face,
                sign = data.sign,
                level = data.level,
                topPhoto = data.topPhoto,
                isFollowed = data.isFollowed,
            )
    }
}
