package dev.frost819.newbv.biliapi.entity.pgc.index

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.biliapi.entity.pgc.PgcType
import org.junit.jupiter.api.Test

/**
 * [IndexOrder]、[SeasonVersion]、[SpokenLanguage]、[Area]、[IsFinish]、
 * [Copyright]、[SeasonStatus]、[SeasonMonth]、[Producer]、[Year]、[ReleaseDate]、[Style]
 * 枚举及其 `getList()` 方法的单元测试。
 */
class IndexParamsTest {
    @Test
    fun `IndexOrder getList for Anime returns expected set`() {
        val list = IndexOrder.getList(PgcType.Anime)
        assertThat(list)
            .containsExactly(
                IndexOrder.FollowCount,
                IndexOrder.UpdateTime,
                IndexOrder.Score,
                IndexOrder.PlayCount,
                IndexOrder.StartTime,
            ).inOrder()
    }

    @Test
    fun `IndexOrder getList for Movie does not include FollowCount`() {
        val list = IndexOrder.getList(PgcType.Movie)
        assertThat(
            list,
        ).containsAtLeast(IndexOrder.PlayCount, IndexOrder.UpdateTime, IndexOrder.PublishTime, IndexOrder.Score)
        assertThat(list).doesNotContain(IndexOrder.FollowCount)
    }

    @Test
    fun `IndexOrderType has Desc and Asc`() {
        assertThat(IndexOrderType.Desc.id).isEqualTo(0)
        assertThat(IndexOrderType.Asc.id).isEqualTo(1)
    }

    @Test
    fun `SeasonVersion getList for Anime returns four entries`() {
        val list = SeasonVersion.getList(PgcType.Anime)
        assertThat(list)
            .containsExactly(
                SeasonVersion.All,
                SeasonVersion.FeatureFilm,
                SeasonVersion.Movies,
                SeasonVersion.Other,
            ).inOrder()
    }

    @Test
    fun `SeasonVersion getList for Movie returns empty`() {
        assertThat(SeasonVersion.getList(PgcType.Movie)).isEmpty()
    }

    @Test
    fun `SpokenLanguage getList for Anime returns three entries`() {
        val list = SpokenLanguage.getList(PgcType.Anime)
        assertThat(list)
            .containsExactly(
                SpokenLanguage.All,
                SpokenLanguage.OriginalSoundtrack,
                SpokenLanguage.ChineseDubbing,
            ).inOrder()
    }

    @Test
    fun `SpokenLanguage getList for Movie returns empty`() {
        assertThat(SpokenLanguage.getList(PgcType.Movie)).isEmpty()
    }

    @Test
    fun `Area getList for Anime returns All Japan America Other`() {
        val list = Area.getList(PgcType.Anime)
        assertThat(list).containsExactly(Area.All, Area.Japan, Area.America, Area.Other).inOrder()
    }

    @Test
    fun `Area getList for Movie includes many countries`() {
        val list = Area.getList(PgcType.Movie)
        assertThat(list).containsAtLeast(
            Area.All,
            Area.MainlandChina,
            Area.ChinaHongKongTaiwan,
            Area.America,
            Area.Japan,
            Area.Korea,
            Area.France,
        )
        assertThat(list.size).isGreaterThan(10)
    }

    @Test
    fun `Area getList for Documentary returns empty`() {
        assertThat(Area.getList(PgcType.Documentary)).isEmpty()
    }

    @Test
    fun `IsFinish getList for Anime returns All Finished Serialization`() {
        val list = IsFinish.getList(PgcType.Anime)
        assertThat(list).containsExactly(IsFinish.All, IsFinish.Finished, IsFinish.Serialization).inOrder()
    }

    @Test
    fun `IsFinish getList for Movie returns empty`() {
        assertThat(IsFinish.getList(PgcType.Movie)).isEmpty()
    }

    @Test
    fun `Copyright getList for Anime returns three entries`() {
        val list = Copyright.getList(PgcType.Anime)
        assertThat(list).containsExactly(Copyright.All, Copyright.Exclusive, Copyright.Other).inOrder()
    }

    @Test
    fun `SeasonStatus getList for Anime returns All Free Paid Prime`() {
        val list = SeasonStatus.getList(PgcType.Anime)
        assertThat(
            list,
        ).containsExactly(SeasonStatus.All, SeasonStatus.Free, SeasonStatus.Paid, SeasonStatus.Prime).inOrder()
    }

    @Test
    fun `SeasonStatus getList for Documentary returns All and Free and Prime`() {
        val list = SeasonStatus.getList(PgcType.Documentary)
        assertThat(list).containsExactly(SeasonStatus.All, SeasonStatus.Free, SeasonStatus.Prime).inOrder()
    }

    @Test
    fun `SeasonMonth getList for Anime returns five entries`() {
        val list = SeasonMonth.getList(PgcType.Anime)
        assertThat(list)
            .containsExactly(
                SeasonMonth.All,
                SeasonMonth.January,
                SeasonMonth.April,
                SeasonMonth.July,
                SeasonMonth.October,
            ).inOrder()
    }

    @Test
    fun `SeasonMonth getList for Movie returns empty`() {
        assertThat(SeasonMonth.getList(PgcType.Movie)).isEmpty()
    }

    @Test
    fun `Producer getList for Documentary returns non-empty list with BBC and CCTV`() {
        val list = Producer.getList(PgcType.Documentary)
        assertThat(list).isNotEmpty()
        assertThat(list).contains(Producer.BBC)
        assertThat(list).contains(Producer.CCTV)
        assertThat(list).contains(Producer.NationalGeographic)
    }

    @Test
    fun `Producer getList for Anime returns empty`() {
        assertThat(Producer.getList(PgcType.Anime)).isEmpty()
    }

    @Test
    fun `Year getList for Anime returns non-empty list starting with All`() {
        val list = Year.getList(PgcType.Anime)
        assertThat(list).isNotEmpty()
        assertThat(list.first()).isEqualTo(Year.All)
        assertThat(list).contains(Year.Year2024)
    }

    @Test
    fun `Year getList for Movie returns empty`() {
        assertThat(Year.getList(PgcType.Movie)).isEmpty()
    }

    @Test
    fun `ReleaseDate getList for Movie returns non-empty list`() {
        val list = ReleaseDate.getList(PgcType.Movie)
        assertThat(list).isNotEmpty()
        assertThat(list.first()).isEqualTo(ReleaseDate.All)
    }

    @Test
    fun `ReleaseDate getList for Anime returns empty`() {
        assertThat(ReleaseDate.getList(PgcType.Anime)).isEmpty()
    }

    @Test
    fun `Style getList for Anime returns non-empty list containing Original and HotBlood`() {
        val list = Style.getList(PgcType.Anime)
        assertThat(list).isNotEmpty()
        assertThat(list).contains(Style.Original)
        assertThat(list).contains(Style.HotBlood)
    }

    @Test
    fun `Style getList for Tv returns non-empty list containing Plot and Emotion`() {
        val list = Style.getList(PgcType.Tv)
        assertThat(list).isNotEmpty()
        assertThat(list).contains(Style.Plot)
        assertThat(list).contains(Style.Emotion)
    }

    @Test
    fun `Style getList for Variety contains TalkShow and RealityShow`() {
        val list = Style.getList(PgcType.Variety)
        assertThat(list).contains(Style.TalkShow)
        assertThat(list).contains(Style.RealityShow)
    }

    @Test
    fun `Style getList for GuoChuang contains XuanHuan and AncientStyle`() {
        val list = Style.getList(PgcType.GuoChuang)
        assertThat(list).isNotEmpty()
        assertThat(list).contains(Style.XuanHuan)
        assertThat(list).contains(Style.AncientStyle)
        assertThat(list).contains(Style.MartialArts)
    }

    @Test
    fun `Style getList for Movie contains Plot and Comedy and ShortFilm`() {
        val list = Style.getList(PgcType.Movie)
        assertThat(list).isNotEmpty()
        assertThat(list).contains(Style.Plot)
        assertThat(list).contains(Style.Comedy)
        assertThat(list).contains(Style.ShortFilm)
        assertThat(list).contains(Style.War)
    }

    @Test
    fun `Style getList for Documentary contains History and Food and Nature`() {
        val list = Style.getList(PgcType.Documentary)
        assertThat(list).isNotEmpty()
        assertThat(list).contains(Style.History)
        assertThat(list).contains(Style.Food)
        assertThat(list).contains(Style.Nature)
        assertThat(list).contains(Style.Animal)
    }

    @Test
    fun `Style getList for all PgcTypes returns All as first element`() {
        for (pgcType in PgcType.entries) {
            val list = Style.getList(pgcType)
            assertThat(list).isNotEmpty()
            assertThat(list.first()).isEqualTo(Style.All)
        }
    }

    @Test
    fun `IndexOrder getList for GuoChuang returns same as Anime`() {
        val animeList = IndexOrder.getList(PgcType.Anime)
        val guoChuangList = IndexOrder.getList(PgcType.GuoChuang)
        assertThat(guoChuangList).containsExactlyElementsIn(animeList).inOrder()
    }

    @Test
    fun `IndexOrder getList for Documentary includes DanmakuCount`() {
        val list = IndexOrder.getList(PgcType.Documentary)
        assertThat(list).contains(IndexOrder.DanmakuCount)
        assertThat(list).contains(IndexOrder.PublishTime)
    }

    @Test
    fun `IndexOrder getList for Tv includes DanmakuCount and FollowCount`() {
        val list = IndexOrder.getList(PgcType.Tv)
        assertThat(list).contains(IndexOrder.DanmakuCount)
        assertThat(list).contains(IndexOrder.FollowCount)
    }

    @Test
    fun `IndexOrder getList for Variety includes PublishTime and DanmakuCount`() {
        val list = IndexOrder.getList(PgcType.Variety)
        assertThat(list).contains(IndexOrder.PublishTime)
        assertThat(list).contains(IndexOrder.DanmakuCount)
    }

    @Test
    fun `IndexOrder id values are unique`() {
        val ids = IndexOrder.entries.map { it.id }
        assertThat(ids.toSet().size).isEqualTo(ids.size)
    }

    @Test
    fun `Style id values cover expected ranges`() {
        assertThat(Style.All.id).isEqualTo(-1)
        assertThat(Style.Movie.id).isEqualTo(-10)
        assertThat(Style.Original.id).isEqualTo(10010)
        assertThat(Style.Culture.id).isEqualTo(10100)
        assertThat(Style.ShortFilm.id).isEqualTo(10104)
    }
}
