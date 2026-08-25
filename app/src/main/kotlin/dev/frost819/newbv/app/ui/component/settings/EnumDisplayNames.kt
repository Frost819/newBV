package dev.frost819.newbv.app.ui.component.settings

import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.ThemeMode
import dev.frost819.newbv.data.datastore.VideoCodec

/** 画质显示名称。 */
val Resolution.displayName: String
    get() = when (this) {
        Resolution.R240P -> "240P"
        Resolution.R360P -> "360P"
        Resolution.R480P -> "480P"
        Resolution.R720P -> "720P"
        Resolution.R720P60 -> "720P60"
        Resolution.R1080P -> "1080P"
        Resolution.R1080PPlus -> "1080P+"
        Resolution.R1080P60 -> "1080P60"
        Resolution.R4K -> "4K"
        Resolution.RHdr -> "HDR"
        Resolution.RDolby -> "Dolby"
        Resolution.R8K -> "8K"
    }

/** 视频编码显示名称。 */
val VideoCodec.displayName: String
    get() = when (this) {
        VideoCodec.AVC -> "AVC/H.264"
        VideoCodec.HEVC -> "HEVC/H.265"
        VideoCodec.AV1 -> "AV1"
        VideoCodec.DVH1 -> "DV H.1"
    }

/** 音频编码显示名称。 */
val Audio.displayName: String
    get() = when (this) {
        Audio.A64K -> "64K"
        Audio.A132K -> "132K"
        Audio.A192K -> "192K"
        Audio.ADolbyAtoms -> "Dolby Atmos"
        Audio.AHiRes -> "Hi-Res"
    }

/** 播放速度显示名称。 */
val PlaySpeed.displayName: String
    get() = when (this) {
        PlaySpeed.X0_5 -> "0.5x"
        PlaySpeed.X1 -> "1x"
        PlaySpeed.X1_25 -> "1.25x"
        PlaySpeed.X1_5 -> "1.5x"
        PlaySpeed.X2 -> "2x"
    }

/** 播放结束动作显示名称。 */
val ActionAfterPlay.displayName: String
    get() = when (this) {
        ActionAfterPlay.Pause -> "暂停"
        ActionAfterPlay.PlayNext -> "播放下一集"
        ActionAfterPlay.Exit -> "退出播放器"
        ActionAfterPlay.PlayRelated -> "播放首个相关视频"
    }

/** 接口类型显示名称。 */
val ApiType.displayName: String
    get() = when (this) {
        ApiType.Web -> "Web"
        ApiType.App -> "App"
    }

/** 主题模式显示名称。 */
val ThemeMode.displayName: String
    get() = when (this) {
        ThemeMode.FollowSystem -> "跟随系统"
        ThemeMode.Dark -> "深色"
        ThemeMode.Light -> "浅色"
    }

/** 首页 Tab 显示名称。 */
val HomeTopNavItem.displayName: String
    get() = when (this) {
        HomeTopNavItem.Dynamics -> "动态"
        HomeTopNavItem.Recommend -> "推荐"
        HomeTopNavItem.Popular -> "热门"
    }

/** 个人页 Tab 显示名称。 */
val PersonalTopNavItem.displayName: String
    get() = when (this) {
        PersonalTopNavItem.ToView -> "稍后再看"
        PersonalTopNavItem.History -> "历史"
        PersonalTopNavItem.Favorite -> "收藏"
        PersonalTopNavItem.FollowingSeason -> "追番"
    }
