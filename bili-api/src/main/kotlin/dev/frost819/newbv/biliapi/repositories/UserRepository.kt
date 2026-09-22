package dev.frost819.newbv.biliapi.repositories

import bilibili.app.dynamic.v2.DynamicGrpcKt
import bilibili.app.dynamic.v2.Refresh
import bilibili.app.dynamic.v2.dynVideoReq
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.user.DynamicVideoData
import dev.frost819.newbv.biliapi.entity.user.FollowedUser
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoData
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoOrder
import dev.frost819.newbv.biliapi.entity.user.SpaceVideoPage
import dev.frost819.newbv.biliapi.entity.user.UserSpaceInfo
import dev.frost819.newbv.biliapi.grpc.utils.handleGrpcException
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.user.FollowAction
import dev.frost819.newbv.biliapi.http.entity.user.FollowActionSource
import dev.frost819.newbv.biliapi.http.entity.user.RelationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.ceil

class UserRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val dynamicStub
        get() =
            runCatching {
                DynamicGrpcKt.DynamicCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    private suspend fun modifyFollow(
        mid: Long,
        action: FollowAction,
        preferApiType: ApiType,
    ): Boolean {
        val response =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.modifyFollow(
                        mid = mid,
                        action = action,
                        actionSource = FollowActionSource.Space,
                        csrf = authRepository.biliJct,
                    )

                ApiType.App ->
                    BiliHttpApi.modifyFollow(
                        mid = mid,
                        action = action,
                        actionSource = FollowActionSource.Space,
                        accessKey = authRepository.accessToken,
                    )
            }
        return response.code == 0
    }

    suspend fun followUser(
        mid: Long,
        preferApiType: ApiType,
    ): Boolean = modifyFollow(mid, FollowAction.AddFollow, preferApiType)

    suspend fun unfollowUser(
        mid: Long,
        preferApiType: ApiType,
    ): Boolean = modifyFollow(mid, FollowAction.DelFollow, preferApiType)

    suspend fun checkIsFollowing(mid: Long): Boolean? {
        if (authRepository.sessionData == null && authRepository.accessToken == null) return null
        return runCatching {
            val response = BiliHttpApi.getRelations(mid = mid).getResponseData()
            listOf(
                RelationType.Followed,
                RelationType.FollowedQuietly,
                RelationType.BothFollowed,
            ).contains(response.relation.attribute)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    suspend fun getFollowingUpCount(
        mid: Long,
        preferApiType: ApiType,
    ): Int {
        if (authRepository.sessionData == null && authRepository.accessToken == null) return 0
        return runCatching {
            val response =
                when (preferApiType) {
                    ApiType.Web ->
                        BiliHttpApi.getRelationStat(mid = mid)
                    ApiType.App ->
                        BiliHttpApi.getRelationStat(
                            mid = mid,
                            accessKey = authRepository.accessToken,
                        )
                }.getResponseData()
            response.following
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: 0
    }

    suspend fun addSeasonFollow(
        seasonId: Int,
        preferApiType: ApiType,
    ): String =
        when (preferApiType) {
            ApiType.Web ->
                BiliHttpApi
                    .addSeasonFollow(
                        seasonId = seasonId,
                        csrf = authRepository.biliJct!!,
                    ).getResponseData()
                    .toast

            ApiType.App ->
                BiliHttpApi
                    .addSeasonFollowApp(
                        seasonId = seasonId,
                        accessKey = authRepository.accessToken ?: "",
                    ).getResponseData()
                    .toast
        }

    suspend fun delSeasonFollow(
        seasonId: Int,
        preferApiType: ApiType,
    ): String =
        when (preferApiType) {
            ApiType.Web ->
                BiliHttpApi
                    .delSeasonFollow(
                        seasonId = seasonId,
                        csrf = authRepository.biliJct!!,
                    ).getResponseData()
                    .toast

            ApiType.App ->
                BiliHttpApi
                    .delSeasonFollowApp(
                        seasonId = seasonId,
                        accessKey = authRepository.accessToken ?: "",
                    ).getResponseData()
                    .toast
        }

    /**
     * 获取用户空间信息（昵称、头像、签名、等级、关注状态等）。
     *
     * 对应接口：`GET /x/space/acc/info`
     */
    suspend fun getUserInfo(mid: Long): UserSpaceInfo {
        val response = BiliHttpApi.getUserInfo(uid = mid).getResponseData()
        return UserSpaceInfo.fromUserInfoData(response)
    }

    suspend fun getSpaceVideos(
        mid: Long,
        order: SpaceVideoOrder = SpaceVideoOrder.PubDate,
        page: SpaceVideoPage = SpaceVideoPage(),
        preferApiType: ApiType,
    ): SpaceVideoData =
        when (preferApiType) {
            ApiType.Web -> {
                val webSpaceVideoData =
                    BiliHttpApi
                        .getWebUserSpaceVideos(
                            mid = mid,
                            order = order.value,
                            pageNumber = page.nextWebPageNumber,
                            pageSize = page.nextWebPageSize,
                        ).getResponseData()
                SpaceVideoData.fromWebSpaceVideoData(webSpaceVideoData)
            }

            ApiType.App -> {
                val appSpaceVideoData =
                    BiliHttpApi
                        .getAppUserSpaceVideos(
                            mid = mid,
                            lastAvid = page.lastAvid,
                            order = order.value,
                            ts = System.currentTimeMillis(),
                            accessKey = authRepository.accessToken ?: "",
                        ).getResponseData()
                SpaceVideoData.fromAppSpaceVideoData(appSpaceVideoData)
            }
        }

    suspend fun getDynamicVideos(
        page: Int,
        offset: String,
        updateBaseline: String,
        preferApiType: ApiType,
    ): DynamicVideoData =
        when (preferApiType) {
            ApiType.Web -> {
                val responseData =
                    BiliHttpApi
                        .getDynamicList(
                            type = "video",
                            page = page,
                            offset = offset,
                        ).getResponseData()
                DynamicVideoData.fromDynamicData(responseData)
            }

            ApiType.App -> {
                // App 通道未就绪时给出明确异常，而不是在 !! 处抛 NPE，
                // 便于上层统一处理为错误状态并提供重试
                val stub = dynamicStub ?: throw IllegalStateException("App gRPC channel is not initialized")
                runCatching {
                    stub.dynVideo(
                        dynVideoReq {
                            this.page = page
                            this.offset = offset
                            this.updateBaseline = updateBaseline
                            localTime = 8
                            refreshType =
                                if (offset == "") Refresh.refresh_new else Refresh.refresh_history
                        },
                    )
                }.getOrElse { handleGrpcException(it) }
                    .let { DynamicVideoData.fromDynamicData(it) }
            }
        }

    suspend fun getFollowedUsers(
        mid: Long,
        preferApiType: ApiType,
    ): List<FollowedUser> =
        when (preferApiType) {
            ApiType.Web -> {
                val result = mutableListOf<FollowedUser>()
                val firstResponse =
                    BiliHttpApi
                        .getUserFollow(
                            mid = mid,
                        ).getResponseData()
                val userCount = firstResponse.total
                val pageCount = ceil((userCount.toFloat() / 50)).toInt()
                result.addAll(firstResponse.list.map { FollowedUser.fromHttpFollowedUser(it) })
                withContext(Dispatchers.IO) {
                    (2..pageCount)
                        .map { pageNumber ->
                            async {
                                BiliHttpApi
                                    .getUserFollow(
                                        mid = mid,
                                        pageNumber = pageNumber,
                                    ).getResponseData()
                            }
                        }.awaitAll()
                        .forEach { userFollowData ->
                            result.addAll(userFollowData.list.map { FollowedUser.fromHttpFollowedUser(it) })
                        }
                }
                result
            }

            ApiType.App -> {
                val result = mutableListOf<FollowedUser>()
                val firstResponse =
                    BiliHttpApi
                        .getUserFollow(
                            mid = mid,
                            accessKey = authRepository.accessToken,
                        ).getResponseData()
                val userCount = firstResponse.total
                val pageCount = ceil((userCount.toFloat() / 50)).toInt()
                result.addAll(firstResponse.list.map { FollowedUser.fromHttpFollowedUser(it) })
                withContext(Dispatchers.IO) {
                    (2..pageCount)
                        .map { pageNumber ->
                            async {
                                BiliHttpApi
                                    .getUserFollow(
                                        mid = mid,
                                        pageNumber = pageNumber,
                                        accessKey = authRepository.accessToken,
                                    ).getResponseData()
                            }
                        }.awaitAll()
                        .forEach { userFollowData ->
                            result.addAll(userFollowData.list.map { FollowedUser.fromHttpFollowedUser(it) })
                        }
                }
                result
            }
        }
}
