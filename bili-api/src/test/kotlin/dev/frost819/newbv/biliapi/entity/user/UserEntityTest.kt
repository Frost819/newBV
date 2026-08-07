package dev.frost819.newbv.biliapi.entity.user

import bilibili.app.archive.v1.author
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UserSpaceInfo]、[FollowedUser]、[Author] 实体 HTTP→Domain 转换方法的单元测试。
 */
class UserEntityTest {
    @Test
    fun `UserSpaceInfo fromUserInfoData maps all fields`() {
        val data =
            dev.frost819.newbv.biliapi.http.entity.user.UserInfoData(
                mid = 12345L,
                name = "测试用户",
                sex = "男",
                face = "http://face.test",
                faceNft = 0,
                faceNftType = 0,
                sign = "签名",
                rank = 10000,
                level = 5,
                jointime = 0,
                moral = 0,
                silence = 0,
                coins = 0f,
                fansBadge = false,
                fansMedal = fakeFansMedal(),
                official = fakeOfficial(),
                vip = fakeVip(),
                pendant = fakePendant(),
                nameplate = fakeNameplate(),
                userHonourInfo = fakeUserHonours(),
                isFollowed = true,
                topPhoto = "http://top.test",
                sys_notice = fakeSysNotice(),
                live_room = fakeLiveRoom(),
                birthday = "01-01",
                school = null,
                profession = fakeProfession(),
                series = fakeSeries(),
                isSeniorMember = 0,
                gaiaResType = 0,
                isRisk = false,
                elec = fakeElec(),
            )

        val info = UserSpaceInfo.fromUserInfoData(data)

        assertThat(info.mid).isEqualTo(12345L)
        assertThat(info.name).isEqualTo("测试用户")
        assertThat(info.face).isEqualTo("http://face.test")
        assertThat(info.sign).isEqualTo("签名")
        assertThat(info.level).isEqualTo(5)
        assertThat(info.topPhoto).isEqualTo("http://top.test")
        assertThat(info.isFollowed).isTrue()
    }

    @Test
    fun `FollowedUser fromHttpFollowedUser maps mid uname face sign`() {
        val followedUser =
            dev.frost819.newbv.biliapi.http.entity.user.UserFollowData.FollowedUser(
                mid = 999L,
                attribute = 2,
                mtime = 1700000000,
                tag = null,
                special = 0,
                uname = "关注用户",
                face = "http://face.test",
                sign = "签名内容",
                officialVerify = fakeOfficialVerify(),
                vip = fakeFollowedUserVip(),
                nftIcon = "",
                recReason = "",
                trackId = "t1",
            )

        val result = FollowedUser.fromHttpFollowedUser(followedUser)

        assertThat(result.mid).isEqualTo(999L)
        assertThat(result.name).isEqualTo("关注用户")
        assertThat(result.avatar).isEqualTo("http://face.test")
        assertThat(result.sign).isEqualTo("签名内容")
    }

    @Test
    fun `Author holds mid name face`() {
        val author = Author(mid = 100L, name = "UP主", face = "http://face.test")
        assertThat(author.mid).isEqualTo(100L)
        assertThat(author.name).isEqualTo("UP主")
        assertThat(author.face).isEqualTo("http://face.test")
    }

    @Test
    fun `HistoryItemType has Unknown Archive Pgc`() {
        assertThat(
            HistoryItemType.entries,
        ).containsExactly(HistoryItemType.Unknown, HistoryItemType.Archive, HistoryItemType.Pgc)
    }

    // ------------------------------------------------------------------
    // Author.fromAuthor(gRPC)
    // ------------------------------------------------------------------

    @Test
    fun `Author fromAuthor gRPC maps mid name face`() {
        val grpcAuthor =
            author {
                mid = 456L
                name = "gRPC UP主"
                face = "http://grpc-face.test"
            }

        val author = Author.fromAuthor(grpcAuthor)

        assertThat(author.mid).isEqualTo(456L)
        assertThat(author.name).isEqualTo("gRPC UP主")
        assertThat(author.face).isEqualTo("http://grpc-face.test")
    }

    @Test
    fun `Author fromAuthor gRPC with default values`() {
        val grpcAuthor = author { }

        val author = Author.fromAuthor(grpcAuthor)

        assertThat(author.mid).isEqualTo(0L)
        assertThat(author.name).isEqualTo("")
        assertThat(author.face).isEqualTo("")
    }

    @Suppress("LongMethod")
    private fun fakeFansMedal() =
        dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.FansMedal(show = false, wear = false, medal = null)

    private fun fakeOfficial() =
        dev.frost819.newbv.biliapi.http.entity.user.Official(role = 0, title = "", desc = "", type = -1)

    private fun fakeVip() =
        dev.frost819.newbv.biliapi.http.entity.user.Vip(
            type = 0,
            status = 0,
            dueDate = 0L,
            vipPayType = 0,
            themeType = 0,
            label =
                dev.frost819.newbv.biliapi.http.entity.user.Vip.Label(
                    path = "",
                    text = "",
                    labelTheme = "",
                    textColor = "",
                    bgStyle = 0,
                    bgColor = "",
                    borderColor = "",
                ),
            avatarSubscript = 0,
            nicknameColor = "",
            role = 0,
            avatarSubscriptUrl = "",
            tvVipStatus = 0,
            tvVipPayType = 0,
        )

    private fun fakePendant() =
        dev.frost819.newbv.biliapi.http.entity.user.Pendant(
            pid = 0,
            name = "",
            image = "",
            expire = 0,
            imageEnhance = "",
            imageEnhanceFrame = "",
        )

    private fun fakeNameplate() =
        dev.frost819.newbv.biliapi.http.entity.user.Nameplate(
            nid = 0,
            name = "",
            image = "",
            imageSmall = "",
            level = "",
            condition = "",
        )

    private fun fakeUserHonours() = dev.frost819.newbv.biliapi.http.entity.user.UserHonours(mid = 0L)

    private fun fakeSysNotice() = dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.SysNotice()

    private fun fakeLiveRoom() =
        dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.LiveRoom(
            roomStatus = 0,
            liveStatus = 0,
            url = "",
            title = "",
            cover = "",
            watchedShow =
                dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.LiveRoom.WatchedShow(
                    switch = false,
                    num = 0,
                    textSmall = "",
                    textLarge = "",
                    icon = "",
                    iconLocation = "",
                    iconWeb = "",
                ),
            roomId = 0,
            roundStatus = 0,
            broadcastType = 0,
        )

    private fun fakeProfession() =
        dev.frost819.newbv.biliapi.http.entity.user.Profession(name = "", department = "", title = "", isShow = 0)

    private fun fakeSeries() =
        dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.Series(
            userUpgradeStatus = 0,
            showUpgradeWindow = false,
        )

    private fun fakeElec() =
        dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.Elec(
            showInfo =
                dev.frost819.newbv.biliapi.http.entity.user.UserInfoData.Elec.ElecShowInfo(
                    show = false,
                    state = 0,
                    title = "",
                    icon = "",
                    jumpUrl = "",
                ),
        )

    private fun fakeOfficialVerify() = dev.frost819.newbv.biliapi.http.entity.user.OfficialVerify(type = -1, desc = "")

    private fun fakeFollowedUserVip() =
        dev.frost819.newbv.biliapi.http.entity.user.UserFollowData.FollowedUser.Vip(
            vipType = 0,
            vipDueDate = 0L,
            dueRemark = "",
            accessStatus = 0,
            vipStatus = 0,
            vipStatusWarn = "",
            themeType = 0,
            label =
                dev.frost819.newbv.biliapi.http.entity.user.Vip.Label(
                    path = "",
                    text = "",
                    labelTheme = "",
                    textColor = "",
                    bgStyle = 0,
                    bgColor = "",
                    borderColor = "",
                ),
        )
}
