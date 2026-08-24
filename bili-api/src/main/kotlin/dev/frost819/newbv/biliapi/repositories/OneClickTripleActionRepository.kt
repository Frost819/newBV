package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.entity.ApiType
import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.video.OneClickTripleAction

class OneClickTripleActionRepository(private val authRepository: AuthRepository) {
    suspend fun sendVideoOneClickTripleAction(
        aid: Long,
        preferApiType: ApiType,
        bvid: String? = null,
    ): OneClickTripleAction? {
        val (success, message, data) =
            when (preferApiType) {
                ApiType.Web ->
                    BiliHttpApi.sendVideoOneClickTripleAction(
                        avid = aid,
                        bvid = bvid,
                        csrf = authRepository.biliJct ?: "",
                    )

                ApiType.App ->
                    BiliHttpApi.sendVideoOneClickTripleActionApp(
                        avid = aid,
                        accessKey = authRepository.accessToken ?: "",
                    )
            }
        if (!success) throw Exception(message)
        return data
    }
}
