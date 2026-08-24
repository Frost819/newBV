package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi

class CoinRepository(private val authRepository: AuthRepository) {
    suspend fun checkVideoCoined(
        aid: Long,
        preferApiType: ApiType,
        bvid: String? = null,
    ): Boolean {
        return when (preferApiType) {
            ApiType.Web ->
                BiliHttpApi.checkVideoSentCoin(
                    avid = aid,
                    bvid = bvid,
                )

            ApiType.App ->
                BiliHttpApi.checkVideoSentCoin(
                    avid = aid,
                    bvid = bvid,
                    accessKey = authRepository.accessToken,
                )
        }
    }

    suspend fun sendVideoCoin(
        aid: Long,
        multiply: Int = 1,
        preferApiType: ApiType,
        bvid: String? = null,
    ) {
        val (success, message) =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.sendVideoCoin(
                        avid = aid,
                        bvid = bvid,
                        multiply = multiply,
                        csrf = authRepository.biliJct ?: "",
                    )

                ApiType.App ->
                    BiliHttpApi.sendVideoCoinApp(
                        avid = aid,
                        multiply = multiply,
                        accessKey = authRepository.accessToken ?: "",
                    )
            }
        if (!success) throw Exception(message)
    }
}
