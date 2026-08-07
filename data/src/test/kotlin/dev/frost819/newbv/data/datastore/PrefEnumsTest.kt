package dev.frost819.newbv.data.datastore

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [PrefEnums] 中所有枚举 companion 方法的单元测试。
 *
 * 补充 [PrefsTest] 仅测试无效输入的不足，覆盖全部有效路径。
 */
class PrefEnumsTest {

    // ===== ApiType =====

    @Test
    fun `ApiType fromOrdinal returns Web for 0`() {
        assertThat(ApiType.fromOrdinal(0)).isEqualTo(ApiType.Web)
    }

    @Test
    fun `ApiType fromOrdinal returns App for 1`() {
        assertThat(ApiType.fromOrdinal(1)).isEqualTo(ApiType.App)
    }

    // ===== Resolution =====

    @Test
    fun `Resolution fromCode returns R240P for code 6`() {
        assertThat(Resolution.fromCode(6)).isEqualTo(Resolution.R240P)
    }

    @Test
    fun `Resolution fromCode returns R360P for code 16`() {
        assertThat(Resolution.fromCode(16)).isEqualTo(Resolution.R360P)
    }

    @Test
    fun `Resolution fromCode returns R480P for code 32`() {
        assertThat(Resolution.fromCode(32)).isEqualTo(Resolution.R480P)
    }

    @Test
    fun `Resolution fromCode returns R720P for code 64`() {
        assertThat(Resolution.fromCode(64)).isEqualTo(Resolution.R720P)
    }

    @Test
    fun `Resolution fromCode returns R720P60 for code 74`() {
        assertThat(Resolution.fromCode(74)).isEqualTo(Resolution.R720P60)
    }

    @Test
    fun `Resolution fromCode returns R1080P for code 80`() {
        assertThat(Resolution.fromCode(80)).isEqualTo(Resolution.R1080P)
    }

    @Test
    fun `Resolution fromCode returns R1080PPlus for code 112`() {
        assertThat(Resolution.fromCode(112)).isEqualTo(Resolution.R1080PPlus)
    }

    @Test
    fun `Resolution fromCode returns R1080P60 for code 116`() {
        assertThat(Resolution.fromCode(116)).isEqualTo(Resolution.R1080P60)
    }

    @Test
    fun `Resolution fromCode returns R4K for code 120`() {
        assertThat(Resolution.fromCode(120)).isEqualTo(Resolution.R4K)
    }

    @Test
    fun `Resolution fromCode returns RHdr for code 125`() {
        assertThat(Resolution.fromCode(125)).isEqualTo(Resolution.RHdr)
    }

    @Test
    fun `Resolution fromCode returns RDolby for code 126`() {
        assertThat(Resolution.fromCode(126)).isEqualTo(Resolution.RDolby)
    }

    @Test
    fun `Resolution fromCode returns R8K for code 127`() {
        assertThat(Resolution.fromCode(127)).isEqualTo(Resolution.R8K)
    }

    // ===== VideoCodec =====

    @Test
    fun `VideoCodec fromCode returns AVC for ordinal 0`() {
        assertThat(VideoCodec.fromCode(0)).isEqualTo(VideoCodec.AVC)
    }

    @Test
    fun `VideoCodec fromCode returns HEVC for ordinal 1`() {
        assertThat(VideoCodec.fromCode(1)).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `VideoCodec fromCode returns AV1 for ordinal 2`() {
        assertThat(VideoCodec.fromCode(2)).isEqualTo(VideoCodec.AV1)
    }

    @Test
    fun `VideoCodec fromCode returns DVH1 for ordinal 3`() {
        assertThat(VideoCodec.fromCode(3)).isEqualTo(VideoCodec.DVH1)
    }

    @Test
    fun `VideoCodec fromCodecString matches AVC for avc1 prefix`() {
        assertThat(VideoCodec.fromCodecString("avc1.640028")).isEqualTo(VideoCodec.AVC)
    }

    @Test
    fun `VideoCodec fromCodecString matches HEVC for hev1 prefix`() {
        assertThat(VideoCodec.fromCodecString("hev1.1.6.L120.B0")).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `VideoCodec fromCodecString matches HEVC for hvc1 alternate prefix`() {
        assertThat(VideoCodec.fromCodecString("hvc1.1.6.L153.90")).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `VideoCodec fromCodecString matches AV1 for av01 prefix`() {
        assertThat(VideoCodec.fromCodecString("av01.0.12M.08")).isEqualTo(VideoCodec.AV1)
    }

    @Test
    fun `VideoCodec fromCodecString matches DVH1 for dvh1 prefix`() {
        assertThat(VideoCodec.fromCodecString("dvh1.05.01")).isEqualTo(VideoCodec.DVH1)
    }

    @Test
    fun `VideoCodec fromCodecString returns null for empty string`() {
        assertThat(VideoCodec.fromCodecString("")).isNull()
    }

    @Test
    fun `VideoCodec fromCodecId returns AVC for id 7`() {
        assertThat(VideoCodec.fromCodecId(7)).isEqualTo(VideoCodec.AVC)
    }

    @Test
    fun `VideoCodec fromCodecId returns HEVC for id 12`() {
        assertThat(VideoCodec.fromCodecId(12)).isEqualTo(VideoCodec.HEVC)
    }

    @Test
    fun `VideoCodec fromCodecId returns AV1 for id 13`() {
        assertThat(VideoCodec.fromCodecId(13)).isEqualTo(VideoCodec.AV1)
    }

    @Test
    fun `VideoCodec fromCodecId returns DVH1 for id 0`() {
        assertThat(VideoCodec.fromCodecId(0)).isEqualTo(VideoCodec.DVH1)
    }

    // ===== ActionAfterPlay =====

    @Test
    fun `ActionAfterPlay fromCode returns Pause for 0`() {
        assertThat(ActionAfterPlay.fromCode(0)).isEqualTo(ActionAfterPlay.Pause)
    }

    @Test
    fun `ActionAfterPlay fromCode returns PlayNext for 1`() {
        assertThat(ActionAfterPlay.fromCode(1)).isEqualTo(ActionAfterPlay.PlayNext)
    }

    @Test
    fun `ActionAfterPlay fromCode returns Exit for 2`() {
        assertThat(ActionAfterPlay.fromCode(2)).isEqualTo(ActionAfterPlay.Exit)
    }

    @Test
    fun `ActionAfterPlay fromCode returns PlayRelated for 3`() {
        assertThat(ActionAfterPlay.fromCode(3)).isEqualTo(ActionAfterPlay.PlayRelated)
    }

    // ===== Audio =====

    @Test
    fun `Audio fromCode returns A64K for 30216`() {
        assertThat(Audio.fromCode(30216)).isEqualTo(Audio.A64K)
    }

    @Test
    fun `Audio fromCode returns A132K for 30232`() {
        assertThat(Audio.fromCode(30232)).isEqualTo(Audio.A132K)
    }

    @Test
    fun `Audio fromCode returns A192K for 30280`() {
        assertThat(Audio.fromCode(30280)).isEqualTo(Audio.A192K)
    }

    @Test
    fun `Audio fromCode returns ADolbyAtoms for 30250`() {
        assertThat(Audio.fromCode(30250)).isEqualTo(Audio.ADolbyAtoms)
    }

    @Test
    fun `Audio fromCode returns AHiRes for 30251`() {
        assertThat(Audio.fromCode(30251)).isEqualTo(Audio.AHiRes)
    }

    // ===== PlaySpeed =====

    @Test
    fun `PlaySpeed fromCode returns X0_5 for 0`() {
        assertThat(PlaySpeed.fromCode(0)).isEqualTo(PlaySpeed.X0_5)
    }

    @Test
    fun `PlaySpeed fromCode returns X1 for 1`() {
        assertThat(PlaySpeed.fromCode(1)).isEqualTo(PlaySpeed.X1)
    }

    @Test
    fun `PlaySpeed fromCode returns X1_25 for 2`() {
        assertThat(PlaySpeed.fromCode(2)).isEqualTo(PlaySpeed.X1_25)
    }

    @Test
    fun `PlaySpeed fromCode returns X1_5 for 3`() {
        assertThat(PlaySpeed.fromCode(3)).isEqualTo(PlaySpeed.X1_5)
    }

    @Test
    fun `PlaySpeed fromCode returns X2 for 4`() {
        assertThat(PlaySpeed.fromCode(4)).isEqualTo(PlaySpeed.X2)
    }

    @Test
    fun `PlaySpeed fromSpeed returns X0_5 for 0_5`() {
        assertThat(PlaySpeed.fromSpeed(0.5f)).isEqualTo(PlaySpeed.X0_5)
    }

    @Test
    fun `PlaySpeed fromSpeed returns X1_25 for 1_25`() {
        assertThat(PlaySpeed.fromSpeed(1.25f)).isEqualTo(PlaySpeed.X1_25)
    }

    @Test
    fun `PlaySpeed fromSpeed returns X1_5 for 1_5`() {
        assertThat(PlaySpeed.fromSpeed(1.5f)).isEqualTo(PlaySpeed.X1_5)
    }

    // ===== LeftNaviItem =====

    @Test
    fun `LeftNaviItem fromOrdinal returns Search for 0`() {
        assertThat(LeftNaviItem.fromOrdinal(0)).isEqualTo(LeftNaviItem.Search)
    }

    @Test
    fun `LeftNaviItem fromOrdinal returns Personal for 1`() {
        assertThat(LeftNaviItem.fromOrdinal(1)).isEqualTo(LeftNaviItem.Personal)
    }

    @Test
    fun `LeftNaviItem fromOrdinal returns Home for 2`() {
        assertThat(LeftNaviItem.fromOrdinal(2)).isEqualTo(LeftNaviItem.Home)
    }

    @Test
    fun `LeftNaviItem fromOrdinal returns UGC for 3`() {
        assertThat(LeftNaviItem.fromOrdinal(3)).isEqualTo(LeftNaviItem.UGC)
    }

    @Test
    fun `LeftNaviItem fromOrdinal returns PGC for 4`() {
        assertThat(LeftNaviItem.fromOrdinal(4)).isEqualTo(LeftNaviItem.PGC)
    }

    @Test
    fun `LeftNaviItem fromOrdinal returns Live for 5`() {
        assertThat(LeftNaviItem.fromOrdinal(5)).isEqualTo(LeftNaviItem.Live)
    }

    // ===== HomeTopNavItem =====

    @Test
    fun `HomeTopNavItem fromCode returns Dynamics for 0`() {
        assertThat(HomeTopNavItem.fromCode(0)).isEqualTo(HomeTopNavItem.Dynamics)
    }

    @Test
    fun `HomeTopNavItem fromCode returns Recommend for 1`() {
        assertThat(HomeTopNavItem.fromCode(1)).isEqualTo(HomeTopNavItem.Recommend)
    }

    @Test
    fun `HomeTopNavItem fromCode returns Popular for 2`() {
        assertThat(HomeTopNavItem.fromCode(2)).isEqualTo(HomeTopNavItem.Popular)
    }

    // ===== PersonalTopNavItem =====

    @Test
    fun `PersonalTopNavItem fromOrdinal returns ToView for 0`() {
        assertThat(PersonalTopNavItem.fromOrdinal(0)).isEqualTo(PersonalTopNavItem.ToView)
    }

    @Test
    fun `PersonalTopNavItem fromOrdinal returns History for 1`() {
        assertThat(PersonalTopNavItem.fromOrdinal(1)).isEqualTo(PersonalTopNavItem.History)
    }

    @Test
    fun `PersonalTopNavItem fromOrdinal returns Favorite for 2`() {
        assertThat(PersonalTopNavItem.fromOrdinal(2)).isEqualTo(PersonalTopNavItem.Favorite)
    }

    @Test
    fun `PersonalTopNavItem fromOrdinal returns FollowingSeason for 3`() {
        assertThat(PersonalTopNavItem.fromOrdinal(3)).isEqualTo(PersonalTopNavItem.FollowingSeason)
    }

    // ===== ThemeMode =====

    @Test
    fun `ThemeMode fromOrdinal returns FollowSystem for 0`() {
        assertThat(ThemeMode.fromOrdinal(0)).isEqualTo(ThemeMode.FollowSystem)
    }

    @Test
    fun `ThemeMode fromOrdinal returns Dark for 1`() {
        assertThat(ThemeMode.fromOrdinal(1)).isEqualTo(ThemeMode.Dark)
    }

    @Test
    fun `ThemeMode fromOrdinal returns Light for 2`() {
        assertThat(ThemeMode.fromOrdinal(2)).isEqualTo(ThemeMode.Light)
    }
}
