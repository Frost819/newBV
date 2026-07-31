package dev.frost819.newbv.biliapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SPI 接口返回的 buvid 数据。
 *
 * 通过 `/x/frontend/finger/spi` 获取，包含 B 站注册的 buvid3 和 buvid4。
 * 投币等敏感接口要求 Cookie 中的 buvid3 是 B 站注册过的值。
 *
 * @property b3 buvid3，需存入 Cookie。
 * @property b4 buvid4。
 */
@Serializable
data class SpiData(
    @SerialName("b_3") val b3: String? = null,
    @SerialName("b_4") val b4: String? = null,
)

/**
 * SPI 获取结果。
 *
 * @property buvid3 B 站注册的 buvid3。
 * @property deviceCookies 设备 cookie 字符串（如 `buvid3=xxx; b_nut=100`）。
 */
data class SpiResult(
    val buvid3: String,
    val deviceCookies: String,
)
