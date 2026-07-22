package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.http.entity.video.OneClickTripleAction

class OneClickTripleActionRepository(private val authRepository: AuthRepository) {
    suspend fun sendVideoOneClickTripleAction(
        aid: Long,
        bvid: String? = null
    ): OneClickTripleAction? {
        val (success, message, data)
                = BiliHttpApi.sendVideoOneClickTripleAction(
            avid = aid,
            bvid = bvid, csrf = authRepository.biliJct ?: "",
            sessData = authRepository.sessionData!!,
        )
        if (!success) throw Exception("投币失败：$message")
        return data
    }
}