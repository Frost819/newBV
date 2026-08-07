package dev.frost819.newbv.biliapi.entity.ugc

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * [UgcTypeV2] 枚举的属性与 companion object 列表验证。
 */
class UgcTypeV2Test {
    @Test
    fun `UgcTypeV2 Douga has correct tid codename and channelId`() {
        assertThat(UgcTypeV2.Douga.tid).isEqualTo(1005)
        assertThat(UgcTypeV2.Douga.codename).isEqualTo("douga")
        assertThat(UgcTypeV2.Douga.channelId).isEqualTo(7)
    }

    @Test
    fun `UgcTypeV2 Game has correct tid codename and channelId`() {
        assertThat(UgcTypeV2.Game.tid).isEqualTo(1008)
        assertThat(UgcTypeV2.Game.codename).isEqualTo("game")
        assertThat(UgcTypeV2.Game.channelId).isEqualTo(8)
    }

    @Test
    fun `UgcTypeV2 sub-category has null channelId`() {
        assertThat(UgcTypeV2.DougaFanAnime.channelId).isNull()
        assertThat(UgcTypeV2.GameRpg.channelId).isNull()
    }

    @Test
    fun `UgcTypeV2 entries is non-empty`() {
        assertThat(UgcTypeV2.entries).isNotEmpty()
        assertThat(UgcTypeV2.entries.size).isGreaterThan(100)
    }

    @Test
    fun `dougaList has 18 entries and all are Douga sub-categories`() {
        assertThat(UgcTypeV2.dougaList).hasSize(18)
        assertThat(UgcTypeV2.dougaList).contains(UgcTypeV2.DougaFanAnime)
        assertThat(UgcTypeV2.dougaList).contains(UgcTypeV2.DougaOther)
    }

    @Test
    fun `gameList has 16 entries`() {
        assertThat(UgcTypeV2.gameList).hasSize(16)
    }

    @Test
    fun `kichikuList has 5 entries`() {
        assertThat(UgcTypeV2.kichikuList).hasSize(5)
    }

    @Test
    fun `musicList has 12 entries`() {
        assertThat(UgcTypeV2.musicList).hasSize(12)
    }

    @Test
    fun `danceList has 9 entries`() {
        assertThat(UgcTypeV2.danceList).hasSize(9)
    }

    @Test
    fun `cinephileList has 8 entries`() {
        assertThat(UgcTypeV2.cinephileList).hasSize(8)
    }

    @Test
    fun `entList has 7 entries`() {
        assertThat(UgcTypeV2.entList).hasSize(7)
    }

    @Test
    fun `knowledgeList has 12 entries`() {
        assertThat(UgcTypeV2.knowledgeList).hasSize(12)
    }

    @Test
    fun `techList has 7 entries`() {
        assertThat(UgcTypeV2.techList).hasSize(7)
    }

    @Test
    fun `informationList has 4 entries`() {
        assertThat(UgcTypeV2.informationList).hasSize(4)
    }

    @Test
    fun `foodList has 5 entries`() {
        assertThat(UgcTypeV2.foodList).hasSize(5)
    }

    @Test
    fun `shortplayList has 4 entries`() {
        assertThat(UgcTypeV2.shortplayList).hasSize(4)
    }

    @Test
    fun `carList has 5 entries`() {
        assertThat(UgcTypeV2.carList).hasSize(5)
    }

    @Test
    fun `fashionList has 9 entries`() {
        assertThat(UgcTypeV2.fashionList).hasSize(9)
    }

    @Test
    fun `sportsList has 10 entries`() {
        assertThat(UgcTypeV2.sportsList).hasSize(10)
    }

    @Test
    fun `animalList has 5 entries`() {
        assertThat(UgcTypeV2.animalList).hasSize(5)
    }

    @Test
    fun `vlogList has 4 entries`() {
        assertThat(UgcTypeV2.vlogList).hasSize(4)
    }

    @Test
    fun `paintingList has 4 entries`() {
        assertThat(UgcTypeV2.paintingList).hasSize(4)
    }

    @Test
    fun `aiList has 3 entries`() {
        assertThat(UgcTypeV2.aiList).hasSize(3)
    }

    @Test
    fun `mysticismList has 5 entries`() {
        assertThat(UgcTypeV2.mysticismList).hasSize(5)
    }

    @Test
    fun `all top-level categories have non-null channelId`() {
        val topLevelCategories =
            listOf(
                UgcTypeV2.Douga, UgcTypeV2.Game, UgcTypeV2.Kichiku, UgcTypeV2.Music,
                UgcTypeV2.Dance, UgcTypeV2.Cinephile, UgcTypeV2.Ent, UgcTypeV2.Knowledge,
                UgcTypeV2.Tech, UgcTypeV2.Information, UgcTypeV2.Food, UgcTypeV2.Shortplay,
                UgcTypeV2.Car, UgcTypeV2.Fashion, UgcTypeV2.Sports, UgcTypeV2.Animal,
                UgcTypeV2.Vlog, UgcTypeV2.Painting, UgcTypeV2.Ai, UgcTypeV2.Home,
                UgcTypeV2.Outdoors, UgcTypeV2.Gym, UgcTypeV2.Handmake, UgcTypeV2.Travel,
                UgcTypeV2.Rural, UgcTypeV2.Parenting, UgcTypeV2.Health, UgcTypeV2.Emotion,
                UgcTypeV2.LifeJoy, UgcTypeV2.LifeExperience, UgcTypeV2.Mysticism,
            )
        topLevelCategories.forEach { assertThat(it.channelId).isNotNull() }
    }
}
