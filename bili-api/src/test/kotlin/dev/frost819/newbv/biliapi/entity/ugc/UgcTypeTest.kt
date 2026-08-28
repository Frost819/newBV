package dev.frost819.newbv.biliapi.entity.ugc

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UgcType] 枚举的属性与数量验证。
 */
@Suppress("DEPRECATION")
class UgcTypeTest {
    @Test
    fun `UgcType Douga has correct rid codename and locId`() {
        assertThat(UgcType.Douga.rid).isEqualTo(1)
        assertThat(UgcType.Douga.codename).isEqualTo("douga")
        assertThat(UgcType.Douga.locId).isEqualTo(4973)
    }

    @Test
    fun `UgcType Game has correct rid and locId`() {
        assertThat(UgcType.Game.rid).isEqualTo(4)
        assertThat(UgcType.Game.codename).isEqualTo("game")
        assertThat(UgcType.Game.locId).isEqualTo(4991)
    }

    @Test
    fun `UgcType Music has correct rid and locId`() {
        assertThat(UgcType.Music.rid).isEqualTo(3)
        assertThat(UgcType.Music.locId).isEqualTo(4979)
    }

    @Test
    fun `UgcType with default locId is negative one`() {
        assertThat(UgcType.DougaMad.locId).isEqualTo(-1)
        assertThat(UgcType.GameStandAlone.locId).isEqualTo(-1)
    }

    @Test
    fun `UgcType entries is non-empty`() {
        assertThat(UgcType.entries).isNotEmpty()
        assertThat(UgcType.entries.size).isGreaterThan(50)
    }

    @Test
    fun `UgcType top-level categories have locId set`() {
        val topLevelCategories =
            listOf(
                UgcType.Douga,
                UgcType.Game,
                UgcType.Kichiku,
                UgcType.Music,
                UgcType.Dance,
                UgcType.Cinephile,
                UgcType.Ent,
                UgcType.Knowledge,
                UgcType.Tech,
                UgcType.Information,
                UgcType.Food,
                UgcType.Life,
                UgcType.Car,
                UgcType.Fashion,
                UgcType.Sports,
                UgcType.Animal,
            )
        topLevelCategories.forEach { assertThat(it.locId).isGreaterThan(0) }
    }

    @Test
    fun `UgcType sub-categories have default locId`() {
        val topLevel =
            setOf(
                UgcType.Douga,
                UgcType.Game,
                UgcType.Kichiku,
                UgcType.Music,
                UgcType.Dance,
                UgcType.Cinephile,
                UgcType.Ent,
                UgcType.Knowledge,
                UgcType.Tech,
                UgcType.Information,
                UgcType.Food,
                UgcType.Life,
                UgcType.Car,
                UgcType.Fashion,
                UgcType.Sports,
                UgcType.Animal,
            )
        UgcType.entries
            .filter { it !in topLevel }
            .forEach { assertThat(it.locId).isEqualTo(-1) }
    }
}
