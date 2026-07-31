package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliHttpApi

class CoinRepository(private val authRepository: AuthRepository) {
    suspend fun checkVideoCoined(
        aid: Long,
        bvid: String? = null,
    ): Boolean {
        val like =
            BiliHttpApi.checkVideoSentCoin(
                avid = aid,
                bvid = bvid,
            )
        return like
    }

    suspend fun sendVideoCoin(
        aid: Long,
        bvid: String? = null,
        multiply: Int = 1,
    ) {
        val csrf = authRepository.biliJct ?: ""
        val (success, message) =
            BiliHttpApi.sendVideoCoin(
                avid = aid,
                bvid = bvid,
                multiply = multiply,
                csrf = csrf,
            )
        if (!success) throw Exception(message)
    }
}
