package dev.frost819.newbv.core.theme

import androidx.compose.material3.Typography as CommonTypography
import androidx.tv.material3.Typography as TvTypography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * 字体排版定义。
 *
 * 提供 TV Material3 与普通 Material3 两套 Typography。
 * TV 版本针对 10 英尺观看距离增大字号。
 */
object BVTypography {

    /** TV Material3 Typography（适配 TV 10 英尺观看距离）。 */
    val tv: TvTypography = TvTypography(
        displaySmall = TextStyle(fontSize = 28.sp),
        displayMedium = TextStyle(fontSize = 36.sp),
        displayLarge = TextStyle(fontSize = 44.sp),
        headlineSmall = TextStyle(fontSize = 22.sp),
        headlineMedium = TextStyle(fontSize = 26.sp),
        headlineLarge = TextStyle(fontSize = 30.sp),
        titleSmall = TextStyle(fontSize = 16.sp),
        titleMedium = TextStyle(fontSize = 18.sp),
        titleLarge = TextStyle(fontSize = 22.sp),
        bodySmall = TextStyle(fontSize = 14.sp),
        bodyMedium = TextStyle(fontSize = 16.sp),
        bodyLarge = TextStyle(fontSize = 18.sp),
        labelSmall = TextStyle(fontSize = 14.sp),
        labelMedium = TextStyle(fontSize = 15.sp),
        labelLarge = TextStyle(fontSize = 16.sp),
    )

    /** 普通 Material3 Typography（用于非 TV 组件，同样增大字号适配 TV）。 */
    val common: CommonTypography = CommonTypography(
        displaySmall = TextStyle(fontSize = 28.sp),
        displayMedium = TextStyle(fontSize = 36.sp),
        displayLarge = TextStyle(fontSize = 44.sp),
        headlineSmall = TextStyle(fontSize = 22.sp),
        headlineMedium = TextStyle(fontSize = 26.sp),
        headlineLarge = TextStyle(fontSize = 30.sp),
        titleSmall = TextStyle(fontSize = 16.sp),
        titleMedium = TextStyle(fontSize = 18.sp),
        titleLarge = TextStyle(fontSize = 22.sp),
        bodySmall = TextStyle(fontSize = 14.sp),
        bodyMedium = TextStyle(fontSize = 16.sp),
        bodyLarge = TextStyle(fontSize = 18.sp),
        labelSmall = TextStyle(fontSize = 14.sp),
        labelMedium = TextStyle(fontSize = 15.sp),
        labelLarge = TextStyle(fontSize = 16.sp),
    )
}
