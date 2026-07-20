package dev.frost819.newbv.core.theme

import androidx.compose.material3.Typography as CommonTypography
import androidx.tv.material3.Typography as TvTypography

/**
 * 字体排版定义。
 *
 * 提供 TV Material3 与普通 Material3 两套 Typography。
 * 当前使用默认值，后续可按需自定义字号/字重。
 */
object BVTypography {

    /** TV Material3 Typography。 */
    val tv: TvTypography = TvTypography()

    /** 普通 Material3 Typography（用于非 TV 组件）。 */
    val common: CommonTypography = CommonTypography()
}
