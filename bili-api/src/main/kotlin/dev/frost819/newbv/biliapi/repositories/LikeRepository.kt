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
            )
        if (!success) {
            throw Exception(message)
        }
    }
}
