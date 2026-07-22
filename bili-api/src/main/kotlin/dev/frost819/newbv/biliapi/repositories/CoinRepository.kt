package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliHttpApi

class CoinRepository(private val authRepository: AuthRepository) {
    suspend fun checkVideoCoined(
        aid: Long,
        bvid: String? = null,
    ): Boolean {
        val like = BiliHttpApi.checkVideoSentCoin(
            avid = aid,
            bvid = bvid,
            sessData = authRepository.sessionData!!
        )
        return like
    }

    suspend fun sendVideoCoin(
        aid: Long,
        bvid: String? = null,
        multiply: Int = 1,
    ) {
        val (success, message) = BiliHttpApi.sendVideoCoin(
            avid = aid,
            bvid = bvid,
            multiply = multiply,
            csrf = authRepository.biliJct ?: "",
            sessData = authRepository.sessionData!!,
            buvid3 = authRepository.buvid3!!,
        )
        if (!success) throw Exception("投币失败：$message")
    }
}