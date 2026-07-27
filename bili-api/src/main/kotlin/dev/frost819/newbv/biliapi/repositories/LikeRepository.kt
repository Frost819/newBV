package dev.frost819.newbv.biliapi.repositories

import dev.frost819.newbv.biliapi.http.BiliHttpApi

class LikeRepository(private val authRepository: AuthRepository) {
    suspend fun checkVideoLiked(
        aid: Long,
        bvid: String? = null,
    ): Boolean {
        val like =
            BiliHttpApi.checkVideoLiked(
                avid = aid,
                bvid = bvid,
                sessData = authRepository.sessionData!!,
            )
        return like
    }

    suspend fun updateVideoLiked(
        aid: Long,
        bvid: String? = null,
        like: Boolean,
    ) {
        val (success, message) =
            BiliHttpApi.sendVideoLike(
                avid = aid,
                bvid = bvid,
                like = like,
                csrf = authRepository.biliJct ?: "",
                sessData = authRepository.sessionData!!,
            )
        if (!success) {
            throw Exception("点赞失败: $message")
        }
    }
}
