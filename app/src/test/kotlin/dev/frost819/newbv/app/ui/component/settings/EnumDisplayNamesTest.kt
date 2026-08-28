package dev.frost819.newbv.app.ui.component.settings

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.ActionAfterPlay
import dev.frost819.newbv.data.datastore.ApiType
import dev.frost819.newbv.data.datastore.Audio
import dev.frost819.newbv.data.datastore.HomeTopNavItem
import dev.frost819.newbv.data.datastore.PersonalTopNavItem
import dev.frost819.newbv.data.datastore.PlaySpeed
import dev.frost819.newbv.data.datastore.Resolution
import dev.frost819.newbv.data.datastore.ThemeMode
import dev.frost819.newbv.data.datastore.VideoCodec
import org.junit.jupiter.api.Test

/**
 * 设置枚举 displayName 扩展属性的单元测试。
 */
class EnumDisplayNamesTest {
    @Test
    fun resolution_displayNames_allNonEmpty() {
        Resolution.entries.forEach { resolution ->
            assertThat(resolution.displayName).isNotEmpty()
        }
    }

    @Test
    fun resolution_displayNames_knownValues() {
        assertThat(Resolution.R1080P.displayName).isEqualTo("1080P")
        assertThat(Resolution.R4K.displayName).isEqualTo("4K")
        assertThat(Resolution.R8K.displayName).isEqualTo("8K")
        assertThat(Resolution.RHdr.displayName).isEqualTo("HDR")
    }

    @Test
    fun videoCodec_displayNames_allNonEmpty() {
        VideoCodec.entries.forEach { codec ->
            assertThat(codec.displayName).isNotEmpty()
        }
    }

    @Test
    fun videoCodec_displayNames_knownValues() {
        assertThat(VideoCodec.AVC.displayName).isEqualTo("AVC/H.264")
        assertThat(VideoCodec.HEVC.displayName).isEqualTo("HEVC/H.265")
        assertThat(VideoCodec.AV1.displayName).isEqualTo("AV1")
    }

    @Test
    fun audio_displayNames_allNonEmpty() {
        Audio.entries.forEach { audio ->
            assertThat(audio.displayName).isNotEmpty()
        }
    }

    @Test
    fun audio_displayNames_knownValues() {
        assertThat(Audio.A192K.displayName).isEqualTo("192K")
        assertThat(Audio.ADolbyAtoms.displayName).isEqualTo("Dolby Atmos")
        assertThat(Audio.AHiRes.displayName).isEqualTo("Hi-Res")
    }

    @Test
    fun playSpeed_displayNames_allNonEmpty() {
        PlaySpeed.entries.forEach { speed ->
            assertThat(speed.displayName).isNotEmpty()
        }
    }

    @Test
    fun playSpeed_displayNames_knownValues() {
        assertThat(PlaySpeed.X0_5.displayName).isEqualTo("0.5x")
        assertThat(PlaySpeed.X1.displayName).isEqualTo("1x")
        assertThat(PlaySpeed.X2.displayName).isEqualTo("2x")
    }

    @Test
    fun actionAfterPlay_displayNames_allNonEmpty() {
        ActionAfterPlay.entries.forEach { action ->
            assertThat(action.displayName).isNotEmpty()
        }
    }

    @Test
    fun actionAfterPlay_displayNames_knownValues() {
        assertThat(ActionAfterPlay.Pause.displayName).isEqualTo("暂停")
        assertThat(ActionAfterPlay.PlayNext.displayName).isEqualTo("播放下一集")
        assertThat(ActionAfterPlay.Exit.displayName).isEqualTo("退出播放器")
        assertThat(ActionAfterPlay.PlayRelated.displayName).isEqualTo("播放首个相关视频")
    }

    @Test
    fun apiType_displayNames_allNonEmpty() {
        ApiType.entries.forEach { apiType ->
            assertThat(apiType.displayName).isNotEmpty()
        }
    }

    @Test
    fun apiType_displayNames_knownValues() {
        assertThat(ApiType.Web.displayName).isEqualTo("Web")
        assertThat(ApiType.App.displayName).isEqualTo("App")
    }

    @Test
    fun themeMode_displayNames_allNonEmpty() {
        ThemeMode.entries.forEach { mode ->
            assertThat(mode.displayName).isNotEmpty()
        }
    }

    @Test
    fun themeMode_displayNames_knownValues() {
        assertThat(ThemeMode.FollowSystem.displayName).isEqualTo("跟随系统")
        assertThat(ThemeMode.Dark.displayName).isEqualTo("深色")
        assertThat(ThemeMode.Light.displayName).isEqualTo("浅色")
    }

    @Test
    fun homeTopNavItem_displayNames_allNonEmpty() {
        HomeTopNavItem.entries.forEach { item ->
            assertThat(item.displayName).isNotEmpty()
        }
    }

    @Test
    fun homeTopNavItem_displayNames_knownValues() {
        assertThat(HomeTopNavItem.Dynamics.displayName).isEqualTo("动态")
        assertThat(HomeTopNavItem.Recommend.displayName).isEqualTo("推荐")
        assertThat(HomeTopNavItem.Popular.displayName).isEqualTo("热门")
    }

    @Test
    fun personalTopNavItem_displayNames_allNonEmpty() {
        PersonalTopNavItem.entries.forEach { item ->
            assertThat(item.displayName).isNotEmpty()
        }
    }

    @Test
    fun personalTopNavItem_displayNames_knownValues() {
        assertThat(PersonalTopNavItem.ToView.displayName).isEqualTo("稍后再看")
        assertThat(PersonalTopNavItem.History.displayName).isEqualTo("历史")
        assertThat(PersonalTopNavItem.Favorite.displayName).isEqualTo("收藏")
        assertThat(PersonalTopNavItem.FollowingSeason.displayName).isEqualTo("追番")
    }
}
