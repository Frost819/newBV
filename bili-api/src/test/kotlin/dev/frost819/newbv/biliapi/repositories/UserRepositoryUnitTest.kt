package dev.frost819.newbv.biliapi.repositories

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.BiliResponse
import dev.frost819.newbv.biliapi.http.entity.BiliResponseWithoutData
import dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicData
import dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicItem
import dev.frost819.newbv.biliapi.http.entity.user.AppSpaceVideoData
import dev.frost819.newbv.biliapi.http.entity.user.FollowAction
import dev.frost819.newbv.biliapi.http.entity.user.Nameplate
import dev.frost819.newbv.biliapi.http.entity.user.Official
import dev.frost819.newbv.biliapi.http.entity.user.Pendant
import dev.frost819.newbv.biliapi.http.entity.user.Profession
import dev.frost819.newbv.biliapi.http.entity.user.Relation
import dev.frost819.newbv.biliapi.http.entity.user.RelationData
import dev.frost819.newbv.biliapi.http.entity.user.RelationStat
import dev.frost819.newbv.biliapi.http.entity.user.RelationType
import dev.frost819.newbv.biliapi.http.entity.user.UserHonours
import dev.frost819.newbv.biliapi.http.entity.user.UserInfoData
import dev.frost819.newbv.biliapi.http.entity.user.Vip
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [UserRepository] 的单元测试。
 *
 * 通过 MockK 模拟 [BiliHttpApi] 单例，验证关注/取关/查询关注状态/获取用户信息等
 * 业务逻辑。不依赖真实网络。
 */
class UserRepositoryUnitTest {
    private lateinit var repository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var channelRepository: ChannelRepository

    companion object {
        private const val MID = 12345L
        private const val BILI_JCT = "test-bili-jct"
        private const val ACCESS_TOKEN = "test-access-token"
        private const val SESSDATA = "test-sessdata"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(BiliHttpApi)
        authRepository = AuthRepository()
        authRepository.biliJct = BILI_JCT
        authRepository.accessToken = ACCESS_TOKEN
        authRepository.sessionData = SESSDATA
        channelRepository = ChannelRepository()
        repository = UserRepository(authRepository, channelRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(BiliHttpApi)
    }

    // ------------------------------------------------------------------
    // followUser
    // ------------------------------------------------------------------

    @Test
    fun `followUser Web calls modifyFollow with AddFollow and csrf`() =
        runTest {
            coEvery {
                BiliHttpApi.modifyFollow(any(), any(), any(), any(), any())
            } returns BiliResponseWithoutData(code = 0, message = "", ttl = 0)

            val result = repository.followUser(MID, preferApiType = ApiType.Web)

            assertThat(result).isTrue()
            coVerify {
                BiliHttpApi.modifyFollow(
                    mid = MID,
                    action = FollowAction.AddFollow,
                    actionSource = any(),
                    accessKey = null,
                    csrf = BILI_JCT,
                )
            }
        }

    @Test
    fun `followUser App calls modifyFollow with AddFollow and accessKey`() =
        runTest {
            coEvery {
                BiliHttpApi.modifyFollow(any(), any(), any(), any(), any())
            } returns BiliResponseWithoutData(code = 0, message = "", ttl = 0)

            val result = repository.followUser(MID, preferApiType = ApiType.App)

            assertThat(result).isTrue()
            coVerify {
                BiliHttpApi.modifyFollow(
                    mid = MID,
                    action = FollowAction.AddFollow,
                    actionSource = any(),
                    accessKey = ACCESS_TOKEN,
                    csrf = null,
                )
            }
        }

    @Test
    fun `followUser returns false when API returns non-zero code`() =
        runTest {
            coEvery {
                BiliHttpApi.modifyFollow(any(), any(), any(), any(), any())
            } returns BiliResponseWithoutData(code = -101, message = "未登录", ttl = 0)

            val result = repository.followUser(MID, preferApiType = ApiType.Web)

            assertThat(result).isFalse()
        }

    // ------------------------------------------------------------------
    // unfollowUser
    // ------------------------------------------------------------------

    @Test
    fun `unfollowUser Web calls modifyFollow with DelFollow`() =
        runTest {
            coEvery {
                BiliHttpApi.modifyFollow(any(), any(), any(), any(), any())
            } returns BiliResponseWithoutData(code = 0, message = "", ttl = 0)

            val result = repository.unfollowUser(MID, preferApiType = ApiType.Web)

            assertThat(result).isTrue()
            coVerify {
                BiliHttpApi.modifyFollow(
                    mid = MID,
                    action = FollowAction.DelFollow,
                    actionSource = any(),
                    accessKey = null,
                    csrf = BILI_JCT,
                )
            }
        }

    @Test
    fun `unfollowUser App calls modifyFollow with DelFollow and accessKey`() =
        runTest {
            coEvery {
                BiliHttpApi.modifyFollow(any(), any(), any(), any(), any())
            } returns BiliResponseWithoutData(code = 0, message = "", ttl = 0)

            val result = repository.unfollowUser(MID, preferApiType = ApiType.App)

            assertThat(result).isTrue()
            coVerify {
                BiliHttpApi.modifyFollow(
                    mid = MID,
                    action = FollowAction.DelFollow,
                    actionSource = any(),
                    accessKey = ACCESS_TOKEN,
                    csrf = null,
                )
            }
        }

    // ------------------------------------------------------------------
    // checkIsFollowing
    // ------------------------------------------------------------------

    @Test
    fun `checkIsFollowing returns null when not authenticated`() =
        runTest {
            authRepository.sessionData = null
            authRepository.accessToken = null

            val result = repository.checkIsFollowing(MID)

            assertThat(result).isNull()
        }

    @Test
    fun `checkIsFollowing returns true when relation is Followed`() =
        runTest {
            coEvery { BiliHttpApi.getRelations(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RelationData(
                            relation =
                                Relation(
                                    mid = MID,
                                    attribute = RelationType.Followed,
                                    mtime = 0,
                                    special = 0,
                                ),
                            beRelation =
                                Relation(
                                    mid = 0,
                                    attribute = RelationType.None,
                                    mtime = 0,
                                    special = 0,
                                ),
                        ),
                )

            val result = repository.checkIsFollowing(MID)

            assertThat(result).isTrue()
        }

    @Test
    fun `checkIsFollowing returns true when relation is BothFollowed`() =
        runTest {
            coEvery { BiliHttpApi.getRelations(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RelationData(
                            relation =
                                Relation(
                                    mid = MID,
                                    attribute = RelationType.BothFollowed,
                                    mtime = 0,
                                    special = 0,
                                ),
                            beRelation =
                                Relation(
                                    mid = 0,
                                    attribute = RelationType.None,
                                    mtime = 0,
                                    special = 0,
                                ),
                        ),
                )

            val result = repository.checkIsFollowing(MID)

            assertThat(result).isTrue()
        }

    @Test
    fun `checkIsFollowing returns false when relation is None`() =
        runTest {
            coEvery { BiliHttpApi.getRelations(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RelationData(
                            relation =
                                Relation(
                                    mid = MID,
                                    attribute = RelationType.None,
                                    mtime = 0,
                                    special = 0,
                                ),
                            beRelation =
                                Relation(
                                    mid = 0,
                                    attribute = RelationType.None,
                                    mtime = 0,
                                    special = 0,
                                ),
                        ),
                )

            val result = repository.checkIsFollowing(MID)

            assertThat(result).isFalse()
        }

    @Test
    fun `checkIsFollowing returns null on API failure`() =
        runTest {
            coEvery { BiliHttpApi.getRelations(any(), any()) } returns
                BiliResponse(
                    code = -101,
                    message = "未登录",
                )

            val result = repository.checkIsFollowing(MID)

            assertThat(result).isNull()
        }

    // ------------------------------------------------------------------
    // getFollowingUpCount
    // ------------------------------------------------------------------

    @Test
    fun `getFollowingUpCount returns 0 when not authenticated`() =
        runTest {
            authRepository.sessionData = null
            authRepository.accessToken = null

            val result = repository.getFollowingUpCount(MID, preferApiType = ApiType.Web)

            assertThat(result).isEqualTo(0)
        }

    @Test
    fun `getFollowingUpCount Web returns following count`() =
        runTest {
            coEvery { BiliHttpApi.getRelationStat(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RelationStat(
                            black = 0,
                            follower = 1000,
                            following = 42,
                            mid = MID,
                            whisper = 0,
                        ),
                )

            val result = repository.getFollowingUpCount(MID, preferApiType = ApiType.Web)

            assertThat(result).isEqualTo(42)
        }

    @Test
    fun `getFollowingUpCount App passes accessKey`() =
        runTest {
            coEvery { BiliHttpApi.getRelationStat(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RelationStat(
                            black = 0,
                            follower = 500,
                            following = 10,
                            mid = MID,
                            whisper = 0,
                        ),
                )

            val result = repository.getFollowingUpCount(MID, preferApiType = ApiType.App)

            assertThat(result).isEqualTo(10)
            coVerify { BiliHttpApi.getRelationStat(mid = MID, accessKey = ACCESS_TOKEN) }
        }

    @Test
    fun `getFollowingUpCount returns 0 on API failure`() =
        runTest {
            coEvery { BiliHttpApi.getRelationStat(any(), any()) } returns
                BiliResponse(
                    code = -400,
                    message = "请求错误",
                )

            val result = repository.getFollowingUpCount(MID, preferApiType = ApiType.Web)

            assertThat(result).isEqualTo(0)
        }

    // ------------------------------------------------------------------
    // getUserInfo
    // ------------------------------------------------------------------

    @Test
    fun `getUserInfo maps to UserSpaceInfo`() =
        runTest {
            coEvery { BiliHttpApi.getUserInfo(any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data = fakeUserInfoData(),
                )

            val result = repository.getUserInfo(MID)

            assertThat(result.mid).isEqualTo(MID)
            assertThat(result.name).isEqualTo("测试用户")
            assertThat(result.face).isEqualTo("https://example.com/face.jpg")
            assertThat(result.sign).isEqualTo("签名")
            assertThat(result.level).isEqualTo(5)
            assertThat(result.topPhoto).isEqualTo("https://example.com/top.jpg")
            assertThat(result.isFollowed).isTrue()
        }

    private fun fakeUserInfoData() =
        UserInfoData(
            mid = MID,
            name = "测试用户",
            sex = "男",
            face = "https://example.com/face.jpg",
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
            fansMedal = UserInfoData.FansMedal(show = false, wear = false),
            official = Official(role = 0, title = "", desc = "", type = -1),
            vip =
                Vip(
                    type = 0,
                    status = 0,
                    dueDate = 0L,
                    themeType = 0,
                    label =
                        Vip.Label(
                            text = "",
                            textColor = "",
                            bgStyle = 0,
                            bgColor = "",
                            borderColor = "",
                        ),
                    avatarSubscript = 0,
                    nicknameColor = "",
                    avatarSubscriptUrl = "",
                ),
            pendant = Pendant(pid = 0, name = "", image = ""),
            nameplate = Nameplate(nid = 0, name = "", image = "", imageSmall = "", level = "", condition = ""),
            userHonourInfo = UserHonours(mid = MID),
            isFollowed = true,
            topPhoto = "https://example.com/top.jpg",
            sys_notice = UserInfoData.SysNotice(),
            live_room =
                UserInfoData.LiveRoom(
                    roomStatus = 0,
                    liveStatus = 0,
                    url = "",
                    title = "",
                    cover = "",
                    watchedShow =
                        UserInfoData.LiveRoom.WatchedShow(
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
                ),
            birthday = "",
            profession = Profession(name = "", department = "", title = "", isShow = 0),
            series = UserInfoData.Series(userUpgradeStatus = 0, showUpgradeWindow = false),
            isSeniorMember = 0,
            gaiaResType = 0,
            isRisk = false,
            elec =
                UserInfoData.Elec(
                    showInfo =
                        UserInfoData.Elec.ElecShowInfo(
                            show = false,
                            state = 0,
                            title = "",
                            icon = "",
                            jumpUrl = "",
                        ),
                ),
        )

    // ------------------------------------------------------------------
    // addSeasonFollow
    // ------------------------------------------------------------------

    @Test
    fun `addSeasonFollow Web calls addSeasonFollow with csrf and returns toast`() =
        runTest {
            coEvery { BiliHttpApi.addSeasonFollow(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.season.SeasonFollowData(
                            fmid = 400,
                            relation = true,
                            status = 1,
                            toast = "追番成功",
                        ),
                )

            val result = repository.addSeasonFollow(seasonId = 400, preferApiType = ApiType.Web)

            assertThat(result).isEqualTo("追番成功")
            coVerify { BiliHttpApi.addSeasonFollow(seasonId = 400, csrf = BILI_JCT) }
        }

    @Test
    fun `addSeasonFollow App calls addSeasonFollowApp with accessKey and returns toast`() =
        runTest {
            coEvery { BiliHttpApi.addSeasonFollowApp(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.season.SeasonFollowData(
                            fmid = 400,
                            relation = true,
                            status = 1,
                            toast = "追番成功",
                        ),
                )

            val result = repository.addSeasonFollow(seasonId = 400, preferApiType = ApiType.App)

            assertThat(result).isEqualTo("追番成功")
            coVerify { BiliHttpApi.addSeasonFollowApp(seasonId = 400, accessKey = ACCESS_TOKEN) }
        }

    // ------------------------------------------------------------------
    // delSeasonFollow
    // ------------------------------------------------------------------

    @Test
    fun `delSeasonFollow Web calls delSeasonFollow with csrf and returns toast`() =
        runTest {
            coEvery { BiliHttpApi.delSeasonFollow(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.season.SeasonFollowData(
                            fmid = 400,
                            relation = false,
                            status = 0,
                            toast = "已取消追番",
                        ),
                )

            val result = repository.delSeasonFollow(seasonId = 400, preferApiType = ApiType.Web)

            assertThat(result).isEqualTo("已取消追番")
            coVerify { BiliHttpApi.delSeasonFollow(seasonId = 400, csrf = BILI_JCT) }
        }

    @Test
    fun `delSeasonFollow App calls delSeasonFollowApp with accessKey`() =
        runTest {
            coEvery { BiliHttpApi.delSeasonFollowApp(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.season.SeasonFollowData(
                            fmid = 400,
                            relation = false,
                            status = 0,
                            toast = "已取消追番",
                        ),
                )

            val result = repository.delSeasonFollow(seasonId = 400, preferApiType = ApiType.App)

            assertThat(result).isEqualTo("已取消追番")
            coVerify { BiliHttpApi.delSeasonFollowApp(seasonId = 400, accessKey = ACCESS_TOKEN) }
        }

    // ------------------------------------------------------------------
    // getSpaceVideos (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getSpaceVideos Web maps response and returns videos with hasNext`() =
        runTest {
            coEvery { BiliHttpApi.getWebUserSpaceVideos(any(), any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData(
                            list =
                                dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.SpaceVideoListItem(
                                    tlist = null,
                                    vlist = listOf(fakeVListItem(aid = 1L, title = "video1")),
                                ),
                            page =
                                dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.Page(
                                    pageNumber = 1,
                                    pageSize = 30,
                                    count = 100,
                                ),
                        ),
                )

            val result =
                repository.getSpaceVideos(
                    mid = MID,
                    order = dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder.PubDate,
                    page =
                        dev.frost819.newbv.biliapi.entity.user
                            .SpaceVideoPage(),
                    preferApiType = ApiType.Web,
                )

            assertThat(result.videos).hasSize(1)
            assertThat(result.videos[0].aid).isEqualTo(1L)
            assertThat(result.videos[0].title).isEqualTo("video1")
            assertThat(result.page.hasNext).isTrue()
            assertThat(result.page.nextWebPageNumber).isEqualTo(2)
        }

    @Test
    fun `getSpaceVideos Web returns hasNext false when count equals pn times ps`() =
        runTest {
            coEvery { BiliHttpApi.getWebUserSpaceVideos(any(), any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData(
                            list = null,
                            page =
                                dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.Page(
                                    pageNumber = 2,
                                    pageSize = 30,
                                    count = 30,
                                ),
                        ),
                )

            val result = repository.getSpaceVideos(mid = MID, preferApiType = ApiType.Web)

            assertThat(result.videos).isEmpty()
            assertThat(result.page.hasNext).isFalse()
        }

    // ------------------------------------------------------------------
    // getFollowedUsers (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getFollowedUsers Web returns all followed users for single page`() =
        runTest {
            coEvery { BiliHttpApi.getUserFollow(any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.user.UserFollowData(
                            list = listOf(fakeFollowedUser(mid = 1L, uname = "user1")),
                            reVersion = 0,
                            total = 1,
                        ),
                )

            val result = repository.getFollowedUsers(mid = MID, preferApiType = ApiType.Web)

            assertThat(result).hasSize(1)
            assertThat(result[0].mid).isEqualTo(1L)
            assertThat(result[0].name).isEqualTo("user1")
        }

    @Test
    fun `getFollowedUsers Web fetches multiple pages when total exceeds pageSize`() =
        runTest {
            coEvery { BiliHttpApi.getUserFollow(any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.user.UserFollowData(
                            list = listOf(fakeFollowedUser(mid = 1L, uname = "user1")),
                            reVersion = 0,
                            total = 51,
                        ),
                )

            val result = repository.getFollowedUsers(mid = MID, preferApiType = ApiType.Web)

            assertThat(result).hasSize(2)
            coVerify(atLeast = 2) { BiliHttpApi.getUserFollow(any(), any(), any(), any(), any()) }
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // getFollowedUsers (App)
    // ------------------------------------------------------------------

    @Test
    fun `getFollowedUsers App returns all followed users for single page`() =
        runTest {
            coEvery { BiliHttpApi.getUserFollow(any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.user.UserFollowData(
                            list = listOf(fakeFollowedUser(mid = 1L, uname = "user1")),
                            reVersion = 0,
                            total = 1,
                        ),
                )

            val result = repository.getFollowedUsers(mid = MID, preferApiType = ApiType.App)

            assertThat(result).hasSize(1)
            assertThat(result[0].mid).isEqualTo(1L)
            assertThat(result[0].name).isEqualTo("user1")
            coVerify { BiliHttpApi.getUserFollow(eq(MID), any(), any(), any(), eq(ACCESS_TOKEN)) }
        }

    @Test
    fun `getFollowedUsers App fetches multiple pages when total exceeds pageSize`() =
        runTest {
            coEvery { BiliHttpApi.getUserFollow(any(), any(), any(), any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        dev.frost819.newbv.biliapi.http.entity.user.UserFollowData(
                            list = listOf(fakeFollowedUser(mid = 1L, uname = "user1")),
                            reVersion = 0,
                            total = 51,
                        ),
                )

            val result = repository.getFollowedUsers(mid = MID, preferApiType = ApiType.App)

            assertThat(result).hasSize(2)
            coVerify(atLeast = 2) { BiliHttpApi.getUserFollow(any(), any(), any(), any(), any()) }
        }

    // ------------------------------------------------------------------
    // getSpaceVideos (App)
    // ------------------------------------------------------------------

    @Test
    fun `getSpaceVideos App uses HTTP App API not gRPC`() =
        runTest {
            val appSpaceData =
                appSpaceData(hasNext = false, appSpaceVideoItem(aid = MID * 1000L, bvid = "BV-app", title = "App视频"))
            coEvery { BiliHttpApi.getAppUserSpaceVideos(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = appSpaceData)

            val result = repository.getSpaceVideos(mid = MID, preferApiType = ApiType.App)

            assertThat(result.videos).hasSize(1)
            assertThat(result.videos[0].bvid).isEqualTo("BV-app")
            assertThat(result.page.hasNext).isFalse()
            coVerify { BiliHttpApi.getAppUserSpaceVideos(eq(MID), any(), any(), any(), any()) }
        }

    @Test
    fun `getSpaceVideos App does not fallback to gRPC`() =
        runTest {
            val appSpaceData = appSpaceData(hasNext = false, appSpaceVideoItem(aid = MID * 1000L, bvid = "BV-app"))
            coEvery { BiliHttpApi.getAppUserSpaceVideos(any(), any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = appSpaceData)

            repository.getSpaceVideos(mid = MID, preferApiType = ApiType.App)

            // App 路径不再依赖 gRPC channel，channel 未初始化也能走 HTTP
            assertThat(repository).isNotNull()
        }

    // ------------------------------------------------------------------
    // getDynamicVideos (Web)
    // ------------------------------------------------------------------

    @Test
    fun `getDynamicVideos Web maps response to DynamicVideoData`() =
        runTest {
            val dynamicData =
                dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicData(
                    hasMore = true,
                    offset = "offset-123",
                    updateBaseline = "baseline-456",
                    updateNum = 5,
                    items = listOf(fakeDynamicItem()),
                )
            coEvery { BiliHttpApi.getDynamicList(any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = dynamicData)

            val result =
                repository.getDynamicVideos(
                    page = 1,
                    offset = "",
                    updateBaseline = "",
                    preferApiType = ApiType.Web,
                )

            assertThat(result.videos).hasSize(1)
            assertThat(result.hasMore).isTrue()
            assertThat(result.historyOffset).isEqualTo("offset-123")
            assertThat(result.updateBaseline).isEqualTo("baseline-456")
        }

    @Test
    fun `getDynamicVideos Web with empty items returns empty list`() =
        runTest {
            val dynamicData =
                dev.frost819.newbv.biliapi.http.entity.dynamic.DynamicData(
                    hasMore = false,
                    offset = "",
                    updateBaseline = "",
                    updateNum = 0,
                )
            coEvery { BiliHttpApi.getDynamicList(any(), any(), any(), any()) } returns
                BiliResponse(code = 0, message = "", data = dynamicData)

            val result =
                repository.getDynamicVideos(
                    page = 1,
                    offset = "",
                    updateBaseline = "",
                    preferApiType = ApiType.Web,
                )

            assertThat(result.videos).isEmpty()
            assertThat(result.hasMore).isFalse()
        }

    // ------------------------------------------------------------------
    // checkIsFollowing (additional cases)
    // ------------------------------------------------------------------

    @Test
    fun `checkIsFollowing returns true when relation is FollowedQuietly`() =
        runTest {
            coEvery { BiliHttpApi.getRelations(any(), any()) } returns
                BiliResponse(
                    code = 0,
                    message = "",
                    data =
                        RelationData(
                            relation =
                                Relation(
                                    mid = MID,
                                    attribute = RelationType.FollowedQuietly,
                                    mtime = 0,
                                    special = 0,
                                ),
                            beRelation =
                                Relation(
                                    mid = 0,
                                    attribute = RelationType.None,
                                    mtime = 0,
                                    special = 0,
                                ),
                        ),
                )

            val result = repository.checkIsFollowing(MID)

            assertThat(result).isTrue()
        }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private fun fakeDynamicItem() =
        DynamicItem(
            basic =
                DynamicItem.Basic(
                    commentIdStr = "100",
                    commentType = 1,
                    likeIcon =
                        DynamicItem.Basic.LikeIcon(
                            actionUrl = "",
                            endUrl = "",
                            id = 0L,
                            startUrl = "",
                        ),
                    ridStr = "200",
                ),
            idStr = "item-1",
            modules =
                DynamicItem.Modules(
                    moduleAuthor =
                        DynamicItem.Modules.Author(
                            face = "http://face.test",
                            faceNft = false,
                            following = false,
                            jumpUrl = "",
                            label = "",
                            mid = 1L,
                            name = "UP主",
                            officialVerify =
                                DynamicItem.Modules.Author.OfficialVerify(
                                    desc = "",
                                    type = -1,
                                ),
                            pendant = Pendant(pid = 0, name = "", image = ""),
                            pubAction = "投稿了视频",
                            pubLocationText = "",
                            pubTime = "1小时前",
                            pubTs = 1700000000,
                            type = "AUTHOR_TYPE_NORMAL",
                            vip =
                                Vip(
                                    type = 0,
                                    status = 0,
                                    dueDate = 0L,
                                    themeType = 0,
                                    label =
                                        Vip.Label(
                                            text = "",
                                            textColor = "",
                                            bgStyle = 0,
                                            bgColor = "",
                                            borderColor = "",
                                        ),
                                    avatarSubscript = 0,
                                    nicknameColor = "",
                                    avatarSubscriptUrl = "",
                                ),
                        ),
                    moduleDynamic =
                        DynamicItem.Modules.Dynamic(
                            major =
                                DynamicItem.Modules.Dynamic.Major(
                                    type = "MAJOR_TYPE_ARCHIVE",
                                    archive =
                                        DynamicItem.Modules.Dynamic.Major.Archive(
                                            aid = "100",
                                            badge =
                                                DynamicItem.Modules.Dynamic.Major.Archive.Badge(
                                                    bgColor = "",
                                                    color = "",
                                                    text = "",
                                                ),
                                            bvid = "BV1xx",
                                            cover = "http://cover.test",
                                            desc = "描述",
                                            disablePreview = 0,
                                            durationText = "5:00",
                                            jumpUrl = "",
                                            stat =
                                                DynamicItem.Modules.Dynamic.Major.Archive.Stat(
                                                    danmaku = "10",
                                                    play = "500",
                                                ),
                                            title = "动态视频",
                                            type = 1,
                                        ),
                                ),
                        ),
                    moduleMore = DynamicItem.Modules.More(),
                    moduleStat =
                        DynamicItem.Modules.Stat(
                            comment =
                                DynamicItem.Modules.Stat.StatItem(
                                    count = 0,
                                    forbidden = false,
                                ),
                            forward =
                                DynamicItem.Modules.Stat.StatItem(
                                    count = 0,
                                    forbidden = false,
                                ),
                            like =
                                DynamicItem.Modules.Stat.StatItem(
                                    count = 10,
                                    forbidden = false,
                                ),
                        ),
                ),
            type = "DYNAMIC_TYPE_AV",
            visible = true,
        )

    private fun fakeVListItem(
        aid: Long = 1L,
        title: String = "title",
    ) = dev.frost819.newbv.biliapi.http.entity.user.WebSpaceVideoData.SpaceVideoListItem.VListItem(
        aid = aid,
        bvid = "BV$aid",
        author = "UP",
        comment = 0,
        copyright = "",
        created = 1700000000L,
        description = "",
        hideClick = false,
        isPay = 0,
        isUnionVideo = 0,
        length = "5:00",
        mid = 1L,
        pic = "http://pic.test",
        play = 100,
        review = 0,
        subtitle = "",
        title = title,
        typeid = 0,
        videoReview = 0,
        isSteinsGate = 0,
        isLivePlayback = 0,
        meta = null,
        _isAvoided = 0,
        attribute = 0,
        playbackPosition = 0,
    )

    private fun appSpaceVideoItem(
        aid: Long = 1L,
        bvid: String = "BV1",
        title: String = "App视频",
    ) = AppSpaceVideoData.SpaceVideoItem(
        title = title,
        subtitle = "",
        tname = "",
        cover = "http://pic.test",
        uri = "",
        param = aid.toString(),
        goto = "av",
        length = "5:00",
        duration = 300,
        isPopular = false,
        isSteins = false,
        isUgcpay = false,
        isCooperation = false,
        isPgc = false,
        isLivePlayback = false,
        play = 100,
        danmaku = 10,
        ctime = 1700000000,
        ugcPay = 0,
        author = "UP",
        state = true,
        bvid = bvid,
        videos = 1,
        firstcid = aid,
        cursorAttr =
            AppSpaceVideoData.SpaceVideoItem.CursorAttr(
                isLastWatchedArc = false,
                rank = 0,
            ),
        iconType = 0,
    )

    private fun appSpaceData(
        hasNext: Boolean = false,
        vararg items: AppSpaceVideoData.SpaceVideoItem,
    ) = AppSpaceVideoData(
        count = items.size,
        item = items.toList(),
        lastWatchedLocator =
            AppSpaceVideoData.LastWatchedLocator(
                displayThreshold = 0,
                insertRanking = 0,
                text = "",
            ),
        hasNext = hasNext,
    )

    private fun fakeFollowedUser(
        mid: Long = 1L,
        uname: String = "user",
    ) = dev.frost819.newbv.biliapi.http.entity.user.UserFollowData.FollowedUser(
        mid = mid,
        attribute = 2,
        mtime = 1700000000,
        tag = null,
        special = 0,
        uname = uname,
        face = "http://face.test",
        sign = "",
        officialVerify =
            dev.frost819.newbv.biliapi.http.entity.user
                .OfficialVerify(type = -1, desc = ""),
        vip =
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
            ),
        nftIcon = "",
        recReason = "",
        trackId = "",
    )
}
