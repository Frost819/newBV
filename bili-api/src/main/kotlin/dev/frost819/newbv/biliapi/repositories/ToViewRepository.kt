package dev.frost819.newbv.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.user.ToViewData
import dev.frost819.newbv.biliapi.http.BiliHttpApi

class ToViewRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val historyStub
        get() =
            runCatching {
                HistoryGrpcKt.HistoryCoroutineStub(channelRepository.requireDefaultChannel())
            }.getOrNull()

    private fun requireCsrf(): String =
        authRepository.biliJct?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("bili_jct is empty")

    private fun requireAccessToken(): String =
        authRepository.accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("access_token is empty")

    suspend fun getToView(
        cursor: Long,
        preferApiType: ApiType,
    ): ToViewData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi.getToView(
                        // viewAt = cursor,
                    ).getResponseData()
                ToViewData.fromToViewResponse(data)
            }

            ApiType.App -> {
                requireAccessToken()
                val reply =
                    historyStub?.cursorV2(
                        bilibili.app.interfaces.v1.cursorV2Req {
                            this.cursor = bilibili.app.interfaces.v1.cursor { max = cursor }
                            business = "toview"
                        },
                    ) ?: throw IllegalStateException("App gRPC history stub is not initialized")
                ToViewData.fromToViewResponse(reply)
            }
        }
    }

    suspend fun addToView(
        aid: Long,
        bvid: String? = null,
        preferApiType: ApiType = ApiType.Web,
    ) {
        val (success, message) =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.addToView(avid = aid, bvid = bvid, csrf = requireCsrf())
                ApiType.App ->
                    BiliHttpApi.addToViewWithAccessKey(
                        avid = aid,
                        bvid = bvid,
                        accessKey = requireAccessToken(),
                    )
            }
        if (!success) throw Exception("添加到稍后再看失败：$message")
    }

    suspend fun delToView(
        aid: Long,
        viewed: Boolean = false,
        preferApiType: ApiType = ApiType.Web,
    ) {
        val (success, message) =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.delToView(viewed = viewed, avid = aid, csrf = requireCsrf())
                ApiType.App ->
                    BiliHttpApi.delToViewWithAccessKey(
                        viewed = viewed,
                        avid = aid,
                        accessKey = requireAccessToken(),
                    )
            }
        if (!success) throw Exception("删除稍后再看失败：$message")
    }
}
