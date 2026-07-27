package dev.frost819.newbv.biliapi.http.entity.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户头像挂件
 *
 * @param urlImageAniCut
 */
@Serializable
data class UserGarb(
    @SerialName("url_image_ani_cut")
    val urlImageAniCut: String,
)
