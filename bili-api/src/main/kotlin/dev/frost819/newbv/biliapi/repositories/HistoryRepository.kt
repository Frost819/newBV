package dev.frost819.newbv.biliapi.repositories

import bilibili.app.interfaces.v1.HistoryGrpcKt
import bilibili.app.interfaces.v1.cursor
import bilibili.app.interfaces.v1.cursorV2Req
import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.entity.user.HistoryData
import dev.frost819.newbv.biliapi.http.BiliHttpApi

class HistoryRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) {
    private val historyStub
        get() =
            runCatching {
                HistoryGrpcKt.HistoryCoroutineStub(channelRepository.defaultChannel!!)
            }.getOrNull()

    suspend fun getHistories(
        cursor: Long,
        preferApiType: ApiType = ApiType.Web,
    ): HistoryData {
        return when (preferApiType) {
            ApiType.Web -> {
                val data =
                    BiliHttpApi.getHistories(
                        viewAt = cursor,
                        sessData = authRepository.sessionData!!,
                    ).getResponseData()
                HistoryData.fromHistoryResponse(data)
            }

            ApiType.App -> {
                val reply =
                    historyStub?.cursorV2(
                        cursorV2Req {
                            this.cursor =
                                cursor {
                                    max = cursor
                                }
                            business = "archive"
                        },
                    )
                HistoryData.fromHistoryResponse(reply!!)
            }
        }
    }
}
